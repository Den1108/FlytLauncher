package com.flyt.launcher;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Запускает Minecraft через ProcessBuilder.
 * Читает version.json, собирает classpath и аргументы JVM/игры.
 */
public class GameLauncher {

    private final String gameDir;
    private final String versionsDir;
    private final String librariesDir;
    private final String assetsDir;
    private final String nativesDir;

    public GameLauncher(String baseDir) {
        this.gameDir      = baseDir;
        this.versionsDir  = baseDir + File.separator + "versions";
        this.librariesDir = baseDir + File.separator + "libraries";
        this.assetsDir    = baseDir + File.separator + "assets";
        this.nativesDir   = baseDir + File.separator + "natives";
    }

    /**
     * Запускает указанную версию Minecraft.
     *
     * @param version   например "1.20.4"
     * @param nickname  никнейм игрока (офлайн-режим)
     * @param ramMB     сколько МБ выделить (-Xmx)
     * @param extraJvm  дополнительные JVM флаги, например "-XX:+UseG1GC"
     * @return Process запущенного Minecraft
     */
    public Process launch(String version, String nickname, int ramMB, String extraJvm,
                          java.util.function.BiConsumer<Integer, String> progressCallback) throws Exception {
        // Путь к version.json
        File versionJson = new File(versionsDir + File.separator + version
                + File.separator + version + ".json");

        if (!versionJson.exists()) {
            throw new FileNotFoundException("version.json не найден: " + versionJson.getAbsolutePath()
                    + "\nСначала скачайте версию через Downloader.");
        }

        // Читаем version.json
        JsonObject meta = JsonParser.parseReader(new FileReader(versionJson)).getAsJsonObject();

        String mainClass  = meta.get("mainClass").getAsString();
        String assetIndex = meta.getAsJsonObject("assetIndex").get("id").getAsString();

        // Собираем classpath
        List<String> classpath = buildClasspath(meta, version);

        // Папка для нативных библиотек конкретной версии
        String nativesPath = nativesDir + File.separator + version;
        new File(nativesPath).mkdirs();

        // UUID для офлайн-режима (стабильный от никнейма)
        String offlineUUID = generateOfflineUUID(nickname);

        // Собираем команду
        List<String> cmd = new ArrayList<>();

        // java (ищем или скачиваем автоматически)
        cmd.add(getJavaExecutable(progressCallback));

        // Память
        cmd.add("-Xmx" + ramMB + "M");
        cmd.add("-Xms" + Math.min(512, ramMB) + "M");

        // Нативные библиотеки
        cmd.add("-Djava.library.path=" + nativesPath);
        cmd.add("-Dminecraft.launcher.brand=FlytLauncher");
        cmd.add("-Dminecraft.launcher.version=1.0.0");

        // Дополнительные JVM аргументы
        if (extraJvm != null && !extraJvm.isBlank()) {
            cmd.addAll(Arrays.asList(extraJvm.split("\\s+")));
        }

        // JVM аргументы из version.json
        boolean cpAddedByJvmArgs = false;
        if (meta.has("arguments") && meta.getAsJsonObject("arguments").has("jvm")) {
            parseArguments(meta.getAsJsonObject("arguments").getAsJsonArray("jvm"), cmd,
                    nickname, offlineUUID, version, nativesPath, assetIndex, classpath);
            cpAddedByJvmArgs = cmd.contains("-cp") || cmd.contains("-classpath");
        }

        // classpath — добавляем только если не было в jvm аргументах
        if (!cpAddedByJvmArgs) {
            cmd.add("-cp");
            cmd.add(String.join(File.pathSeparator, classpath));
        }

        // Главный класс
        cmd.add(mainClass);

        // Аргументы игры
        if (meta.has("arguments") && meta.getAsJsonObject("arguments").has("game")) {
            parseArguments(meta.getAsJsonObject("arguments").getAsJsonArray("game"), cmd,
                    nickname, offlineUUID, version, nativesPath, assetIndex, classpath);
            // Явно добавляем --assetsDir и --assetIndex на случай если их не передал version.json
            if (!cmd.contains("--assetsDir")) {
                cmd.add("--assetsDir"); cmd.add(assetsDir);
            }
            if (!cmd.contains("--assetIndex")) {
                cmd.add("--assetIndex"); cmd.add(assetIndex);
            }
        } else if (meta.has("minecraftArguments")) {
            // Старый формат (1.12 и ниже)
            String oldArgs = meta.get("minecraftArguments").getAsString();
            oldArgs = replaceVars(oldArgs, nickname, offlineUUID, version, assetIndex);
            cmd.addAll(Arrays.asList(oldArgs.split("\\s+")));
        }

        System.out.println("[GameLauncher] Команда запуска:\n" + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(gameDir));
        pb.redirectErrorStream(true); // stderr → stdout

        // Перенаправляем вывод в файл лога
        File logFile = new File(gameDir + File.separator + "launcher.log");
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

        return pb.start();
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /** Собирает список .jar файлов для -cp */
    private List<String> buildClasspath(JsonObject meta, String version) {
        List<String> cp = new ArrayList<>();

        // Библиотеки
        if (meta.has("libraries")) {
            for (JsonElement el : meta.getAsJsonArray("libraries")) {
                JsonObject lib = el.getAsJsonObject();

                // Проверяем правила (os, features)
                if (!isLibraryAllowed(lib)) continue;

                if (lib.has("downloads")) {
                    JsonObject artifact = lib.getAsJsonObject("downloads")
                            .getAsJsonObject("artifact");
                    if (artifact != null && artifact.has("path")) {
                        String path = librariesDir + File.separator
                                + artifact.get("path").getAsString()
                                        .replace("/", File.separator);
                        if (new File(path).exists()) {
                            cp.add(path);
                        }
                    }
                }
            }
        }

        // Основной .jar игры
        String clientJar = versionsDir + File.separator + version
                + File.separator + version + ".jar";
        if (new File(clientJar).exists()) {
            cp.add(clientJar);
        }

        return cp;
    }

    /** Парсит аргументы из нового формата (1.13+) */
    private void parseArguments(JsonArray args, List<String> cmd,
            String nickname, String uuid, String version,
            String nativesPath, String assetIndex, List<String> classpath) {

        for (JsonElement el : args) {
            if (el.isJsonPrimitive()) {
                // Простая строка — подставляем переменные и добавляем
                String val = replaceVars(el.getAsString(), nickname, uuid, version, assetIndex);
                if (!val.isEmpty() && !val.equals("${classpath}")) {
                    cmd.add(val);
                }
            } else if (el.isJsonObject()) {
                // Условный аргумент с rules: { "rules": [...], "value": "..." или [...] }
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("rules") || isRuleAllowed(obj.getAsJsonArray("rules"))) {
                    JsonElement value = obj.get("value");
                    if (value == null) continue;
                    if (value.isJsonPrimitive()) {
                        String val = replaceVars(value.getAsString(), nickname, uuid, version, assetIndex);
                        if (!val.isEmpty() && !val.equals("${classpath}")) cmd.add(val);
                    } else if (value.isJsonArray()) {
                        for (JsonElement v : value.getAsJsonArray()) {
                            String val = replaceVars(v.getAsString(), nickname, uuid, version, assetIndex);
                            if (!val.isEmpty() && !val.equals("${classpath}")) cmd.add(val);
                        }
                    }
                }
            }
        }
    }

    /** Проверяет разрешают ли rules добавить аргумент */
    private boolean isRuleAllowed(JsonArray rules) {
        boolean allowed = false;
        String os = getOSName();
        for (JsonElement r : rules) {
            JsonObject rule = r.getAsJsonObject();
            String action = rule.get("action").getAsString();
            if (!rule.has("os") && !rule.has("features")) {
                allowed = action.equals("allow");
            } else if (rule.has("os")) {
                String ruleOs = rule.getAsJsonObject("os").get("name").getAsString();
                if (ruleOs.equals(os)) allowed = action.equals("allow");
            }
            // features (демо режим и т.п.) — игнорируем, не добавляем
        }
        return allowed;
    }

    /** Подставляет переменные вида ${...} */
    private String replaceVars(String template, String nickname, String uuid,
                                String version, String assetIndex) {
        // Для virtual-ассетов (1.7.2 и старше) game_assets указывает на virtual/legacy
        // Для современных версий — просто assets/
        String gameAssets = assetsDir + File.separator + "virtual" + File.separator + "legacy";
        if (!new File(gameAssets).exists()) {
            gameAssets = assetsDir;
        }

        return template
                .replace("${auth_player_name}",  nickname)
                .replace("${auth_uuid}",          uuid)
                .replace("${auth_access_token}",  "0")       // офлайн
                .replace("${user_type}",           "legacy")
                .replace("${version_name}",        version)
                .replace("${assets_root}",         assetsDir)
                .replace("${game_assets}",         gameAssets) // для старых версий
                .replace("${assets_index_name}",   assetIndex)
                .replace("${game_directory}",      gameDir)
                .replace("${version_type}",        "release")
                .replace("${natives_directory}",   nativesDir + File.separator + version)
                .replace("${launcher_name}",       "FlytLauncher")
                .replace("${launcher_version}",    "1.0.0")
                .replace("${classpath}",           "");       // classpath добавляем через -cp
    }

    /** Проверяет правила библиотеки (нужна ли она на текущей ОС) */
    private boolean isLibraryAllowed(JsonObject lib) {
        if (!lib.has("rules")) return true;

        String os = getOSName();
        boolean allowed = false;

        for (JsonElement rule : lib.getAsJsonArray("rules")) {
            JsonObject r = rule.getAsJsonObject();
            String action = r.get("action").getAsString();

            if (!r.has("os")) {
                allowed = action.equals("allow");
            } else {
                String ruleOs = r.getAsJsonObject("os").get("name").getAsString();
                if (ruleOs.equals(os)) {
                    allowed = action.equals("allow");
                }
            }
        }
        return allowed;
    }

    private String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))   return "windows";
        if (os.contains("mac"))   return "osx";
        return "linux";
    }

    /**
     * Возвращает путь к java.exe.
     * Порядок поиска:
     *  1. Уже скачанный JRE в <gameDir>/runtime/
     *  2. JRE внутри jpackage-сборки (java.home)
     *  3. JAVA_HOME
     *  4. Системный PATH (where/which)
     *  5. Автоматическая загрузка Adoptium JRE 17
     */
    private String getJavaExecutable(BiConsumer<Integer, String> progressCallback) throws Exception {
        boolean isWin  = System.getProperty("os.name").toLowerCase().contains("win");
        String javaExe = isWin ? "java.exe" : "java";

        // 1. Скачанный нами JRE в папке runtime/
        JavaDownloader jd = new JavaDownloader(gameDir);
        File runtimeJava = findInRuntime(javaExe);
        if (runtimeJava != null) return runtimeJava.getAbsolutePath();

        // 2. java.home (работает в обычном JDK, но не в jpackage-сборке)
        String home = System.getProperty("java.home");
        if (home != null) {
            File f = new File(home, "bin" + File.separator + javaExe);
            if (f.exists()) return f.getAbsolutePath();
        }

        // 3. JAVA_HOME переменная окружения
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File f = new File(javaHome, "bin" + File.separator + javaExe);
            if (f.exists()) return f.getAbsolutePath();
        }

        // 4. Ищем через where/which
        try {
            String finder = isWin ? "where" : "which";
            Process p = new ProcessBuilder(finder, "java").start();
            String result = new String(p.getInputStream().readAllBytes()).trim();
            if (!result.isEmpty()) {
                String firstLine = result.split("\\r?\\n")[0].trim();
                if (new File(firstLine).exists()) return firstLine;
            }
        } catch (Exception ignored) {}

        // 5. Java не найдена — качаем автоматически
        if (progressCallback != null) progressCallback.accept(0, "Java не найдена, начинаем загрузку...");
        jd.setProgressCallback(progressCallback);
        return jd.getOrDownloadJava();
    }

    /** Ищет java в папке runtime внутри gameDir */
    private File findInRuntime(String javaExe) {
        File dir = new File(gameDir + File.separator + "runtime");
        if (!dir.exists()) return null;
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null) return null;
        for (File sub : subdirs) {
            File c = new File(sub, "bin" + File.separator + javaExe);
            if (c.exists()) return c;
            File[] inner = sub.listFiles(File::isDirectory);
            if (inner != null) for (File d : inner) {
                File c2 = new File(d, "bin" + File.separator + javaExe);
                if (c2.exists()) return c2;
            }
        }
        return null;
    }

    /** Генерирует стабильный UUID для офлайн-режима на основе никнейма */
    private String generateOfflineUUID(String nickname) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes());
        return uuid.toString().replace("-", "");
    }
}
