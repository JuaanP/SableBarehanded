package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class GrabRotationController {
    private static final double VECTOR_EPSILON = 1.0E-7;
    private static final double ANGLE_EPSILON = 1.0E-7;

    public static void applyRotation(Player player, double yaw, double pitch, boolean clientPrefersCenter) {
        if (!ServerConfig.INSTANCE.enableRotation) return;

        GrabSession grab = ServerGrabManager.getGrabSession(player);
        if (grab == null || grab.subLevel.isRemoved()) return;

        grab.rotateAroundCenter = clientPrefersCenter;

        if (grab.rotationTicksLeft == 0) {
            grab.baseOrientation.set(grab.subLevel.logicalPose().orientation());
            grab.targetGlobalOrientation.set(grab.baseOrientation);
            Vector3d currentActualPivotPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(grab.localPivot));
            grab.anchorGlobalOrigin.set(currentActualPivotPos);
            GrabPhysicsController.rebuildConstraint(grab);
        }

        grab.rotationTicksLeft = ServerConfig.INSTANCE.rotationTicksWindow;

        boolean hasSuperStrength = GrabSession.hasSuperStrength(player);
        double mass = grab.subLevel.getMassTracker().getMass();
        double massFactor = hasSuperStrength ? 1.0 : (1.0 / (1.0 + mass * ServerConfig.INSTANCE.rotationMassDampingFactor));

        double yawDelta = yaw * massFactor;
        double pitchDelta = pitch * massFactor;

        if (ServerConfig.INSTANCE.preventFastRotations) {
            double magnitude = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
            if (magnitude > ServerConfig.INSTANCE.maxRotationSpeed) {
                double scale = ServerConfig.INSTANCE.maxRotationSpeed / magnitude;
                yawDelta *= scale;
                pitchDelta *= scale;
            }
        }

        final Vec3 look = player.getLookAngle();
        final Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);

        Vec3 right = look.cross(worldUp);
        double rLen = right.length();
        if (rLen < VECTOR_EPSILON) {
            double yr = Math.toRadians(player.getYRot());
            right = new Vec3(Math.cos(yr), 0.0, Math.sin(yr));
        } else {
            right = right.scale(1.0 / rLen);
        }

        final Vec3 camUp = right.cross(look).normalize();

        final double rx = right.x * pitchDelta + camUp.x * yawDelta;
        final double ry = right.y * pitchDelta + camUp.y * yawDelta;
        final double rz = right.z * pitchDelta + camUp.z * yawDelta;
        final double angle = Math.sqrt(rx * rx + ry * ry + rz * rz);

        if (angle > ANGLE_EPSILON) {
            final double inv = 1.0 / angle;
            grab.targetGlobalOrientation
                    .premul(new Quaterniond(new AxisAngle4d(angle, rx * inv, ry * inv, rz * inv)))
                    .normalize();
        }
    }
}