package dev.juaanp.sablebarehanded.client.handler;

import dev.juaanp.sablebarehanded.client.ClientAssemblyTracker;
import dev.juaanp.sablebarehanded.client.ClientGrabSession;
import dev.juaanp.sablebarehanded.client.ClientPayloadHandler;
import dev.juaanp.sablebarehanded.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ThirdPersonAnimationHandler {

    public static void applyGrabPose(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart rightSleeve, ModelPart leftSleeve, float swimAmount) {
        if (!(entity instanceof Player player)) return;

        if (ClientConfig.INSTANCE.hideThirdPersonArms) return;

        boolean isLocalPlayer = player == Minecraft.getInstance().player;
        boolean isGrabbing = ClientPayloadHandler.GRABBING_PLAYERS.contains(player.getUUID());

        if (isLocalPlayer) {
            isGrabbing = isGrabbing || ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();
        }

        if (!isGrabbing) return;

        player.yBodyRot = player.yHeadRot;
        player.yBodyRotO = player.yHeadRotO;

        float progress = 1.0F;
        if (isLocalPlayer && ClientAssemblyTracker.isActive()) {
            progress = ClientAssemblyTracker.smoothPullIntensity;
        }

        float targetRotX = (float) Math.toRadians(-90.0) * progress;
        float targetRotY = 0.0F;
        float targetRotZ = 0.0F;

        if (swimAmount > 0.0F) {
            targetRotX += swimAmount * ((float) Math.PI / 2F);
        }

        rightArm.xRot = Mth.lerp(progress, rightArm.xRot, targetRotX);
        rightArm.yRot = targetRotY;
        rightArm.zRot = targetRotZ;

        leftArm.xRot = Mth.lerp(progress, leftArm.xRot, targetRotX);
        leftArm.yRot = targetRotY;
        leftArm.zRot = targetRotZ;

        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
    }
}