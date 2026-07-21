package dev.juaanp.barehanded.util;

import dev.juaanp.barehanded.Constants;
import dev.juaanp.barehanded.compat.RagdollCompatService;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class AssemblyBehaviorHelper {

    public static boolean isIgnored(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return true;

        if (state.is(Constants.Tags.GRABBABLE)) return false;

        if (ServerConfig.INSTANCE.useWhitelistMode) {
            return true;
        }

        if (state.is(Blocks.SPAWNER)) {
            return !ServerConfig.INSTANCE.allowGrabbingSpawners;
        }

        if (state.getDestroySpeed(level, pos) < 0.0F) {
            if (!ServerConfig.INSTANCE.allowGrabbingUnbreakableBlocks) {
                return true;
            }
            if (!state.getFluidState().isEmpty() && !state.isSolidRender(level, pos)) return true;
            return false;
        }

        if (state.is(Constants.Tags.UNGRABBABLE)) return true;

        if (!state.getFluidState().isEmpty() && !state.isSolidRender(level, pos)) {
            return true;
        }

        return false;
    }

    public static boolean isFastLift(Level level, BlockPos pos, BlockState state) {
        return state.hasBlockEntity() && !state.isCollisionShapeFullBlock(level, pos);
    }

    public static List<BlockPos> getConnectedBlocks(Level level, BlockPos pos) {
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(pos);
        BlockState baseState = level.getBlockState(pos);

        if (baseState.getBlock() instanceof ChestBlock) {
            var type = baseState.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                blocks.add(pos.relative(ChestBlock.getConnectedDirection(baseState)));
            }
        } else if (baseState.getBlock() instanceof DoorBlock) {
            var half = baseState.getValue(DoorBlock.HALF);
            if (half == DoubleBlockHalf.LOWER) {
                blocks.add(pos.above());
            } else {
                blocks.add(pos.below());
            }
        } else if (baseState.getBlock() instanceof BedBlock) {
            var part = baseState.getValue(BedBlock.PART);
            var facing = baseState.getValue(BedBlock.FACING);
            if (part == BedPart.HEAD) {
                blocks.add(pos.relative(facing.getOpposite()));
            } else {
                blocks.add(pos.relative(facing));
            }
        }

        Set<BlockPos> assembly = new java.util.LinkedHashSet<>(blocks);
        Queue<BlockPos> queue = new LinkedList<>(blocks);

        LevelReader simulatedLevel = (LevelReader) Proxy.newProxyInstance(
                LevelReader.class.getClassLoader(),
                new Class<?>[]{LevelReader.class},
                (proxy, method, args) -> {
                    if (args != null && args.length == 1 && args[0] instanceof BlockPos p) {
                        if (assembly.contains(p)) {
                            Class<?> returnType = method.getReturnType();
                            if (returnType == BlockState.class) {
                                return Blocks.AIR.defaultBlockState();
                            } else if (returnType == net.minecraft.world.level.material.FluidState.class) {
                                return Fluids.EMPTY.defaultFluidState();
                            } else if (returnType == net.minecraft.world.level.block.entity.BlockEntity.class) {
                                return null;
                            }
                        }
                    }
                    try {
                        return method.invoke(level, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos adj = current.relative(dir);
                if (assembly.contains(adj)) continue;

                BlockState adjState = level.getBlockState(adj);

                if (isIgnored(level, adj, adjState)) continue;

                if (adjState.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) continue;

                if (!adjState.canSurvive(simulatedLevel, adj)) {
                    assembly.add(adj);
                    queue.add(adj);
                }
            }
        }

        List<BlockPos> result = new ArrayList<>(assembly);
        int limit = ServerConfig.INSTANCE.blockLimit;

        if (limit > 0 && result.size() > limit) {
            return result.subList(0, limit);
        }

        return result;
    }

    public static int calculateAssemblyTicks(Player player, Level level, List<BlockPos> blocks) {
        boolean isCreativeSuper = player.isCreative() && ServerConfig.INSTANCE.creativeSuperStrength;
        if (isCreativeSuper) return 1;

        int totalTicks = 0;
        for (BlockPos pos : blocks) {
            BlockState state = level.getBlockState(pos);
            if (isFastLift(level, pos, state)) {
                totalTicks += ServerConfig.INSTANCE.fastLiftAssemblyTicks;
            } else {
                float progressPerTick = state.getDestroyProgress(player, level, pos);
                if (progressPerTick <= 0.0F) return Integer.MAX_VALUE;
                int vanillaTicks = (int) Math.ceil(1.0F / progressPerTick);
                totalTicks += vanillaTicks;
            }
        }

        double strengthMulti = EncumbranceHelper.getStrengthMultiplier(player);

        return (int) Math.max(1, (totalTicks / strengthMulti) / ServerConfig.INSTANCE.barehandedAssemblySpeedMultiplier);
    }
}