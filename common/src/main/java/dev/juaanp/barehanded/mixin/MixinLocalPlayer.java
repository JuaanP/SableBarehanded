package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.handler.ClientInteractionHandler;
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
}