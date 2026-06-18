package dev.juaanp.barehanded.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.juaanp.barehanded.config.ClientConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RenderAnimationHandler {

    public static void renderGrabArm(
            AbstractClientPlayer player, InteractionHand hand,
            float swingProgress, float equippedProgress, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer,
            int combinedLight, EntityRenderDispatcher dispatcher, float ease) {

        if (ClientConfig.INSTANCE.hideFirstPersonArms) return;

        boolean isMainHand = (hand == InteractionHand.MAIN_HAND);
        boolean isVanillaVisible = isMainHand || !stack.isEmpty();
        boolean needsSwapOffset = !stack.isEmpty() || !isVanillaVisible;

        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isRight = (arm == HumanoidArm.RIGHT);
        float side = isRight ? 1.0F : -1.0F;

        PoseStack vanillaStack = new PoseStack();
        vanillaStack.translate(side * 0.64F, -0.6F + equippedProgress * -0.6F, -0.72F);
        vanillaStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        vanillaStack.translate(side * -1.0F, 3.6F, 3.5F);
        vanillaStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        vanillaStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        vanillaStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        vanillaStack.translate(side * 5.6F, 0.0F, 0.0F);
        Matrix4f matVanilla = vanillaStack.last().pose();

        PoseStack customStack = new PoseStack();
        customStack.translate(
                side * (float) ClientConfig.INSTANCE.grabArmOffsetX,
                (float) ClientConfig.INSTANCE.grabArmOffsetY + equippedProgress * -0.6F,
                (float) ClientConfig.INSTANCE.grabArmOffsetZ
        );
        customStack.mulPose(Axis.XP.rotationDegrees(-60.0F));
        customStack.mulPose(Axis.YP.rotationDegrees(side * 5.0F));
        customStack.mulPose(Axis.ZP.rotationDegrees(side * 15.0F));
        customStack.mulPose(Axis.YP.rotationDegrees(side * 200.0F));
        Matrix4f matCustom = customStack.last().pose();

        Vector3f transVanilla = matVanilla.getTranslation(new Vector3f());
        Vector3f transCustom = matCustom.getTranslation(new Vector3f());
        Quaternionf rotVanilla = matVanilla.getNormalizedRotation(new Quaternionf());
        Quaternionf rotCustom = matCustom.getNormalizedRotation(new Quaternionf());

        Vector3f lerpedTrans = transVanilla.lerp(transCustom, ease);
        Quaternionf slerpedRot = rotVanilla.slerp(rotCustom, ease);

        float customArmYOffset = needsSwapOffset ? -(1.0F - ease) * 1.5F : 0.0F;
        float dip = (float) Math.sin(ease * Math.PI);

        poseStack.pushPose();
        poseStack.translate(lerpedTrans.x(), lerpedTrans.y() - (0.8F * dip) + customArmYOffset, lerpedTrans.z());
        poseStack.mulPose(Axis.XP.rotationDegrees(50.0F * dip));

        if (swingProgress > 0.0F && ease < 0.95F) {
            float swingBlend = swingProgress * (1.0F - ease);
            float swingRotX = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI * 2.0F) * 0.3F * swingBlend;
            poseStack.mulPose(Axis.XP.rotation(swingRotX));
        }

        poseStack.mulPose(slerpedRot);

        PlayerRenderer playerRenderer = (PlayerRenderer) dispatcher.getRenderer(player);

        ClientRenderState.isRenderingCustomArm = true;

        if (isRight) {
            playerRenderer.renderRightHand(poseStack, buffer, combinedLight, player);
        } else {
            playerRenderer.renderLeftHand(poseStack, buffer, combinedLight, player);
        }

        ClientRenderState.isRenderingCustomArm = false;

        poseStack.popPose();
    }
}