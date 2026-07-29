package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BarehandedConfigScreen {

    private static final ServerConfig SERVER_DEFAULTS = new ServerConfig();
    private static final ClientConfig CLIENT_DEFAULTS = new ClientConfig();

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.barehanded.title"));

        builder.setSavingRunnable(() -> {
            ServerConfig.save();
            ClientConfig.save();

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && (mc.hasSingleplayerServer() || mc.player.hasPermissions(2))) {
                String json = new com.google.gson.Gson().toJson(ServerConfig.INSTANCE);
                dev.juaanp.barehanded.platform.Services.NETWORK.sendUpdateServerConfig(json);
            }
        });

        ConfigCategory server = builder.getOrCreateCategory(Component.translatable("config.barehanded.category.server"));
        ConfigCategory client = builder.getOrCreateCategory(Component.translatable("config.barehanded.category.client"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        buildServerCategories(server, eb);
        buildClientCategories(client, eb);

        return builder.build();
    }

    private static void buildServerCategories(ConfigCategory server, ConfigEntryBuilder eb) {
        buildCoreGrab(server, eb);
        buildRotation(server, eb);
        buildAssembling(server, eb);
        buildDisassembling(server, eb);
        buildEncumbrance(server, eb);
        buildExhaustion(server, eb);
        buildSuspension(server, eb);
        buildVelocity(server, eb);
        buildCollision(server, eb);
        buildAdvanced(server, eb);
        buildMovementPenalty(server, eb);
        buildLeadPrediction(server, eb);
        buildCompat(server, eb);
    }

    private static void buildCoreGrab(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder grab = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.grab"));
        addDouble(grab, eb, "maxForce", 1.0, 1000000.0, ServerConfig.INSTANCE.maxForce, SERVER_DEFAULTS.maxForce, v -> ServerConfig.INSTANCE.maxForce = v);
        addDouble(grab, eb, "minDistance", 0.1, 1024.0, ServerConfig.INSTANCE.minDistance, SERVER_DEFAULTS.minDistance, v -> ServerConfig.INSTANCE.minDistance = v);
        addDouble(grab, eb, "grabReachBonus", 0.0, 1024.0, ServerConfig.INSTANCE.grabReachBonus, SERVER_DEFAULTS.grabReachBonus, v -> ServerConfig.INSTANCE.grabReachBonus = v);
        addDouble(grab, eb, "grabStabilization", 0.0, 1.0, ServerConfig.INSTANCE.grabStabilization, SERVER_DEFAULTS.grabStabilization, v -> ServerConfig.INSTANCE.grabStabilization = v);
        addInt(grab, eb, "blockLimit", 0, 10000, ServerConfig.INSTANCE.blockLimit, SERVER_DEFAULTS.blockLimit, v -> ServerConfig.INSTANCE.blockLimit = v);
        addDouble(grab, eb, "minPhysicsMass", 0.0, 100.0, ServerConfig.INSTANCE.minPhysicsMass, SERVER_DEFAULTS.minPhysicsMass, v -> ServerConfig.INSTANCE.minPhysicsMass = v);
        addBoolean(grab, eb, "creativeSuperStrength", ServerConfig.INSTANCE.creativeSuperStrength, SERVER_DEFAULTS.creativeSuperStrength, v -> ServerConfig.INSTANCE.creativeSuperStrength = v);
        addBoolean(grab, eb, "spectatorSuperStrength", ServerConfig.INSTANCE.spectatorSuperStrength, SERVER_DEFAULTS.spectatorSuperStrength, v -> ServerConfig.INSTANCE.spectatorSuperStrength = v);
        addDouble(grab, eb, "strengthLevelMultiplier", 0.0, 100.0, ServerConfig.INSTANCE.strengthLevelMultiplier, SERVER_DEFAULTS.strengthLevelMultiplier, v -> ServerConfig.INSTANCE.strengthLevelMultiplier = v);
        addBoolean(grab, eb, "preventPropSurfing", ServerConfig.INSTANCE.preventPropSurfing, SERVER_DEFAULTS.preventPropSurfing, v -> ServerConfig.INSTANCE.preventPropSurfing = v);
        addBoolean(grab, eb, "preventGrabbingWhilePassenger", ServerConfig.INSTANCE.preventGrabbingWhilePassenger, SERVER_DEFAULTS.preventGrabbingWhilePassenger, v -> ServerConfig.INSTANCE.preventGrabbingWhilePassenger = v);
        addBoolean(grab, eb, "allowSpectatorGrabbing", ServerConfig.INSTANCE.allowSpectatorGrabbing, SERVER_DEFAULTS.allowSpectatorGrabbing, v -> ServerConfig.INSTANCE.allowSpectatorGrabbing = v);

        addBoolean(grab, eb, "enableDistanceScroll", ServerConfig.INSTANCE.enableDistanceScroll, SERVER_DEFAULTS.enableDistanceScroll, v -> ServerConfig.INSTANCE.enableDistanceScroll = v);
        addDouble(grab, eb, "scrollMinDistance", 0.5, 5.0, ServerConfig.INSTANCE.scrollMinDistance, SERVER_DEFAULTS.scrollMinDistance, v -> ServerConfig.INSTANCE.scrollMinDistance = v);
        addDouble(grab, eb, "scrollMaxDistance", 2.0, 256.0, ServerConfig.INSTANCE.scrollMaxDistance, SERVER_DEFAULTS.scrollMaxDistance, v -> ServerConfig.INSTANCE.scrollMaxDistance = v);
        addBoolean(grab, eb, "dynamicScrollMaxDistance", ServerConfig.INSTANCE.dynamicScrollMaxDistance, SERVER_DEFAULTS.dynamicScrollMaxDistance, v -> ServerConfig.INSTANCE.dynamicScrollMaxDistance = v);

        addBoolean(grab, eb, "preventGravityInSubLevels", ServerConfig.INSTANCE.preventGravityInSubLevels, SERVER_DEFAULTS.preventGravityInSubLevels, v -> ServerConfig.INSTANCE.preventGravityInSubLevels = v);
        addBoolean(grab, eb, "allowGrabbingSpawners", ServerConfig.INSTANCE.allowGrabbingSpawners, SERVER_DEFAULTS.allowGrabbingSpawners, v -> ServerConfig.INSTANCE.allowGrabbingSpawners = v);
        addBoolean(grab, eb, "useWhitelistMode", ServerConfig.INSTANCE.useWhitelistMode, SERVER_DEFAULTS.useWhitelistMode, v -> ServerConfig.INSTANCE.useWhitelistMode = v);
        addBoolean(grab, eb, "allowGrabbingUnbreakableBlocks", ServerConfig.INSTANCE.allowGrabbingUnbreakableBlocks, SERVER_DEFAULTS.allowGrabbingUnbreakableBlocks, v -> ServerConfig.INSTANCE.allowGrabbingUnbreakableBlocks = v);

        addBoolean(grab, eb, "preventGrabbingSubLevelsWithUngrabbableBlocks",
                ServerConfig.INSTANCE.preventGrabbingSubLevelsWithUngrabbableBlocks,
                SERVER_DEFAULTS.preventGrabbingSubLevelsWithUngrabbableBlocks,
                v -> ServerConfig.INSTANCE.preventGrabbingSubLevelsWithUngrabbableBlocks = v);

        server.addEntry(grab.build());
    }

    private static void buildRotation(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder rotation = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.rotation"));
        addBoolean(rotation, eb, "enableRotation", ServerConfig.INSTANCE.enableRotation, SERVER_DEFAULTS.enableRotation, v -> ServerConfig.INSTANCE.enableRotation = v);
        addBoolean(rotation, eb, "cameraLockedRotationX", ServerConfig.INSTANCE.cameraLockedRotationX, SERVER_DEFAULTS.cameraLockedRotationX, v -> ServerConfig.INSTANCE.cameraLockedRotationX = v);
        addBoolean(rotation, eb, "cameraLockedRotationY", ServerConfig.INSTANCE.cameraLockedRotationY, SERVER_DEFAULTS.cameraLockedRotationY, v -> ServerConfig.INSTANCE.cameraLockedRotationY = v);
        addDouble(rotation, eb, "rotationStabilization", 0.0, 1.0, ServerConfig.INSTANCE.rotationStabilization, SERVER_DEFAULTS.rotationStabilization, v -> ServerConfig.INSTANCE.rotationStabilization = v);
        addDouble(rotation, eb, "maxRotationSpeed", 0.0, 3.14, ServerConfig.INSTANCE.maxRotationSpeed, SERVER_DEFAULTS.maxRotationSpeed, v -> ServerConfig.INSTANCE.maxRotationSpeed = v);
        addBoolean(rotation, eb, "preventFastRotations", ServerConfig.INSTANCE.preventFastRotations, SERVER_DEFAULTS.preventFastRotations, v -> ServerConfig.INSTANCE.preventFastRotations = v);
        addDouble(rotation, eb, "rotationMassDampingFactor", 0.0, 100.0, ServerConfig.INSTANCE.rotationMassDampingFactor, SERVER_DEFAULTS.rotationMassDampingFactor, v -> ServerConfig.INSTANCE.rotationMassDampingFactor = v);
        addInt(rotation, eb, "rotationTicksWindow", 1, 1200, ServerConfig.INSTANCE.rotationTicksWindow, SERVER_DEFAULTS.rotationTicksWindow, v -> ServerConfig.INSTANCE.rotationTicksWindow = v);
        addDouble(rotation, eb, "rotationRebuildThreshold", 0.01, 3.14, ServerConfig.INSTANCE.rotationRebuildThreshold, SERVER_DEFAULTS.rotationRebuildThreshold, v -> ServerConfig.INSTANCE.rotationRebuildThreshold = v);

        addDouble(rotation, eb, "angularBrakeThreshold", 0.0, 3.14, ServerConfig.INSTANCE.angularBrakeThreshold, SERVER_DEFAULTS.angularBrakeThreshold, v -> ServerConfig.INSTANCE.angularBrakeThreshold = v);
        addDouble(rotation, eb, "angularBrakeMultiplier", 1.0, 100.0, ServerConfig.INSTANCE.angularBrakeMultiplier, SERVER_DEFAULTS.angularBrakeMultiplier, v -> ServerConfig.INSTANCE.angularBrakeMultiplier = v);

        server.addEntry(rotation.build());
    }

    private static void buildAssembling(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder assembly = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.assembling"));
        addBoolean(assembly, eb, "enableBarehandedAssembly", ServerConfig.INSTANCE.enableBarehandedAssembly, SERVER_DEFAULTS.enableBarehandedAssembly, v -> ServerConfig.INSTANCE.enableBarehandedAssembly = v);
        addBoolean(assembly, eb, "enableRipOffBlocks", ServerConfig.INSTANCE.enableRipOffBlocks, SERVER_DEFAULTS.enableRipOffBlocks, v -> ServerConfig.INSTANCE.enableRipOffBlocks = v);
        addDouble(assembly, eb, "barehandedAssemblySpeedMultiplier", 0.1, 1000.0, ServerConfig.INSTANCE.barehandedAssemblySpeedMultiplier, SERVER_DEFAULTS.barehandedAssemblySpeedMultiplier, v -> ServerConfig.INSTANCE.barehandedAssemblySpeedMultiplier = v);
        addDouble(assembly, eb, "barehandedAssemblyMaxDistance", 1.0, 1024.0, ServerConfig.INSTANCE.barehandedAssemblyMaxDistance, SERVER_DEFAULTS.barehandedAssemblyMaxDistance, v -> ServerConfig.INSTANCE.barehandedAssemblyMaxDistance = v);
        addDouble(assembly, eb, "assemblyServerDistanceTolerance", 0.0, 1024.0, ServerConfig.INSTANCE.assemblyServerDistanceTolerance, SERVER_DEFAULTS.assemblyServerDistanceTolerance, v -> ServerConfig.INSTANCE.assemblyServerDistanceTolerance = v);
        addDouble(assembly, eb, "assemblyClientDistanceTolerance", 0.0, 1024.0, ServerConfig.INSTANCE.assemblyClientDistanceTolerance, SERVER_DEFAULTS.assemblyClientDistanceTolerance, v -> ServerConfig.INSTANCE.assemblyClientDistanceTolerance = v);
        addInt(assembly, eb, "fastLiftAssemblyTicks", 1, 12000, ServerConfig.INSTANCE.fastLiftAssemblyTicks, SERVER_DEFAULTS.fastLiftAssemblyTicks, v -> ServerConfig.INSTANCE.fastLiftAssemblyTicks = v);
        addDouble(assembly, eb, "pullThreshold", 0.0, 100.0, ServerConfig.INSTANCE.pullThreshold, SERVER_DEFAULTS.pullThreshold, v -> ServerConfig.INSTANCE.pullThreshold = v);
        addDouble(assembly, eb, "pullResistanceMultiplier", 0.0, 1000.0, ServerConfig.INSTANCE.pullResistanceMultiplier, SERVER_DEFAULTS.pullResistanceMultiplier, v -> ServerConfig.INSTANCE.pullResistanceMultiplier = v);
        addDouble(assembly, eb, "assemblyMovementDamping", 0.0, 1.0, ServerConfig.INSTANCE.assemblyMovementDamping, SERVER_DEFAULTS.assemblyMovementDamping, v -> ServerConfig.INSTANCE.assemblyMovementDamping = v);
        addDouble(assembly, eb, "assemblyTetherStiffness", 0.0, 10.0, ServerConfig.INSTANCE.assemblyTetherStiffness, SERVER_DEFAULTS.assemblyTetherStiffness, v -> ServerConfig.INSTANCE.assemblyTetherStiffness = v);
        addDouble(assembly, eb, "assemblyMaxStretchBuffer", 0.0, 20.0, ServerConfig.INSTANCE.assemblyMaxStretchBuffer, SERVER_DEFAULTS.assemblyMaxStretchBuffer, v -> ServerConfig.INSTANCE.assemblyMaxStretchBuffer = v);

        addBoolean(assembly, eb, "enablePhysicsBlockPlacement", ServerConfig.INSTANCE.enablePhysicsBlockPlacement, SERVER_DEFAULTS.enablePhysicsBlockPlacement, v -> ServerConfig.INSTANCE.enablePhysicsBlockPlacement = v);
        server.addEntry(assembly.build());
    }

    private static void buildDisassembling(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder dis = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.disassembling"));

        addBoolean(dis, eb, "enableImpactDisassemble", ServerConfig.INSTANCE.enableImpactDisassemble, SERVER_DEFAULTS.enableImpactDisassemble, v -> ServerConfig.INSTANCE.enableImpactDisassemble = v);
        addDouble(dis, eb, "impactForceThreshold", 1.0, 1000.0, ServerConfig.INSTANCE.impactForceThreshold, SERVER_DEFAULTS.impactForceThreshold, v -> ServerConfig.INSTANCE.impactForceThreshold = v);
        addDouble(dis, eb, "impactMinSpeed", 0.0, 10.0, ServerConfig.INSTANCE.impactMinSpeed, SERVER_DEFAULTS.impactMinSpeed, v -> ServerConfig.INSTANCE.impactMinSpeed = v);
        addDouble(dis, eb, "impactContactDistance", 0.0, 10.0, ServerConfig.INSTANCE.impactContactDistance, SERVER_DEFAULTS.impactContactDistance, v -> ServerConfig.INSTANCE.impactContactDistance = v);
        addDouble(dis, eb, "impactSlowdownRatio", 0.01, 0.50, ServerConfig.INSTANCE.impactSlowdownRatio, SERVER_DEFAULTS.impactSlowdownRatio, v -> ServerConfig.INSTANCE.impactSlowdownRatio = v);
        addBoolean(dis, eb, "impactRequireIntentionalThrow", ServerConfig.INSTANCE.impactRequireIntentionalThrow, SERVER_DEFAULTS.impactRequireIntentionalThrow, v -> ServerConfig.INSTANCE.impactRequireIntentionalThrow = v);
        addDouble(dis, eb, "impactThrowSpeedRatio", 1.0, 5.0, ServerConfig.INSTANCE.impactThrowSpeedRatio, SERVER_DEFAULTS.impactThrowSpeedRatio, v -> ServerConfig.INSTANCE.impactThrowSpeedRatio = v);
        addBoolean(dis, eb, "impactBreakReplaceableBlocks", ServerConfig.INSTANCE.impactBreakReplaceableBlocks, SERVER_DEFAULTS.impactBreakReplaceableBlocks, v -> ServerConfig.INSTANCE.impactBreakReplaceableBlocks = v);
        addDouble(dis, eb, "impactRotationTolerance", 0.0, 45.0, ServerConfig.INSTANCE.impactRotationTolerance, SERVER_DEFAULTS.impactRotationTolerance, v -> ServerConfig.INSTANCE.impactRotationTolerance = v);
        addDouble(dis, eb, "impactPositionTolerance", 0.0, 0.5, ServerConfig.INSTANCE.impactPositionTolerance, SERVER_DEFAULTS.impactPositionTolerance, v -> ServerConfig.INSTANCE.impactPositionTolerance = v);

        addBoolean(dis, eb, "enableKeybindDisassemble", ServerConfig.INSTANCE.enableKeybindDisassemble, SERVER_DEFAULTS.enableKeybindDisassemble, v -> ServerConfig.INSTANCE.enableKeybindDisassemble = v);
        addBoolean(dis, eb, "showDisassembleMessages", ServerConfig.INSTANCE.showDisassembleMessages, SERVER_DEFAULTS.showDisassembleMessages, v -> ServerConfig.INSTANCE.showDisassembleMessages = v);
        addDouble(dis, eb, "keybindRotationTolerance", 0.0, 45.0, ServerConfig.INSTANCE.keybindRotationTolerance, SERVER_DEFAULTS.keybindRotationTolerance, v -> ServerConfig.INSTANCE.keybindRotationTolerance = v);
        addDouble(dis, eb, "keybindPositionTolerance", 0.0, 0.5, ServerConfig.INSTANCE.keybindPositionTolerance, SERVER_DEFAULTS.keybindPositionTolerance, v -> ServerConfig.INSTANCE.keybindPositionTolerance = v);

        addInt(dis, eb, "disassembleBlockLimit", 0, 50000, ServerConfig.INSTANCE.disassembleBlockLimit, SERVER_DEFAULTS.disassembleBlockLimit, v -> ServerConfig.INSTANCE.disassembleBlockLimit = v);
        server.addEntry(dis.build());
    }

    private static void buildEncumbrance(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder encumbrance = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.encumbrance"));
        addBoolean(encumbrance, eb, "enableEncumbrance", ServerConfig.INSTANCE.enableEncumbrance, SERVER_DEFAULTS.enableEncumbrance, v -> ServerConfig.INSTANCE.enableEncumbrance = v);
        addDouble(encumbrance, eb, "physicsGravity", 0.1, 1000.0, ServerConfig.INSTANCE.physicsGravity, SERVER_DEFAULTS.physicsGravity, v -> ServerConfig.INSTANCE.physicsGravity = v);
        addDouble(encumbrance, eb, "maxMovementPenalty", 0.0, 1.0, ServerConfig.INSTANCE.maxMovementPenalty, SERVER_DEFAULTS.maxMovementPenalty, v -> ServerConfig.INSTANCE.maxMovementPenalty = v);
        addDouble(encumbrance, eb, "jumpPreventionThreshold", 0.0, 1.0, ServerConfig.INSTANCE.jumpPreventionThreshold, SERVER_DEFAULTS.jumpPreventionThreshold, v -> ServerConfig.INSTANCE.jumpPreventionThreshold = v);
        addDouble(encumbrance, eb, "sneakPreventionThreshold", 0.0, 1.0, ServerConfig.INSTANCE.sneakPreventionThreshold, SERVER_DEFAULTS.sneakPreventionThreshold, v -> ServerConfig.INSTANCE.sneakPreventionThreshold = v);
        addDouble(encumbrance, eb, "maxCameraPenalty", 0.0, 1.0, ServerConfig.INSTANCE.maxCameraPenalty, SERVER_DEFAULTS.maxCameraPenalty, v -> ServerConfig.INSTANCE.maxCameraPenalty = v);
        addDouble(encumbrance, eb, "scrollSpeedReduction", 0.0, 1.0, ServerConfig.INSTANCE.scrollSpeedReduction, SERVER_DEFAULTS.scrollSpeedReduction, v -> ServerConfig.INSTANCE.scrollSpeedReduction = v);

        addBoolean(encumbrance, eb, "enablePhysicalTether", ServerConfig.INSTANCE.enablePhysicalTether, SERVER_DEFAULTS.enablePhysicalTether, v -> ServerConfig.INSTANCE.enablePhysicalTether = v);
        addDouble(encumbrance, eb, "armStretchTolerance", 0.0, 50.0, ServerConfig.INSTANCE.armStretchTolerance, SERVER_DEFAULTS.armStretchTolerance, v -> ServerConfig.INSTANCE.armStretchTolerance = v);
        addDouble(encumbrance, eb, "tetherStiffnessBase", 0.0, 10.0, ServerConfig.INSTANCE.tetherStiffnessBase, SERVER_DEFAULTS.tetherStiffnessBase, v -> ServerConfig.INSTANCE.tetherStiffnessBase = v);
        addDouble(encumbrance, eb, "tetherStiffnessMultiplier", 0.0, 50.0, ServerConfig.INSTANCE.tetherStiffnessMultiplier, SERVER_DEFAULTS.tetherStiffnessMultiplier, v -> ServerConfig.INSTANCE.tetherStiffnessMultiplier = v);
        addDouble(encumbrance, eb, "tetherVerticalSmoothing", 0.0, 1.0, ServerConfig.INSTANCE.tetherVerticalSmoothing, SERVER_DEFAULTS.tetherVerticalSmoothing, v -> ServerConfig.INSTANCE.tetherVerticalSmoothing = v);
        addDouble(encumbrance, eb, "recoilVelocityThreshold", 0.0, 1.0, ServerConfig.INSTANCE.recoilVelocityThreshold, SERVER_DEFAULTS.recoilVelocityThreshold, v -> ServerConfig.INSTANCE.recoilVelocityThreshold = v);
        addDouble(encumbrance, eb, "tetherHardEscapeBuffer", 0.0, 100.0, ServerConfig.INSTANCE.tetherHardEscapeBuffer, SERVER_DEFAULTS.tetherHardEscapeBuffer, v -> ServerConfig.INSTANCE.tetherHardEscapeBuffer = v);
        server.addEntry(encumbrance.build());
    }

    private static void buildExhaustion(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder exhaustion = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.exhaustion"));
        addBoolean(exhaustion, eb, "enableExhaustion", ServerConfig.INSTANCE.enableExhaustion, SERVER_DEFAULTS.enableExhaustion, v -> ServerConfig.INSTANCE.enableExhaustion = v);
        addDouble(exhaustion, eb, "exhaustionIdleRate", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionIdleRate, SERVER_DEFAULTS.exhaustionIdleRate, v -> ServerConfig.INSTANCE.exhaustionIdleRate = v);
        addDouble(exhaustion, eb, "exhaustionMovementRate", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionMovementRate, SERVER_DEFAULTS.exhaustionMovementRate, v -> ServerConfig.INSTANCE.exhaustionMovementRate = v);
        addDouble(exhaustion, eb, "exhaustionTensionRate", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionTensionRate, SERVER_DEFAULTS.exhaustionTensionRate, v -> ServerConfig.INSTANCE.exhaustionTensionRate = v);
        addDouble(exhaustion, eb, "exhaustionForceRate", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionForceRate, SERVER_DEFAULTS.exhaustionForceRate, v -> ServerConfig.INSTANCE.exhaustionForceRate = v);
        addDouble(exhaustion, eb, "exhaustionPassiveThreshold", 0.0, 1000.0, ServerConfig.INSTANCE.exhaustionPassiveThreshold, SERVER_DEFAULTS.exhaustionPassiveThreshold, v -> ServerConfig.INSTANCE.exhaustionPassiveThreshold = v);
        addDouble(exhaustion, eb, "exhaustionSupportHeightThreshold", 0.0, 10.0, ServerConfig.INSTANCE.exhaustionSupportHeightThreshold, SERVER_DEFAULTS.exhaustionSupportHeightThreshold, v -> ServerConfig.INSTANCE.exhaustionSupportHeightThreshold = v);
        addDouble(exhaustion, eb, "exhaustionLowSupportMultiplier", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionLowSupportMultiplier, SERVER_DEFAULTS.exhaustionLowSupportMultiplier, v -> ServerConfig.INSTANCE.exhaustionLowSupportMultiplier = v);
        addDouble(exhaustion, eb, "exhaustionMaxOverStretch", 0.0, 100.0, ServerConfig.INSTANCE.exhaustionMaxOverStretch, SERVER_DEFAULTS.exhaustionMaxOverStretch, v -> ServerConfig.INSTANCE.exhaustionMaxOverStretch = v);
        addDouble(exhaustion, eb, "exhaustionKineticReferenceSpeed", 0.1, 100.0, ServerConfig.INSTANCE.exhaustionKineticReferenceSpeed, SERVER_DEFAULTS.exhaustionKineticReferenceSpeed, v -> ServerConfig.INSTANCE.exhaustionKineticReferenceSpeed = v);
        addDouble(exhaustion, eb, "exhaustionVerticalWeightFactor", 0.0, 100.0, ServerConfig.INSTANCE.exhaustionVerticalWeightFactor, SERVER_DEFAULTS.exhaustionVerticalWeightFactor, v -> ServerConfig.INSTANCE.exhaustionVerticalWeightFactor = v);
        server.addEntry(exhaustion.build());
    }

    private static void buildSuspension(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder suspension = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.suspension"));
        addInt(suspension, eb, "standingOnGrabSuspendTicks", 0, 12000, ServerConfig.INSTANCE.standingOnGrabSuspendTicks, SERVER_DEFAULTS.standingOnGrabSuspendTicks, v -> ServerConfig.INSTANCE.standingOnGrabSuspendTicks = v);
        addDouble(suspension, eb, "grabProximityEyeSuspendDistance", 0.0, 1024.0, ServerConfig.INSTANCE.grabProximityEyeSuspendDistance, SERVER_DEFAULTS.grabProximityEyeSuspendDistance, v -> ServerConfig.INSTANCE.grabProximityEyeSuspendDistance = v);
        addDouble(suspension, eb, "grabProximityBodySuspendDistance", 0.0, 1024.0, ServerConfig.INSTANCE.grabProximityBodySuspendDistance, SERVER_DEFAULTS.grabProximityBodySuspendDistance, v -> ServerConfig.INSTANCE.grabProximityBodySuspendDistance = v);
        addDouble(suspension, eb, "tensionSuspendThreshold", 0.0, 10000.0, ServerConfig.INSTANCE.tensionSuspendThreshold, SERVER_DEFAULTS.tensionSuspendThreshold, v -> ServerConfig.INSTANCE.tensionSuspendThreshold = v);
        addDouble(suspension, eb, "tensionBreakThreshold", 0.0, 10000.0, ServerConfig.INSTANCE.tensionBreakThreshold, SERVER_DEFAULTS.tensionBreakThreshold, v -> ServerConfig.INSTANCE.tensionBreakThreshold = v);
        addDouble(suspension, eb, "creativeTensionSuspendThreshold", 0.0, 10000.0, ServerConfig.INSTANCE.creativeTensionSuspendThreshold, SERVER_DEFAULTS.creativeTensionSuspendThreshold, v -> ServerConfig.INSTANCE.creativeTensionSuspendThreshold = v);
        addDouble(suspension, eb, "creativeTensionBreakThreshold", 0.0, 10000.0, ServerConfig.INSTANCE.creativeTensionBreakThreshold, SERVER_DEFAULTS.creativeTensionBreakThreshold, v -> ServerConfig.INSTANCE.creativeTensionBreakThreshold = v);
        server.addEntry(suspension.build());
    }

    private static void buildVelocity(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder velocity = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.velocity"));
        addDouble(velocity, eb, "maxPlayerVelocityYUp", 0.0, 1000.0, ServerConfig.INSTANCE.maxPlayerVelocityYUp, SERVER_DEFAULTS.maxPlayerVelocityYUp, v -> ServerConfig.INSTANCE.maxPlayerVelocityYUp = v);
        addDouble(velocity, eb, "maxPlayerVelocityYDown", -1000.0, 0.0, ServerConfig.INSTANCE.maxPlayerVelocityYDown, SERVER_DEFAULTS.maxPlayerVelocityYDown, v -> ServerConfig.INSTANCE.maxPlayerVelocityYDown = v);
        addDouble(velocity, eb, "maxPlayerVelocityXZ", 0.0, 1000.0, ServerConfig.INSTANCE.maxPlayerVelocityXZ, SERVER_DEFAULTS.maxPlayerVelocityXZ, v -> ServerConfig.INSTANCE.maxPlayerVelocityXZ = v);
        server.addEntry(velocity.build());
    }

    private static void buildCollision(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder colls = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.collision"));
        addBoolean(colls, eb, "ignoreCollisionsGrabSelf", ServerConfig.INSTANCE.ignoreCollisionsGrabSelf, SERVER_DEFAULTS.ignoreCollisionsGrabSelf, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabSelf = v);
        addBoolean(colls, eb, "ignoreCollisionsGrabOtherPlayers", ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers, SERVER_DEFAULTS.ignoreCollisionsGrabOtherPlayers, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers = v);
        addBoolean(colls, eb, "ignoreCollisionsGrabEntities", ServerConfig.INSTANCE.ignoreCollisionsGrabEntities, SERVER_DEFAULTS.ignoreCollisionsGrabEntities, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabEntities = v);
        addBoolean(colls, eb, "ignoreCollisionsGrabEverything", ServerConfig.INSTANCE.ignoreCollisionsGrabEverything, SERVER_DEFAULTS.ignoreCollisionsGrabEverything, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabEverything = v);
        addDouble(colls, eb, "selfCollisionIgnoreDistanceSq", 0.0, 1000.0, ServerConfig.INSTANCE.selfCollisionIgnoreDistanceSq, SERVER_DEFAULTS.selfCollisionIgnoreDistanceSq, v -> ServerConfig.INSTANCE.selfCollisionIgnoreDistanceSq = v);
        addBoolean(colls, eb, "ignoreCollisionsRotationSelf", ServerConfig.INSTANCE.ignoreCollisionsRotationSelf, SERVER_DEFAULTS.ignoreCollisionsRotationSelf, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationSelf = v);
        addBoolean(colls, eb, "ignoreCollisionsRotationOtherPlayers", ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers, SERVER_DEFAULTS.ignoreCollisionsRotationOtherPlayers, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers = v);
        addBoolean(colls, eb, "ignoreCollisionsRotationEntities", ServerConfig.INSTANCE.ignoreCollisionsRotationEntities, SERVER_DEFAULTS.ignoreCollisionsRotationEntities, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationEntities = v);
        addBoolean(colls, eb, "ignoreCollisionsRotationEverything", ServerConfig.INSTANCE.ignoreCollisionsRotationEverything, SERVER_DEFAULTS.ignoreCollisionsRotationEverything, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationEverything = v);
        server.addEntry(colls.build());
    }

    private static void buildAdvanced(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder advanced = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.advanced"));
        addDouble(advanced, eb, "stiffness", 1.0, 10000000.0, ServerConfig.INSTANCE.stiffness, SERVER_DEFAULTS.stiffness, v -> ServerConfig.INSTANCE.stiffness = v);
        addDouble(advanced, eb, "damping", 1.0, 10000000.0, ServerConfig.INSTANCE.damping, SERVER_DEFAULTS.damping, v -> ServerConfig.INSTANCE.damping = v);
        addDouble(advanced, eb, "angularDamping", 1.0, 10000000.0, ServerConfig.INSTANCE.angularDamping, SERVER_DEFAULTS.angularDamping, v -> ServerConfig.INSTANCE.angularDamping = v);
        addDouble(advanced, eb, "creativeStrengthMultiplier", 1.0, 100000.0, ServerConfig.INSTANCE.creativeStrengthMultiplier, SERVER_DEFAULTS.creativeStrengthMultiplier, v -> ServerConfig.INSTANCE.creativeStrengthMultiplier = v);
        addDouble(advanced, eb, "speedStiffnessMultiplierFactor", 0.0, 100000.0, ServerConfig.INSTANCE.speedStiffnessMultiplierFactor, SERVER_DEFAULTS.speedStiffnessMultiplierFactor, v -> ServerConfig.INSTANCE.speedStiffnessMultiplierFactor = v);
        addDouble(advanced, eb, "maxSpeedStiffnessMultiplier", 1.0, 10004.0, ServerConfig.INSTANCE.maxSpeedStiffnessMultiplier, SERVER_DEFAULTS.maxSpeedStiffnessMultiplier, v -> ServerConfig.INSTANCE.maxSpeedStiffnessMultiplier = v);
        addDouble(advanced, eb, "baseAngularForceFactor", 0.0, 1.0, ServerConfig.INSTANCE.baseAngularForceFactor, SERVER_DEFAULTS.baseAngularForceFactor, v -> ServerConfig.INSTANCE.baseAngularForceFactor = v);

        addDouble(advanced, eb, "stableAngularForceMassBase", 0.0, 1000000.0, ServerConfig.INSTANCE.stableAngularForceMassBase, SERVER_DEFAULTS.stableAngularForceMassBase, v -> ServerConfig.INSTANCE.stableAngularForceMassBase = v);
        addDouble(advanced, eb, "stableAngularForceMassFactor", 0.0, 100000.0, ServerConfig.INSTANCE.stableAngularForceMassFactor, SERVER_DEFAULTS.stableAngularForceMassFactor, v -> ServerConfig.INSTANCE.stableAngularForceMassFactor = v);

        addDouble(advanced, eb, "rotatingAngularStiffnessBase", 0.0, 1000.0, ServerConfig.INSTANCE.rotatingAngularStiffnessBase, SERVER_DEFAULTS.rotatingAngularStiffnessBase, v -> ServerConfig.INSTANCE.rotatingAngularStiffnessBase = v);
        addDouble(advanced, eb, "rotatingAngularStiffnessRange", 0.0, 5000.0, ServerConfig.INSTANCE.rotatingAngularStiffnessRange, SERVER_DEFAULTS.rotatingAngularStiffnessRange, v -> ServerConfig.INSTANCE.rotatingAngularStiffnessRange = v);
        addDouble(advanced, eb, "swayAngularStiffnessBase", 0.0, 1000.0, ServerConfig.INSTANCE.swayAngularStiffnessBase, SERVER_DEFAULTS.swayAngularStiffnessBase, v -> ServerConfig.INSTANCE.swayAngularStiffnessBase = v);
        addDouble(advanced, eb, "swayAngularStiffnessRange", 0.0, 5000.0, ServerConfig.INSTANCE.swayAngularStiffnessRange, SERVER_DEFAULTS.swayAngularStiffnessRange, v -> ServerConfig.INSTANCE.swayAngularStiffnessRange = v);

        addDouble(advanced, eb, "minAngularForceForSmallObjects", 0.0, 100.0, ServerConfig.INSTANCE.minAngularForceForSmallObjects, SERVER_DEFAULTS.minAngularForceForSmallObjects, v -> ServerConfig.INSTANCE.minAngularForceForSmallObjects = v);

        addDouble(advanced, eb, "stabilizationExponent", 0.1, 10.0, ServerConfig.INSTANCE.stabilizationExponent, SERVER_DEFAULTS.stabilizationExponent, v -> ServerConfig.INSTANCE.stabilizationExponent = v);
        addDouble(advanced, eb, "creativeMaxMotorForce", 1.0, 1e15, ServerConfig.INSTANCE.creativeMaxMotorForce, SERVER_DEFAULTS.creativeMaxMotorForce, v -> ServerConfig.INSTANCE.creativeMaxMotorForce = v);

        addDouble(advanced, eb, "heavyObjectMassCurveMultiplier", 0.0, 100.0, ServerConfig.INSTANCE.heavyObjectMassCurveMultiplier, SERVER_DEFAULTS.heavyObjectMassCurveMultiplier, v -> ServerConfig.INSTANCE.heavyObjectMassCurveMultiplier = v);
        addDouble(advanced, eb, "heavyObjectMaxForceFactor", 0.0, 10.0, ServerConfig.INSTANCE.heavyObjectMaxForceFactor, SERVER_DEFAULTS.heavyObjectMaxForceFactor, v -> ServerConfig.INSTANCE.heavyObjectMaxForceFactor = v);
        addDouble(advanced, eb, "grabElasticityStiffnessFactor", 0.0, 10.0, ServerConfig.INSTANCE.grabElasticityStiffnessFactor, SERVER_DEFAULTS.grabElasticityStiffnessFactor, v -> ServerConfig.INSTANCE.grabElasticityStiffnessFactor = v);
        addDouble(advanced, eb, "grabElasticityDampingFactor", 0.0, 10.0, ServerConfig.INSTANCE.grabElasticityDampingFactor, SERVER_DEFAULTS.grabElasticityDampingFactor, v -> ServerConfig.INSTANCE.grabElasticityDampingFactor = v);
        addDouble(advanced, eb, "swayStiffnessEdgeFactor", 0.0, 10.0, ServerConfig.INSTANCE.swayStiffnessEdgeFactor, SERVER_DEFAULTS.swayStiffnessEdgeFactor, v -> ServerConfig.INSTANCE.swayStiffnessEdgeFactor = v);
        addDouble(advanced, eb, "swayStiffnessEdgeRangeFactor", 0.0, 10.0, ServerConfig.INSTANCE.swayStiffnessEdgeRangeFactor, SERVER_DEFAULTS.swayStiffnessEdgeRangeFactor, v -> ServerConfig.INSTANCE.swayStiffnessEdgeRangeFactor = v);
        addDouble(advanced, eb, "freePivotDampingMultiplier", 0.0, 100.0, ServerConfig.INSTANCE.freePivotDampingMultiplier, SERVER_DEFAULTS.freePivotDampingMultiplier, v -> ServerConfig.INSTANCE.freePivotDampingMultiplier = v);

        server.addEntry(advanced.build());
    }

    private static void buildMovementPenalty(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder movementPenalty = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.movement_penalty"));
        addDouble(movementPenalty, eb, "baseMovementPenalty", 0.0, 1.0, ServerConfig.INSTANCE.baseMovementPenalty, SERVER_DEFAULTS.baseMovementPenalty, v -> ServerConfig.INSTANCE.baseMovementPenalty = v);
        addDouble(movementPenalty, eb, "weightPenaltyMultiplier", 0.0, 5.0, ServerConfig.INSTANCE.weightPenaltyMultiplier, SERVER_DEFAULTS.weightPenaltyMultiplier, v -> ServerConfig.INSTANCE.weightPenaltyMultiplier = v);
        addDouble(movementPenalty, eb, "tensionPenaltyMultiplier", 0.0, 5.0, ServerConfig.INSTANCE.tensionPenaltyMultiplier, SERVER_DEFAULTS.tensionPenaltyMultiplier, v -> ServerConfig.INSTANCE.tensionPenaltyMultiplier = v);
        addDouble(movementPenalty, eb, "kineticPenaltyMultiplier", 0.0, 5.0, ServerConfig.INSTANCE.kineticPenaltyMultiplier, SERVER_DEFAULTS.kineticPenaltyMultiplier, v -> ServerConfig.INSTANCE.kineticPenaltyMultiplier = v);
        addDouble(movementPenalty, eb, "minSpeedWhileGrabbing", 0.0, 1.0, ServerConfig.INSTANCE.minSpeedWhileGrabbing, SERVER_DEFAULTS.minSpeedWhileGrabbing, v -> ServerConfig.INSTANCE.minSpeedWhileGrabbing = v);
        addDouble(movementPenalty, eb, "tensionPenaltyStartOffset", 0.0, 100.0, ServerConfig.INSTANCE.tensionPenaltyStartOffset, SERVER_DEFAULTS.tensionPenaltyStartOffset, v -> ServerConfig.INSTANCE.tensionPenaltyStartOffset = v);
        addDouble(movementPenalty, eb, "tensionPenaltyMaxDistance", 0.1, 1000.0, ServerConfig.INSTANCE.tensionPenaltyMaxDistance, SERVER_DEFAULTS.tensionPenaltyMaxDistance, v -> ServerConfig.INSTANCE.tensionPenaltyMaxDistance = v);
        addDouble(movementPenalty, eb, "kineticPenaltyReferenceSpeed", 0.1, 100.0, ServerConfig.INSTANCE.kineticPenaltyReferenceSpeed, SERVER_DEFAULTS.kineticPenaltyReferenceSpeed, v -> ServerConfig.INSTANCE.kineticPenaltyReferenceSpeed = v);
        server.addEntry(movementPenalty.build());
    }

    private static void buildLeadPrediction(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder leadPrediction = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.lead_prediction"));
        addDouble(leadPrediction, eb, "leadVelocityThreshold", 0.0, 100.0, ServerConfig.INSTANCE.leadVelocityThreshold, SERVER_DEFAULTS.leadVelocityThreshold, v -> ServerConfig.INSTANCE.leadVelocityThreshold = v);
        addDouble(leadPrediction, eb, "leadPredictionFactor", 0.0, 100.0, ServerConfig.INSTANCE.leadPredictionFactor, SERVER_DEFAULTS.leadPredictionFactor, v -> ServerConfig.INSTANCE.leadPredictionFactor = v);
        addDouble(leadPrediction, eb, "leadDownwardClamp", -100.0, 0.0, ServerConfig.INSTANCE.leadDownwardClamp, SERVER_DEFAULTS.leadDownwardClamp, v -> ServerConfig.INSTANCE.leadDownwardClamp = v);
        server.addEntry(leadPrediction.build());
    }

    private static void buildCompat(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder compat = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.compat"));

        addBoolean(compat, eb, "sableRagdollsCompatAllowGrabbingPlayerRagdolls",
                ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingPlayerRagdolls,
                SERVER_DEFAULTS.sableRagdollsCompatAllowGrabbingPlayerRagdolls,
                v -> ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingPlayerRagdolls = v);

        addBoolean(compat, eb, "sableRagdollsCompatAllowGrabbingMobRagdolls",
                ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingMobRagdolls,
                SERVER_DEFAULTS.sableRagdollsCompatAllowGrabbingMobRagdolls,
                v -> ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingMobRagdolls = v);

        addDouble(compat, eb, "sableRagdollsCompatMobRagdollMaxSize",
                0.1, 1000.0,
                ServerConfig.INSTANCE.sableRagdollsCompatMobRagdollMaxSize,
                SERVER_DEFAULTS.sableRagdollsCompatMobRagdollMaxSize,
                v -> ServerConfig.INSTANCE.sableRagdollsCompatMobRagdollMaxSize = v);

        var treeEnum = eb.startEnumSelector(
                        Component.translatable("config.barehanded.option.treeAssemblyMode"),
                        ServerConfig.TreeAssemblyMode.class,
                        ServerConfig.INSTANCE.treeAssemblyMode
                )
                .setDefaultValue(SERVER_DEFAULTS.treeAssemblyMode)
                .setTooltip(Component.translatable("config.barehanded.option.treeAssemblyMode.tooltip"))
                .setSaveConsumer(v -> ServerConfig.INSTANCE.treeAssemblyMode = v)
                .build();
        compat.add(treeEnum);

        server.addEntry(compat.build());
    }

    private static void buildClientCategories(ConfigCategory client, ConfigEntryBuilder eb) {
        buildInput(client, eb);
        buildRender(client, eb);
        buildHud(client, eb);
        buildAssemblyClient(client, eb);
    }

    private static void buildInput(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder inp = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.input"));

        addDouble(inp, eb, "verticalRotationSensitivity", 0.1, 2.0, ClientConfig.INSTANCE.verticalRotationSensitivity, CLIENT_DEFAULTS.verticalRotationSensitivity, v -> ClientConfig.INSTANCE.verticalRotationSensitivity = v);
        addDouble(inp, eb, "horizontalRotationSensitivity", 0.1, 2.0, ClientConfig.INSTANCE.horizontalRotationSensitivity, CLIENT_DEFAULTS.horizontalRotationSensitivity, v -> ClientConfig.INSTANCE.horizontalRotationSensitivity = v);
        addDouble(inp, eb, "scrollDistanceSensitivity", 0.1, 2.0, ClientConfig.INSTANCE.scrollDistanceSensitivity, CLIENT_DEFAULTS.scrollDistanceSensitivity, v -> ClientConfig.INSTANCE.scrollDistanceSensitivity = v);
        addBoolean(inp, eb, "invertVerticalRotation", ClientConfig.INSTANCE.invertVerticalRotation, CLIENT_DEFAULTS.invertVerticalRotation, v -> ClientConfig.INSTANCE.invertVerticalRotation = v);
        addBoolean(inp, eb, "invertHorizontalRotation", ClientConfig.INSTANCE.invertHorizontalRotation, CLIENT_DEFAULTS.invertHorizontalRotation, v -> ClientConfig.INSTANCE.invertHorizontalRotation = v);
        addBoolean(inp, eb, "rotateAroundCenter", ClientConfig.INSTANCE.rotateAroundCenter, CLIENT_DEFAULTS.rotateAroundCenter, v -> ClientConfig.INSTANCE.rotateAroundCenter = v);
        addBoolean(inp, eb, "preventMovementWhileRotating", ClientConfig.INSTANCE.preventMovementWhileRotating, CLIENT_DEFAULTS.preventMovementWhileRotating, v -> ClientConfig.INSTANCE.preventMovementWhileRotating = v);

        client.addEntry(inp.build());
    }

    private static void buildRender(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder rnd = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.render"));

        addBoolean(rnd, eb, "hideHandsWhileGrabbing", ClientConfig.INSTANCE.hideHandsWhileGrabbing, CLIENT_DEFAULTS.hideHandsWhileGrabbing, v -> ClientConfig.INSTANCE.hideHandsWhileGrabbing = v);
        addBoolean(rnd, eb, "hideFirstPersonArms", ClientConfig.INSTANCE.hideFirstPersonArms, CLIENT_DEFAULTS.hideFirstPersonArms, v -> ClientConfig.INSTANCE.hideFirstPersonArms = v);
        addBoolean(rnd, eb, "hideThirdPersonArms", ClientConfig.INSTANCE.hideThirdPersonArms, CLIENT_DEFAULTS.hideThirdPersonArms, v -> ClientConfig.INSTANCE.hideThirdPersonArms = v);
        addDouble(rnd, eb, "armTransitionSpeed", 0.05, 0.5, ClientConfig.INSTANCE.armTransitionSpeed, CLIENT_DEFAULTS.armTransitionSpeed, v -> ClientConfig.INSTANCE.armTransitionSpeed = v);

        client.addEntry(rnd.build());
    }

    private static void buildAssemblyClient(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder asm = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.assembly_client"));

        addDouble(asm, eb, "assemblyShakeMultiplier", 0.0, 0.2, ClientConfig.INSTANCE.assemblyShakeMultiplier, CLIENT_DEFAULTS.assemblyShakeMultiplier, v -> ClientConfig.INSTANCE.assemblyShakeMultiplier = v);
        addBoolean(asm, eb, "preventAssemblyWhenMining", ClientConfig.INSTANCE.preventAssemblyWhenMining, CLIENT_DEFAULTS.preventAssemblyWhenMining, v -> ClientConfig.INSTANCE.preventAssemblyWhenMining = v);
        addDouble(asm, eb, "barehandedAssemblyMiningThreshold", 0.01, 1.0, ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold, CLIENT_DEFAULTS.barehandedAssemblyMiningThreshold, v -> ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold = v);
        addBoolean(asm, eb, "unsneakOnTreeBreak", ClientConfig.INSTANCE.unsneakOnTreeBreak, CLIENT_DEFAULTS.unsneakOnTreeBreak, v -> ClientConfig.INSTANCE.unsneakOnTreeBreak = v);

        client.addEntry(asm.build());
    }

    private static void buildHud(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder hud = eb.startSubCategory(Component.translatable("config.barehanded.subcategory.hud"));

        addBoolean(hud, eb, "hideGrabHud", ClientConfig.INSTANCE.hideGrabHud, CLIENT_DEFAULTS.hideGrabHud, v -> ClientConfig.INSTANCE.hideGrabHud = v);
        addBoolean(hud, eb, "hidePhysicsPlacementOverlay", ClientConfig.INSTANCE.hidePhysicsPlacementOverlay, CLIENT_DEFAULTS.hidePhysicsPlacementOverlay, v -> ClientConfig.INSTANCE.hidePhysicsPlacementOverlay = v);
        addBoolean(hud, eb, "showPhysicsPlacementMessage", ClientConfig.INSTANCE.showPhysicsPlacementMessage, CLIENT_DEFAULTS.showPhysicsPlacementMessage, v -> ClientConfig.INSTANCE.showPhysicsPlacementMessage = v);

        client.addEntry(hud.build());
    }

    private static void addDouble(SubCategoryBuilder sub, ConfigEntryBuilder eb, String key, double min, double max, double current, double defaultValue, java.util.function.Consumer<Double> save) {
        var b = eb.startDoubleField(Component.translatable("config.barehanded.option." + key), current)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setTooltip(Component.translatable("config.barehanded.option." + key + ".tooltip"))
                .setSaveConsumer(save);
        sub.add(b.build());
    }

    private static void addInt(SubCategoryBuilder sub, ConfigEntryBuilder eb, String key, int min, int max, int current, int defaultValue, java.util.function.Consumer<Integer> save) {
        var b = eb.startIntField(Component.translatable("config.barehanded.option." + key), current)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setTooltip(Component.translatable("config.barehanded.option." + key + ".tooltip"))
                .setSaveConsumer(save);
        sub.add(b.build());
    }

    private static void addBoolean(SubCategoryBuilder sub, ConfigEntryBuilder eb, String key, boolean current, boolean defaultValue, java.util.function.Consumer<Boolean> save) {
        var b = eb.startBooleanToggle(Component.translatable("config.barehanded.option." + key), current)
                .setDefaultValue(defaultValue)
                .setTooltip(Component.translatable("config.barehanded.option." + key + ".tooltip"))
                .setSaveConsumer(save);
        sub.add(b.build());
    }
}