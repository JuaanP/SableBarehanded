package dev.juaanp.barehanded.client.handler;

import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.ClientInputTracker;
import dev.juaanp.barehanded.client.ClientTickOrchestrator;
import dev.juaanp.barehanded.client.KeyBindings;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.physics.GrabPhysicsController;
import dev.juaanp.barehanded.platform.Services;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ClientInteractionHandler {

    public static boolean shouldCancelInteraction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        if (ClientInputTracker.suppressMiningUntilRelease || ClientInputTracker.suppressUseUntilRelease) {
            return true;
        }

        if (ClientAssemblyTracker.isActive() || ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync) {
            return true;
        }

        boolean attackDown = ClientTickOrchestrator.isActionDown(mc.options.keyAttack);
        boolean useDown = ClientTickOrchestrator.isActionDown(mc.options.keyUse);
        boolean bothDown = attackDown && useDown;
        boolean grabToggle = ClientInputTracker.grabToggleActive;

        if (bothDown || grabToggle) {
            if (isValidGrabTarget(mc)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isValidGrabTarget(Minecraft mc) {
        if (!ClientInputTracker.canInitiateGrab()) return false;
        if (!mc.player.getMainHandItem().isEmpty()) return false;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        Vector3d hitPos = new Vector3d(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
        double distanceToHitExact = mc.player.getEyePosition().distanceTo(blockHit.getLocation());

        SubLevel subLevel = Sable.HELPER.getContaining(mc.level, hitPos);
        boolean isAltDown = ClientTickOrchestrator.isActionDown(KeyBindings.PIVOT_KEY);

        if (subLevel != null) {
            if (mc.player.isShiftKeyDown() && isAltDown && ServerConfig.INSTANCE.enableRipOffBlocks) {
                Vector3d localCenter = new Vector3d(blockHit.getBlockPos().getX() + 0.5, blockHit.getBlockPos().getY() + 0.5, blockHit.getBlockPos().getZ() + 0.5);
                Vector3d globalCenter = subLevel.logicalPose().transformPosition(localCenter);
                double distanceToHitCenter = mc.player.getEyePosition().distanceTo(new Vec3(globalCenter.x, globalCenter.y, globalCenter.z));
                return distanceToHitCenter <= ServerConfig.INSTANCE.barehandedAssemblyMaxDistance;
            } else {
                double reach = GrabPhysicsController.getGrabReach(mc.player);
                return distanceToHitExact <= reach;
            }
        }

        if (!ServerConfig.INSTANCE.enableBarehandedAssembly) return false;
        if (!mc.player.isShiftKeyDown()) return false;

        double distanceToHitCenter = mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(blockHit.getBlockPos()));
        return distanceToHitCenter <= ServerConfig.INSTANCE.barehandedAssemblyMaxDistance;
    }

    public static InteractionResult handleItemUseOn(Player player, InteractionHand hand, BlockHitResult hitResult) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        if (ClientInputTracker.suppressUseUntilRelease) {
            return InteractionResult.FAIL;
        }

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
        return null;
    }

    public static InteractionResult handleItemUse(Player player, InteractionHand hand) {
        if (ClientInputTracker.suppressUseUntilRelease) {
            return InteractionResult.FAIL;
        }

        if (ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive()) {
            return InteractionResult.FAIL;
        }
        return null;
    }

    public static boolean shouldCancelEntityInteraction() {
        if (ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive()) {
            return true;
        }
        return false;
    }
}