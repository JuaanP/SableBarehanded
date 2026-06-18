package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.network.StartGrabbingAnimationPacket;
import dev.juaanp.barehanded.network.StopGrabbingAnimationPacket;
import dev.juaanp.barehanded.network.SyncGrabStatePacket;
import dev.juaanp.barehanded.physics.GrabCollisionHandler;
import dev.juaanp.barehanded.network.SyncGhostStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClientPayloadHandler {
    public static final Set<UUID> GRABBING_PLAYERS = new HashSet<>();
    public static final Set<UUID> ASSEMBLING_PLAYERS = new java.util.HashSet<>();

    public static void handleStartGrabbingAnimation(StartGrabbingAnimationPacket packet) {
        Player player = getPlayerFromId(packet.entityId());
        if (player != null) {
            GRABBING_PLAYERS.add(player.getUUID());
        }
    }

    public static void handleSyncGrabState(SyncGrabStatePacket packet) {
        Player player = getPlayerFromId(packet.entityId());
        if (player == Minecraft.getInstance().player) {
            ClientGrabSession.syncFromServer(packet.entityId(), packet.mass(), packet.subLevelId(), packet.localPivot(), packet.distance());
        }
    }

    public static void handleStopGrabbingAnimation(StopGrabbingAnimationPacket packet) {
        Player player = getPlayerFromId(packet.entityId());
        if (player != null) {
            GRABBING_PLAYERS.remove(player.getUUID());

            if (player == Minecraft.getInstance().player) {
                ClientGrabSession.reset();
                ClientAssemblyTracker.reset();
            }
        }
    }

    public static void handleSyncGhostState(SyncGhostStatePacket packet) {
        GrabCollisionHandler.setClientGhostState(packet.subLevelId(), packet.grabberId(), packet.collisionMask());
    }

    private static Player getPlayerFromId(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    public static void handleAssemblyStateSync(dev.juaanp.barehanded.network.AssemblyStateS2CPacket packet) {
        Player player = getPlayerFromId(packet.entityId());
        if (player != null) {
            if (packet.active()) {
                ASSEMBLING_PLAYERS.add(player.getUUID());
            } else {
                ASSEMBLING_PLAYERS.remove(player.getUUID());
            }
        }
    }
}