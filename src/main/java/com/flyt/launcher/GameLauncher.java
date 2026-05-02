package com.flyt.launcher;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

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
    public Process launch(String version, String nickname, int ramMB, String extraJvm) throws Exception {
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

        // java
        cmd.add(getJavaExecutable());

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

        // JVM аргументы из version.json (если есть)
        if (meta.has("arguments") && meta.getAsJsonObject("arguments").has("jvm")) {
            parseArguments(meta.getAsJsonObject("arguments").getAsJsonArray("jvm"), cmd,
                    nickname, offlineUUID, version, nativesPath, classpath);
        }

        // classpath
        cmd.add("-cp");
        cmd.add(String.join(File.pathSeparator, classpath));

        // Главный класс
        cmd.add(mainClass);

        // Аргументы игры
        if (meta.has("arguments") && meta.getAsJsonObject("arguments").has("game")) {
            parseArguments(meta.getAsJsonObject("arguments").getAsJsonArray("game"), cmd,
                    nickname, offlineUUID, version, nativesPath, classpath);
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
            String nativesPath, List<String> classpath) {

        for (JsonElement el : args) {
            if (el.isJsonPrimitive()) {
                String val = el.getAsString();
                val = replaceVars(val, nickname, uuid, version,
                        versionsDir.contains("assets") ? "1.20" : "1.20");
                cmd.add(val);
            }
            // JsonObject — условные аргументы (rules), пропускаем сложные случаи
        }
    }

    /** Подставляет переменные вида ${...} */
    private String replaceVars(String template, String nickname, String uuid,
                                String version, String assetIndex) {
        return template
                .replace("${auth_player_name}",  nickname)
                .replace("${auth_uuid}",          uuid)
                .replace("${auth_access_token}",  "0") // офлайн
                .replace("${user_type}",           "legacy")
                .replace("${version_name}",        version)
                .replace("${assets_root}",         assetsDir)
                .replace("${assets_index_name}",   assetIndex)
                .replace("${game_directory}",      gameDir)
                .replace("${version_type}",        "release")
                .replace("${natives_directory}",   nativesDir + File.separator + version)
                .replace("${launcher_name}",       "FlytLauncher")
                .replace("${launcher_version}",    "1.0.0")
                .replace("${classpath}",           ""); // classpath добавляем отдельно
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

    private String getJavaExecutable() {
        String home = System.getProperty("java.home");
        String java = home + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().contains("win")) java += ".exe";
        return java;
    }

    /** Генерирует стабильный UUID для офлайн-режима на основе никнейма */
    private String generateOfflineUUID(String nickname) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes());
        return uuid.toString().replace("-", "");
    }
}
