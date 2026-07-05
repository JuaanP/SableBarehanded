package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class GrabRotationController {
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

        double smoothingFactor = 0.7;  // 70% de la entrada actual, 30% de inercia
        double yawDelta = yaw * massFactor * smoothingFactor;
        double pitchDelta = pitch * massFactor * smoothingFactor;

        if (ServerConfig.INSTANCE.preventFastRotations) {
            yawDelta = Mth.clamp(yawDelta, -ServerConfig.INSTANCE.maxRotationSpeed, ServerConfig.INSTANCE.maxRotationSpeed);
            pitchDelta = Mth.clamp(pitchDelta, -ServerConfig.INSTANCE.maxRotationSpeed, ServerConfig.INSTANCE.maxRotationSpeed);
        }

        Vec3 forward = player.getLookAngle();
        Vector3d forwardVec = new Vector3d(forward.x, forward.y, forward.z).normalize();

        Vec3 right = player.calculateViewVector(0.0f, player.getYRot() - 90.0f);
        Vector3d rightAxis = new Vector3d(right.x, right.y, right.z).normalize();

        Vector3d upAxis = new Vector3d(rightAxis).cross(forwardVec).normalize();

        Quaterniond deltaRot = new Quaterniond()
                .rotateAxis(-yawDelta, upAxis.x, upAxis.y, upAxis.z)
                .rotateAxis(pitchDelta, rightAxis.x, rightAxis.y, rightAxis.z);

        grab.targetGlobalOrientation.premul(deltaRot).normalize();
    }
}