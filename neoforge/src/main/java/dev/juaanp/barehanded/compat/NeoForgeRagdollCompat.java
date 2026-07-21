package dev.juaanp.barehanded.compat;

import dev.juaanp.barehanded.Constants;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.api.RagdollSession;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.UUID;

public final class NeoForgeRagdollCompat implements IRagdollCompat {
    
    private static final String RAGDOLL_MOD_ID = "sable_player_ragdoll";
    private static final String PLAYER_ID_NBT_KEY = "playerId";
    private final boolean loaded;
    
    public NeoForgeRagdollCompat() {
        this.loaded = ModList.get().isLoaded(RAGDOLL_MOD_ID);
        if (loaded) {
            Constants.LOG.info("[Barehanded] Sable Player Ragdolls detected - compatibility enabled");
        }
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public boolean isPlayerRagdollSubLevel(ServerSubLevel subLevel) {
        if (!loaded || subLevel == null || subLevel.isRemoved()) return false;
        try {
            return RagdollAPI.isRagdollSubLevel(subLevel);
        } catch (Throwable t) {
            return false;
        }
    }
    
    @Override
    public boolean isMobRagdollSubLevel(ServerSubLevel subLevel) {
        if (!loaded || subLevel == null || subLevel.isRemoved()) return false;
        try {
            return MobRagdollAssembly.isRagdollPart(subLevel.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }
    
    @Override
    public boolean isAnyRagdollSubLevel(ServerSubLevel subLevel) {
        return isPlayerRagdollSubLevel(subLevel) || isMobRagdollSubLevel(subLevel);
    }
    
    @Override
    public boolean releaseRagdoll(ServerLevel level, ServerSubLevel subLevel) {
        if (!loaded || subLevel == null || level == null) return false;
        try {
            if (isPlayerRagdollSubLevel(subLevel)) {
                return releasePlayerRagdoll(level, subLevel);
            } else if (isMobRagdollSubLevel(subLevel)) {
                return releaseMobRagdoll(level, subLevel);
            }
        } catch (Throwable t) {
            Constants.LOG.error("[Barehanded] Failed to release ragdoll", t);
        }
        return false;
    }
    
    private boolean releasePlayerRagdoll(ServerLevel level, ServerSubLevel subLevel) {
        UUID playerId = extractPlayerIdFromSubLevel(subLevel);
        if (playerId != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                RagdollSession session = RagdollAPI.activeSession(player);
                if (session != null) {
                    session.release();
                    return true;
                }
            }
        }
        return false;
    }
    
    private UUID extractPlayerIdFromSubLevel(ServerSubLevel subLevel) {
        try {
            CompoundTag tag = subLevel.getUserDataTag();
            if (tag != null && tag.hasUUID(PLAYER_ID_NBT_KEY)) {
                return tag.getUUID(PLAYER_ID_NBT_KEY);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[Barehanded] Failed to read playerId from subLevel NBT", t);
        }
        return null;
    }
    
    private boolean releaseMobRagdoll(ServerLevel level, ServerSubLevel subLevel) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && RagdollAPI.isMobRagdolled(living)) {
                if (isMobRagdollOwner(living, subLevel)) {
                    RagdollAPI.releaseMob(living);
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isMobRagdollOwner(LivingEntity entity, ServerSubLevel subLevel) {
        try {
            return MobRagdollAssembly.isConverted(entity.getUUID())
                    && MobRagdollAssembly.isRagdollPart(subLevel.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }
    
    @Override
    public boolean isPlayerRidingOwnRagdoll(Player player, ServerSubLevel targetSubLevel) {
        if (!loaded || player == null || targetSubLevel == null) return false;
        try {
            dev.ryanhcode.sable.sublevel.SubLevel trackingSubLevel =
                    dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(player);
            if (trackingSubLevel instanceof ServerSubLevel serverTracking) {
                if (isAnyRagdollSubLevel(serverTracking) && isAnyRagdollSubLevel(targetSubLevel)) {
                    return true;
                }
            }
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                String className = vehicle.getClass().getName();
                if (className.contains("RagdollSeatEntity")) {
                    return true;
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[Barehanded] Failed to check if player is riding own ragdoll", t);
        }
        return false;
    }
    
    @Override
    public boolean isLocalPlayerRidingRagdoll(Player player) {
        if (!loaded || player == null) return false;
        Entity vehicle = player.getVehicle();
        if (vehicle == null) return false;
        String className = vehicle.getClass().getName();
        return className.contains("RagdollSeatEntity") ||
                className.contains("sableplayerragdoll");
    }
}