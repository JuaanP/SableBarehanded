package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.util.BlockReplacementHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class DisassembleHandler {

    public record PlacementResult(BlockPos plotAnchor, BlockPos disassemblyGoal, Rotation rotation) {}

    public static boolean disassemble(ServerLevel worldLevel, ServerSubLevel subLevel,
                                      BlockPos subLevelAnchor, BlockPos disassemblyGoal, Rotation rotation,
                                      BlockState impactedBlock, Direction impactFace, BlockState placedBlockState) {
        BoundingBox3i plotBounds = new BoundingBox3i(subLevel.getPlot().getBoundingBox());

        int angle = rotation == Rotation.NONE ? 0 : (4 - rotation.ordinal());
        SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(
                subLevelAnchor, disassemblyGoal, angle, rotation, worldLevel
        );

        ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
        LevelPlot plot = subLevel.getPlot();
        ServerLevel subLevelLevel = (ServerLevel) subLevel.getLevel();

        for (PlotChunkHolder chunk : plot.getLoadedChunks()) {
            BoundingBox3ic localChunkBounds = chunk.getBoundingBox();
            if (localChunkBounds == null || localChunkBounds == BoundingBox3i.EMPTY) continue;

            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
                for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                    for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                        BlockPos pos = new BlockPos(
                                x + chunk.getPos().getMinBlockX(), y,
                                z + chunk.getPos().getMinBlockZ()
                        );
                        BlockState state = subLevelLevel.getBlockState(pos);
                        if (!state.isAir()) blocks.add(pos);
                    }
                }
            }
        }

        if (!blocks.isEmpty()) {
            blocks.sort((p1, p2) -> Integer.compare(p2.getY(), p1.getY()));

            ObjectArrayList<BlockPos> blocksToDestroy = new ObjectArrayList<>();

            for (BlockPos plotPos : blocks) {
                BlockPos worldPos = transform.apply(plotPos);
                BlockState existingState = worldLevel.getBlockState(worldPos);

                if (!existingState.isAir()) {
                    if (ServerConfig.INSTANCE.impactBreakReplaceableBlocks &&
                            BlockReplacementHelper.isReplaceable(existingState, worldLevel, worldPos)) {
                        if (!blocksToDestroy.contains(worldPos)) {
                            blocksToDestroy.add(worldPos);
                        }
                    } else {
                        return false;
                    }
                }
            }

            boolean hasSupport = false;
            for (BlockPos plotPos : blocks) {
                BlockPos worldPos = transform.apply(plotPos);
                if (hasAdjacentSolidBlock(worldLevel, worldPos, blocksToDestroy)) {
                    hasSupport = true;
                    break;
                }
            }

            if (!hasSupport) return false;

            for (BlockPos pos : blocksToDestroy) {
                worldLevel.destroyBlock(pos, true);
            }

            ((ServerLevelPlot) plot).kickAllEntities();
            SubLevelAssemblyHelper.moveBlocks(subLevelLevel, transform, blocks);
        } else {
            subLevel.markRemoved();
        }

        SubLevelAssemblyHelper.moveTrackingPoints(worldLevel, plotBounds, null, transform);

        boolean hasImpactEffects = impactedBlock != null && impactFace != null;
        boolean hasPlacementEffects = placedBlockState != null && !placedBlockState.isAir();

        if (hasImpactEffects) {
            SoundType impactedSound = impactedBlock.getSoundType();
            worldLevel.playSound(null, disassemblyGoal, impactedSound.getBreakSound(), SoundSource.BLOCKS, 1.0f, 0.8f);
            spawnImpactParticles(worldLevel, disassemblyGoal, impactFace, impactedBlock);
        }

        if (hasPlacementEffects) {
            SoundType placedSound = placedBlockState.getSoundType();
            float placePitch = hasImpactEffects ? 1.2f : 0.8f;
            float placeVolume = hasImpactEffects ? 0.9f : 1.5f;
            worldLevel.playSound(null, disassemblyGoal, placedSound.getPlaceSound(), SoundSource.BLOCKS, placeVolume, placePitch);
            Direction placementFace = hasImpactEffects ? impactFace.getOpposite() : Direction.UP;
            spawnPlacementParticles(worldLevel, disassemblyGoal, placementFace, placedBlockState);
        } else if (!hasImpactEffects) {
            worldLevel.playSound(null, disassemblyGoal, net.minecraft.sounds.SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        return true;
    }

    private static boolean hasAdjacentSolidBlock(ServerLevel level, BlockPos pos, ObjectArrayList<BlockPos> blocksToDestroy) {
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            if (blocksToDestroy != null && blocksToDestroy.contains(adjacent)) continue;

            BlockState state = level.getBlockState(adjacent);
            if (!state.isAir() && state.isCollisionShapeFullBlock(level, adjacent)) return true;
        }
        return false;
    }

    public static PlacementResult computePlacement(ServerSubLevel subLevel, Direction impactFace, BlockPos targetWorldBlock) {
        BoundingBox3dc bounds = subLevel.boundingBox();
        Vec3 faceCenterWorld = getFaceCenter(bounds, impactFace);
        Vector3d localContactPoint = subLevel.logicalPose().transformPositionInverse(new Vector3d(faceCenterWorld.x, faceCenterWorld.y, faceCenterWorld.z));

        BlockPos plotAnchor = findNearestSolidPlotBlock(subLevel, localContactPoint);
        if (plotAnchor == null) plotAnchor = BlockPos.containing(localContactPoint.x, localContactPoint.y, localContactPoint.z);

        Vec3 targetFaceCenter = Vec3.atCenterOf(targetWorldBlock).add(
                impactFace.getOpposite().getStepX() * 0.5,
                impactFace.getOpposite().getStepY() * 0.5,
                impactFace.getOpposite().getStepZ() * 0.5
        );

        Vector3d plotAnchorCenter = new Vector3d(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
        Vector3d offset = new Vector3d(localContactPoint).sub(plotAnchorCenter);

        Rotation rotation = getSubLevelRotation(subLevel);
        int angle = rotation == Rotation.NONE ? 0 : (4 - rotation.ordinal());
        double angleRad = angle * Math.PI / 2.0;

        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        Vector3d rotatedOffset = new Vector3d(
                offset.x * cos + offset.z * sin, offset.y,
                -offset.x * sin + offset.z * cos
        );

        Vector3d disassemblyGoalCenter = new Vector3d(targetFaceCenter.x, targetFaceCenter.y, targetFaceCenter.z).sub(rotatedOffset);
        BlockPos disassemblyGoal = BlockPos.containing(disassemblyGoalCenter.x, disassemblyGoalCenter.y, disassemblyGoalCenter.z);

        return new PlacementResult(plotAnchor, disassemblyGoal, rotation);
    }

    public static PlacementResult computePlacementAtCurrentPosition(ServerSubLevel subLevel) {
        BlockPos plotAnchor = getFirstSolidBlockPos(subLevel);
        if (plotAnchor == null) plotAnchor = BlockPos.ZERO;

        Vector3d localCenter = new Vector3d(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
        Vector3d worldCenter = subLevel.logicalPose().transformPosition(localCenter);

        BlockPos disassemblyGoal = BlockPos.containing(worldCenter.x, worldCenter.y, worldCenter.z);
        Rotation rotation = getSubLevelRotation(subLevel);

        return new PlacementResult(plotAnchor, disassemblyGoal, rotation);
    }

    public static BlockPos getFirstSolidBlockPos(ServerSubLevel subLevel) {
        ServerLevel subLevelLevel = (ServerLevel) subLevel.getLevel();
        for (PlotChunkHolder chunk : subLevel.getPlot().getLoadedChunks()) {
            BoundingBox3ic bounds = chunk.getBoundingBox();
            if (bounds == null || bounds == BoundingBox3i.EMPTY) continue;
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        BlockPos pos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                        if (!subLevelLevel.getBlockState(pos).isAir()) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BlockState getFirstBlockState(ServerSubLevel subLevel) {
        BlockPos pos = getFirstSolidBlockPos(subLevel);
        if (pos != null) {
            return ((ServerLevel) subLevel.getLevel()).getBlockState(pos);
        }
        return null;
    }

    private static BlockPos findNearestSolidPlotBlock(ServerSubLevel subLevel, Vector3d targetLocal) {
        LevelPlot plot = subLevel.getPlot();
        ServerLevel subLevelLevel = (ServerLevel) subLevel.getLevel();
        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;

        for (PlotChunkHolder chunk : plot.getLoadedChunks()) {
            BoundingBox3ic localChunkBounds = chunk.getBoundingBox();
            if (localChunkBounds == null || localChunkBounds == BoundingBox3i.EMPTY) continue;

            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
                for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                    for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                        BlockPos pos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                        BlockState state = subLevelLevel.getBlockState(pos);
                        if (!state.isAir()) {
                            double dist = pos.distToCenterSqr(targetLocal.x, targetLocal.y, targetLocal.z);
                            if (dist < minDist) { minDist = dist; nearest = pos; }
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private static void spawnPlacementParticles(ServerLevel level, BlockPos pos, Direction face, BlockState state) {
        Vec3 center = Vec3.atCenterOf(pos);
        double x = center.x + face.getStepX() * 0.5;
        double y = center.y + face.getStepY() * 0.5;
        double z = center.z + face.getStepZ() * 0.5;
        BlockParticleOption data = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 20; i++) {
            double sx = face.getAxis() == Direction.Axis.X ? 0 : (level.random.nextDouble() - 0.5) * 0.8;
            double sy = face.getAxis() == Direction.Axis.Y ? 0 : (level.random.nextDouble() - 0.5) * 0.8;
            double sz = face.getAxis() == Direction.Axis.Z ? 0 : (level.random.nextDouble() - 0.5) * 0.8;
            double vx = face.getStepX() * 0.15 + (level.random.nextDouble() - 0.5) * 0.15;
            double vy = face.getStepY() * 0.15 + (level.random.nextDouble() - 0.5) * 0.15;
            double vz = face.getStepZ() * 0.15 + (level.random.nextDouble() - 0.5) * 0.15;
            level.sendParticles(data, x + sx, y + sy, z + sz, 1, vx, vy, vz, 0.1);
        }
    }

    private static void spawnImpactParticles(ServerLevel level, BlockPos pos, Direction face, BlockState state) {
        Vec3 center = Vec3.atCenterOf(pos);
        double x = center.x + face.getStepX() * 0.5;
        double y = center.y + face.getStepY() * 0.5;
        double z = center.z + face.getStepZ() * 0.5;
        BlockParticleOption data = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 30; i++) {
            double sx = face.getAxis() == Direction.Axis.X ? 0 : (level.random.nextDouble() - 0.5) * 0.8;
            double sy = face.getAxis() == Direction.Axis.Y ? 0 : (level.random.nextDouble() - 0.5) * 0.8;
            double sz = face.getAxis() == Direction.Axis.Z ? 0 : (level.random.nextDouble() - 0.5) * 0.8;
            double vx = face.getStepX() * 0.3 + (level.random.nextDouble() - 0.5) * 0.2;
            double vy = face.getStepY() * 0.3 + (level.random.nextDouble() - 0.5) * 0.2;
            double vz = face.getStepZ() * 0.3 + (level.random.nextDouble() - 0.5) * 0.2;
            level.sendParticles(data, x + sx, y + sy, z + sz, 1, vx, vy, vz, 0.1);
        }
    }

    public static Vec3 getFaceCenter(BoundingBox3dc bounds, Direction face) {
        double x = (bounds.minX() + bounds.maxX()) / 2.0;
        double y = (bounds.minY() + bounds.maxY()) / 2.0;
        double z = (bounds.minZ() + bounds.maxZ()) / 2.0;
        if (face == Direction.EAST) x = bounds.maxX();
        else if (face == Direction.WEST) x = bounds.minX();
        else if (face == Direction.UP) y = bounds.maxY();
        else if (face == Direction.DOWN) y = bounds.minY();
        else if (face == Direction.SOUTH) z = bounds.maxZ();
        else if (face == Direction.NORTH) z = bounds.minZ();
        return new Vec3(x, y, z);
    }

    public static Rotation getSubLevelRotation(ServerSubLevel subLevel) {
        Quaterniond q = subLevel.logicalPose().orientation();
        double yaw = Math.atan2(2.0 * (q.y * q.w - q.x * q.z), 1.0 - 2.0 * (q.y * q.y + q.x * q.x));
        double degrees = (Math.toDegrees(yaw) + 360) % 360;
        if (degrees >= 315 || degrees < 45) return Rotation.NONE;
        if (degrees >= 45 && degrees < 135) return Rotation.CLOCKWISE_90;
        if (degrees >= 135 && degrees < 225) return Rotation.CLOCKWISE_180;
        return Rotation.COUNTERCLOCKWISE_90;
    }

    public static boolean isAlignedToGrid(ServerSubLevel subLevel, double rotationTolerance, double positionTolerance) {
        Quaterniond orientation = subLevel.logicalPose().orientation();
        double angle = Math.toDegrees(2.0 * Math.acos(Math.abs(orientation.w)));
        if (angle > 180.0) angle = 360.0 - angle;
        double nearest90 = Math.round(angle / 90.0) * 90.0;
        double rotationDeviation = Math.abs(angle - nearest90);
        double effectiveRotTolerance = rotationDeviation < rotationTolerance * 0.5 ? rotationTolerance * 1.5 : rotationTolerance;
        if (rotationDeviation > effectiveRotTolerance) return false;

        if (positionTolerance >= 1.0) return true;

        BlockPos anchor = getFirstSolidBlockPos(subLevel);
        if (anchor == null) anchor = BlockPos.ZERO;
        Vector3d localCenter = new Vector3d(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5);
        Vector3d worldCenter = subLevel.logicalPose().transformPosition(localCenter);

        double fracX = Math.abs(worldCenter.x - Math.floor(worldCenter.x));
        double fracY = Math.abs(worldCenter.y - Math.floor(worldCenter.y));
        double fracZ = Math.abs(worldCenter.z - Math.floor(worldCenter.z));

        double errX = Math.abs(fracX - 0.5);
        double errY = Math.abs(fracY - 0.5);
        double errZ = Math.abs(fracZ - 0.5);

        int goodCoords = 0;
        if (errX < positionTolerance) goodCoords++;
        if (errY < positionTolerance) goodCoords++;
        if (errZ < positionTolerance) goodCoords++;
        return goodCoords >= 2;
    }

    public static int getBlockCount(ServerSubLevel subLevel) {
        ServerLevel subLevelLevel = (ServerLevel) subLevel.getLevel();
        int count = 0;
        for (PlotChunkHolder chunk : subLevel.getPlot().getLoadedChunks()) {
            BoundingBox3ic bounds = chunk.getBoundingBox();
            if (bounds == null || bounds == BoundingBox3i.EMPTY) continue;
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        BlockPos pos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                        if (!subLevelLevel.getBlockState(pos).isAir()) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
}