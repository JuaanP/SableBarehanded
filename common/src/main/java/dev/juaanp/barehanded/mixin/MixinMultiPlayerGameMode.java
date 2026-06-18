package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.handler.ClientInteractionHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void barehanded$onStartDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (ClientInteractionHandler.shouldCancelInteraction()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void barehanded$onContinueDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (ClientInteractionHandler.shouldCancelInteraction()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void barehanded$onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult override = ClientInteractionHandler.handleItemUseOn(player, hand, result);
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void barehanded$onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult override = ClientInteractionHandler.handleItemUse(player, hand);
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void barehanded$onInteractEntity(Player player, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ClientInteractionHandler.shouldCancelEntityInteraction()) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void barehanded$onInteractEntityAt(Player player, Entity target, EntityHitResult result, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ClientInteractionHandler.shouldCancelEntityInteraction()) cir.setReturnValue(InteractionResult.FAIL);
    }
}