package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.handler.ClientRenderState;
import dev.juaanp.barehanded.client.handler.ThirdPersonAnimationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerModel.class, priority = 2000)
public class MixinPlayerModel {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void barehanded$onSetupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        float swimAmount = entity.getSwimAmount(mc.getTimer().getGameTimeDeltaPartialTick(true));
        ThirdPersonAnimationHandler.applyGrabPose(entity, model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve, swimAmount);

        if (entity instanceof AbstractClientPlayer player && player == mc.player) {
            if (mc.options.getCameraType().isFirstPerson()) {

                if (!ClientRenderState.isRenderingCustomArm) {
                    boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();

                    if (isGrabbing) {
                        model.rightArm.visible = false;
                        model.rightSleeve.visible = false;
                        model.leftArm.visible = false;
                        model.leftSleeve.visible = false;
                    }
                }
            }
        }
    }
}