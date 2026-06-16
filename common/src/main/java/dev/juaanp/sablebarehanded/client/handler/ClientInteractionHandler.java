package dev.juaanp.sablebarehanded.client.handler;

import dev.juaanp.sablebarehanded.client.ClientAssemblyTracker;
import dev.juaanp.sablebarehanded.client.ClientGrabSession;
import dev.juaanp.sablebarehanded.client.ClientInputTracker;
import dev.juaanp.sablebarehanded.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ClientInteractionHandler {

    public static boolean shouldCancelInteraction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        if (ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive()) {
            return true;
        }

        boolean bothDown = mc.options.keyAttack.isDown() && mc.options.keyUse.isDown();
        return bothDown && mc.player.getMainHandItem().isEmpty();
    }

    public static InteractionResult handleItemUseOn(Player player, InteractionHand hand, BlockHitResult hitResult) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        if (ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive()) {
            return InteractionResult.FAIL;
        }

        if (ClientInputTracker.isPlaceToggleActive()) {
            if (dev.ryanhcode.sable.Sable.HELPER.getContainingClient(hitResult.getBlockPos()) != null) {
                return null;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BlockItem blockItem) {

                BlockState defaultState = blockItem.getBlock().defaultBlockState();
                SoundType soundType = defaultState.getSoundType();
                BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());
                mc.level.playSound(player, placePos, soundType.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

                ClientGrabSession.startWaiting();

                Services.NETWORK.sendPhysicsPlaceRequest(hitResult.getBlockPos(), hitResult.getDirection(), hand == InteractionHand.MAIN_HAND);

                player.swing(hand);

                return InteractionResult.SUCCESS;
            }
        }

        boolean bothDown = mc.options.keyAttack.isDown() && mc.options.keyUse.isDown();
        if (bothDown && player.getMainHandItem().isEmpty()) {
            return InteractionResult.FAIL;
        }

        return null;
    }

    public static InteractionResult handleItemUse(Player player, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        if (ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive()) {
            return InteractionResult.FAIL;
        }

        boolean bothDown = mc.options.keyAttack.isDown() && mc.options.keyUse.isDown();
        if (bothDown && player.getMainHandItem().isEmpty()) {
            return InteractionResult.FAIL;
        }

        return null;
    }

    public static boolean shouldCancelEntityInteraction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive()) {
            return true;
        }

        boolean bothDown = mc.options.keyAttack.isDown() && mc.options.keyUse.isDown();
        return bothDown && mc.player.getMainHandItem().isEmpty();
    }
}