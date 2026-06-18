package dev.juaanp.barehanded.platform;

import dev.juaanp.barehanded.network.*;
import dev.juaanp.barehanded.platform.services.INetworkHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

public class FabricNetworkHelper implements INetworkHelper {
    @Override
    public void sendStartGrabbingAnimation(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StartGrabbingAnimationPacket packet = new StartGrabbingAnimationPacket(player.getId());
            for (ServerPlayer tracking : PlayerLookup.tracking(serverPlayer)) {
                ServerPlayNetworking.send(tracking, packet);
            }
            ServerPlayNetworking.send(serverPlayer, packet);
        }
    }

    @Override
    public void sendStopGrabbingAnimation(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StopGrabbingAnimationPacket packet = new StopGrabbingAnimationPacket(player.getId());
            for (ServerPlayer tracking : PlayerLookup.tracking(serverPlayer)) {
                ServerPlayNetworking.send(tracking, packet);
            }
            ServerPlayNetworking.send(serverPlayer, packet);
        }
    }

    @Override
    public void sendRequestGrab(BlockPos pos) {
        ClientPlayNetworking.send(new RequestGrabPacket(pos));
    }

    @Override
    public void sendAssembleGrabRequest(BlockPos pos) {
        ClientPlayNetworking.send(new AssembleGrabPacket(pos));
    }

    @Override
    public void sendStopGrabbingRequest() {
        ClientPlayNetworking.send(new StopGrabbingPacket());
    }

    @Override
    public void sendRotateGrab(double deltaX, double deltaY, boolean rotateAroundCenter) {
        ClientPlayNetworking.send(new RotateGrabPacket(deltaX, deltaY, rotateAroundCenter));
    }

    @Override
    public void sendGhostStateSync(ServerSubLevel subLevel, UUID grabberId, byte collisionMask) {
        SyncGhostStatePacket packet = new SyncGhostStatePacket(subLevel.getUniqueId(), grabberId, collisionMask);
        for (ServerPlayer player : PlayerLookup.world(subLevel.getLevel())) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    @Override
    public void sendSyncGrabState(Player player, double mass, UUID subLevelId, org.joml.Vector3d localPivot, double distance) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncGrabStatePacket(player.getId(), mass, subLevelId, localPivot, distance));
        }
    }

    @Override
    public void sendDisassembleRequest() {
        ClientPlayNetworking.send(new DisassembleRequestPacket());
    }

    @Override
    public void sendPhysicsPlaceRequest(BlockPos pos, net.minecraft.core.Direction face, boolean isMainHand) {
        ClientPlayNetworking.send(new PhysicsPlaceRequestPacket(pos, face, isMainHand));
    }

    @Override
    public void sendUpdateServerConfig(String json) {
        ClientPlayNetworking.send(new UpdateServerConfigPacket(json));
    }

    @Override
    public void broadcastSyncConfig(net.minecraft.server.MinecraftServer server, String json) {
        SyncConfigPacket packet = new SyncConfigPacket(json);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    @Override
    public void sendAssemblyStateToServer(boolean active) {
        ClientPlayNetworking.send(new AssemblyStateC2SPacket(active));
    }

    @Override
    public void broadcastAssemblyStateToTrackers(net.minecraft.server.level.ServerPlayer player, boolean active) {
        AssemblyStateS2CPacket packet = new AssemblyStateS2CPacket(player.getId(), active);
        for (net.minecraft.server.level.ServerPlayer tracking : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracking, packet);
        }
    }

    @Override
    public void sendAdjustDistance(double amount) {
        ClientPlayNetworking.send(new AdjustDistancePacket(amount));
    }
}