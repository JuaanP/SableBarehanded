package dev.juaanp.barehanded.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.juaanp.barehanded.Constants;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ClientConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = Paths.get("config", Constants.MOD_ID + "-client.json").toFile();

    public int configVersion = 11;

    public double verticalRotationSensitivity = 0.1;
    public double horizontalRotationSensitivity = 0.1;
    public double scrollDistanceSensitivity = 0.5;
    public boolean invertVerticalRotation = false;
    public boolean invertHorizontalRotation = false;
    public boolean rotateAroundCenter = false;
    public boolean preventMovementWhileRotating = false;

    public double armTransitionSpeed = 0.13;
    public double armGrabLowerOffset = 1.5;
    public double armTransitionDelay = 0.2;
    public double armVanillaDropDistance = 1.2;
    public double armEaseFullThreshold = 0.99;

    public boolean hideHandsWhileGrabbing = false;
    public boolean hideFirstPersonArms = false;
    public boolean hideThirdPersonArms = false;

    public double assemblyShakeMultiplier = 0.04;
    public double shakeFrequencyX = 3.0;
    public double shakeFrequencyY = 4.0;
    public double shakeFrequencyZ = 5.0;
    public double visualShakeThreshold = 0.3;

    public boolean hideGrabHud = false;
    public boolean hidePhysicsPlacementOverlay = false;
    public boolean showPhysicsPlacementMessage = true;

    public double grabArmOffsetX = 0.18;
    public double grabArmOffsetY = -0.6;
    public double grabArmOffsetZ = -0.2;

    public boolean preventAssemblyWhenMining = true;
    public double barehandedAssemblyMiningThreshold = 0.08;
    public boolean unsneakOnTreeBreak = true;

    public int regrabDebounceTicks = 2;

    public static ClientConfig INSTANCE = new ClientConfig();

    public static void load() {
        Path path = FILE.toPath();
        Path backupPath = path.resolveSibling(FILE.getName() + ".backup");

        try {
            if (Files.exists(path)) {
                ClientConfig loaded;
                try (FileReader reader = new FileReader(FILE)) {
                    loaded = GSON.fromJson(reader, ClientConfig.class);
                }

                if (loaded != null) {
                    if (loaded.configVersion < INSTANCE.configVersion) {
                        LOGGER.warn("Sable Barehanded client config outdated (v{} -> v{}). Backing up to {}.backup and resetting to new defaults...",
                                loaded.configVersion, INSTANCE.configVersion, FILE.getName());

                        Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
                        save();
                    } else {
                        INSTANCE = loaded;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load client config", e);
        }
    }

    public static void save() {
        try {
            if (!FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save client config", e);
        }
    }
}