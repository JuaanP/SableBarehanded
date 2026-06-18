package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.handler.MovementInputHandler;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void barehanded$onTick(boolean isMovingSlowly, float slowFactor, CallbackInfo ci) {
        KeyboardInput input = (KeyboardInput) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        boolean isSeated = player.isPassenger() && player.getVehicle() != null;

        if (MovementInputHandler.shouldPreventMovement()) {
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.jumping = false;
            if (!isSeated) input.shiftKeyDown = false;
            return;
        }

        double encumbrance = ClientGrabSession.getEffectiveEncumbranceRatio(player);

        if (encumbrance > 0.0) {
            float movementScale = (float) (1.0 - (encumbrance * ServerConfig.INSTANCE.maxMovementPenalty));
            input.forwardImpulse *= movementScale;
            input.leftImpulse *= movementScale;

            if (encumbrance >= ServerConfig.INSTANCE.jumpPreventionThreshold) {
                input.jumping = false;
            }

            if (!isSeated && encumbrance >= ServerConfig.INSTANCE.sneakPreventionThreshold) {
                if (!ClientAssemblyTracker.isActive()) {
                    input.shiftKeyDown = false;
                }
            }
        }
    }
}