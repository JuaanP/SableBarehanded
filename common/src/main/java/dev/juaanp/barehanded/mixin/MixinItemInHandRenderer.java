package dev.juaanp.barehanded.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.ClientPayloadHandler;
import dev.juaanp.barehanded.client.handler.RenderAnimationHandler;
import dev.juaanp.barehanded.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandRenderer.class, priority = 10)
public class MixinItemInHandRenderer {

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private Minecraft minecraft;

    @Unique private static final float VANILLA_HIDE_MULTIPLIER = 3.0F;
    @Unique private static final double ARM_DROP_DISTANCE = 1.2D;

    @Unique private float transitionProgress = 0.0F;
    @Unique private float oldTransitionProgress = 0.0F;

    @Unique
    private float barehanded$calculateSmoothStep(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        this.oldTransitionProgress = this.transitionProgress;

        boolean isGrabbing = this.minecraft.player != null && (
                ClientPayloadHandler.GRABBING_PLAYERS.contains(this.minecraft.player.getUUID()) ||
                        ClientGrabSession.isHoldingGrab ||
                        ClientAssemblyTracker.assemblyChargeTicks > 0
        );

        float speed = (float) ClientConfig.INSTANCE.armTransitionSpeed;
        if (isGrabbing) {
            this.transitionProgress += speed;
        } else {
            this.transitionProgress -= speed;
        }
        this.transitionProgress = Mth.clamp(this.transitionProgress, 0.0F, 1.0F);
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void barehanded$onRenderHandsWithItems(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer, net.minecraft.client.player.LocalPlayer player, int combinedLight, CallbackInfo ci) {

        float t = Mth.lerp(partialTicks, this.oldTransitionProgress, this.transitionProgress);
        int charge = ClientAssemblyTracker.assemblyChargeTicks;

        if (charge > 0 && ClientAssemblyTracker.smoothPullIntensity > 0.01F && player.getMainHandItem().isEmpty()) {
            float maxTicks = Math.max(1.0F, (float) ClientAssemblyTracker.currentRequiredAssemblyTicks);
            float progress = Math.min((float) charge / maxTicks, 1.0F);

            float visualThreshold = (float) ClientConfig.INSTANCE.visualShakeThreshold;
            if (progress >= visualThreshold) {
                float pullFactor = ClientAssemblyTracker.smoothPullIntensity;
                float shakeIntensity = progress * pullFactor * (float) ClientConfig.INSTANCE.assemblyShakeMultiplier;

                if (shakeIntensity > 0.001F) {
                    float time = player.tickCount + partialTicks;
                    poseStack.translate(
                            Mth.sin(time * (float) ClientConfig.INSTANCE.shakeFrequencyX) * shakeIntensity,
                            Mth.cos(time * (float) ClientConfig.INSTANCE.shakeFrequencyY) * shakeIntensity,
                            Mth.sin(time * (float) ClientConfig.INSTANCE.shakeFrequencyZ) * shakeIntensity
                    );
                }
            }
        }

        if (t <= 0.0F || player.isInvisible()) return;

        float ease = barehanded$calculateSmoothStep(t);
        float vanillaT = Mth.clamp(t * VANILLA_HIDE_MULTIPLIER, 0.0F, 1.0F);

        float mainSwing = player.swingingArm == InteractionHand.MAIN_HAND ? player.getAttackAnim(partialTicks) : 0.0F;
        float offSwing = player.swingingArm == InteractionHand.OFF_HAND ? player.getAttackAnim(partialTicks) : 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, -(1.0F - ease) * ARM_DROP_DISTANCE, 0.0D);

        RenderAnimationHandler.renderGrabArm(
                player, InteractionHand.MAIN_HAND, mainSwing, 0.0F,
                player.getMainHandItem(), poseStack, buffer, combinedLight, this.entityRenderDispatcher, ease);

        RenderAnimationHandler.renderGrabArm(
                player, InteractionHand.OFF_HAND, offSwing, 0.0F,
                player.getOffhandItem(), poseStack, buffer, combinedLight, this.entityRenderDispatcher, ease);

        poseStack.popPose();

        float vanillaEase = barehanded$calculateSmoothStep(vanillaT);
        poseStack.translate(0.0D, -vanillaEase * ARM_DROP_DISTANCE, 0.0D);
    }
}