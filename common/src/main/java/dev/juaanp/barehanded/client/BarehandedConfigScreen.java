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
                .setTitle(Component.literal("Sable Barehanded"));

        builder.setSavingRunnable(() -> {
            ServerConfig.save();
            ClientConfig.save();

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && (mc.hasSingleplayerServer() || mc.player.hasPermissions(2))) {
                String json = new com.google.gson.Gson().toJson(ServerConfig.INSTANCE);
                dev.juaanp.barehanded.platform.Services.NETWORK.sendUpdateServerConfig(json);
            }
        });

        ConfigCategory server = builder.getOrCreateCategory(Component.literal("Server Settings"));
        ConfigCategory client = builder.getOrCreateCategory(Component.literal("Client Settings"));
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
        SubCategoryBuilder grab = eb.startSubCategory(Component.literal("Grab"));
        addDouble(grab, eb, "Max Force (N)", "Maximum spring force applied to move a grabbed object.", 1.0, 1000000.0, ServerConfig.INSTANCE.maxForce, SERVER_DEFAULTS.maxForce, v -> ServerConfig.INSTANCE.maxForce = v);
        addDouble(grab, eb, "Min Distance (m)", "Minimum hold distance from the player's eye to the anchor.", 0.1, 1024.0, ServerConfig.INSTANCE.minDistance, SERVER_DEFAULTS.minDistance, v -> ServerConfig.INSTANCE.minDistance = v);
        addDouble(grab, eb, "Grab Reach Bonus (m)", "Extra reach added on top of the block-interaction-range attribute.", 0.0, 1024.0, ServerConfig.INSTANCE.grabReachBonus, SERVER_DEFAULTS.grabReachBonus, v -> ServerConfig.INSTANCE.grabReachBonus = v);
        addDouble(grab, eb, "Grab Stabilization", "Positional stabilization factor (0 = loose, 1 = rigid).", 0.0, 1.0, ServerConfig.INSTANCE.grabStabilization, SERVER_DEFAULTS.grabStabilization, v -> ServerConfig.INSTANCE.grabStabilization = v);
        addInt(grab, eb, "Block Limit", "Maximum number of connected blocks that can be grabbed at once. 0 = unlimited.", 0, 10000, ServerConfig.INSTANCE.blockLimit, SERVER_DEFAULTS.blockLimit, v -> ServerConfig.INSTANCE.blockLimit = v);
        addDouble(grab, eb, "Min Physics Mass", "Minimum mass required for an object to be grabbable (prevents grabbing tiny particles).", 0.0, 100.0, ServerConfig.INSTANCE.minPhysicsMass, SERVER_DEFAULTS.minPhysicsMass, v -> ServerConfig.INSTANCE.minPhysicsMass = v);
        addBoolean(grab, eb, "Creative Super Strength", "Creative players ignore weight limits and get boosted physics.", ServerConfig.INSTANCE.creativeSuperStrength, SERVER_DEFAULTS.creativeSuperStrength, v -> ServerConfig.INSTANCE.creativeSuperStrength = v);
        addBoolean(grab, eb, "Spectator Super Strength", "Spectator players ignore weight limits and get boosted physics.", ServerConfig.INSTANCE.spectatorSuperStrength, SERVER_DEFAULTS.spectatorSuperStrength, v -> ServerConfig.INSTANCE.spectatorSuperStrength = v);
        addDouble(grab, eb, "Strength Level Multiplier", "Force multiplier per strength level. Formula: 1 + (level × multiplier). Works with any strength level.", 0.0, 100.0, ServerConfig.INSTANCE.strengthLevelMultiplier, SERVER_DEFAULTS.strengthLevelMultiplier, v -> ServerConfig.INSTANCE.strengthLevelMultiplier = v);
        addBoolean(grab, eb, "Prevent Prop-Surfing", "Prevents players from flying by standing on the object they are grabbing.", ServerConfig.INSTANCE.preventPropSurfing, SERVER_DEFAULTS.preventPropSurfing, v -> ServerConfig.INSTANCE.preventPropSurfing = v);
        addBoolean(grab, eb, "Prevent Grabbing While Passenger", "Prevents players from grabbing sub-levels while seated on entities or other sub-levels.", ServerConfig.INSTANCE.preventGrabbingWhilePassenger, SERVER_DEFAULTS.preventGrabbingWhilePassenger, v -> ServerConfig.INSTANCE.preventGrabbingWhilePassenger = v);
        addBoolean(grab, eb, "Allow Spectator Grabbing", "Allows players in spectator mode to grab objects (ghost interaction).", ServerConfig.INSTANCE.allowSpectatorGrabbing, SERVER_DEFAULTS.allowSpectatorGrabbing, v -> ServerConfig.INSTANCE.allowSpectatorGrabbing = v);

        addBoolean(grab, eb, "Enable Distance Scroll", "Allows players to adjust the grab distance using the scroll wheel.", ServerConfig.INSTANCE.enableDistanceScroll, SERVER_DEFAULTS.enableDistanceScroll, v -> ServerConfig.INSTANCE.enableDistanceScroll = v);
        addDouble(grab, eb, "Scroll Min Distance", "Minimum distance allowed when scrolling.", 0.5, 5.0, ServerConfig.INSTANCE.scrollMinDistance, SERVER_DEFAULTS.scrollMinDistance, v -> ServerConfig.INSTANCE.scrollMinDistance = v);
        addDouble(grab, eb, "Scroll Max Distance", "Maximum distance allowed when scrolling.", 2.0, 256.0, ServerConfig.INSTANCE.scrollMaxDistance, SERVER_DEFAULTS.scrollMaxDistance, v -> ServerConfig.INSTANCE.scrollMaxDistance = v);
        addBoolean(grab, eb, "Dynamic Max Scroll", "Automatically matches the max scroll distance to the player's reach.", ServerConfig.INSTANCE.dynamicScrollMaxDistance, SERVER_DEFAULTS.dynamicScrollMaxDistance, v -> ServerConfig.INSTANCE.dynamicScrollMaxDistance = v);

        addBoolean(grab, eb, "Prevent Gravity In Sub-Levels", "Stops blocks like sand or gravel from falling down while they are part of a grabbed structure.", ServerConfig.INSTANCE.preventGravityInSubLevels, SERVER_DEFAULTS.preventGravityInSubLevels, v -> ServerConfig.INSTANCE.preventGravityInSubLevels = v);
        addBoolean(grab, eb, "Allow Grabbing Spawners", "Overrides all restrictions to explicitly allow grabbing Monster Spawners.", ServerConfig.INSTANCE.allowGrabbingSpawners, SERVER_DEFAULTS.allowGrabbingSpawners, v -> ServerConfig.INSTANCE.allowGrabbingSpawners = v);
        addBoolean(grab, eb, "Use Whitelist Mode", "If true, ONLY blocks in the #barehanded:grabbable tag can be grabbed. If false, all blocks except those in #barehanded:ungrabbable can be grabbed.", ServerConfig.INSTANCE.useWhitelistMode, SERVER_DEFAULTS.useWhitelistMode, v -> ServerConfig.INSTANCE.useWhitelistMode = v);
        addBoolean(grab, eb, "Allow Grabbing Unbreakable Blocks", "Allows grabbing blocks with destroySpeed < 0 (bedrock, barriers, etc). In whitelist mode, these blocks must also be in #barehanded:grabbable.", ServerConfig.INSTANCE.allowGrabbingUnbreakableBlocks, SERVER_DEFAULTS.allowGrabbingUnbreakableBlocks, v -> ServerConfig.INSTANCE.allowGrabbingUnbreakableBlocks = v);

        addBoolean(grab, eb, "Prevent Grabbing Sub-Levels with Ungrabbable Blocks",
                "If true, prevents grabbing an entire sub-level if it contains any block from the #barehanded:ungrabbable tag.",
                ServerConfig.INSTANCE.preventGrabbingSubLevelsWithUngrabbableBlocks,
                SERVER_DEFAULTS.preventGrabbingSubLevelsWithUngrabbableBlocks,
                v -> ServerConfig.INSTANCE.preventGrabbingSubLevelsWithUngrabbableBlocks = v);

        server.addEntry(grab.build());
    }

    private static void buildRotation(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder rotation = eb.startSubCategory(Component.literal("Rotation"));
        addBoolean(rotation, eb, "Enable Rotation", "Allows players to rotate grabbed objects using the mouse.", ServerConfig.INSTANCE.enableRotation, SERVER_DEFAULTS.enableRotation, v -> ServerConfig.INSTANCE.enableRotation = v);
        addBoolean(rotation, eb, "Camera Locked Rotation X (Pitch)", "Grabbed object automatically tilts up and down with your camera.", ServerConfig.INSTANCE.cameraLockedRotationX, SERVER_DEFAULTS.cameraLockedRotationX, v -> ServerConfig.INSTANCE.cameraLockedRotationX = v);
        addBoolean(rotation, eb, "Camera Locked Rotation Y (Yaw)", "Grabbed object automatically turns left and right with your camera.", ServerConfig.INSTANCE.cameraLockedRotationY, SERVER_DEFAULTS.cameraLockedRotationY, v -> ServerConfig.INSTANCE.cameraLockedRotationY = v);
        addDouble(rotation, eb, "Rotation Stabilization", "How rigidly the object holds its rotation when idle (0 = loose, 1 = rigid).", 0.0, 1.0, ServerConfig.INSTANCE.rotationStabilization, SERVER_DEFAULTS.rotationStabilization, v -> ServerConfig.INSTANCE.rotationStabilization = v);
        addDouble(rotation, eb, "Max Rotation Speed", "Maximum allowed rotation speed per tick (radians).", 0.0, 3.14, ServerConfig.INSTANCE.maxRotationSpeed, SERVER_DEFAULTS.maxRotationSpeed, v -> ServerConfig.INSTANCE.maxRotationSpeed = v);
        addBoolean(rotation, eb, "Prevent Fast Rotations", "Clamps rotation speed to prevent physics explosions from fast mouse flicks.", ServerConfig.INSTANCE.preventFastRotations, SERVER_DEFAULTS.preventFastRotations, v -> ServerConfig.INSTANCE.preventFastRotations = v);
        addDouble(rotation, eb, "Rotation Mass Damping Factor", "How much object mass slows down rotation input.", 0.0, 100.0, ServerConfig.INSTANCE.rotationMassDampingFactor, SERVER_DEFAULTS.rotationMassDampingFactor, v -> ServerConfig.INSTANCE.rotationMassDampingFactor = v);
        addInt(rotation, eb, "Rotation Ticks Window", "Ticks the rotation motor stays active after the last mouse input.", 1, 1200, ServerConfig.INSTANCE.rotationTicksWindow, SERVER_DEFAULTS.rotationTicksWindow, v -> ServerConfig.INSTANCE.rotationTicksWindow = v);
        addDouble(rotation, eb, "Rotation Rebuild Threshold", "Angle (radians) that triggers a constraint pivot rebuild to prevent twisting.", 0.01, 3.14, ServerConfig.INSTANCE.rotationRebuildThreshold, SERVER_DEFAULTS.rotationRebuildThreshold, v -> ServerConfig.INSTANCE.rotationRebuildThreshold = v);

        addDouble(rotation, eb, "Angular Brake Threshold", "Angle (radians) where the angular brake activates.", 0.0, 3.14, ServerConfig.INSTANCE.angularBrakeThreshold, SERVER_DEFAULTS.angularBrakeThreshold, v -> ServerConfig.INSTANCE.angularBrakeThreshold = v);
        addDouble(rotation, eb, "Angular Brake Multiplier", "Damping multiplier when angular brake is active.", 1.0, 100.0, ServerConfig.INSTANCE.angularBrakeMultiplier, SERVER_DEFAULTS.angularBrakeMultiplier, v -> ServerConfig.INSTANCE.angularBrakeMultiplier = v);

        server.addEntry(rotation.build());
    }

    private static void buildAssembling(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder assembly = eb.startSubCategory(Component.literal("Assembling"));
        addBoolean(assembly, eb, "Enable Assembly", "Allows players to pull and detach connected blocks from the world.", ServerConfig.INSTANCE.enableBarehandedAssembly, SERVER_DEFAULTS.enableBarehandedAssembly, v -> ServerConfig.INSTANCE.enableBarehandedAssembly = v);
        addBoolean(assembly, eb, "Enable Rip-Off Blocks", "Allows tearing individual blocks off a grabbed sub-level using sneak.", ServerConfig.INSTANCE.enableRipOffBlocks, SERVER_DEFAULTS.enableRipOffBlocks, v -> ServerConfig.INSTANCE.enableRipOffBlocks = v);
        addDouble(assembly, eb, "Detach Speed Multiplier", "Multiplier for the time it takes to pull blocks out of the world.", 0.1, 1000.0, ServerConfig.INSTANCE.barehandedAssemblySpeedMultiplier, SERVER_DEFAULTS.barehandedAssemblySpeedMultiplier, v -> ServerConfig.INSTANCE.barehandedAssemblySpeedMultiplier = v);
        addDouble(assembly, eb, "Detach Max Distance (m)", "Maximum distance from the player to start pulling blocks.", 1.0, 1024.0, ServerConfig.INSTANCE.barehandedAssemblyMaxDistance, SERVER_DEFAULTS.barehandedAssemblyMaxDistance, v -> ServerConfig.INSTANCE.barehandedAssemblyMaxDistance = v);
        addDouble(assembly, eb, "Server Tolerance (m)", "Server-side grace distance added when validating assembly requests.", 0.0, 1024.0, ServerConfig.INSTANCE.assemblyServerDistanceTolerance, SERVER_DEFAULTS.assemblyServerDistanceTolerance, v -> ServerConfig.INSTANCE.assemblyServerDistanceTolerance = v);
        addDouble(assembly, eb, "Client Tolerance (m)", "Client-side grace distance before cancelling the pull charge.", 0.0, 1024.0, ServerConfig.INSTANCE.assemblyClientDistanceTolerance, SERVER_DEFAULTS.assemblyClientDistanceTolerance, v -> ServerConfig.INSTANCE.assemblyClientDistanceTolerance = v);
        addInt(assembly, eb, "Fast-Lift Ticks", "Charge ticks for blocks with a BlockEntity but non-full collision (chests, barrels...).", 1, 12000, ServerConfig.INSTANCE.fastLiftAssemblyTicks, SERVER_DEFAULTS.fastLiftAssemblyTicks, v -> ServerConfig.INSTANCE.fastLiftAssemblyTicks = v);
        addDouble(assembly, eb, "Pull Threshold (m)", "Distance the player must pull back to advance the assembly charge.", 0.0, 100.0, ServerConfig.INSTANCE.pullThreshold, SERVER_DEFAULTS.pullThreshold, v -> ServerConfig.INSTANCE.pullThreshold = v);
        addDouble(assembly, eb, "Pull Resistance Multiplier", "How much the pulled object resists the player's movement.", 0.0, 1000.0, ServerConfig.INSTANCE.pullResistanceMultiplier, SERVER_DEFAULTS.pullResistanceMultiplier, v -> ServerConfig.INSTANCE.pullResistanceMultiplier = v);
        addDouble(assembly, eb, "Movement Damping", "Dampens player movement while actively charging a detach.", 0.0, 1.0, ServerConfig.INSTANCE.assemblyMovementDamping, SERVER_DEFAULTS.assemblyMovementDamping, v -> ServerConfig.INSTANCE.assemblyMovementDamping = v);
        addDouble(assembly, eb, "Assembly Tether Stiffness", "Elastic stiffness when the player stretches beyond the max detach distance.", 0.0, 10.0, ServerConfig.INSTANCE.assemblyTetherStiffness, SERVER_DEFAULTS.assemblyTetherStiffness, v -> ServerConfig.INSTANCE.assemblyTetherStiffness = v);
        addDouble(assembly, eb, "Assembly Max Stretch Buffer (m)", "Absolute maximum distance before the assembly charge forcefully breaks.", 0.0, 20.0, ServerConfig.INSTANCE.assemblyMaxStretchBuffer, SERVER_DEFAULTS.assemblyMaxStretchBuffer, v -> ServerConfig.INSTANCE.assemblyMaxStretchBuffer = v);

        addBoolean(assembly, eb, "Enable Physics Placement", "Allows placing blocks directly as physics objects using the toggle key.", ServerConfig.INSTANCE.enablePhysicsBlockPlacement, SERVER_DEFAULTS.enablePhysicsBlockPlacement, v -> ServerConfig.INSTANCE.enablePhysicsBlockPlacement = v);
        server.addEntry(assembly.build());
    }

    private static void buildDisassembling(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder dis = eb.startSubCategory(Component.literal("Disassembling"));

        addBoolean(dis, eb, "Enable Impact Disassemble", "Slam a grabbed sublevel into a world block to disassemble it.", ServerConfig.INSTANCE.enableImpactDisassemble, SERVER_DEFAULTS.enableImpactDisassemble, v -> ServerConfig.INSTANCE.enableImpactDisassemble = v);
        addDouble(dis, eb, "Impact Force Threshold", "Minimum impact force (mass × delta velocity) required to trigger disassembly.", 1.0, 1000.0, ServerConfig.INSTANCE.impactForceThreshold, SERVER_DEFAULTS.impactForceThreshold, v -> ServerConfig.INSTANCE.impactForceThreshold = v);
        addDouble(dis, eb, "Impact Min Speed (blocks/tick)", "Minimum velocity the object must have to trigger an impact check. 0.5-0.8 recommended.", 0.0, 10.0, ServerConfig.INSTANCE.impactMinSpeed, SERVER_DEFAULTS.impactMinSpeed, v -> ServerConfig.INSTANCE.impactMinSpeed = v);
        addDouble(dis, eb, "Impact Contact Distance (blocks)", "Maximum distance from sublevel face to world block to register collision.", 0.0, 10.0, ServerConfig.INSTANCE.impactContactDistance, SERVER_DEFAULTS.impactContactDistance, v -> ServerConfig.INSTANCE.impactContactDistance = v);
        addDouble(dis, eb, "Slowdown Ratio", "Percentage of speed loss per tick required to detect impact (0.15 = 15%). Lower = more sensitive.", 0.01, 0.50, ServerConfig.INSTANCE.impactSlowdownRatio, SERVER_DEFAULTS.impactSlowdownRatio, v -> ServerConfig.INSTANCE.impactSlowdownRatio = v);
        addBoolean(dis, eb, "Require Intentional Throw", "Only disassemble when the player actively throws the object (faster than player). Prevents accidental disassembles.", ServerConfig.INSTANCE.impactRequireIntentionalThrow, SERVER_DEFAULTS.impactRequireIntentionalThrow, v -> ServerConfig.INSTANCE.impactRequireIntentionalThrow = v);
        addDouble(dis, eb, "Throw Speed Ratio", "How much faster the object must move compared to the player to count as a throw.", 1.0, 5.0, ServerConfig.INSTANCE.impactThrowSpeedRatio, SERVER_DEFAULTS.impactThrowSpeedRatio, v -> ServerConfig.INSTANCE.impactThrowSpeedRatio = v);
        addBoolean(dis, eb, "Break Replaceable Blocks", "Automatically break non-solid blocks (flowers, grass, buttons, saplings) before disassembling into the solid block behind them.", ServerConfig.INSTANCE.impactBreakReplaceableBlocks, SERVER_DEFAULTS.impactBreakReplaceableBlocks, v -> ServerConfig.INSTANCE.impactBreakReplaceableBlocks = v);
        addDouble(dis, eb, "Impact Rotation Tolerance (deg)", "Maximum rotation deviation from grid alignment (multiples of 90°) for impact disassembly.", 0.0, 45.0, ServerConfig.INSTANCE.impactRotationTolerance, SERVER_DEFAULTS.impactRotationTolerance, v -> ServerConfig.INSTANCE.impactRotationTolerance = v);
        addDouble(dis, eb, "Impact Position Tolerance (blocks)", "Maximum position deviation from block grid (integer coords) for impact disassembly.", 0.0, 0.5, ServerConfig.INSTANCE.impactPositionTolerance, SERVER_DEFAULTS.impactPositionTolerance, v -> ServerConfig.INSTANCE.impactPositionTolerance = v);

        addBoolean(dis, eb, "Enable Keybind Disassemble", "Allows pressing a keybind (default Q) to disassemble an aligned sublevel.", ServerConfig.INSTANCE.enableKeybindDisassemble, SERVER_DEFAULTS.enableKeybindDisassemble, v -> ServerConfig.INSTANCE.enableKeybindDisassemble = v);
        addBoolean(dis, eb, "Show Disassemble Messages", "Shows success or error messages in the action bar when disassembling.", ServerConfig.INSTANCE.showDisassembleMessages, SERVER_DEFAULTS.showDisassembleMessages, v -> ServerConfig.INSTANCE.showDisassembleMessages = v);
        addDouble(dis, eb, "Keybind Rotation Tolerance (deg)", "Maximum rotation deviation from grid alignment for keybind disassembly.", 0.0, 45.0, ServerConfig.INSTANCE.keybindRotationTolerance, SERVER_DEFAULTS.keybindRotationTolerance, v -> ServerConfig.INSTANCE.keybindRotationTolerance = v);
        addDouble(dis, eb, "Keybind Position Tolerance (blocks)", "Maximum position deviation from grid for keybind disassembly.", 0.0, 0.5, ServerConfig.INSTANCE.keybindPositionTolerance, SERVER_DEFAULTS.keybindPositionTolerance, v -> ServerConfig.INSTANCE.keybindPositionTolerance = v);

        addInt(dis, eb, "Max Disassemble Blocks", "Maximum number of blocks a grabbed structure can have to allow disassembly. 0 = unlimited.", 0, 50000, ServerConfig.INSTANCE.disassembleBlockLimit, SERVER_DEFAULTS.disassembleBlockLimit, v -> ServerConfig.INSTANCE.disassembleBlockLimit = v);
        server.addEntry(dis.build());
    }

    private static void buildEncumbrance(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder encumbrance = eb.startSubCategory(Component.literal("Encumbrance & Tethering"));
        addBoolean(encumbrance, eb, "Enable Encumbrance", "Enables player movement and camera penalties based on grabbed object mass.", ServerConfig.INSTANCE.enableEncumbrance, SERVER_DEFAULTS.enableEncumbrance, v -> ServerConfig.INSTANCE.enableEncumbrance = v);
        addDouble(encumbrance, eb, "Physics Gravity", "Gravity constant (m/s^2) used to convert mass into resting force (Weight = Mass × Gravity).", 0.1, 1000.0, ServerConfig.INSTANCE.physicsGravity, SERVER_DEFAULTS.physicsGravity, v -> ServerConfig.INSTANCE.physicsGravity = v);
        addDouble(encumbrance, eb, "Max Movement Penalty", "Maximum speed reduction (0.85 = 85% slower) when holding heavy objects.", 0.0, 1.0, ServerConfig.INSTANCE.maxMovementPenalty, SERVER_DEFAULTS.maxMovementPenalty, v -> ServerConfig.INSTANCE.maxMovementPenalty = v);
        addDouble(encumbrance, eb, "Jump Prevention", "Encumbrance ratio (0.0 to 1.0) at which jumping is disabled.", 0.0, 1.0, ServerConfig.INSTANCE.jumpPreventionThreshold, SERVER_DEFAULTS.jumpPreventionThreshold, v -> ServerConfig.INSTANCE.jumpPreventionThreshold = v);
        addDouble(encumbrance, eb, "Sneak Prevention", "Encumbrance ratio (0.0 to 1.0) at which sneaking is disabled.", 0.0, 1.0, ServerConfig.INSTANCE.sneakPreventionThreshold, SERVER_DEFAULTS.sneakPreventionThreshold, v -> ServerConfig.INSTANCE.sneakPreventionThreshold = v);
        addDouble(encumbrance, eb, "Max Camera Penalty", "Maximum camera sensitivity reduction when holding heavy objects.", 0.0, 1.0, ServerConfig.INSTANCE.maxCameraPenalty, SERVER_DEFAULTS.maxCameraPenalty, v -> ServerConfig.INSTANCE.maxCameraPenalty = v);
        addDouble(encumbrance, eb, "Scroll Speed Reduction", "Reduces scroll interpolation speed for heavy objects (0 = no reduction, 1 = max reduction).", 0.0, 1.0, ServerConfig.INSTANCE.scrollSpeedReduction, SERVER_DEFAULTS.scrollSpeedReduction, v -> ServerConfig.INSTANCE.scrollSpeedReduction = v);

        addBoolean(encumbrance, eb, "Enable Tether", "Enables the physical pull-back when stretching arms beyond the limit.", ServerConfig.INSTANCE.enablePhysicalTether, SERVER_DEFAULTS.enablePhysicalTether, v -> ServerConfig.INSTANCE.enablePhysicalTether = v);
        addDouble(encumbrance, eb, "Arm Stretch Tolerance", "Extra distance (m) arms can stretch before the tether pulls the player.", 0.0, 50.0, ServerConfig.INSTANCE.armStretchTolerance, SERVER_DEFAULTS.armStretchTolerance, v -> ServerConfig.INSTANCE.armStretchTolerance = v);
        addDouble(encumbrance, eb, "Tether Stiffness Base", "Base pull strength of the tether when stretched.", 0.0, 10.0, ServerConfig.INSTANCE.tetherStiffnessBase, SERVER_DEFAULTS.tetherStiffnessBase, v -> ServerConfig.INSTANCE.tetherStiffnessBase = v);
        addDouble(encumbrance, eb, "Tether Stiffness Multiplier", "Additional pull strength based on the object's encumbrance.", 0.0, 50.0, ServerConfig.INSTANCE.tetherStiffnessMultiplier, SERVER_DEFAULTS.tetherStiffnessMultiplier, v -> ServerConfig.INSTANCE.tetherStiffnessMultiplier = v);
        addDouble(encumbrance, eb, "Tether Vertical Smoothing", "Reduces vertical yanking to prevent the player from being launched into the air.", 0.0, 1.0, ServerConfig.INSTANCE.tetherVerticalSmoothing, SERVER_DEFAULTS.tetherVerticalSmoothing, v -> ServerConfig.INSTANCE.tetherVerticalSmoothing = v);
        addDouble(encumbrance, eb, "Recoil Velocity Threshold", "Minimum away-velocity required for the object to apply a recoil/pullback force.", 0.0, 1.0, ServerConfig.INSTANCE.recoilVelocityThreshold, SERVER_DEFAULTS.recoilVelocityThreshold, v -> ServerConfig.INSTANCE.recoilVelocityThreshold = v);
        addDouble(encumbrance, eb, "Tether Hard Escape Buffer", "Hard distance limit where the tether forcefully strips away-velocity to prevent escaping.", 0.0, 100.0, ServerConfig.INSTANCE.tetherHardEscapeBuffer, SERVER_DEFAULTS.tetherHardEscapeBuffer, v -> ServerConfig.INSTANCE.tetherHardEscapeBuffer = v);
        server.addEntry(encumbrance.build());
    }

    private static void buildExhaustion(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder exhaustion = eb.startSubCategory(Component.literal("Hunger & Exhaustion"));
        addBoolean(exhaustion, eb, "Enable Exhaustion", "Consumes player hunger based on physical effort when grabbing objects.", ServerConfig.INSTANCE.enableExhaustion, SERVER_DEFAULTS.enableExhaustion, v -> ServerConfig.INSTANCE.enableExhaustion = v);
        addDouble(exhaustion, eb, "Idle Drain Rate", "Exhaustion added per tick just by holding a heavy object in the air.", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionIdleRate, SERVER_DEFAULTS.exhaustionIdleRate, v -> ServerConfig.INSTANCE.exhaustionIdleRate = v);
        addDouble(exhaustion, eb, "Movement Drain Rate", "Exhaustion multiplier based on player walking/jumping speed while holding weight.", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionMovementRate, SERVER_DEFAULTS.exhaustionMovementRate, v -> ServerConfig.INSTANCE.exhaustionMovementRate = v);
        addDouble(exhaustion, eb, "Tension Drain Rate", "Extra exhaustion multiplier when pulling against a stuck heavy object.", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionTensionRate, SERVER_DEFAULTS.exhaustionTensionRate, v -> ServerConfig.INSTANCE.exhaustionTensionRate = v);
        addDouble(exhaustion, eb, "Force Drain Rate", "Exhaustion multiplier based on kinetic energy (moving heavy blocks fast).", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionForceRate, SERVER_DEFAULTS.exhaustionForceRate, v -> ServerConfig.INSTANCE.exhaustionForceRate = v);
        addDouble(exhaustion, eb, "Passive Force Threshold", "Weight in Newtons that is considered 'free' to hold without any effort.", 0.0, 1000.0, ServerConfig.INSTANCE.exhaustionPassiveThreshold, SERVER_DEFAULTS.exhaustionPassiveThreshold, v -> ServerConfig.INSTANCE.exhaustionPassiveThreshold = v);
        addDouble(exhaustion, eb, "Support Height Threshold", "Relative height below which the object is considered partially supported by the ground.", 0.0, 10.0, ServerConfig.INSTANCE.exhaustionSupportHeightThreshold, SERVER_DEFAULTS.exhaustionSupportHeightThreshold, v -> ServerConfig.INSTANCE.exhaustionSupportHeightThreshold = v);
        addDouble(exhaustion, eb, "Low Support Multiplier", "Reduces exhaustion effort when the object is resting on the ground.", 0.0, 1.0, ServerConfig.INSTANCE.exhaustionLowSupportMultiplier, SERVER_DEFAULTS.exhaustionLowSupportMultiplier, v -> ServerConfig.INSTANCE.exhaustionLowSupportMultiplier = v);
        addDouble(exhaustion, eb, "Max Over Stretch", "Maximum over-stretch distance factored into tension exhaustion.", 0.0, 100.0, ServerConfig.INSTANCE.exhaustionMaxOverStretch, SERVER_DEFAULTS.exhaustionMaxOverStretch, v -> ServerConfig.INSTANCE.exhaustionMaxOverStretch = v);
        addDouble(exhaustion, eb, "Kinetic Reference Speed", "Block speed (m/t) at which kinetic exhaustion reaches its maximum.", 0.1, 100.0, ServerConfig.INSTANCE.exhaustionKineticReferenceSpeed, SERVER_DEFAULTS.exhaustionKineticReferenceSpeed, v -> ServerConfig.INSTANCE.exhaustionKineticReferenceSpeed = v);
        addDouble(exhaustion, eb, "Vertical Weight Factor", "Multiplier for vertical player movement (jumping) when calculating exhaustion.", 0.0, 100.0, ServerConfig.INSTANCE.exhaustionVerticalWeightFactor, SERVER_DEFAULTS.exhaustionVerticalWeightFactor, v -> ServerConfig.INSTANCE.exhaustionVerticalWeightFactor = v);
        server.addEntry(exhaustion.build());
    }

    private static void buildSuspension(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder suspension = eb.startSubCategory(Component.literal("Physics Suspension"));
        addInt(suspension, eb, "Standing-on-Grab Suspend Ticks", "Ticks physics stays suspended after stepping off a grabbed object.", 0, 12000, ServerConfig.INSTANCE.standingOnGrabSuspendTicks, SERVER_DEFAULTS.standingOnGrabSuspendTicks, v -> ServerConfig.INSTANCE.standingOnGrabSuspendTicks = v);
        addDouble(suspension, eb, "Proximity Eye Suspend Distance", "Eye-to-block distance that suspends physics to prevent clipping.", 0.0, 1024.0, ServerConfig.INSTANCE.grabProximityEyeSuspendDistance, SERVER_DEFAULTS.grabProximityEyeSuspendDistance, v -> ServerConfig.INSTANCE.grabProximityEyeSuspendDistance = v);
        addDouble(suspension, eb, "Proximity Body Suspend Distance", "Foot-to-block distance that suspends physics to prevent clipping.", 0.0, 1024.0, ServerConfig.INSTANCE.grabProximityBodySuspendDistance, SERVER_DEFAULTS.grabProximityBodySuspendDistance, v -> ServerConfig.INSTANCE.grabProximityBodySuspendDistance = v);
        addDouble(suspension, eb, "Tension Suspend Threshold", "Tension distance that temporarily suspends physics motors to prevent jitter.", 0.0, 10000.0, ServerConfig.INSTANCE.tensionSuspendThreshold, SERVER_DEFAULTS.tensionSuspendThreshold, v -> ServerConfig.INSTANCE.tensionSuspendThreshold = v);
        addDouble(suspension, eb, "Tension Break Threshold", "Tension distance that forcefully breaks the grab.", 0.0, 10000.0, ServerConfig.INSTANCE.tensionBreakThreshold, SERVER_DEFAULTS.tensionBreakThreshold, v -> ServerConfig.INSTANCE.tensionBreakThreshold = v);
        addDouble(suspension, eb, "Creative Tension Suspend", "Tension suspend threshold in Creative Super Strength mode.", 0.0, 10000.0, ServerConfig.INSTANCE.creativeTensionSuspendThreshold, SERVER_DEFAULTS.creativeTensionSuspendThreshold, v -> ServerConfig.INSTANCE.creativeTensionSuspendThreshold = v);
        addDouble(suspension, eb, "Creative Tension Break", "Tension break threshold in Creative Super Strength mode.", 0.0, 10000.0, ServerConfig.INSTANCE.creativeTensionBreakThreshold, SERVER_DEFAULTS.creativeTensionBreakThreshold, v -> ServerConfig.INSTANCE.creativeTensionBreakThreshold = v);
        server.addEntry(suspension.build());
    }

    private static void buildVelocity(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder velocity = eb.startSubCategory(Component.literal("Player Velocity Limits"));
        addDouble(velocity, eb, "Max Velocity Y Up (m/t)", "Maximum upward velocity allowed while grabbing.", 0.0, 1000.0, ServerConfig.INSTANCE.maxPlayerVelocityYUp, SERVER_DEFAULTS.maxPlayerVelocityYUp, v -> ServerConfig.INSTANCE.maxPlayerVelocityYUp = v);
        addDouble(velocity, eb, "Max Velocity Y Down (m/t)", "Maximum downward velocity allowed while grabbing.", -1000.0, 0.0, ServerConfig.INSTANCE.maxPlayerVelocityYDown, SERVER_DEFAULTS.maxPlayerVelocityYDown, v -> ServerConfig.INSTANCE.maxPlayerVelocityYDown = v);
        addDouble(velocity, eb, "Max Velocity XZ (m/t)", "Maximum horizontal velocity allowed while grabbing.", 0.0, 1000.0, ServerConfig.INSTANCE.maxPlayerVelocityXZ, SERVER_DEFAULTS.maxPlayerVelocityXZ, v -> ServerConfig.INSTANCE.maxPlayerVelocityXZ = v);
        server.addEntry(velocity.build());
    }

    private static void buildCollision(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder colls = eb.startSubCategory(Component.literal("Collision Filters"));
        addBoolean(colls, eb, "[Grab] Ignore Self", "Prevents the grabbed object from colliding with the grabber.", ServerConfig.INSTANCE.ignoreCollisionsGrabSelf, SERVER_DEFAULTS.ignoreCollisionsGrabSelf, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabSelf = v);
        addBoolean(colls, eb, "[Grab] Ignore Other Players", "Prevents the grabbed object from colliding with other players.", ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers, SERVER_DEFAULTS.ignoreCollisionsGrabOtherPlayers, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers = v);
        addBoolean(colls, eb, "[Grab] Ignore Entities", "Prevents the grabbed object from colliding with mobs/entities.", ServerConfig.INSTANCE.ignoreCollisionsGrabEntities, SERVER_DEFAULTS.ignoreCollisionsGrabEntities, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabEntities = v);
        addBoolean(colls, eb, "[Grab] Ignore Everything", "Makes the grabbed object completely non-collidable (Ghost mode).", ServerConfig.INSTANCE.ignoreCollisionsGrabEverything, SERVER_DEFAULTS.ignoreCollisionsGrabEverything, v -> ServerConfig.INSTANCE.ignoreCollisionsGrabEverything = v);
        addDouble(colls, eb, "Self Collision Ignore Distance Sq", "Squared distance to force-ignore self-collision to prevent physics penetration loops.", 0.0, 1000.0, ServerConfig.INSTANCE.selfCollisionIgnoreDistanceSq, SERVER_DEFAULTS.selfCollisionIgnoreDistanceSq, v -> ServerConfig.INSTANCE.selfCollisionIgnoreDistanceSq = v);
        addBoolean(colls, eb, "[Rotate] Ignore Self", "Prevents collisions with the grabber while actively rotating.", ServerConfig.INSTANCE.ignoreCollisionsRotationSelf, SERVER_DEFAULTS.ignoreCollisionsRotationSelf, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationSelf = v);
        addBoolean(colls, eb, "[Rotate] Ignore Other Players", "Prevents collisions with other players while rotating.", ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers, SERVER_DEFAULTS.ignoreCollisionsRotationOtherPlayers, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers = v);
        addBoolean(colls, eb, "[Rotate] Ignore Entities", "Prevents collisions with mobs/entities while rotating.", ServerConfig.INSTANCE.ignoreCollisionsRotationEntities, SERVER_DEFAULTS.ignoreCollisionsRotationEntities, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationEntities = v);
        addBoolean(colls, eb, "[Rotate] Ignore Everything", "Makes the object completely non-collidable while rotating.", ServerConfig.INSTANCE.ignoreCollisionsRotationEverything, SERVER_DEFAULTS.ignoreCollisionsRotationEverything, v -> ServerConfig.INSTANCE.ignoreCollisionsRotationEverything = v);
        server.addEntry(colls.build());
    }

    private static void buildAdvanced(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder advanced = eb.startSubCategory(Component.literal("Advanced Physics Tuning"));
        addDouble(advanced, eb, "Stiffness", "Base linear spring stiffness. Higher = more rigid but can cause jitter.", 1.0, 10000000.0, ServerConfig.INSTANCE.stiffness, SERVER_DEFAULTS.stiffness, v -> ServerConfig.INSTANCE.stiffness = v);
        addDouble(advanced, eb, "Damping", "Base linear spring damping. Higher = less bouncing/overshoot.", 1.0, 10000000.0, ServerConfig.INSTANCE.damping, SERVER_DEFAULTS.damping, v -> ServerConfig.INSTANCE.damping = v);
        addDouble(advanced, eb, "Angular Damping", "Base angular (rotational) damping. Prevents objects from spinning endlessly.", 1.0, 10000000.0, ServerConfig.INSTANCE.angularDamping, SERVER_DEFAULTS.angularDamping, v -> ServerConfig.INSTANCE.angularDamping = v);
        addDouble(advanced, eb, "Creative Strength Multiplier", "Multiplies stiffness, damping and force in Creative Super Strength mode.", 1.0, 100000.0, ServerConfig.INSTANCE.creativeStrengthMultiplier, SERVER_DEFAULTS.creativeStrengthMultiplier, v -> ServerConfig.INSTANCE.creativeStrengthMultiplier = v);
        addDouble(advanced, eb, "Speed Stiffness Factor", "Factor scaling spring stiffness with player speed. Keeps object close when running.", 0.0, 100000.0, ServerConfig.INSTANCE.speedStiffnessMultiplierFactor, SERVER_DEFAULTS.speedStiffnessMultiplierFactor, v -> ServerConfig.INSTANCE.speedStiffnessMultiplierFactor = v);
        addDouble(advanced, eb, "Max Speed Stiffness Mult", "Cap on the speed-based stiffness multiplier.", 1.0, 10000.0, ServerConfig.INSTANCE.maxSpeedStiffnessMultiplier, SERVER_DEFAULTS.maxSpeedStiffnessMultiplier, v -> ServerConfig.INSTANCE.maxSpeedStiffnessMultiplier = v);
        addDouble(advanced, eb, "Base Angular Force Factor", "Fraction of maxForce used as minimum angular force.", 0.0, 1.0, ServerConfig.INSTANCE.baseAngularForceFactor, SERVER_DEFAULTS.baseAngularForceFactor, v -> ServerConfig.INSTANCE.baseAngularForceFactor = v);

        addDouble(advanced, eb, "Stable Angular Force: Mass Base", "Constant term in the stable angular force formula.", 0.0, 1000000.0, ServerConfig.INSTANCE.stableAngularForceMassBase, SERVER_DEFAULTS.stableAngularForceMassBase, v -> ServerConfig.INSTANCE.stableAngularForceMassBase = v);
        addDouble(advanced, eb, "Stable Angular Force: Mass Factor", "Mass coefficient in the stable angular force formula.", 0.0, 100000.0, ServerConfig.INSTANCE.stableAngularForceMassFactor, SERVER_DEFAULTS.stableAngularForceMassFactor, v -> ServerConfig.INSTANCE.stableAngularForceMassFactor = v);

        addDouble(advanced, eb, "Rotating Angular Stiffness: Base", "Base angular stiffness multiplier while actively rotating.", 0.0, 1000.0, ServerConfig.INSTANCE.rotatingAngularStiffnessBase, SERVER_DEFAULTS.rotatingAngularStiffnessBase, v -> ServerConfig.INSTANCE.rotatingAngularStiffnessBase = v);
        addDouble(advanced, eb, "Rotating Angular Stiffness: Range", "Range component of the rotation stiffness multiplier.", 0.0, 5000.0, ServerConfig.INSTANCE.rotatingAngularStiffnessRange, SERVER_DEFAULTS.rotatingAngularStiffnessRange, v -> ServerConfig.INSTANCE.rotatingAngularStiffnessRange = v);
        addDouble(advanced, eb, "Sway Angular Stiffness: Base", "Base angular stiffness multiplier when idle (sway mode).", 0.0, 1000.0, ServerConfig.INSTANCE.swayAngularStiffnessBase, SERVER_DEFAULTS.swayAngularStiffnessBase, v -> ServerConfig.INSTANCE.swayAngularStiffnessBase = v);
        addDouble(advanced, eb, "Sway Angular Stiffness: Range", "Range component of the idle sway stiffness multiplier.", 0.0, 5000.0, ServerConfig.INSTANCE.swayAngularStiffnessRange, SERVER_DEFAULTS.swayAngularStiffnessRange, v -> ServerConfig.INSTANCE.swayAngularStiffnessRange = v);

        addDouble(advanced, eb, "Min Angular Force (Small Objects)", "Minimum rotational torque applied to small/light objects to keep them responsive.", 0.0, 100.0, ServerConfig.INSTANCE.minAngularForceForSmallObjects, SERVER_DEFAULTS.minAngularForceForSmallObjects, v -> ServerConfig.INSTANCE.minAngularForceForSmallObjects = v);

        addDouble(advanced, eb, "Stabilization Exponent", "Exponent applied to stabilization factors for non-linear rigidity curves.", 0.1, 10.0, ServerConfig.INSTANCE.stabilizationExponent, SERVER_DEFAULTS.stabilizationExponent, v -> ServerConfig.INSTANCE.stabilizationExponent = v);
        addDouble(advanced, eb, "Creative Max Motor Force", "Maximum numeric force limit for physics motors in Creative mode to prevent engine overflow.", 1.0, 1e15, ServerConfig.INSTANCE.creativeMaxMotorForce, SERVER_DEFAULTS.creativeMaxMotorForce, v -> ServerConfig.INSTANCE.creativeMaxMotorForce = v);

        addDouble(advanced, eb, "Heavy Object Mass Curve", "Multiplier for the logarithmic mass curve. Makes heavy objects feel heavier.", 0.0, 100.0, ServerConfig.INSTANCE.heavyObjectMassCurveMultiplier, SERVER_DEFAULTS.heavyObjectMassCurveMultiplier, v -> ServerConfig.INSTANCE.heavyObjectMassCurveMultiplier = v);
        addDouble(advanced, eb, "Heavy Max Force Factor", "Allows motors to pull slightly harder on massive objects to prevent permanent stalling.", 0.0, 10.0, ServerConfig.INSTANCE.heavyObjectMaxForceFactor, SERVER_DEFAULTS.heavyObjectMaxForceFactor, v -> ServerConfig.INSTANCE.heavyObjectMaxForceFactor = v);
        addDouble(advanced, eb, "Grab Elasticity Stiffness", "Reduces linear stiffness slightly to simulate an organic, elastic grab.", 0.0, 10.0, ServerConfig.INSTANCE.grabElasticityStiffnessFactor, SERVER_DEFAULTS.grabElasticityStiffnessFactor, v -> ServerConfig.INSTANCE.grabElasticityStiffnessFactor = v);
        addDouble(advanced, eb, "Grab Elasticity Damping", "Reduces linear damping slightly to complement grab elasticity.", 0.0, 10.0, ServerConfig.INSTANCE.grabElasticityDampingFactor, SERVER_DEFAULTS.grabElasticityDampingFactor, v -> ServerConfig.INSTANCE.grabElasticityDampingFactor = v);
        addDouble(advanced, eb, "Sway Stiffness Edge Factor", "Base sway multiplier when holding objects from edges (0 = Free Gravity Pivot).", 0.0, 10.0, ServerConfig.INSTANCE.swayStiffnessEdgeFactor, SERVER_DEFAULTS.swayStiffnessEdgeFactor, v -> ServerConfig.INSTANCE.swayStiffnessEdgeFactor = v);
        addDouble(advanced, eb, "Sway Stiffness Edge Range", "Range sway multiplier when holding objects from edges.", 0.0, 10.0, ServerConfig.INSTANCE.swayStiffnessEdgeRangeFactor, SERVER_DEFAULTS.swayStiffnessEdgeRangeFactor, v -> ServerConfig.INSTANCE.swayStiffnessEdgeRangeFactor = v);
        addDouble(advanced, eb, "Free Pivot Damping", "Friction applied when the object hangs freely to prevent endless swinging.", 0.0, 100.0, ServerConfig.INSTANCE.freePivotDampingMultiplier, SERVER_DEFAULTS.freePivotDampingMultiplier, v -> ServerConfig.INSTANCE.freePivotDampingMultiplier = v);

        server.addEntry(advanced.build());
    }

    private static void buildMovementPenalty(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder movementPenalty = eb.startSubCategory(Component.literal("Movement Speed Penalty"));
        addDouble(movementPenalty, eb, "Base Movement Penalty", "Base movement speed reduction when holding any object.", 0.0, 1.0, ServerConfig.INSTANCE.baseMovementPenalty, SERVER_DEFAULTS.baseMovementPenalty, v -> ServerConfig.INSTANCE.baseMovementPenalty = v);
        addDouble(movementPenalty, eb, "Weight Penalty Multiplier", "How much object weight increases movement penalty.", 0.0, 5.0, ServerConfig.INSTANCE.weightPenaltyMultiplier, SERVER_DEFAULTS.weightPenaltyMultiplier, v -> ServerConfig.INSTANCE.weightPenaltyMultiplier = v);
        addDouble(movementPenalty, eb, "Tension Penalty Multiplier", "How much pulling against tension increases movement penalty.", 0.0, 5.0, ServerConfig.INSTANCE.tensionPenaltyMultiplier, SERVER_DEFAULTS.tensionPenaltyMultiplier, v -> ServerConfig.INSTANCE.tensionPenaltyMultiplier = v);
        addDouble(movementPenalty, eb, "Kinetic Penalty Multiplier", "How much block movement (falling/dragging) increases movement penalty.", 0.0, 5.0, ServerConfig.INSTANCE.kineticPenaltyMultiplier, SERVER_DEFAULTS.kineticPenaltyMultiplier, v -> ServerConfig.INSTANCE.kineticPenaltyMultiplier = v);
        addDouble(movementPenalty, eb, "Min Speed While Grabbing", "Minimum movement speed allowed while grabbing (prevents complete freeze).", 0.0, 1.0, ServerConfig.INSTANCE.minSpeedWhileGrabbing, SERVER_DEFAULTS.minSpeedWhileGrabbing, v -> ServerConfig.INSTANCE.minSpeedWhileGrabbing = v);
        addDouble(movementPenalty, eb, "Tension Penalty Start Offset", "Tension distance before movement penalty starts applying.", 0.0, 100.0, ServerConfig.INSTANCE.tensionPenaltyStartOffset, SERVER_DEFAULTS.tensionPenaltyStartOffset, v -> ServerConfig.INSTANCE.tensionPenaltyStartOffset = v);
        addDouble(movementPenalty, eb, "Tension Penalty Max Distance", "Tension distance at which movement penalty reaches its maximum.", 0.1, 1000.0, ServerConfig.INSTANCE.tensionPenaltyMaxDistance, SERVER_DEFAULTS.tensionPenaltyMaxDistance, v -> ServerConfig.INSTANCE.tensionPenaltyMaxDistance = v);
        addDouble(movementPenalty, eb, "Kinetic Penalty Reference Speed", "Block speed at which kinetic movement penalty reaches its maximum.", 0.1, 100.0, ServerConfig.INSTANCE.kineticPenaltyReferenceSpeed, SERVER_DEFAULTS.kineticPenaltyReferenceSpeed, v -> ServerConfig.INSTANCE.kineticPenaltyReferenceSpeed = v);
        server.addEntry(movementPenalty.build());
    }

    private static void buildLeadPrediction(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder leadPrediction = eb.startSubCategory(Component.literal("Lead Prediction"));
        addDouble(leadPrediction, eb, "Lead Velocity Threshold", "Player speed threshold to activate target prediction.", 0.0, 100.0, ServerConfig.INSTANCE.leadVelocityThreshold, SERVER_DEFAULTS.leadVelocityThreshold, v -> ServerConfig.INSTANCE.leadVelocityThreshold = v);
        addDouble(leadPrediction, eb, "Lead Prediction Factor", "Multiplier for predicting the anchor target ahead of the player's movement.", 0.0, 100.0, ServerConfig.INSTANCE.leadPredictionFactor, SERVER_DEFAULTS.leadPredictionFactor, v -> ServerConfig.INSTANCE.leadPredictionFactor = v);
        addDouble(leadPrediction, eb, "Lead Downward Clamp", "Clamps downward prediction to prevent the object from being dragged into the floor.", -100.0, 0.0, ServerConfig.INSTANCE.leadDownwardClamp, SERVER_DEFAULTS.leadDownwardClamp, v -> ServerConfig.INSTANCE.leadDownwardClamp = v);
        server.addEntry(leadPrediction.build());
    }

    private static void buildCompat(ConfigCategory server, ConfigEntryBuilder eb) {
        SubCategoryBuilder compat = eb.startSubCategory(Component.literal("Compatibility"));

        addBoolean(compat, eb, "[Sable: Ragdolls] Allow Grabbing Player Ragdolls",
                "Allows grabbing player ragdolls.",
                ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingPlayerRagdolls,
                SERVER_DEFAULTS.sableRagdollsCompatAllowGrabbingPlayerRagdolls,
                v -> ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingPlayerRagdolls = v);

        addBoolean(compat, eb, "[Sable: Ragdolls] Allow Grabbing Mob Ragdolls",
                "Allows grabbing mob ragdolls.",
                ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingMobRagdolls,
                SERVER_DEFAULTS.sableRagdollsCompatAllowGrabbingMobRagdolls,
                v -> ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingMobRagdolls = v);

        addDouble(compat, eb, "[Sable: Ragdolls] Mob Ragdoll Max Size",
                "Maximum mob size (bounding box width/height) that can be grabbed.",
                0.1, 1000.0,
                ServerConfig.INSTANCE.sableRagdollsCompatMobRagdollMaxSize,
                SERVER_DEFAULTS.sableRagdollsCompatMobRagdollMaxSize,
                v -> ServerConfig.INSTANCE.sableRagdollsCompatMobRagdollMaxSize = v);

        server.addEntry(compat.build());
    }

    private static void buildClientCategories(ConfigCategory client, ConfigEntryBuilder eb) {
        buildInput(client, eb);
        buildRender(client, eb);
        buildHud(client, eb);
        buildAssemblyClient(client, eb);
    }

    private static void buildInput(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder inp = eb.startSubCategory(Component.literal("Input & Controls"));

        addDouble(inp, eb, "Vertical Rotation Sensitivity", "Mouse sensitivity for rotating grabbed sub-levels up/down.", 0.1, 2.0, ClientConfig.INSTANCE.verticalRotationSensitivity, CLIENT_DEFAULTS.verticalRotationSensitivity, v -> ClientConfig.INSTANCE.verticalRotationSensitivity = v);
        addDouble(inp, eb, "Horizontal Rotation Sensitivity", "Mouse sensitivity for rotating grabbed sub-levels left/right.", 0.1, 2.0, ClientConfig.INSTANCE.horizontalRotationSensitivity, CLIENT_DEFAULTS.horizontalRotationSensitivity, v -> ClientConfig.INSTANCE.horizontalRotationSensitivity = v);
        addDouble(inp, eb, "Scroll Sensitivity", "Speed at which the distance adjusts using the mouse wheel.", 0.1, 2.0, ClientConfig.INSTANCE.scrollDistanceSensitivity, CLIENT_DEFAULTS.scrollDistanceSensitivity, v -> ClientConfig.INSTANCE.scrollDistanceSensitivity = v);
        addBoolean(inp, eb, "Invert Vertical Rotation", "Inverts the Y-axis when rotating.", ClientConfig.INSTANCE.invertVerticalRotation, CLIENT_DEFAULTS.invertVerticalRotation, v -> ClientConfig.INSTANCE.invertVerticalRotation = v);
        addBoolean(inp, eb, "Invert Horizontal Rotation", "Inverts the X-axis when rotating.", ClientConfig.INSTANCE.invertHorizontalRotation, CLIENT_DEFAULTS.invertHorizontalRotation, v -> ClientConfig.INSTANCE.invertHorizontalRotation = v);
        addBoolean(inp, eb, "Default Center Pivot", "If true, grabbed sub-levels rotate around their center of mass by default instead of the grab point.", ClientConfig.INSTANCE.rotateAroundCenter, CLIENT_DEFAULTS.rotateAroundCenter, v -> ClientConfig.INSTANCE.rotateAroundCenter = v);

        client.addEntry(inp.build());
    }

    private static void buildRender(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder rnd = eb.startSubCategory(Component.literal("Animation & Rendering"));

        addBoolean(rnd, eb, "Hide Global Arms", "Forces First-Person and Third-Person arms to be hidden.", ClientConfig.INSTANCE.hideHandsWhileGrabbing, CLIENT_DEFAULTS.hideHandsWhileGrabbing, v -> ClientConfig.INSTANCE.hideHandsWhileGrabbing = v);
        addBoolean(rnd, eb, "Hide First Person Arms", "Hides the custom outstretched arms in first person when grabbing.", ClientConfig.INSTANCE.hideFirstPersonArms, CLIENT_DEFAULTS.hideFirstPersonArms, v -> ClientConfig.INSTANCE.hideFirstPersonArms = v);
        addBoolean(rnd, eb, "Hide Third Person Arms", "Hides the custom outstretched arms in third person when grabbing.", ClientConfig.INSTANCE.hideThirdPersonArms, CLIENT_DEFAULTS.hideThirdPersonArms, v -> ClientConfig.INSTANCE.hideThirdPersonArms = v);
        addDouble(rnd, eb, "Arm Transition Speed", "How fast the arms raise when grabbing a block.", 0.05, 0.5, ClientConfig.INSTANCE.armTransitionSpeed, CLIENT_DEFAULTS.armTransitionSpeed, v -> ClientConfig.INSTANCE.armTransitionSpeed = v);

        client.addEntry(rnd.build());
    }

    private static void buildAssemblyClient(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder asm = eb.startSubCategory(Component.literal("Client Assembly"));

        addDouble(asm, eb, "Visual Shake Multiplier", "How intensely the screen/arms shake while tearing a block from the world.", 0.0, 0.2, ClientConfig.INSTANCE.assemblyShakeMultiplier, CLIENT_DEFAULTS.assemblyShakeMultiplier, v -> ClientConfig.INSTANCE.assemblyShakeMultiplier = v);
        addBoolean(asm, eb, "Prevent Assembly When Mining", "Stops barehanded assembly from triggering if the block is actively being mined.", ClientConfig.INSTANCE.preventAssemblyWhenMining, CLIENT_DEFAULTS.preventAssemblyWhenMining, v -> ClientConfig.INSTANCE.preventAssemblyWhenMining = v);
        addDouble(asm, eb, "Mining Prevent Threshold", "Mining progress threshold required to cancel an accidental assembly attempt.", 0.01, 1.0, ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold, CLIENT_DEFAULTS.barehandedAssemblyMiningThreshold, v -> ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold = v);

        client.addEntry(asm.build());
    }

    private static void buildHud(ConfigCategory client, ConfigEntryBuilder eb) {
        SubCategoryBuilder hud = eb.startSubCategory(Component.literal("HUD"));

        addBoolean(hud, eb, "Hide Grab HUD", "Hides rotation/mass HUD when grabbing.", ClientConfig.INSTANCE.hideGrabHud, CLIENT_DEFAULTS.hideGrabHud, v -> ClientConfig.INSTANCE.hideGrabHud = v);
        addBoolean(hud, eb, "Hide Placement Overlay", "Hides permanent Physics Placement HUD overlay.", ClientConfig.INSTANCE.hidePhysicsPlacementOverlay, CLIENT_DEFAULTS.hidePhysicsPlacementOverlay, v -> ClientConfig.INSTANCE.hidePhysicsPlacementOverlay = v);
        addBoolean(hud, eb, "Show Placement Message", "Shows temporary message when toggling physics placement.", ClientConfig.INSTANCE.showPhysicsPlacementMessage, CLIENT_DEFAULTS.showPhysicsPlacementMessage, v -> ClientConfig.INSTANCE.showPhysicsPlacementMessage = v);

        client.addEntry(hud.build());
    }

    private static void addDouble(SubCategoryBuilder sub, ConfigEntryBuilder eb, String title, String tooltip, double min, double max, double current, double defaultValue, java.util.function.Consumer<Double> save) {
        var b = eb.startDoubleField(Component.literal(title), current)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setSaveConsumer(save);
        if (tooltip != null && !tooltip.isEmpty()) b.setTooltip(Component.literal(tooltip));
        sub.add(b.build());
    }

    private static void addInt(SubCategoryBuilder sub, ConfigEntryBuilder eb, String title, String tooltip, int min, int max, int current, int defaultValue, java.util.function.Consumer<Integer> save) {
        var b = eb.startIntField(Component.literal(title), current)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setSaveConsumer(save);
        if (tooltip != null && !tooltip.isEmpty()) b.setTooltip(Component.literal(tooltip));
        sub.add(b.build());
    }

    private static void addBoolean(SubCategoryBuilder sub, ConfigEntryBuilder eb, String title, String tooltip, boolean current, boolean defaultValue, java.util.function.Consumer<Boolean> save) {
        var b = eb.startBooleanToggle(Component.literal(title), current)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(save);
        if (tooltip != null && !tooltip.isEmpty()) b.setTooltip(Component.literal(tooltip));
        sub.add(b.build());
    }
}