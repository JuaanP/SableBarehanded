package dev.juaanp.sablebarehanded.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.juaanp.sablebarehanded.client.ClientAssemblyTracker;
import dev.juaanp.sablebarehanded.client.ClientGrabSession;
import dev.juaanp.sablebarehanded.client.handler.ClientRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRenderer.class, priority = 10)
public class MixinPlayerRenderer {

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void barehanded$gatekeepRightHand(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (barehanded$shouldBlock(player)) ci.cancel();
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void barehanded$gatekeepLeftHand(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (barehanded$shouldBlock(player)) ci.cancel();
    }

    @Unique
    private boolean barehanded$shouldBlock(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();

            if (isGrabbing && !ClientRenderState.isRenderingCustomArm) {
                return true;
            }
        }
        return false;
    }
}