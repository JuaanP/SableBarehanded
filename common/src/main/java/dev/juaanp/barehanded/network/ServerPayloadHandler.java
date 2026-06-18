package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.physics.*;
import dev.juaanp.barehanded.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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

    public static void handleDisassembleRequest(ServerPlayer player, DisassembleRequestPacket packet) {
        GrabSession grab = ServerGrabManager.getGrabSession(player);
        if (grab == null) return;
        KeybindDisassembleHandler.attemptDisassemble(player, grab.subLevel);
    }

    public static void handleStopGrabbing(ServerPlayer player, StopGrabbingPacket packet) {
        ServerGrabManager.stopGrabbing(player.getUUID());
    }

    public static void handlePhysicsPlaceRequest(ServerPlayer player, PhysicsPlaceRequestPacket packet) {
        if (!ServerConfig.INSTANCE.enablePhysicsBlockPlacement) return;

        InteractionHand hand = packet.isMainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof BlockItem)) return;

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
                dev.juaanp.barehanded.config.ServerConfig updated = gson.fromJson(packet.json(), dev.juaanp.barehanded.config.ServerConfig.class);

                if (updated != null) {
                    dev.juaanp.barehanded.config.ServerConfig.INSTANCE = updated;
                    dev.juaanp.barehanded.config.ServerConfig.save();

                    Services.NETWORK.broadcastSyncConfig(player.server, packet.json());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void handleAssemblyState(net.minecraft.server.level.ServerPlayer player, dev.juaanp.barehanded.network.AssemblyStateC2SPacket packet) {
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