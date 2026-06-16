package dev.juaanp.sablebarehanded.platform;

import dev.juaanp.sablebarehanded.network.*;
import dev.juaanp.sablebarehanded.platform.services.INetworkHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

public class NeoForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendStartGrabbingAnimation(Player player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StartGrabbingAnimationPacket(player.getId()));
    }

    @Override
    public void sendStopGrabbingAnimation(Player player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StopGrabbingAnimationPacket(player.getId()));
    }

    @Override
    public void sendRequestGrab(BlockPos pos) {
        PacketDistributor.sendToServer(new RequestGrabPacket(pos));
    }

    @Override
    public void sendAssembleGrabRequest(BlockPos pos) {
        PacketDistributor.sendToServer(new AssembleGrabPacket(pos));
    }

    @Override
    public void sendStopGrabbingRequest() {
        PacketDistributor.sendToServer(new StopGrabbingPacket());
    }

    @Override
    public void sendRotateGrab(double deltaX, double deltaY, boolean rotateAroundCenter) {
        PacketDistributor.sendToServer(new RotateGrabPacket(deltaX, deltaY, rotateAroundCenter));
    }

    @Override
    public void sendGhostStateSync(ServerSubLevel subLevel, UUID grabberId, byte collisionMask) {
        PacketDistributor.sendToPlayersInDimension(subLevel.getLevel(), new SyncGhostStatePacket(subLevel.getUniqueId(), grabberId, collisionMask));
    }

    @Override
    public void sendSyncGrabState(Player player, double mass, java.util.UUID subLevelId, org.joml.Vector3d localPivot, double distance) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncGrabStatePacket(player.getId(), mass, subLevelId, localPivot, distance));
        }
    }

    @Override
    public void sendDisassembleRequest() {
        PacketDistributor.sendToServer(new DisassembleRequestPacket());
    }

    @Override
    public void sendPhysicsPlaceRequest(BlockPos pos, net.minecraft.core.Direction face, boolean isMainHand) {
        PacketDistributor.sendToServer(new dev.juaanp.sablebarehanded.network.PhysicsPlaceRequestPacket(pos, face, isMainHand));
    }

    @Override
    public void sendUpdateServerConfig(String json) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new UpdateServerConfigPacket(json));
    }

    @Override
    public void broadcastSyncConfig(net.minecraft.server.MinecraftServer server, String json) {
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new SyncConfigPacket(json));
    }
}