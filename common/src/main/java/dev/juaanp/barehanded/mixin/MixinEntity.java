package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.ClientInputTracker;
import dev.juaanp.barehanded.client.ClientTickOrchestrator;
import dev.juaanp.barehanded.client.KeyBindings;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double barehanded$modifyYaw(double yaw) {
        if ((Object) this instanceof LocalPlayer player && player.level().isClientSide()) {
            return barehanded$applyCameraPhysics(player, yaw, true);
        }
        return yaw;
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double barehanded$modifyPitch(double pitch) {
        if ((Object) this instanceof LocalPlayer player && player.level().isClientSide()) {
            return barehanded$applyCameraPhysics(player, pitch, false);
        }
        return pitch;
    }

    private double barehanded$applyCameraPhysics(LocalPlayer player, double deltaRot, boolean isYaw) {
        if (Math.abs(deltaRot) < 0.001) return deltaRot;

        if (ClientGrabSession.isHoldingGrab && ClientTickOrchestrator.isActionDown(KeyBindings.ROTATE_KEY)) {
            if (isYaw) {
                ClientInputTracker.pendingYaw += deltaRot;
            } else {
                ClientInputTracker.pendingPitch += deltaRot;
            }
            return 0.0;
        }

        double encumbrance = ClientGrabSession.getEffectiveEncumbranceRatio(player);
        if (encumbrance > 0.0) {
            double scale = 1.0 - (encumbrance * ServerConfig.INSTANCE.maxCameraPenalty);

            Vec3 objectPos = ClientGrabSession.getCurrentObjectPosition();
            if (objectPos != null) {
                Vec3 playerEye = player.getEyePosition();
                Vec3 toObject = objectPos.subtract(playerEye).normalize();
                Vec3 currentLook = player.getLookAngle();
                double currentDot = currentLook.dot(toObject);

                double turningAway = 0.0;
                if (isYaw) {
                    Vec3 predictedLook = currentLook.yRot((float) (-deltaRot * Math.PI / 180.0));
                    turningAway = currentDot - predictedLook.dot(toObject);
                } else {
                    Vec3 predictedLook = currentLook.xRot((float) (-deltaRot * Math.PI / 180.0));
                    turningAway = currentDot - predictedLook.dot(toObject);
                }

                if (turningAway > 0.0) {
                    double dynamicResistance = turningAway * encumbrance * 15.0;
                    double currentAwayness = Math.max(0.0, 1.0 - currentDot);
                    double angleResistance = currentAwayness * encumbrance * 2.5;
                    double directionalScale = 1.0 - (dynamicResistance + angleResistance);
                    scale *= Math.max(0.0, directionalScale);
                }
            }
            return deltaRot * Math.max(0.01, scale);
        }

        return deltaRot;
    }
}