package com.flyt.launcher;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Загружает полный список версий Minecraft из Mojang API.
 * Поддерживает: release, snapshot, old_beta, old_alpha.
 * Кешируется в <baseDir>/versions/version_manifest.json (обновляется при каждом запуске).
 */
public class VersionManager {

    private static final String MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    public record VersionInfo(String id, String type, String releaseTime) {
        public boolean isRelease()  { return "release".equals(type); }
        public boolean isSnapshot() { return "snapshot".equals(type); }
        public boolean isLegacy()   { return "old_beta".equals(type) || "old_alpha".equals(type); }

        /** Красивое отображение типа */
        public String typeLabel() {
            return switch (type) {
                case "release"   -> "Release";
                case "snapshot"  -> "Snapshot";
                case "old_beta"  -> "Beta";
                case "old_alpha" -> "Alpha";
                default          -> type;
            };
        }

        @Override public String toString() { return id; }
    }

    private final String cacheFile;
    private List<VersionInfo> cachedVersions = null;

    public VersionManager(String baseDir) {
        this.cacheFile = baseDir + File.separator + "versions"
                + File.separator + "version_manifest.json";
    }

    /**
     * Возвращает все версии. При первом вызове — загружает из сети и кеширует.
     * При ошибке сети — читает из кеша на диске.
     */
    public List<VersionInfo> getAllVersions() {
        if (cachedVersions != null) return cachedVersions;

        try {
            // Пробуем загрузить из сети
            JsonObject manifest = fetchJson(MANIFEST_URL);
            saveCache(manifest);
            cachedVersions = parseVersions(manifest);
        } catch (Exception netErr) {
            System.out.println("[VersionManager] Нет сети, читаем кеш: " + netErr.getMessage());
            try {
                cachedVersions = parseVersions(readCache());
            } catch (Exception cacheErr) {
                System.err.println("[VersionManager] Кеш недоступен: " + cacheErr.getMessage());
                cachedVersions = Collections.emptyList();
            }
        }
        return cachedVersions;
    }

    /** Только Release версии (1.0 и новее) */
    public List<VersionInfo> getReleases() {
        return getAllVersions().stream().filter(VersionInfo::isRelease).toList();
    }

    /** Только Snapshot */
    public List<VersionInfo> getSnapshots() {
        return getAllVersions().stream().filter(VersionInfo::isSnapshot).toList();
    }

    /** Release + Beta + Alpha (без снапшотов) */
    public List<VersionInfo> getStable() {
        return getAllVersions().stream()
                .filter(v -> v.isRelease() || v.isLegacy()).toList();
    }

    /** Поиск по ID */
    public Optional<VersionInfo> findById(String id) {
        return getAllVersions().stream().filter(v -> v.id().equals(id)).findFirst();
    }

    /** ID последнего release */
    public String getLatestRelease() {
        return getReleases().stream().findFirst().map(VersionInfo::id).orElse("1.20.4");
    }

    /** Сбрасывает кеш (будет перезагружен при следующем вызове) */
    public void invalidate() { cachedVersions = null; }

    // ─── Приватные ───────────────────────────────────────

    private List<VersionInfo> parseVersions(JsonObject manifest) {
        List<VersionInfo> list = new ArrayList<>();
        if (!manifest.has("versions")) return list;
        for (JsonElement el : manifest.getAsJsonArray("versions")) {
            JsonObject v = el.getAsJsonObject();
            list.add(new VersionInfo(
                v.get("id").getAsString(),
                v.get("type").getAsString(),
                v.has("releaseTime") ? v.get("releaseTime").getAsString() : ""
            ));
        }
        return list;
    }

    private JsonObject fetchJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(12_000);
        conn.setRequestProperty("User-Agent", "FlytLauncher/1.0");
        try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private void saveCache(JsonObject manifest) {
        try {
            new File(cacheFile).getParentFile().mkdirs();
            try (Writer w = new FileWriter(cacheFile)) {
                new GsonBuilder().create().toJson(manifest, w);
            }
        } catch (Exception ignored) {}
    }

    private JsonObject readCache() throws Exception {
        try (FileReader r = new FileReader(cacheFile)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }
}
