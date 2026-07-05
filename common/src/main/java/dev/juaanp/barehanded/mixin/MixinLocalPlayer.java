package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.ClientTickOrchestrator;
import dev.juaanp.barehanded.client.KeyBindings;
import dev.juaanp.barehanded.client.handler.ClientInteractionHandler;
import dev.juaanp.barehanded.config.ClientConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void barehanded$onSwing(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (player.getMainHandItem().isEmpty()) {
            if (ClientInteractionHandler.shouldCancelInteraction()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void barehanded$lockMovementOnRotate(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (ClientGrabSession.isHoldingGrab && ClientTickOrchestrator.isActionDown(KeyBindings.ROTATE_KEY) && ClientConfig.INSTANCE.preventMovementWhileRotating) {
            player.input.forwardImpulse = 0.0f;
            player.input.leftImpulse = 0.0f;
            player.input.jumping = false;
            player.input.shiftKeyDown = false;
            player.input.up = false;
            player.input.down = false;
            player.input.left = false;
            player.input.right = false;
        }
    }
}