package dev.juaanp.barehanded.util;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class GrabSessionHelper {

    private static Set<ResourceLocation> dangerousBlockCache = null;

    private static Set<ResourceLocation> getSurfBlocks() {
        if (dangerousBlockCache == null) {
            dangerousBlockCache = new HashSet<>();
            for (String blockName : ServerConfig.INSTANCE.surfMechanicalBlocks) {
                try {
                    if (blockName != null && !blockName.isEmpty()) {
                        ResourceLocation loc = ResourceLocation.parse(blockName);
                        dangerousBlockCache.add(loc);
                    }
                } catch (Exception e) {
                }
            }
        }
        return dangerousBlockCache;
    }

    public static boolean containsSurfMechanicalBlocks(ServerSubLevel subLevel) {
        Set<ResourceLocation> dangerousBlocks = getSurfBlocks();
        if (dangerousBlocks.isEmpty()) return false;

        ServerLevel level = (ServerLevel) subLevel.getLevel();
        if (level == null) return false;

        for (PlotChunkHolder chunk : subLevel.getPlot().getLoadedChunks()) {
            if (chunk == null || chunk.getBoundingBox() == null) continue;

            var bounds = chunk.getBoundingBox();
            int minX = bounds.minX() + chunk.getPos().getMinBlockX();
            int maxX = bounds.maxX() + chunk.getPos().getMinBlockX();
            int minZ = bounds.minZ() + chunk.getPos().getMinBlockZ();
            int maxZ = bounds.maxZ() + chunk.getPos().getMinBlockZ();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        ResourceLocation blockId = state.getBlock().builtInRegistryHolder()
                                .key().location();

                        if (dangerousBlocks.contains(blockId)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}