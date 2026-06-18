package dev.juaanp.barehanded.network;

import com.google.gson.Gson;
import dev.juaanp.barehanded.Constants;
import dev.juaanp.barehanded.config.ServerConfig;

public class ClientConfigSyncHandler {
    private static final Gson GSON = new Gson();

    public static void applyServerConfig(String json) {
        try {
            ServerConfig loaded = GSON.fromJson(json, ServerConfig.class);
            if (loaded != null) {
                ServerConfig.INSTANCE = loaded;
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse synced server config", e);
        }
    }
}