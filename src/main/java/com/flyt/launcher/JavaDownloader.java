package com.flyt.launcher;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.function.BiConsumer;
import java.util.zip.*;

/**
 * Автоматически скачивает и распаковывает Adoptium (Eclipse Temurin) JRE 17.
 * Сохраняет в <baseDir>/runtime/jre-17/
 * После установки возвращает путь к java.exe / java
 */
public class JavaDownloader {

    // Adoptium API — возвращает ссылку на последний JRE 17 для нужной платформы
    private static final String ADOPTIUM_API =
            "https://api.adoptium.net/v3/assets/latest/17/hotspot" +
            "?image_type=jre&vendor=eclipse";

    private final String runtimeDir;
    private BiConsumer<Integer, String> progressCallback;

    public JavaDownloader(String baseDir) {
        this.runtimeDir = baseDir + File.separator + "runtime";
    }

    public void setProgressCallback(BiConsumer<Integer, String> cb) {
        this.progressCallback = cb;
    }

    /**
     * Возвращает путь к java(.exe).
     * Если JRE ещё не скачан — скачивает и распаковывает автоматически.
     */
    public String getOrDownloadJava() throws Exception {
        // Проверяем уже скачанный JRE
        File existing = findExistingJava();
        if (existing != null) return existing.getAbsolutePath();

        // Скачиваем
        progress(0, "Подготовка к загрузке Java 17...");
        downloadJre();

        // Снова ищем после распаковки
        existing = findExistingJava();
        if (existing != null) return existing.getAbsolutePath();

        throw new IOException("Не удалось найти java после установки в " + runtimeDir);
    }

    // ─── Поиск уже установленного JRE ────────────────────────────────────────

    private File findExistingJava() {
        String javaExe = isWindows() ? "java.exe" : "java";
        File dir = new File(runtimeDir);
        if (!dir.exists()) return null;

        // Ищем рекурсивно в runtime/*/bin/java.exe
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null) return null;

        for (File sub : subdirs) {
            File candidate = new File(sub, "bin" + File.separator + javaExe);
            if (candidate.exists() && candidate.canExecute()) return candidate;

            // На один уровень глубже (zip может распаковаться как jre-17.0.x/bin/java.exe)
            File[] inner = sub.listFiles(File::isDirectory);
            if (inner != null) {
                for (File deep : inner) {
                    File c2 = new File(deep, "bin" + File.separator + javaExe);
                    if (c2.exists() && c2.canExecute()) return c2;
                }
            }
        }
        return null;
    }

    // ─── Скачивание JRE ──────────────────────────────────────────────────────

    private void downloadJre() throws Exception {
        progress(5, "Получение информации о Java 17 от Adoptium...");

        String os   = getAdoptiumOS();
        String arch = getAdoptiumArch();
        String url  = ADOPTIUM_API + "&os=" + os + "&architecture=" + arch;

        // Получаем JSON с метаданными сборки
        JsonArray releases = fetchJsonArray(url);
        if (releases == null || releases.isEmpty()) {
            throw new IOException("Adoptium API не вернул релизы для " + os + "/" + arch);
        }

        JsonObject release  = releases.get(0).getAsJsonObject();
        JsonObject binary   = release.getAsJsonObject("binary");
        JsonObject pkg      = binary.getAsJsonObject("package");

        String downloadUrl  = pkg.get("link").getAsString();
        String fileName     = pkg.get("name").getAsString();
        long   fileSize     = pkg.get("size").getAsLong();

        progress(10, "Скачивание Java 17 (" + (fileSize / 1024 / 1024) + " MB)...");

        new File(runtimeDir).mkdirs();
        File archiveFile = new File(runtimeDir, fileName);

        // Скачиваем с прогрессом
        downloadWithProgress(downloadUrl, archiveFile, fileSize);

        progress(80, "Распаковка Java 17...");

        if (fileName.endsWith(".zip")) {
            unzip(archiveFile, new File(runtimeDir));
        } else if (fileName.endsWith(".tar.gz")) {
            untar(archiveFile, new File(runtimeDir));
        }

        // Удаляем архив после распаковки
        archiveFile.delete();

        // На Linux/Mac нужно сделать java исполняемым
        if (!isWindows()) {
            setExecutable(new File(runtimeDir));
        }

        progress(100, "Java 17 установлена!");
    }

    // ─── Скачивание с прогрессом ─────────────────────────────────────────────

    private void downloadWithProgress(String url, File dest, long totalSize) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");

        // Поддержка redirect (Adoptium может редиректить)
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code == 301 || code == 302) {
            String redirect = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(redirect).openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
        }

        try (InputStream in   = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {

            byte[] buf = new byte[16384];
            long downloaded = 0;
            int n;

            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
                if (totalSize > 0) {
                    int pct = 10 + (int)(downloaded * 68.0 / totalSize);
                    if (downloaded % (1024 * 1024) < 16384) { // каждый ~1MB
                        long mb = downloaded / 1024 / 1024;
                        long total = totalSize / 1024 / 1024;
                        progress(pct, "Загрузка Java 17: " + mb + " / " + total + " MB");
                    }
                }
            }
        }
    }

    // ─── Распаковка ZIP ──────────────────────────────────────────────────────

    private void unzip(File zipFile, File destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());

                // Защита от zip-slip
                if (!out.getCanonicalPath().startsWith(destDir.getCanonicalPath())) continue;

                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = zis.read(buf)) != -1) os.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // ─── Распаковка TAR.GZ ───────────────────────────────────────────────────

    private void untar(File tarGz, File destDir) throws Exception {
        // Используем системный tar (есть везде — Windows 10+, Linux, Mac)
        ProcessBuilder pb = new ProcessBuilder(
                "tar", "-xzf", tarGz.getAbsolutePath(), "-C", destDir.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            String err = new String(p.getInputStream().readAllBytes());
            throw new IOException("tar завершился с ошибкой " + exitCode + ": " + err);
        }
    }

    // ─── Установка флага +x ──────────────────────────────────────────────────

    private void setExecutable(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) setExecutable(f);
            else if (f.getName().equals("java") || f.getName().equals("javaw")) {
                f.setExecutable(true, false);
            }
        }
    }

    // ─── HTTP JSON утилита ───────────────────────────────────────────────────

    private JsonArray fetchJsonArray(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");
        conn.setRequestProperty("Accept", "application/json");
        try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
            return JsonParser.parseReader(r).getAsJsonArray();
        }
    }

    // ─── Платформа ───────────────────────────────────────────────────────────

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String getAdoptiumOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))  return "windows";
        if (os.contains("mac"))  return "mac";
        return "linux";
    }

    private String getAdoptiumArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        return "x64";
    }

    private void progress(int pct, String msg) {
        System.out.println("[JavaDownloader] " + pct + "% — " + msg);
        if (progressCallback != null) progressCallback.accept(pct, msg);
    }
}
