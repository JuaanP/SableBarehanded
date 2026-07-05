package dev.juaanp.barehanded.physics.impact;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.physics.DisassembleHandler;
import dev.juaanp.barehanded.util.BlockReplacementHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ImpactFaceDetector {
    private static final double MIN_MOVEMENT_ALIGNMENT = 0.3;
    private static final double MIN_FACE_DIRECTION_ALIGNMENT = 0.2;
    private static final int MAX_REPLACEABLE_DEPTH = 3;

    private ImpactFaceDetector() {}

    public static Optional<ImpactResult> detectImpact(ServerLevel level, ServerSubLevel subLevel, Vec3 approachDir) {
        BoundingBox3dc bounds = subLevel.boundingBox();
        Vec3 sublevelCenter = new Vec3(
                (bounds.minX() + bounds.maxX()) / 2.0,
                (bounds.minY() + bounds.maxY()) / 2.0,
                (bounds.minZ() + bounds.maxZ()) / 2.0
        );

        Direction bestFace = null;
        BlockPos bestBlock = null;
        double bestScore = -1.0;
        List<BlockPos> bestBlocksToBreak = List.of();

        for (Direction face : Direction.values()) {
            Optional<ImpactCandidate> candidate = evaluateFace(level, subLevel, bounds, sublevelCenter, face, approachDir);
            if (candidate.isPresent() && candidate.get().score() > bestScore) {
                bestScore = candidate.get().score();
                bestFace = face;
                bestBlock = candidate.get().worldBlock();
                bestBlocksToBreak = candidate.get().blocksToBreak();
            }
        }

        if (bestFace == null || bestBlock == null) return Optional.empty();
        return Optional.of(new ImpactResult(bestFace, bestBlock, bestScore, bestBlocksToBreak));
    }

    private static Optional<ImpactCandidate> evaluateFace(
            ServerLevel level, ServerSubLevel subLevel, BoundingBox3dc bounds,
            Vec3 sublevelCenter, Direction face, Vec3 approachDir) {
        Vec3 faceNormal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        double movementAlignment = approachDir.dot(faceNormal);
        if (movementAlignment < MIN_MOVEMENT_ALIGNMENT) return Optional.empty();

        Vec3 faceCenter = DisassembleHandler.getFaceCenter(bounds, face);
        Vec3 vectorToFace = faceCenter.subtract(sublevelCenter).normalize();
        if (approachDir.dot(vectorToFace) < MIN_FACE_DIRECTION_ALIGNMENT) return Optional.empty();

        BlockPos faceBlockPos = BlockPos.containing(faceCenter);
        BlockPos currentPos = faceBlockPos.relative(face);

        List<BlockPos> blocksToBreak = new ArrayList<>();
        BlockPos solidBlock = null;

        for (int depth = 0; depth < MAX_REPLACEABLE_DEPTH; depth++) {
            BlockState state = level.getBlockState(currentPos);
            if (state.isAir()) break;
            if (Sable.HELPER.getContaining(level, currentPos) != null) return Optional.empty();

            VoxelShape collisionShape = state.getCollisionShape(level, currentPos);
            boolean hasCollision = !collisionShape.isEmpty();

            if (ServerConfig.INSTANCE.impactBreakReplaceableBlocks &&
                BlockReplacementHelper.isReplaceable(state, level, currentPos)) {
                blocksToBreak.add(currentPos);
                currentPos = currentPos.relative(face);
                continue;
            }

            if (hasCollision) {
                solidBlock = currentPos;
                break;
            }

            currentPos = currentPos.relative(face);
        }

        if (solidBlock == null) return Optional.empty();

        BlockState solidState = level.getBlockState(solidBlock);
        VoxelShape solidShape = solidState.getCollisionShape(level, solidBlock);
        if (solidShape.isEmpty()) return Optional.empty();

        AABB blockBounds = solidShape.bounds().move(solidBlock);
        double contactDistance = calculateContactDistance(faceCenter, blockBounds, face);

        double maxContactDistance = ServerConfig.INSTANCE.impactContactDistance;
        if (movementAlignment > 0.9) maxContactDistance *= 1.5;
        else if (movementAlignment > 0.7) maxContactDistance *= 1.2;

        if (contactDistance > maxContactDistance) return Optional.empty();

        double score = (movementAlignment * movementAlignment) * (1.0 / (1.0 + contactDistance));
        return Optional.of(new ImpactCandidate(solidBlock, score, List.copyOf(blocksToBreak)));
    }

    private static double calculateContactDistance(Vec3 faceCenter, AABB blockBounds, Direction face) {
        Vec3 blockFaceCenter = switch (face) {
            case EAST -> new Vec3(blockBounds.minX, cY(blockBounds), cZ(blockBounds));
            case WEST -> new Vec3(blockBounds.maxX, cY(blockBounds), cZ(blockBounds));
            case UP -> new Vec3(cX(blockBounds), blockBounds.minY, cZ(blockBounds));
            case DOWN -> new Vec3(cX(blockBounds), blockBounds.maxY, cZ(blockBounds));
            case SOUTH -> new Vec3(cX(blockBounds), cY(blockBounds), blockBounds.minZ);
            case NORTH -> new Vec3(cX(blockBounds), cY(blockBounds), blockBounds.maxZ);
        };
        return faceCenter.distanceTo(blockFaceCenter);
    }

    private static double cX(AABB b) { return (b.minX + b.maxX) / 2.0; }
    private static double cY(AABB b) { return (b.minY + b.maxY) / 2.0; }
    private static double cZ(AABB b) { return (b.minZ + b.maxZ) / 2.0; }

    private record ImpactCandidate(BlockPos worldBlock, double score, List<BlockPos> blocksToBreak) {}
}