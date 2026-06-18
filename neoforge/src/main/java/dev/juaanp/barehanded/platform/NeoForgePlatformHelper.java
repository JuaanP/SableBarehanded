package dev.juaanp.barehanded.platform;

import dev.juaanp.barehanded.platform.services.IPlatformHelper;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return net.neoforged.fml.ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !net.neoforged.fml.loading.FMLLoader.isProduction();
    }
}