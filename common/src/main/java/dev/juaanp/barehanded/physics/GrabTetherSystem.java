package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.ryanhcode.sable.Sable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class GrabTetherSystem {
    public static void applyPhysicalTether(Player player, GrabSession grab, double tension, double actualMaxForce) {
        if (!ServerConfig.INSTANCE.enablePhysicalTether || player.isCreative() || player.isSpectator()) return;

        double stretch = tension - grab.distance;
        double armStretchTolerance = ServerConfig.INSTANCE.armStretchTolerance;

        double mass = grab.subLevel.getMassTracker().getMass();
        double objectWeight = mass * ServerConfig.INSTANCE.physicsGravity;
        double weightRatio = Mth.clamp(objectWeight / actualMaxForce, 0.0, 1.0);
        double encumbrance = Math.min(Math.pow(weightRatio, 2.0), 1.0);

        Vector3d currentActualGrabBlockPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(grab.localPivot));
        Vec3 blockPos = new Vec3(currentActualGrabBlockPos.x, currentActualGrabBlockPos.y, currentActualGrabBlockPos.z);
        Vec3 pullDirection = blockPos.subtract(player.getEyePosition()).normalize();
        Vec3 awayDirection = pullDirection.scale(-1.0);
        Vec3 currentVel = player.getDeltaMovement();
        Vec3 newVel = currentVel;

        if (stretch > armStretchTolerance) {
            double activeStretch = stretch - armStretchTolerance;
            double tetherStiffness = ServerConfig.INSTANCE.tetherStiffnessBase + (ServerConfig.INSTANCE.tetherStiffnessMultiplier * encumbrance);

            Vec3 correction = pullDirection.scale(activeStretch * tetherStiffness);
            double correctionY = correction.y < 0 ? correction.y : (correction.y * ServerConfig.INSTANCE.tetherVerticalSmoothing);

            if (ServerConfig.INSTANCE.preventPropSurfing) {
                boolean isStandingOnGrab = Sable.HELPER.getTrackingSubLevel(player) == grab.subLevel;
                if (isStandingOnGrab) {
                    correction = new Vec3(correction.x * 0.1, Math.min(0, correctionY), correction.z * 0.1);
                }
            }

            newVel = new Vec3(
                    newVel.x + correction.x,
                    newVel.y + correctionY,
                    newVel.z + correction.z
            );
        }

        double awaySpeed = currentVel.dot(awayDirection);
        if (awaySpeed > ServerConfig.INSTANCE.recoilVelocityThreshold && encumbrance > 0.1) {
            double recoilBlock = awaySpeed * encumbrance * ServerConfig.INSTANCE.pullResistanceMultiplier;
            newVel = newVel.add(awayDirection.scale(-recoilBlock));
        }

        double maxAllowedDist = grab.distance + armStretchTolerance + ServerConfig.INSTANCE.tetherHardEscapeBuffer;
        if (tension > maxAllowedDist) {
            double escapeSpeed = currentVel.dot(awayDirection);
            if (escapeSpeed > ServerConfig.INSTANCE.recoilVelocityThreshold) {
                newVel = newVel.subtract(awayDirection.scale(escapeSpeed));
            }
        }

        if (newVel.distanceToSqr(currentVel) > PhysicsConstants.VELOCITY_EPSILON) {
            player.setDeltaMovement(newVel);
            player.hurtMarked = true;
        }
    }
}