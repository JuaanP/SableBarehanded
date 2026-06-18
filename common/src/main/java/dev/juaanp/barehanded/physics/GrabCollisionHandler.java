package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GrabCollisionHandler {
    private static final Map<UUID, ClientGhostState> CLIENT_GHOST_STATES = new HashMap<>();

    public static void setClientGhostState(UUID subLevelId, UUID grabberId, byte collisionMask) {
        if (collisionMask == 0) {
            CLIENT_GHOST_STATES.remove(grabberId);
        } else {
            CLIENT_GHOST_STATES.put(grabberId, new ClientGhostState(subLevelId, collisionMask));
        }
    }

    public static boolean shouldIgnoreEntityCollision(SubLevel subLevel, Entity entity) {
        // --- LÓGICA DEL CLIENTE ---
        if (subLevel.getLevel().isClientSide()) {
            for (Map.Entry<UUID, ClientGhostState> entry : CLIENT_GHOST_STATES.entrySet()) {
                ClientGhostState state = entry.getValue();

                if (state.subLevelId().equals(subLevel.getUniqueId())) {
                    UUID grabberId = entry.getKey();
                    boolean isRotating = state.collisionMask() == 2;

                    boolean ignoreEverything = isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEverything : ServerConfig.INSTANCE.ignoreCollisionsGrabEverything;
                    if (ignoreEverything) return true;

                    if (entity instanceof Player player) {
                        boolean ignoreSelf = isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationSelf : ServerConfig.INSTANCE.ignoreCollisionsGrabSelf;
                        boolean ignoreOthers = isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers : ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers;
                        boolean isSelf = player.getUUID().equals(grabberId);

                        return isSelf ? ignoreSelf : ignoreOthers;
                    } else {
                        return isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEntities : ServerConfig.INSTANCE.ignoreCollisionsGrabEntities;
                    }
                }
            }
            return false;
        }

        // --- LÓGICA DEL SERVIDOR ---
        for (Map.Entry<UUID, GrabSession> entry : ServerGrabManager.getActiveGrabs().entrySet()) {
            if (entry.getValue().subLevel.equals(subLevel)) {
                UUID grabberId = entry.getKey();
                boolean isRotating = entry.getValue().isRotating;

                boolean ignoreEverything = isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEverything : ServerConfig.INSTANCE.ignoreCollisionsGrabEverything;
                if (ignoreEverything) return true;

                if (entity instanceof Player player) {
                    boolean ignoreSelf = isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationSelf : ServerConfig.INSTANCE.ignoreCollisionsGrabSelf;
                    boolean ignoreOthers = isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers : ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers;
                    boolean isSelf = player.getUUID().equals(grabberId);

                    if (isSelf) {
                        Vector3d blockPos = entry.getValue().subLevel.logicalPose().transformPosition(new Vector3d(entry.getValue().localPivot));
                        double distSq = player.getEyePosition().distanceToSqr(new Vec3(blockPos.x, blockPos.y, blockPos.z));
                        if (distSq < ServerConfig.INSTANCE.selfCollisionIgnoreDistanceSq) {
                            return true;
                        }
                    }

                    return isSelf ? ignoreSelf : ignoreOthers;
                } else {
                    return isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEntities : ServerConfig.INSTANCE.ignoreCollisionsGrabEntities;
                }
            }
        }
        return false;
    }

    // Definición formal del estado fantasma del cliente
    private record ClientGhostState(UUID subLevelId, byte collisionMask) {}
}