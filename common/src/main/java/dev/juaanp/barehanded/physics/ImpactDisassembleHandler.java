package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.physics.impact.ImpactFaceDetector;
import dev.juaanp.barehanded.physics.impact.ImpactResult;
import dev.juaanp.barehanded.physics.impact.PlayerIntentValidator;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ImpactDisassembleHandler {
    private static final Map<UUID, Vector3d> previousPositions = new HashMap<>();
    private static final Map<UUID, Vector3d> calculatedVelocities = new HashMap<>();
    private static final double M_S_TO_B_T = 1.0 / 20.0;
    private static final double TICKS_PER_SECOND = 20.0;

    public static void checkImpact(ServerPlayer player, ServerSubLevel subLevel, GrabSession grab) {
        if (!ServerConfig.INSTANCE.enableImpactDisassemble) return;

        GrabSession currentGrab = ServerGrabManager.getGrabSession(player);
        if (currentGrab == null || currentGrab.subLevel != subLevel) {
            cleanup(subLevel.getUniqueId());
            return;
        }

        grab.impactTicks++;

        UUID subLevelId = subLevel.getUniqueId();
        Vector3d currentPosition = subLevel.logicalPose().position();
        Vector3d previousPosition = previousPositions.get(subLevelId);

        if (previousPosition == null || grab.impactTicks == 1) {
            previousPositions.put(subLevelId, new Vector3d(currentPosition));
            calculatedVelocities.remove(subLevelId);
            return;
        }

        Vector3d previousVelocity = calculatedVelocities.get(subLevelId);
        Vector3d positionDelta = new Vector3d(currentPosition).sub(previousPosition);
        Vector3d currentVelocity = positionDelta.mul(TICKS_PER_SECOND);

        previousPositions.put(subLevelId, new Vector3d(currentPosition));
        calculatedVelocities.put(subLevelId, new Vector3d(currentVelocity));

        if (grab.impactTicks < 10) return;
        if (previousVelocity == null) return;

        if (!meetsImpactCriteria(previousVelocity, currentVelocity, subLevel)) return;
        if (!PlayerIntentValidator.wasIntentionalImpact(player, subLevel, previousVelocity)) return;

        ServerLevel level = (ServerLevel) player.level();

        ServerSubLevel targetShip = DisassembleHandler.findTargetSubLevel(level, subLevel);
        if (targetShip != null) {
            boolean success = DisassembleHandler.disassembleIntoSubLevel(level, subLevel, targetShip, player);
            if (success) {
                ServerGrabManager.stopGrabbing(player.getUUID());
                cleanup(subLevel.getUniqueId());
            }
            return;
        }

        if (grab.isAltDown) {
            return;
        }

        Vec3 approachDir = PlayerIntentValidator.getApproachDirection(previousVelocity);
        Optional<ImpactResult> impact = ImpactFaceDetector.detectImpact(level, subLevel, approachDir);
        if (impact.isEmpty()) return;

        ImpactResult result = impact.get();

        if (!DisassembleHandler.isAlignedToGrid(subLevel,
                ServerConfig.INSTANCE.impactRotationTolerance,
                ServerConfig.INSTANCE.impactPositionTolerance)) return;

        int limit = ServerConfig.INSTANCE.disassembleBlockLimit;
        if (limit > 0 && DisassembleHandler.getBlockCount(subLevel) > limit) {
            return;
        }

        performDisassemble(level, player, subLevel, result);
    }

    private static boolean meetsImpactCriteria(Vector3d previousVelocity, Vector3d currentVelocity, ServerSubLevel subLevel) {
        double prevSpeedMS = previousVelocity.length();
        double currSpeedMS = currentVelocity.length();
        double prevSpeedBT = prevSpeedMS * M_S_TO_B_T;

        if (prevSpeedBT < ServerConfig.INSTANCE.impactMinSpeed) return false;

        double speedReductionMS = prevSpeedMS - currSpeedMS;
        boolean hasBounced = currentVelocity.dot(previousVelocity) < 0;
        boolean hasSlowedDown = speedReductionMS > prevSpeedMS * ServerConfig.INSTANCE.impactSlowdownRatio;

        if (!hasBounced && !hasSlowedDown) return false;

        double deltaV_MS = hasBounced ? (prevSpeedMS + currSpeedMS) : Math.max(speedReductionMS, 0);
        double impactForce = subLevel.getMassTracker().getMass() * deltaV_MS;

        return impactForce >= ServerConfig.INSTANCE.impactForceThreshold;
    }

    private static void performDisassemble(ServerLevel level, ServerPlayer player,
                                           ServerSubLevel subLevel, ImpactResult impact) {
        DisassembleHandler.PlacementResult placement = DisassembleHandler.computePlacement(
                subLevel, impact.face(), impact.worldBlock()
        );

        BlockState impactedBlock = level.getBlockState(impact.worldBlock());
        BlockState placedBlockState = DisassembleHandler.getFirstBlockState(subLevel);

        boolean success = DisassembleHandler.disassemble(
                level, subLevel,
                placement.plotAnchor(), placement.disassemblyGoal(), placement.rotation(),
                impactedBlock, impact.face(), placedBlockState
        );

        if (success) {
            ServerGrabManager.stopGrabbing(player.getUUID());
            cleanup(subLevel.getUniqueId());
        }
    }

    public static void cleanup(UUID subLevelId) {
        previousPositions.remove(subLevelId);
        calculatedVelocities.remove(subLevelId);
    }
}