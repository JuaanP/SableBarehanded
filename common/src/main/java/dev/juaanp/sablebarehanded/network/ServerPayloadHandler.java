package dev.juaanp.sablebarehanded.network;

import dev.juaanp.sablebarehanded.config.ServerConfig;
import dev.juaanp.sablebarehanded.physics.*;
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
                dev.juaanp.sablebarehanded.config.ServerConfig updated = gson.fromJson(packet.json(), dev.juaanp.sablebarehanded.config.ServerConfig.class);

                if (updated != null) {
                    dev.juaanp.sablebarehanded.config.ServerConfig.INSTANCE = updated;
                    dev.juaanp.sablebarehanded.config.ServerConfig.save();

                    dev.juaanp.sablebarehanded.platform.Services.NETWORK.broadcastSyncConfig(player.server, packet.json());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}