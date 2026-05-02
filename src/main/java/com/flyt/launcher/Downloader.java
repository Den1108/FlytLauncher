package com.flyt.launcher;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.function.*;

/**
 * Скачивает всё необходимое для запуска Minecraft:
 *  1. version_manifest → список всех версий
 *  2. version.json     → метаданные конкретной версии
 *  3. client.jar       → основной jar игры
 *  4. libraries        → все зависимости
 *  5. assets           → текстуры, звуки и т.д.
 */
public class Downloader {

    private static final String MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    private final String versionsDir;
    private final String librariesDir;
    private final String assetsDir;

    /** Колбэк прогресса: (процент 0-100, сообщение) */
    private BiConsumer<Integer, String> progressCallback;

    public Downloader(String baseDir) {
        this.versionsDir  = baseDir + File.separator + "versions";
        this.librariesDir = baseDir + File.separator + "libraries";
        this.assetsDir    = baseDir + File.separator + "assets";
    }

    public void setProgressCallback(BiConsumer<Integer, String> cb) {
        this.progressCallback = cb;
    }

    // ─── Публичный метод — скачать всё для версии ────────────────────────────

    /**
     * Полная установка версии Minecraft.
     * Вызывай в SwingWorker, чтобы не блокировать UI.
     */
    public void downloadVersion(String version) throws Exception {
        progress(0, "Получение манифеста версий...");
        JsonObject manifest = fetchJson(MANIFEST_URL);

        progress(5, "Поиск версии " + version + "...");
        String versionUrl = findVersionUrl(manifest, version);
        if (versionUrl == null) {
            throw new IllegalArgumentException("Версия " + version + " не найдена в манифесте Mojang");
        }

        progress(10, "Загрузка метаданных версии...");
        JsonObject meta = fetchJson(versionUrl);

        // Сохраняем version.json
        File versionDir = new File(versionsDir + File.separator + version);
        versionDir.mkdirs();
        saveJson(meta, new File(versionDir, version + ".json"));

        progress(15, "Загрузка client.jar...");
        downloadClientJar(meta, version, versionDir);

        progress(25, "Загрузка библиотек...");
        downloadLibraries(meta);

        progress(70, "Загрузка ассетов...");
        downloadAssets(meta);

        progress(100, "Установка завершена!");
    }

    // ─── Скачивание client.jar ───────────────────────────────────────────────

    private void downloadClientJar(JsonObject meta, String version, File versionDir) throws Exception {
        JsonObject clientInfo = meta.getAsJsonObject("downloads")
                .getAsJsonObject("client");

        String url  = clientInfo.get("url").getAsString();
        String sha1 = clientInfo.get("sha1").getAsString();
        File   dest = new File(versionDir, version + ".jar");

        downloadFile(url, dest, sha1);
    }

    // ─── Скачивание библиотек ────────────────────────────────────────────────

    private void downloadLibraries(JsonObject meta) throws Exception {
        JsonArray libs = meta.getAsJsonArray("libraries");
        int total = libs.size();
        int done  = 0;

        for (JsonElement el : libs) {
            JsonObject lib = el.getAsJsonObject();

            if (!isLibraryAllowed(lib)) { done++; continue; }

            if (lib.has("downloads")) {
                JsonObject downloads = lib.getAsJsonObject("downloads");

                // Основной artifact
                if (downloads.has("artifact")) {
                    downloadArtifact(downloads.getAsJsonObject("artifact"), librariesDir);
                }

                // Natives (нативные .dll/.so/.dylib)
                if (downloads.has("classifiers")) {
                    String key = getNativesKey(lib);
                    if (key != null && downloads.getAsJsonObject("classifiers").has(key)) {
                        downloadArtifact(
                                downloads.getAsJsonObject("classifiers").getAsJsonObject(key),
                                librariesDir);
                    }
                }
            }

            done++;
            int pct = 25 + (int)((double) done / total * 40);
            progress(pct, "Библиотеки: " + done + "/" + total);
        }
    }

    private void downloadArtifact(JsonObject artifact, String baseDir) throws Exception {
        if (!artifact.has("path") || !artifact.has("url")) return;

        String path = artifact.get("path").getAsString();
        String url  = artifact.get("url").getAsString();
        String sha1 = artifact.has("sha1") ? artifact.get("sha1").getAsString() : null;

        File dest = new File(baseDir + File.separator + path.replace("/", File.separator));
        downloadFile(url, dest, sha1);
    }

    // ─── Скачивание ассетов ──────────────────────────────────────────────────

    private void downloadAssets(JsonObject meta) throws Exception {
        JsonObject assetIndexInfo = meta.getAsJsonObject("assetIndex");
        String indexId  = assetIndexInfo.get("id").getAsString();
        String indexUrl = assetIndexInfo.get("url").getAsString();

        // Скачиваем index
        File indexDir  = new File(assetsDir + File.separator + "indexes");
        File indexFile = new File(indexDir, indexId + ".json");
        indexDir.mkdirs();
        downloadFile(indexUrl, indexFile, null);

        // Читаем и скачиваем объекты
        JsonObject index   = JsonParser.parseReader(new FileReader(indexFile)).getAsJsonObject();
        JsonObject objects = index.getAsJsonObject("objects");

        int total = objects.size();
        int done  = 0;

        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject obj  = entry.getValue().getAsJsonObject();
            String hash     = obj.get("hash").getAsString();
            String prefix   = hash.substring(0, 2);

            String url = "https://resources.download.minecraft.net/" + prefix + "/" + hash;
            File dest  = new File(assetsDir + File.separator + "objects"
                    + File.separator + prefix + File.separator + hash);

            downloadFile(url, dest, hash);

            done++;
            int pct = 70 + (int)((double) done / total * 28);
            if (done % 50 == 0) {
                progress(pct, "Ассеты: " + done + "/" + total);
            }
        }
    }

    // ─── Низкоуровневое скачивание ───────────────────────────────────────────

    /**
     * Скачивает файл по URL, если он ещё не существует или SHA1 не совпадает.
     */
    private void downloadFile(String url, File dest, String expectedSha1) throws Exception {
        // Уже скачан и хэш совпадает?
        if (dest.exists() && expectedSha1 != null) {
            if (sha1(dest).equalsIgnoreCase(expectedSha1)) return;
        }
        if (dest.exists() && expectedSha1 == null) return;

        dest.getParentFile().mkdirs();

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " при скачивании: " + url);
        }

        try (InputStream in  = conn.getInputStream();
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    /** Вычисляет SHA-1 хэш файла */
    private String sha1(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    // ─── JSON утилиты ────────────────────────────────────────────────────────

    private JsonObject fetchJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private void saveJson(JsonObject obj, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        try (Writer w = new FileWriter(dest)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        }
    }

    /** Находит URL для конкретной версии в манифесте */
    private String findVersionUrl(JsonObject manifest, String version) {
        for (JsonElement el : manifest.getAsJsonArray("versions")) {
            JsonObject v = el.getAsJsonObject();
            if (v.get("id").getAsString().equals(version)) {
                return v.get("url").getAsString();
            }
        }
        return null;
    }

    // ─── ОС-специфичные хелперы ──────────────────────────────────────────────

    private boolean isLibraryAllowed(JsonObject lib) {
        if (!lib.has("rules")) return true;
        String os = getOSName();
        boolean allowed = false;
        for (JsonElement rule : lib.getAsJsonArray("rules")) {
            JsonObject r  = rule.getAsJsonObject();
            String action = r.get("action").getAsString();
            if (!r.has("os")) {
                allowed = action.equals("allow");
            } else {
                String ruleOs = r.getAsJsonObject("os").get("name").getAsString();
                if (ruleOs.equals(os)) allowed = action.equals("allow");
            }
        }
        return allowed;
    }

    private String getNativesKey(JsonObject lib) {
        if (!lib.has("natives")) return null;
        JsonObject natives = lib.getAsJsonObject("natives");
        String os = getOSName();
        if (natives.has(os)) {
            String key = natives.get(os).getAsString();
            String arch = System.getProperty("os.arch").contains("64") ? "64" : "32";
            return key.replace("${arch}", arch);
        }
        return null;
    }

    private String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))  return "windows";
        if (os.contains("mac"))  return "osx";
        return "linux";
    }

    private void progress(int pct, String msg) {
        System.out.println("[Downloader] " + pct + "% — " + msg);
        if (progressCallback != null) progressCallback.accept(pct, msg);
    }

    // Чтобы import java.util.Map работал (добавляем явно)
    private static class Map {
        interface Entry<K, V> {
            K getKey(); V getValue();
        }
    }
}
