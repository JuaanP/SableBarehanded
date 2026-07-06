package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.Constants;
import dev.juaanp.barehanded.api.BarehandedEvents;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.platform.Services;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ServerGrabManager {
    private static final ResourceLocation MOVEMENT_PENALTY_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "grab_movement_penalty");
    private static final Map<UUID, GrabSession> ACTIVE_GRABS = new HashMap<>();
    private static final Set<UUID> PENDING_PHYSICS_PLACEMENTS = new HashSet<>();

    public static Map<UUID, GrabSession> getActiveGrabs() {
        return ACTIVE_GRABS;
    }

    public static void markPendingPhysicsPlacement(Player player) {
        PENDING_PHYSICS_PLACEMENTS.add(player.getUUID());
    }

    public static void clearPendingPhysicsPlacement(Player player) {
        PENDING_PHYSICS_PLACEMENTS.remove(player.getUUID());
    }

    public static boolean hasPendingPhysicsPlacement(Player player) {
        return PENDING_PHYSICS_PLACEMENTS.contains(player.getUUID());
    }

    public static boolean canPlayerGrab(Player player) {
        if (player.isSpectator() && !ServerConfig.INSTANCE.allowSpectatorGrabbing) return false;
        if (ACTIVE_GRABS.containsKey(player.getUUID())) return false;

        if (ServerConfig.INSTANCE.preventGrabbingWhilePassenger &&
                player.isPassenger() && player.getVehicle() != null) {
            return false;
        }

        return true;
    }

    public static boolean isHoldingSubLevel(Player player, ServerSubLevel subLevel) {
        GrabSession grab = ACTIVE_GRABS.get(player.getUUID());
        return grab != null && subLevel.equals(grab.subLevel);
    }

    public static boolean isPlayerGrabbing(Player player) {
        return ACTIVE_GRABS.containsKey(player.getUUID());
    }

    public static ServerSubLevel getGrabbedSubLevel(Player player) {
        GrabSession grab = ACTIVE_GRABS.get(player.getUUID());
        return grab != null ? grab.subLevel : null;
    }

    public static GrabSession getGrabSession(Player player) {
        return ACTIVE_GRABS.get(player.getUUID());
    }

    public static void registerGrab(Player player, GrabSession session) {
        ACTIVE_GRABS.put(player.getUUID(), session);
    }

    public static void stopGrabbing(UUID playerId) {
        GrabSession session = ACTIVE_GRABS.remove(playerId);
        if (session != null) {
            if (session.constraintHandle != null && !session.subLevel.isRemoved()) {
                session.pipeline.wakeUp(session.subLevel);
                session.constraintHandle.remove();
            }
            Services.NETWORK.sendGhostStateSync(session.subLevel, playerId, (byte) 0);
            Level level = session.subLevel.getLevel();
            if (level != null) {
                Player player = level.getPlayerByUUID(playerId);
                if (player != null) {
                    clearPlayerMovementPenalty(player);
                    Services.NETWORK.sendStopGrabbingAnimation(player);
                    BarehandedEvents.fireOnRelease(player, session.subLevel);
                }
            }
        }
    }

    public static void clearPlayerMovementPenalty(Player player) {
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(MOVEMENT_PENALTY_ID);
        }
    }

    public static ResourceLocation getMovementPenaltyId() {
        return MOVEMENT_PENALTY_ID;
    }

    public static void onPlayerLoggedOut(Player player) {
        stopGrabbing(player.getUUID());
        clearPendingPhysicsPlacement(player);
    }

    public static void onPlayerDeath(Player player) {
        stopGrabbing(player.getUUID());
        clearPendingPhysicsPlacement(player);
    }
}