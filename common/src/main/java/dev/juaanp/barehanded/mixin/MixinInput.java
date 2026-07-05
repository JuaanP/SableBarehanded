package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.handler.MovementInputHandler;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Input.class)
public class MixinInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void barehanded$onTick(boolean isMovingSlowly, float slowFactor, CallbackInfo ci) {
        Input input = (Input) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        if (!(mc.player instanceof LocalPlayer player)) return;

        if (player.input != input) return;

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