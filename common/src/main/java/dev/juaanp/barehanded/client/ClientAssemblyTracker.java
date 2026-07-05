package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.mixin.accesor.MultiPlayerGameModeAccessor;
import dev.juaanp.barehanded.platform.Services;
import dev.juaanp.barehanded.util.AssemblyBehaviorHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ClientAssemblyTracker {
    public static int assemblyChargeTicks = 0;
    public static BlockPos assemblyTargetPos = null;
    public static SubLevel assemblySubLevel = null;
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
        assemblySubLevel = null;
        initialAssemblyDistance = 0.0;
        isPulling = false;
        smoothPullIntensity = 0.0F;
        smoothPullIntensityInitialized = false;
    }

    private static Vec3 getTargetGlobalCenter() {
        if (assemblySubLevel != null && !assemblySubLevel.isRemoved()) {
            Vector3d localCenter = new Vector3d(assemblyTargetPos.getX() + 0.5, assemblyTargetPos.getY() + 0.5, assemblyTargetPos.getZ() + 0.5);
            Vector3d globalCenter = assemblySubLevel.logicalPose().transformPosition(localCenter);
            return new Vec3(globalCenter.x, globalCenter.y, globalCenter.z);
        }
        return Vec3.atCenterOf(assemblyTargetPos);
    }

    public static void tickAssemblyTether(Minecraft mc) {
        if (!isActive() || assemblyTargetPos == null || mc.player == null) return;

        Vec3 targetCenter = getTargetGlobalCenter();
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

        Vec3 targetCenter = getTargetGlobalCenter();
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

    public static boolean tryStartAssembly(Minecraft mc, BlockHitResult blockHit, boolean isSneaking, boolean isAltDown) {
        if (!ServerConfig.INSTANCE.enableBarehandedAssembly || !isSneaking) return false;

        BlockPos currentPos = blockHit.getBlockPos();
        Vector3d hitPos = new Vector3d(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
        SubLevel subLevel = Sable.HELPER.getContaining(mc.level, hitPos);

        if (subLevel != null && (!isAltDown || !ServerConfig.INSTANCE.enableRipOffBlocks)) return false;

        Level levelToUse = subLevel != null ? subLevel.getLevel() : mc.level;

        Vec3 blockCenter;
        if (subLevel != null) {
            Vector3d localCenter = new Vector3d(currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5);
            Vector3d globalCenter = subLevel.logicalPose().transformPosition(localCenter);
            blockCenter = new Vec3(globalCenter.x, globalCenter.y, globalCenter.z);
        } else {
            blockCenter = Vec3.atCenterOf(currentPos);
        }

        double distanceToHit = mc.player.getEyePosition().distanceTo(blockCenter);
        if (distanceToHit > ServerConfig.INSTANCE.barehandedAssemblyMaxDistance) return false;

        BlockState state = levelToUse.getBlockState(currentPos);
        if (AssemblyBehaviorHelper.isIgnored(levelToUse, currentPos, state)) return false;

        if (ClientConfig.INSTANCE.preventAssemblyWhenMining && mc.gameMode != null) {
            float miningProgress = ((MultiPlayerGameModeAccessor) mc.gameMode).getDestroyProgress();
            if (miningProgress > ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold) return false;
        }

        assemblySubLevel = subLevel;
        assemblyTargetPos = currentPos;
        assemblyChargeTicks = 1;
        isPulling = false;
        initialAssemblyDistance = distanceToHit;

        var blocksToAssemble = AssemblyBehaviorHelper.getConnectedBlocks(levelToUse, currentPos);
        currentRequiredAssemblyTicks = AssemblyBehaviorHelper.calculateAssemblyTicks(mc.player, levelToUse, blocksToAssemble);

        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        return true;
    }
}