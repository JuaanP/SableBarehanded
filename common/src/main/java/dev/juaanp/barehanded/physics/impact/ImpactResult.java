package dev.juaanp.barehanded.physics.impact;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public record ImpactResult(
    Direction face,
    BlockPos worldBlock,
    double score,
    List<BlockPos> blocksToBreak
) {}