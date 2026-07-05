package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.physics.*;
import dev.juaanp.barehanded.platform.Services;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;

public class ServerPayloadHandler {

    public static void handleRequestGrab(ServerPlayer player, RequestGrabPacket packet) {
        GrabActionHandler.startGrabbing(player, packet.blockPos());
    }

    public static void handleAssembleGrab(ServerPlayer player, AssembleGrabPacket packet) {
        GrabActionHandler.assembleAndGrab(player, packet.blockPos());
    }

    public static void handleRotateGrab(ServerPlayer player, RotateGrabPacket packet) {
        GrabRotationController.applyRotation(player, packet.deltaX(), packet.deltaY(), packet.rotateAroundCenter());
    }

    public static void handleDisassembleRequest(DisassembleRequestPacket packet, ServerPlayer player) {
        GrabSession grab = ServerGrabManager.getGrabSession(player);
        if (grab != null) {
            KeybindDisassembleHandler.attemptDisassemble(player, grab.subLevel, packet.isAltDown());
        }
    }

    public static void handleAltState(AltStateC2SPacket packet, ServerPlayer player) {
        GrabSession grab = ServerGrabManager.getGrabSession(player);
        if (grab != null) {
            grab.isAltDown = packet.isAltDown();
        }
    }

    public static void handleStopGrabbing(ServerPlayer player, StopGrabbingPacket packet) {
        ServerGrabManager.stopGrabbing(player.getUUID());
    }

    public static void handlePhysicsPlaceRequest(ServerPlayer player, PhysicsPlaceRequestPacket packet) {
        if (!ServerConfig.INSTANCE.enablePhysicsBlockPlacement) return;

        InteractionHand hand = packet.isMainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        SubLevel hitSubLevel = Sable.HELPER.getContaining(player.level(), packet.pos());
        if (hitSubLevel instanceof ServerSubLevel serverSubLevel) {
            ServerLevel level = (ServerLevel) player.level();
            BlockState stateToPlace = blockItem.getBlock().defaultBlockState();

            Vector3d localHit = new Vector3d(packet.pos().getX() + 0.5, packet.pos().getY() + 0.5, packet.pos().getZ() + 0.5);
            Vector3d globalHit = serverSubLevel.logicalPose().transformPosition(localHit);

            BlockPos placePos = BlockPos.containing(
                    globalHit.x + packet.face().getStepX(),
                    globalHit.y + packet.face().getStepY(),
                    globalHit.z + packet.face().getStepZ()
            );

            if (level.getBlockState(placePos).canBeReplaced()) {
                level.setBlock(placePos, stateToPlace, 3);
                SubLevel newSubLevel = SubLevelAssemblyHelper.assembleBlocks(
                        level, placePos, List.of(placePos), BoundingBox3i.from(List.of(placePos))
                );

                if (newSubLevel != null) {
                    if (!player.isCreative()) stack.shrink(1);
                    level.playSound(null, placePos, stateToPlace.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0f, 0.8f);
                } else {
                    level.setBlock(placePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
            return;
        }

        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(packet.pos()),
                packet.face(), packet.pos(), false
        );

        UseOnContext useOnContext = new UseOnContext(player, hand, hitResult);

        ServerGrabManager.markPendingPhysicsPlacement(player);
        try {
            stack.useOn(useOnContext);
        } finally {
            ServerGrabManager.clearPendingPhysicsPlacement(player);
        }
    }

    public static void handleUpdateServerConfig(ServerPlayer player, UpdateServerConfigPacket packet) {
        boolean isHost = player.server.isSingleplayerOwner(player.getGameProfile());

        if (isHost || player.hasPermissions(2)) {
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                ServerConfig updated = gson.fromJson(packet.json(), ServerConfig.class);

                if (updated != null) {
                    ServerConfig.INSTANCE = updated;
                    ServerConfig.save();

                    Services.NETWORK.broadcastSyncConfig(player.server, packet.json());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void handleAssemblyState(ServerPlayer player, AssemblyStateC2SPacket packet) {
        Services.NETWORK.broadcastAssemblyStateToTrackers(player, packet.active());
    }

    public static void handleAdjustDistance(ServerPlayer player, AdjustDistancePacket packet) {
        if (!ServerConfig.INSTANCE.enableDistanceScroll) return;

        GrabSession grab = ServerGrabManager.getGrabSession(player);
        if (grab == null || grab.subLevel.isRemoved()) return;

        double maxAllowed = GrabPhysicsController.getMaxScrollDistance(player);
        double minAllowed = ServerConfig.INSTANCE.scrollMinDistance;

        grab.targetDistance = (float) net.minecraft.util.Mth.clamp(grab.targetDistance + packet.amount(), minAllowed, maxAllowed);
        Services.NETWORK.sendSyncGrabState(player, grab.subLevel.getMassTracker().getMass(), grab.subLevel.getUniqueId(), grab.localPivot, grab.targetDistance);
    }
}