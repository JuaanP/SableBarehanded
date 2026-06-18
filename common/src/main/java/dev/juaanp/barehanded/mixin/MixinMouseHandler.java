package dev.juaanp.barehanded.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.KeyBindings;
import dev.juaanp.barehanded.client.handler.RotationInputHandler;
import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void barehanded$onScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (ClientGrabSession.isHoldingGrab && yOffset != 0.0) {
            double amount = (yOffset > 0 ? 0.5 : -0.5) * ClientConfig.INSTANCE.scrollDistanceSensitivity;
            Services.NETWORK.sendAdjustDistance(amount);
            ci.cancel();
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void barehanded$onMousePress(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (this.minecraft.screen != null) return;

        boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();
        if (!isGrabbing) return;

        KeyMapping[] ourKeys = {
                KeyBindings.ROTATE_KEY,
                KeyBindings.PIVOT_KEY,
                KeyBindings.DISASSEMBLE_KEY,
                KeyBindings.PLACE_TOGGLE_KEY
        };

        for (KeyMapping ourKey : ourKeys) {
            InputConstants.Key boundKey = InputConstants.getKey(ourKey.saveString());
            if (boundKey.getType() == InputConstants.Type.MOUSE && boundKey.getValue() == button) {
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void barehanded$onTurnPlayer(CallbackInfo ci) {
        if (RotationInputHandler.handleRotation(this.accumulatedDX, this.accumulatedDY)) {
            this.accumulatedDX = 0.0;
            this.accumulatedDY = 0.0;
            ci.cancel();
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void barehanded$restrictCamera(CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        Player player = this.minecraft.player;
        double encumbrance = ClientGrabSession.getEffectiveEncumbranceRatio(player);

        if (encumbrance > 0.0) {
            Vec3 objectPos = ClientGrabSession.getCurrentObjectPosition();

            if (objectPos != null) {
                Vec3 playerEye = player.getEyePosition();
                Vec3 toObject = objectPos.subtract(playerEye).normalize();
                Vec3 currentLook = player.getLookAngle();

                double currentDot = currentLook.dot(toObject);

                double sensitivity = this.minecraft.options.sensitivity().get();
                double f = sensitivity * 0.6F + 0.2F;
                double f1 = f * f * f;
                double f2 = f1 * 8.0F;

                double dYaw = this.accumulatedDX * f2 * 0.15F;
                double dPitch = this.accumulatedDY * f2 * 0.15F;
                if (this.minecraft.options.invertYMouse().get()) {
                    dPitch = -dPitch;
                }

                float newYaw = player.getYRot() + (float)dYaw;
                float newPitch = player.getXRot() + (float)dPitch;
                newPitch = Mth.clamp(newPitch, -90.0F, 90.0F);

                float pRad = newPitch * ((float)Math.PI / 180F);
                float yRad = -newYaw * ((float)Math.PI / 180F);
                float cosY = Mth.cos(yRad);
                float sinY = Mth.sin(yRad);
                float cosP = Mth.cos(pRad);
                float sinP = Mth.sin(pRad);
                Vec3 newLook = new Vec3((double)(sinY * cosP), (double)(-sinP), (double)(cosY * cosP));

                double newDot = newLook.dot(toObject);
                double turningAway = currentDot - newDot;

                double baseScale = 1.0 - (encumbrance * ServerConfig.INSTANCE.maxCameraPenalty);

                if (turningAway > 0.0) {
                    double dynamicResistance = turningAway * encumbrance * 15.0;
                    double currentAwayness = Math.max(0.0, 1.0 - currentDot);
                    double angleResistance = currentAwayness * encumbrance * 2.5;

                    double directionalScale = 1.0 - (dynamicResistance + angleResistance);
                    directionalScale = Math.max(0.0, directionalScale);

                    baseScale *= directionalScale;
                }

                this.accumulatedDX *= baseScale;
                this.accumulatedDY *= baseScale;

            } else {
                double cameraScale = 1.0 - (encumbrance * ServerConfig.INSTANCE.maxCameraPenalty);
                this.accumulatedDX *= cameraScale;
                this.accumulatedDY *= cameraScale;
            }
        }
    }
}