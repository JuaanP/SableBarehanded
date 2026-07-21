package dev.juaanp.barehanded.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.handler.ClientRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 10)
public class MixinEntityRenderDispatcher {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void barehanded$blockFirstPersonBodyRender(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Camera camera = mc.gameRenderer.getMainCamera();
            if (camera.getEntity() == entity && !camera.isDetached()) {
                boolean isGrabbing = (ClientGrabSession.isHoldingGrab && !ClientGrabSession.isWaitingForGrabSync) || ClientAssemblyTracker.isActive();

                if (isGrabbing && !ClientRenderState.isRenderingCustomArm) {
                    ci.cancel();
                }
            }
        }
    }
}