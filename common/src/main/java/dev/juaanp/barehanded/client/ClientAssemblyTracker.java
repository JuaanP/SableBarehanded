package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.mixin.accesor.MultiPlayerGameModeAccessor;
import dev.juaanp.barehanded.platform.Services;
import dev.juaanp.barehanded.util.AssemblyBehaviorHelper;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ClientAssemblyTracker {
    public static int assemblyChargeTicks = 0;
    public static BlockPos assemblyTargetPos = null;
    public static int currentRequiredAssemblyTicks = 20;
    public static double initialAssemblyDistance = 0.0;
    public static boolean isPulling = false;
    public static float smoothPullIntensity = 0.0F;
    public static boolean smoothPullIntensityInitialized = false;

    public static boolean isActive() {
        return assemblyTargetPos != null;
    }

    public static void reset() {
        assemblyChargeTicks = 0;
        assemblyTargetPos = null;
        initialAssemblyDistance = 0.0;
        isPulling = false;
        smoothPullIntensity = 0.0F;
        smoothPullIntensityInitialized = false;
    }

    public static void tickAssemblyTether(Minecraft mc) {
        if (!isActive() || assemblyTargetPos == null || mc.player == null) return;

        Vec3 targetCenter = Vec3.atCenterOf(assemblyTargetPos);
        Vec3 playerEye = mc.player.getEyePosition();
        double currentDist = playerEye.distanceTo(targetCenter);
        double maxDist = ServerConfig.INSTANCE.barehandedAssemblyMaxDistance;
        double buffer = ServerConfig.INSTANCE.assemblyMaxStretchBuffer;

        if (currentDist > maxDist + buffer) {
            reset();
            return;
        }

        if (currentDist > maxDist) {
            smoothPullIntensity = 1.0F;
            isPulling = true;

            Vec3 toTarget = targetCenter.subtract(playerEye).normalize();
            Vec3 awayDir = toTarget.scale(-1.0);
            Vec3 currentVel = mc.player.getDeltaMovement();
            double awaySpeed = currentVel.dot(awayDir);

            if (awaySpeed > 0) {
                Vec3 newVel = currentVel.subtract(awayDir.scale(awaySpeed));
                double overStretch = currentDist - maxDist;
                newVel = newVel.add(toTarget.scale(overStretch * ServerConfig.INSTANCE.assemblyTetherStiffness));
                mc.player.setDeltaMovement(newVel);
            }
        }
    }

    public static void tickCharge(Minecraft mc, boolean isSneaking) {
        if (!isSneaking) {
            reset();
            return;
        }

        Vec3 targetCenter = Vec3.atCenterOf(assemblyTargetPos);
        Vec3 playerEyePos = mc.player.getEyePosition();
        double currentDist = playerEyePos.distanceTo(targetCenter);

        double stretch = currentDist - initialAssemblyDistance;
        boolean requiresPulling = currentRequiredAssemblyTicks > 2;

        float targetPull = 0.0F;
        boolean shouldAdvanceCharge = false;

        if (!requiresPulling) {
            targetPull = 1.0F;
            shouldAdvanceCharge = true;
        } else {
            if (stretch > ServerConfig.INSTANCE.pullThreshold) {
                targetPull = 1.0F;
                shouldAdvanceCharge = true;
            } else if (stretch > 0.05) {
                targetPull = (float) (stretch / ServerConfig.INSTANCE.pullThreshold);
                shouldAdvanceCharge = true;
            }
        }

        if (!smoothPullIntensityInitialized) {
            smoothPullIntensity = targetPull;
            smoothPullIntensityInitialized = true;
        } else {
            smoothPullIntensity += (targetPull - smoothPullIntensity) * 0.15F;
        }
        isPulling = smoothPullIntensity > 0.05F;

        if (shouldAdvanceCharge) {
            assemblyChargeTicks++;
        }

        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();

        if (assemblyChargeTicks >= currentRequiredAssemblyTicks) {
            Services.NETWORK.sendAssembleGrabRequest(assemblyTargetPos);
            ClientGrabSession.startWaiting();
            reset();
        }
    }

    public static boolean tryStartAssembly(Minecraft mc, BlockHitResult blockHit, boolean isSneaking) {
        if (!ServerConfig.INSTANCE.enableBarehandedAssembly || !isSneaking) return false;

        BlockPos currentPos = blockHit.getBlockPos();
        Vec3 blockCenter = Vec3.atCenterOf(currentPos);
        double distanceToHit = mc.player.getEyePosition().distanceTo(blockCenter);

        if (distanceToHit > ServerConfig.INSTANCE.barehandedAssemblyMaxDistance) return false;

        BlockState state = mc.level.getBlockState(currentPos);
        if (AssemblyBehaviorHelper.isIgnored(mc.level, currentPos, state)) return false;

        if (ClientConfig.INSTANCE.preventAssemblyWhenMining && mc.gameMode != null) {
            float miningProgress = ((MultiPlayerGameModeAccessor) mc.gameMode).getDestroyProgress();
            if (miningProgress > ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold) return false;
        }

        Vector3d hitPos = new Vector3d(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
        if (Sable.HELPER.getContaining(mc.level, hitPos) != null) return false;

        assemblyTargetPos = currentPos;
        assemblyChargeTicks = 1;
        isPulling = false;
        initialAssemblyDistance = distanceToHit;

        var blocksToAssemble = AssemblyBehaviorHelper.getConnectedBlocks(mc.level, currentPos);
        currentRequiredAssemblyTicks = AssemblyBehaviorHelper.calculateAssemblyTicks(mc.player, mc.level, blocksToAssemble);

        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        return true;
    }
}