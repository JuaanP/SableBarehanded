package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.platform.Services;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void barehanded$onScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (ClientGrabSession.isHoldingGrab && yOffset != 0.0) {
            double amount = (yOffset > 0 ? 0.5 : -0.5) * ClientConfig.INSTANCE.scrollDistanceSensitivity;
            Services.NETWORK.sendAdjustDistance(amount);
            ci.cancel();
        }
    }
}