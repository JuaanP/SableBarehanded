package dev.juaanp.barehanded.compat;

import dev.juaanp.barehanded.Constants;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.api.RagdollSession;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
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

    public boolean canGrabMobRagdoll(ServerSubLevel subLevel) {
        if (!loaded || subLevel == null || subLevel.isRemoved()) return false;

        try {
            if (!MobRagdollAssembly.isRagdollPart(subLevel.getUniqueId())) {
                return false;
            }

            LivingEntity mob = findMobRagdollEntity(subLevel);
            if (mob == null) return false;

            double mobSize = getMobSize(mob);
            if (mobSize > ServerConfig.INSTANCE.sableRagdollsCompatMobRagdollMaxSize) {
                return false;
            }

            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[Barehanded] Failed to check mob ragdoll grabability", t);
            return false;
        }
    }

    private LivingEntity findMobRagdollEntity(ServerSubLevel subLevel) {
        try {
            ServerLevel level = (ServerLevel) subLevel.getLevel();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && RagdollAPI.isMobRagdolled(living)) {
                    if (MobRagdollAssembly.isConverted(living.getUUID())
                            && MobRagdollAssembly.isRagdollPart(subLevel.getUniqueId())) {
                        return living;
                    }
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[Barehanded] Failed to find mob ragdoll entity", t);
        }
        return null;
    }

    private double getMobSize(LivingEntity mob) {
        double width = mob.getBbWidth();
        double height = mob.getBbHeight();
        return Math.max(width, height);
    }

    @Override
    public boolean containsRagdollBlocks(ServerSubLevel subLevel) {
        if (!loaded || subLevel == null || subLevel.isRemoved()) return false;
        if (subLevel.getPlot() == null) return false;

        try {
            ServerLevel level = (ServerLevel) subLevel.getLevel();
            for (PlotChunkHolder chunk : subLevel.getPlot().getLoadedChunks()) {
                BoundingBox3ic bounds = chunk.getBoundingBox();
                if (bounds == null || bounds == BoundingBox3i.EMPTY) continue;
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                            BlockPos pos = new BlockPos(
                                    x + chunk.getPos().getMinBlockX(), y,
                                    z + chunk.getPos().getMinBlockZ()
                            );
                            BlockState state = level.getBlockState(pos);
                            if (!state.isAir()) {
                                ResourceLocation blockId = state.getBlock()
                                        .builtInRegistryHolder().key().location();
                                if (RAGDOLL_MOD_ID.equals(blockId.getNamespace())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[Barehanded] Failed to check ragdoll blocks in sublevel", t);
        }
        return false;
    }

    @Override
    public boolean isAnyRagdollSubLevel(ServerSubLevel subLevel) {
        if (isPlayerRagdollSubLevel(subLevel)) return true;
        if (isMobRagdollSubLevel(subLevel)) return true;
        return containsRagdollBlocks(subLevel);
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

    @Override
    public boolean canGrabSubLevel(Player player, ServerSubLevel subLevel) {
        if (!loaded || subLevel == null || subLevel.isRemoved()) return true;

        try {
            if (isPlayerRidingOwnRagdoll(player, subLevel)) {
                return false;
            }

            if (isPlayerRagdollSubLevel(subLevel)) {
                return ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingPlayerRagdolls;
            }

            if (isMobRagdollSubLevel(subLevel)) {
                if (!ServerConfig.INSTANCE.sableRagdollsCompatAllowGrabbingMobRagdolls) {
                    return false;
                }
                return canGrabMobRagdoll(subLevel);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[Barehanded] Error checking ragdoll grab permission", t);
        }

        return true;
    }
}