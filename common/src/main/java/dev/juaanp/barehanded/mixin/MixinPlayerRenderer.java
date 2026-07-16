package dev.juaanp.barehanded.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.handler.ClientRenderState;
import net.minecraft.client.Camera;
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

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void barehanded$gatekeepRightHand(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (barehanded$shouldBlock(player)) {
            poseStack.pushPose();
            poseStack.scale(0.001F, 0.001F, 0.001F);
        }
    }

    @Inject(method = "renderRightHand", at = @At("RETURN"))
    private void barehanded$gatekeepRightHandReturn(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (barehanded$shouldBlock(player)) poseStack.popPose();
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void barehanded$gatekeepLeftHand(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (barehanded$shouldBlock(player)) {
            poseStack.pushPose();
            poseStack.scale(0.001F, 0.001F, 0.001F);
        }
    }

    @Inject(method = "renderLeftHand", at = @At("RETURN"))
    private void barehanded$gatekeepLeftHandReturn(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (barehanded$shouldBlock(player)) poseStack.popPose();
    }

    @Unique
    private boolean barehanded$shouldBlock(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Camera camera = mc.gameRenderer.getMainCamera();
            if (camera.getEntity() == player && !camera.isDetached()) {
                boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();
                if (isGrabbing && !ClientRenderState.isRenderingCustomArm) {
                    return true;
                }
            }
        }
        return false;
    }
}