package dev.juaanp.barehanded.compat;

import dev.juaanp.barehanded.Constants;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TreeModCompatHelper {

    private static final List<String> TREE_MOD_IDS = List.of(
            "dynamictrees",
            "treephysics",
            "fallingtree",
            "treechop",
            "ht_tree_plant",
            "tree_harvester",
            "lumberjack"
    );

    private static final TagKey<Block> C_LOGS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "logs"));
    private static final TagKey<Block> NEOFORGE_LOGS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("neoforge", "logs"));
    private static final TagKey<Block> FORGE_LOGS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "logs"));

    private static Boolean anyTreeModDetected = null;

    public static boolean isAnyTreeModLoaded() {
        if (anyTreeModDetected == null) {
            boolean found = false;
            for (String modId : TREE_MOD_IDS) {
                if (Services.PLATFORM.isModLoaded(modId)) {
                    found = true;
                    Constants.LOG.info("[Barehanded] Tree mod detected: {}", modId);
                    break;
                }
            }
            anyTreeModDetected = found;
        }
        return anyTreeModDetected;
    }

    public static boolean isLogBlock(BlockState state) {
        if (state == null || state.isAir()) return false;

        if (state.is(BlockTags.LOGS) || state.is(Constants.Tags.TREE_LOGS)
                || state.is(C_LOGS) || state.is(NEOFORGE_LOGS) || state.is(FORGE_LOGS)) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null) {
            String namespace = id.getNamespace().toLowerCase();
            if (TREE_MOD_IDS.contains(namespace)) {
                return true;
            }
        }

        return false;
    }

    public static ServerConfig.TreeAssemblyMode getEffectiveMode() {
        ServerConfig.TreeAssemblyMode mode = ServerConfig.INSTANCE.treeAssemblyMode;
        if (mode == ServerConfig.TreeAssemblyMode.AUTO) {
            return isAnyTreeModLoaded() ? ServerConfig.TreeAssemblyMode.BREAK : ServerConfig.TreeAssemblyMode.NONE;
        }
        return mode;
    }

    public static void breakBlockAsPlayer(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean wasShiftDown = serverPlayer.isShiftKeyDown();
            try {
                serverPlayer.setShiftKeyDown(false);
                serverPlayer.gameMode.destroyBlock(pos);
            } finally {
                serverPlayer.setShiftKeyDown(wasShiftDown);
            }
        } else if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.destroyBlock(pos, true, player);
        }
    }
}
