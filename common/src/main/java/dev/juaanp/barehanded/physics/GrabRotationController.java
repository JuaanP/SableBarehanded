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

            Vector3d localAnchor = clientPrefersCenter ? grab.localCenterOfMass : grab.localPivot;
            Vector3d currentActualAnchorPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(localAnchor));
            grab.anchorGlobalOrigin.set(currentActualAnchorPos);

            GrabPhysicsController.rebuildConstraint(grab);
        }

        grab.rotationTicksLeft = ServerConfig.INSTANCE.rotationTicksWindow;

        boolean hasSuperStrength = GrabSession.hasSuperStrength(player);
        double mass = grab.subLevel.getMassTracker().getMass();
        double massFactor = hasSuperStrength ? 1.0 : (1.0 / (1.0 + mass * ServerConfig.INSTANCE.rotationMassDampingFactor));

        double smoothingFactor = 0.7;
        double yawDelta = yaw * massFactor * smoothingFactor;
        double pitchDelta = pitch * massFactor * smoothingFactor;

        if (ServerConfig.INSTANCE.preventFastRotations) {
            yawDelta = Mth.clamp(yawDelta, -ServerConfig.INSTANCE.maxRotationSpeed, ServerConfig.INSTANCE.maxRotationSpeed);
            pitchDelta = Mth.clamp(pitchDelta, -ServerConfig.INSTANCE.maxRotationSpeed, ServerConfig.INSTANCE.maxRotationSpeed);
        }

        Vec3 look = player.getLookAngle();
        Vector3d forward = new Vector3d(look.x, look.y, look.z).normalize();
        Vector3d worldUp = new Vector3d(0.0, 1.0, 0.0);

        Vector3d rightAxis = new Vector3d(forward).cross(worldUp).normalize();
        if (rightAxis.lengthSquared() < 0.001) rightAxis.set(1.0, 0.0, 0.0);

        Vector3d upAxis = new Vector3d(rightAxis).cross(forward).normalize();

        Quaterniond pitchQuat = new Quaterniond().rotateAxis(pitchDelta, rightAxis);
        Quaterniond yawQuat = new Quaterniond().rotateAxis(-yawDelta, upAxis);

        grab.targetGlobalOrientation.premul(pitchQuat);
        grab.targetGlobalOrientation.premul(yawQuat);
        grab.targetGlobalOrientation.normalize();
    }
}