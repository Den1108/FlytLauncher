package com.flyt.launcher;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Устанавливает Fabric или Forge поверх уже скачанной версии Minecraft.
 *
 * Fabric: использует meta.fabricmc.net API → скачивает profile JSON и libraries
 * Forge:  скачивает installer JAR и запускает его headless (--installClient)
 */
public class ModLoader {

    public enum Type { VANILLA, FABRIC, FORGE }

    private static final String FABRIC_META  = "https://meta.fabricmc.net/v2";
    private static final String FORGE_MAVEN  = "https://maven.minecraftforge.net/net/minecraftforge/forge";

    private final String baseDir;
    private final String versionsDir;
    private final String librariesDir;
    private BiConsumer<Integer, String> progressCb;

    public ModLoader(String baseDir) {
        this.baseDir     = baseDir;
        this.versionsDir  = baseDir + File.separator + "versions";
        this.librariesDir = baseDir + File.separator + "libraries";
    }

    public void setProgressCallback(BiConsumer<Integer, String> cb) { this.progressCb = cb; }

    // ═══════════════════════════════════════════════════════
    // FABRIC
    // ═══════════════════════════════════════════════════════

    /**
     * Возвращает список доступных версий Fabric loader для данной версии MC.
     * Пустой список = Fabric не поддерживает эту версию.
     */
    public List<String> getFabricLoaderVersions(String mcVersion) throws Exception {
        String url = FABRIC_META + "/versions/loader/" + mcVersion;
        try {
            JsonArray arr = fetchArray(url);
            List<String> result = new ArrayList<>();
            for (JsonElement el : arr) {
                String v = el.getAsJsonObject()
                        .getAsJsonObject("loader")
                        .get("version").getAsString();
                result.add(v);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Устанавливает Fabric.
     * Создаёт version profile в versions/<mcVer>-fabric-<loaderVer>/
     * и скачивает все libraries.
     *
     * @return идентификатор версии для запуска (например "1.20.4-fabric-0.15.7")
     */
    public String installFabric(String mcVersion, String loaderVersion) throws Exception {
        progress(0, "Установка Fabric " + loaderVersion + " для " + mcVersion + "…");

        String profileUrl = FABRIC_META + "/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json";
        JsonObject profile = fetchObject(profileUrl);

        String versionId = mcVersion + "-fabric-" + loaderVersion;
        File versionDir = new File(versionsDir + File.separator + versionId);
        versionDir.mkdirs();

        // Сохраняем profile JSON
        progress(10, "Сохранение Fabric profile…");
        try (Writer w = new FileWriter(new File(versionDir, versionId + ".json"))) {
            new GsonBuilder().setPrettyPrinting().create().toJson(profile, w);
        }

        // Скачиваем libraries из profile
        if (profile.has("libraries")) {
            JsonArray libs = profile.getAsJsonArray("libraries");
            int total = libs.size(), done = 0;
            for (JsonElement el : libs) {
                JsonObject lib = el.getAsJsonObject();
                downloadFabricLibrary(lib);
                done++;
                progress(10 + (int)(done * 85.0 / total), "Fabric libraries: " + done + "/" + total);
            }
        }

        progress(100, "Fabric установлен: " + versionId);
        return versionId;
    }

    private void downloadFabricLibrary(JsonObject lib) throws Exception {
        if (!lib.has("name")) return;

        // Разбираем maven координаты: group:artifact:version
        String name = lib.get("name").getAsString();
        String[] parts = name.split(":");
        if (parts.length < 3) return;

        String group   = parts[0].replace(".", "/");
        String artifact= parts[1];
        String version = parts[2];

        String path = group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
        File dest = new File(librariesDir + File.separator + path.replace("/", File.separator));

        if (dest.exists()) return;
        dest.getParentFile().mkdirs();

        // Определяем URL: из "url" поля или стандартный Maven
        String baseUrl = lib.has("url") ? lib.get("url").getAsString()
                       : "https://maven.fabricmc.net/";
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url = baseUrl + path;
        downloadFile(url, dest);
    }

    // ═══════════════════════════════════════════════════════
    // FORGE
    // ═══════════════════════════════════════════════════════

    /**
     * Возвращает список доступных версий Forge для данной версии MC.
     */
    public List<String> getForgeVersions(String mcVersion) throws Exception {
        // Forge Maven metadata
        String url = FORGE_MAVEN + "/maven-metadata.xml";
        try {
            HttpURLConnection conn = openConn(url);
            String xml = new String(conn.getInputStream().readAllBytes());
            conn.disconnect();

            List<String> result = new ArrayList<>();
            // Парсим XML вручную (без зависимостей)
            int idx = 0;
            while (true) {
                int start = xml.indexOf("<version>", idx);
                if (start < 0) break;
                int end = xml.indexOf("</version>", start);
                String v = xml.substring(start + 9, end).trim();
                // Фильтруем по mcVersion
                if (v.startsWith(mcVersion + "-")) {
                    String forgeVer = v.substring(mcVersion.length() + 1);
                    result.add(0, forgeVer); // новые сверху
                }
                idx = end + 10;
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Устанавливает Forge.
     * Скачивает installer JAR и запускает его с флагом --installClient.
     *
     * @return идентификатор версии для запуска (например "1.20.4-forge-49.0.19")
     */
    public String installForge(String mcVersion, String forgeVersion, String javaPath) throws Exception {
        progress(0, "Установка Forge " + forgeVersion + " для " + mcVersion + "…");

        String fullVer     = mcVersion + "-" + forgeVersion;
        String installerName = "forge-" + fullVer + "-installer.jar";
        String installerUrl  = FORGE_MAVEN + "/" + fullVer + "/" + installerName;

        File installerFile = new File(baseDir + File.separator + "temp" + File.separator + installerName);
        installerFile.getParentFile().mkdirs();

        progress(5, "Загрузка Forge installer (" + installerName + ")…");
        downloadFile(installerUrl, installerFile);

        progress(50, "Запуск Forge installer…");

        // ИСПРАВЛЕНИЕ: Создаем файл-пустышку launcher_profiles.json, 
        // чтобы установщик Forge не завершался с кодом 1 из-за его отсутствия.
        File profilesFile = new File(baseDir, "launcher_profiles.json");
        if (!profilesFile.exists()) {
            profilesFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(profilesFile)) {
                writer.write("{\"profiles\": {}}");
            }
        }

        // Запускаем installer headless
        ProcessBuilder pb = new ProcessBuilder(
            javaPath,
            "-jar", installerFile.getAbsolutePath(),
            "--installClient", baseDir
        );
        pb.directory(new File(baseDir));
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // Читаем вывод установщика
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String finalLine = line;
                    System.out.println("[Forge Installer] " + finalLine);
                }
            } catch (Exception ignored) {}
        });
        reader.start();

        int exit = proc.waitFor();
        installerFile.delete(); // убираем installer

        if (exit != 0) {
            throw new IOException("Forge installer завершился с кодом " + exit);
        }

        String versionId = mcVersion + "-forge-" + forgeVersion;
        progress(100, "Forge установлен: " + versionId);
        return versionId;
    }

    // ═══════════════════════════════════════════════════════
    // ОПРЕДЕЛЕНИЕ MAINCLASS + ВЕРСИИ ДЛЯ ЗАПУСКА
    // ═══════════════════════════════════════════════════════

    /**
     * Возвращает ID версии которую нужно передать в GameLauncher.launch()
     * Если модлоадер не установлен — возвращает mcVersion.
     */
    public String resolveVersionId(String mcVersion, Type loaderType, String loaderVersion) {
        return switch (loaderType) {
            case FABRIC  -> mcVersion + "-fabric-" + loaderVersion;
            case FORGE   -> mcVersion + "-forge-" + loaderVersion;
            case VANILLA -> mcVersion;
        };
    }

    /**
     * Проверяет установлен ли данный модлоадер (есть ли version JSON на диске)
     */
    public boolean isInstalled(String versionId) {
        File json = new File(versionsDir + File.separator + versionId
                + File.separator + versionId + ".json");
        return json.exists();
    }

    // ═══════════════════════════════════════════════════════
    // HTTP УТИЛИТЫ
    // ═══════════════════════════════════════════════════════

    private JsonObject fetchObject(String url) throws Exception {
        HttpURLConnection conn = openConn(url);
        try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private JsonArray fetchArray(String url) throws Exception {
        HttpURLConnection conn = openConn(url);
        try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
            return JsonParser.parseReader(r).getAsJsonArray();
        }
    }

    private HttpURLConnection openConn(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(20_000);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private void downloadFile(String url, File dest) throws Exception {
        if (dest.exists()) return;
        dest.getParentFile().mkdirs();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");
        if (conn.getResponseCode() != 200)
            throw new IOException("HTTP " + conn.getResponseCode() + " — " + url);
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] buf = new byte[16384]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    private void progress(int pct, String msg) {
        System.out.println("[ModLoader] " + pct + "% — " + msg);
        if (progressCb != null) progressCb.accept(pct, msg);
    }
}