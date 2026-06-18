package dev.juaanp.barehanded.platform.services;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import java.util.UUID;

public interface INetworkHelper {
    void sendStartGrabbingAnimation(Player player);
    void sendStopGrabbingAnimation(Player player);
    void sendRequestGrab(BlockPos pos);
    void sendAssembleGrabRequest(BlockPos pos);
    void sendStopGrabbingRequest();
    void sendRotateGrab(double deltaX, double deltaY, boolean rotateAroundCenter);
    void sendGhostStateSync(ServerSubLevel subLevel, UUID grabberId, byte collisionMask);
    void sendSyncGrabState(Player player, double mass, UUID subLevelId, Vector3d localPivot, double distance);
    void sendDisassembleRequest();
    void sendPhysicsPlaceRequest(BlockPos pos, net.minecraft.core.Direction face, boolean isMainHand);
    void sendUpdateServerConfig(String json);
    void broadcastSyncConfig(net.minecraft.server.MinecraftServer server, String json);
    void sendAssemblyStateToServer(boolean active);
    void broadcastAssemblyStateToTrackers(net.minecraft.server.level.ServerPlayer player, boolean active);
    void sendAdjustDistance(double amount);
}