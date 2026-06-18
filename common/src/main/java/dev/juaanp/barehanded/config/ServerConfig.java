package dev.juaanp.barehanded.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.juaanp.barehanded.Constants;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;

public class ServerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = Paths.get("config", Constants.MOD_ID + "-server.json").toFile();

    public int configVersion = 6;

    public double maxForce = 120.0;
    public double minDistance = 1.5;
    public double grabReachBonus = 0.0;
    public double grabStabilization = 0.01;
    public boolean creativeSuperStrength = true;
    public double strength1Multiplier = 2.0;
    public double strength2Multiplier = 4.0;

    public boolean preventGravityInSubLevels = true;
    public boolean allowGrabbingSpawners = false;

    public boolean enableDistanceScroll = true;
    public double scrollMinDistance = 1.0;
    public double scrollMaxDistance = 12.0;

    public boolean enableRotation = true;
    public double rotationStabilization = 0.2;
    public double maxRotationSpeed = 0.2;
    public boolean preventFastRotations = true;
    public double rotationMassDampingFactor = 0.02;
    public int rotationTicksWindow = 5;
    public double rotationRebuildThreshold = 0.25;

    public boolean enableBarehandedAssembly = true;
    public double barehandedAssemblySpeedMultiplier = 1.0;
    public double barehandedAssemblyMaxDistance = 2.5;
    public double assemblyServerDistanceTolerance = 1.0;
    public double assemblyClientDistanceTolerance = 1.5;
    public int fastLiftAssemblyTicks = 1;
    public double pullThreshold = 0.05;
    public double pullResistanceMultiplier = 0.6;
    public double assemblyMovementDamping = 0.5;
    public double assemblyTetherStiffness = 0.5;
    public double assemblyMaxStretchBuffer = 2.0;

    public boolean enablePhysicsBlockPlacement = true;

    public int blockLimit = 0;

    public boolean enableImpactDisassemble = true;
    public double impactForceThreshold = 3.55;
    public double impactMinSpeed = 0.55;
    public double impactContactDistance = 1.5;
    public double impactSlowdownRatio = 0.1;
    public boolean impactRequireIntentionalThrow = true;
    public double impactThrowSpeedRatio = 2.0;
    public boolean impactBreakReplaceableBlocks = true;
    public double impactRotationTolerance = 45.0;
    public double impactPositionTolerance = 0.5;

    public boolean enableKeybindDisassemble = true;
    public boolean showDisassembleMessages = true;
    public double keybindRotationTolerance = 45.0;
    public double keybindPositionTolerance = 0.5;
    public boolean ignoreCollisionsGrabEverything = false;
    public boolean ignoreCollisionsGrabEntities = false;
    public boolean ignoreCollisionsGrabOtherPlayers = false;
    public boolean ignoreCollisionsGrabSelf = false;
    public double selfCollisionIgnoreDistanceSq = 4.0;

    public int disassembleBlockLimit = 6;

    public boolean ignoreCollisionsRotationEverything = false;
    public boolean ignoreCollisionsRotationEntities = true;
    public boolean ignoreCollisionsRotationOtherPlayers = true;
    public boolean ignoreCollisionsRotationSelf = true;

    public int standingOnGrabSuspendTicks = 15;
    public double grabProximityEyeSuspendDistance = 0.8;
    public double grabProximityBodySuspendDistance = 0.6;
    public double tensionSuspendThreshold = 3.5;
    public double tensionBreakThreshold = 3.0;
    public double creativeTensionSuspendThreshold = 64.0;
    public double creativeTensionBreakThreshold = 64.0;

    public double maxPlayerVelocityYUp = 2.5;
    public double maxPlayerVelocityYDown = -4.0;
    public double maxPlayerVelocityXZ = 2.5;

    public double stiffness = 800.0;
    public double damping = 45.0;
    public double angularDamping = 60.0;
    public double creativeStrengthMultiplier = 10.0;
    public double speedStiffnessMultiplierFactor = 2.5;
    public double maxSpeedStiffnessMultiplier = 3.5;
    public double baseAngularForceFactor = 0.15;
    public double stableAngularForceMassBase = 10.0;
    public double stableAngularForceMassFactor = 0.5;
    public double rotatingAngularStiffnessBase = 1.5;
    public double rotatingAngularStiffnessRange = 4.5;
    public double swayAngularStiffnessBase = 0.6;
    public double swayAngularStiffnessRange = 5.4;
    public double stabilizationExponent = 3.0;

    public double baseMovementPenalty = 0.0;
    public double weightPenaltyMultiplier = 0.02;
    public double tensionPenaltyMultiplier = 0.05;
    public double kineticPenaltyMultiplier = 0.02;
    public double minSpeedWhileGrabbing = 0.05;
    public double tensionPenaltyStartOffset = 0.5;
    public double tensionPenaltyMaxDistance = 5.0;
    public double kineticPenaltyReferenceSpeed = 1.0;

    public boolean enableEncumbrance = true;
    public double physicsGravity = 9.81;
    public double maxMovementPenalty = 0.85;
    public double jumpPreventionThreshold = 0.7;
    public double sneakPreventionThreshold = 0.3;
    public double maxCameraPenalty = 0.6;
    public boolean enablePhysicalTether = true;
    public double armStretchTolerance = 0.3;
    public double tetherStiffnessBase = 0.15;
    public double tetherStiffnessMultiplier = 0.85;
    public double tetherVerticalSmoothing = 0.4;
    public double recoilVelocityThreshold = 0.01;
    public double tetherHardEscapeBuffer = 2.0;

    public boolean enableExhaustion = true;
    public double exhaustionIdleRate = 0.02;
    public double exhaustionMovementRate = 0.08;
    public double exhaustionTensionRate = 0.04;
    public double exhaustionForceRate = 0.06;
    public double exhaustionPassiveThreshold = 20.0;
    public double exhaustionSupportHeightThreshold = 0.8;
    public double exhaustionLowSupportMultiplier = 0.5;
    public double exhaustionMaxOverStretch = 2.0;
    public double exhaustionKineticReferenceSpeed = 3.0;
    public double exhaustionVerticalWeightFactor = 4.0;

    public double minPhysicsMass = 0.01;

    public double leadVelocityThreshold = 0.12;
    public double leadPredictionFactor = 0.5;
    public double leadDownwardClamp = 0.0;
    public double creativeMaxMotorForce = 1e12;

    public static ServerConfig INSTANCE = new ServerConfig();

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader reader = new FileReader(FILE)) {
                    ServerConfig loaded = GSON.fromJson(reader, ServerConfig.class);
                    if (loaded != null) {
                        if (loaded.configVersion < INSTANCE.configVersion) {
                            LOGGER.warn("Sable Barehanded server config outdated (v{} -> v{}). Migrating...",
                                    loaded.configVersion, INSTANCE.configVersion);
                            loaded.configVersion = INSTANCE.configVersion;
                            INSTANCE = loaded;
                            save();
                        } else {
                            INSTANCE = loaded;
                        }
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load server config", e);
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
            LOGGER.error("Failed to save server config", e);
        }
    }
}