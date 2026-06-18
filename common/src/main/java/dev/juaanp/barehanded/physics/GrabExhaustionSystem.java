package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class GrabExhaustionSystem {

    public static void applyExhaustion(Player player, GrabSession grab, double tension, double actualMaxForce, boolean suspendPhysics) {
        if (!ServerConfig.INSTANCE.enableExhaustion || player.isCreative() || player.isSpectator() || player.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;

        double mass = grab.subLevel.getMassTracker().getMass();
        double objectWeight = mass * ServerConfig.INSTANCE.physicsGravity;

        double freeWeightN = ServerConfig.INSTANCE.exhaustionPassiveThreshold;
        double effectiveWeight = Math.max(0.0, objectWeight - freeWeightN);
        double weightRatio = Mth.clamp(effectiveWeight / Math.max(1.0, actualMaxForce - freeWeightN), 0.0, 1.0);

        double supportFactor = suspendPhysics ? 0.0 : 1.0;
        Vector3d currentActualGrabBlockPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(grab.localPivot));
        Vec3 blockPos = new Vec3(currentActualGrabBlockPos.x, currentActualGrabBlockPos.y, currentActualGrabBlockPos.z);
        double relativeHeight = blockPos.y - player.getY();
        if (relativeHeight < ServerConfig.INSTANCE.exhaustionSupportHeightThreshold && supportFactor > 0) {
            supportFactor *= ServerConfig.INSTANCE.exhaustionLowSupportMultiplier;
        }

        double baseEffort = weightRatio * supportFactor;

        double blockSpeed = grab.subLevel.latestLinearVelocity.length();
        double kineticRatio = Mth.clamp(blockSpeed / ServerConfig.INSTANCE.exhaustionKineticReferenceSpeed, 0.0, 1.0);
        double kineticEffort = kineticRatio * weightRatio;

        double tensionEffort = 0.0;
        double maxAllowedDist = grab.distance + ServerConfig.INSTANCE.armStretchTolerance;
        if (tension > maxAllowedDist) {
            double overStretch = Math.min(tension - maxAllowedDist, ServerConfig.INSTANCE.exhaustionMaxOverStretch);
            tensionEffort = (overStretch / ServerConfig.INSTANCE.exhaustionMaxOverStretch) * weightRatio;
        }

        double exertionRatio = Mth.clamp(baseEffort + kineticEffort + tensionEffort, 0.0, 1.0);

        if (exertionRatio > 0.001) {
            double dX = player.getX() - player.xo;
            double dY = player.getY() - player.yo;
            double dZ = player.getZ() - player.zo;

            double horizontalSpeed = Math.sqrt(dX * dX + dZ * dZ);
            double verticalSpeed = Math.max(0.0, dY);

            double weightedSpeed = horizontalSpeed + (verticalSpeed * ServerConfig.INSTANCE.exhaustionVerticalWeightFactor);

            double idleEffort = ServerConfig.INSTANCE.exhaustionIdleRate * baseEffort;
            double moveEffort = ServerConfig.INSTANCE.exhaustionMovementRate * exertionRatio * (weightedSpeed * 20.0);
            double forceEffort = ServerConfig.INSTANCE.exhaustionForceRate * (kineticEffort + tensionEffort);

            float totalExhaustion = (float) (idleEffort + moveEffort + forceEffort);

            if (totalExhaustion > 0.0f) {
                player.causeFoodExhaustion(totalExhaustion);
            }
        }
    }
}