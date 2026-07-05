package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.platform.Services;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.UUID;

public class ClientGrabSession {
    public static boolean isHoldingGrab = false;
    public static boolean isWaitingForGrabSync = false;
    public static boolean pendingStopGrab = false;
    public static int waitingTicks = 0;

    public static double grabbedMass = 0.0;
    public static UUID grabbedSubLevelId = null;
    public static Vector3d localGrabPivot = null;
    public static double grabRestDistance = 0.0;
    public static double currentTetherStrain = 0.0;

    public static void startWaiting() {
        isHoldingGrab = true;
        isWaitingForGrabSync = true;
        currentTetherStrain = 0.0;
        waitingTicks = 0;
    }

    public static void syncFromServer(int entityId, double mass, UUID subLevelId, Vector3d localPivot, double distance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        isHoldingGrab = true;
        isWaitingForGrabSync = false;
        grabbedMass = mass;
        grabbedSubLevelId = subLevelId;
        ClientGrabSession.localGrabPivot = localPivot;
        grabRestDistance = distance;
        currentTetherStrain = 0.0;
    }

    public static void reset() {
        grabbedMass = 0.0;
        grabbedSubLevelId = null;
        localGrabPivot = null;
        grabRestDistance = 0.0;
        currentTetherStrain = 0.0;
        isHoldingGrab = false;
        isWaitingForGrabSync = false;
        pendingStopGrab = false;
        waitingTicks = 0;
    }

    public static void forceResetAndNotify() {
        boolean shouldNotify = isHoldingGrab || isWaitingForGrabSync;
        reset();
        if (shouldNotify) {
            Services.NETWORK.sendStopGrabbingRequest();
        }
    }

    public static void tickTetherStrain(Player player) {
        if (!isHoldingGrab || grabbedSubLevelId == null || localGrabPivot == null) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!ServerConfig.INSTANCE.enablePhysicalTether) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
        if (container == null) {
            forceResetAndNotify();
            return;
        }

        SubLevel subLevel = container.getSubLevel(grabbedSubLevelId);
        if (subLevel != null && !subLevel.isRemoved()) {
            Vector3d actualGlobalPosJoml = subLevel.logicalPose().transformPosition(new Vector3d(localGrabPivot));
            Vec3 actualPos = new Vec3(actualGlobalPosJoml.x, actualGlobalPosJoml.y, actualGlobalPosJoml.z);

            double currentDist = player.getEyePosition().distanceTo(actualPos);
            double stretchBeyondRest = currentDist - grabRestDistance;

            if (stretchBeyondRest > ServerConfig.INSTANCE.armStretchTolerance) {
                double activeStretch = stretchBeyondRest - ServerConfig.INSTANCE.armStretchTolerance;
                currentTetherStrain = Mth.clamp(activeStretch / 2.0, 0.0, 1.0);
            } else {
                currentTetherStrain *= 0.85;
            }
        } else {
            forceResetAndNotify();
        }
    }

    public static Vec3 getCurrentObjectPosition() {
        if (!isHoldingGrab || grabbedSubLevelId == null || localGrabPivot == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
        if (container != null) {
            SubLevel subLevel = container.getSubLevel(grabbedSubLevelId);
            if (subLevel != null && !subLevel.isRemoved()) {
                Vector3d actualGlobalPosJoml = subLevel.logicalPose().transformPosition(new Vector3d(localGrabPivot));
                return new Vec3(actualGlobalPosJoml.x, actualGlobalPosJoml.y, actualGlobalPosJoml.z);
            }
        }
        return null;
    }

    public static double getEncumbranceRatio(Player player) {
        if (!ServerConfig.INSTANCE.enableEncumbrance || !isHoldingGrab) return 0.0;
        if (player.isCreative() || player.isSpectator()) return 0.0;

        if (grabbedMass <= 0.0) return 0.0;

        double strengthMultiplier = 1.0;
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            strengthMultiplier = amplifier == 0 ? ServerConfig.INSTANCE.strength1Multiplier : ServerConfig.INSTANCE.strength2Multiplier;
        }

        double maxCapacity = ServerConfig.INSTANCE.maxForce * strengthMultiplier;
        double objectWeight = grabbedMass * ServerConfig.INSTANCE.physicsGravity;
        double rawRatio = objectWeight / maxCapacity;

        return Math.min(Math.pow(rawRatio, 2.0), 1.0);
    }

    public static double getEffectiveEncumbranceRatio(Player player) {
        if (ClientAssemblyTracker.isActive()) return 1.0;
        if (isHoldingGrab) {
            return getEncumbranceRatio(player);
        }
        return 0.0;
    }
}