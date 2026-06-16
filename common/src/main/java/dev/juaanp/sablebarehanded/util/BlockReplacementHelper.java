package dev.juaanp.sablebarehanded.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlockReplacementHelper {
    private BlockReplacementHelper() {}

    public static boolean isReplaceable(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.isAir()) return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            AABB bounds = shape.bounds();
            double volume = (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY) * (bounds.maxZ - bounds.minZ);
            if (volume > 0.05) return false;
        }

        if (state.getLightBlock(level, pos) > 1) return false;
        if (state.getBlock().defaultDestroyTime() > 0.5f) return false;

        PushReaction pushReaction = state.getPistonPushReaction();
        if (pushReaction == PushReaction.DESTROY) return true;
        if (state.canBeReplaced()) return true;

        return false;
    }

    public static boolean breakIfReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isReplaceable(state, level, pos)) {
            level.destroyBlock(pos, true);
            return true;
        }
        return false;
    }
}