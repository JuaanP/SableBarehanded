package dev.juaanp.sablebarehanded.mixin;

import dev.juaanp.sablebarehanded.client.handler.ClientInteractionHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void barehanded$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ClientInteractionHandler.shouldCancelInteraction()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void barehanded$onContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (ClientInteractionHandler.shouldCancelInteraction()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void barehanded$onStartUseItem(CallbackInfo ci) {
        if (ClientInteractionHandler.shouldCancelInteraction()) {
            ci.cancel();
        }
    }
}