package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.api.BarehandedEvents;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.platform.Services;
import dev.juaanp.barehanded.util.AssemblyBehaviorHelper;
import dev.juaanp.barehanded.util.GrabSessionHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

public class GrabActionHandler {

    public static void startGrabbing(Player player, BlockPos pos) {
        Level level = player.level();
        if (level.isClientSide()) return;

        if (!ServerGrabManager.canPlayerGrab(player) || !player.getMainHandItem().isEmpty()) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        SubLevel target = Sable.HELPER.getContaining(level, pos);
        if (!(target instanceof ServerSubLevel serverSubLevel)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        Vector3d localGrabPosJoml = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vector3d globalGrabBlockPos = serverSubLevel.logicalPose().transformPosition(new Vector3d(localGrabPosJoml));

        float distance = (float) player.getEyePosition().distanceTo(new Vec3(globalGrabBlockPos.x, globalGrabBlockPos.y, globalGrabBlockPos.z));
        double reach = GrabPhysicsController.getGrabReach(player);

        double serverTolerance = ServerConfig.INSTANCE.assemblyServerDistanceTolerance;

        if (distance > (reach + serverTolerance)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        if (!BarehandedEvents.fireBeforeGrab(player, serverSubLevel)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline = ((ServerSubLevelContainer) SubLevelContainer.getContainer(level)).physicsSystem().getPipeline();

        org.joml.Vector3dc com = serverSubLevel.getMassTracker().getCenterOfMass();
        if (com == null || serverSubLevel.getMassTracker().getMass() <= ServerConfig.INSTANCE.minPhysicsMass) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        int limit = ServerConfig.INSTANCE.blockLimit;
        if (limit > 0) {
            int blockCount = 0;
            ServerLevel subLevelLevel = (ServerLevel) serverSubLevel.getLevel();
            for (dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder chunk : serverSubLevel.getPlot().getLoadedChunks()) {
                BoundingBox3ic bounds = chunk.getBoundingBox();
                if (bounds == null) continue;
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                            BlockPos iterPos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                            if (!subLevelLevel.getBlockState(iterPos).isAir()) {
                                blockCount++;
                                if (blockCount > limit) {
                                    player.displayClientMessage(Component.literal("[Sable] Too many blocks (Limit: " + limit + ")").withStyle(ChatFormatting.RED), true);
                                    Services.NETWORK.sendStopGrabbingAnimation(player);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        Vector3d localCenterOfMass = new Vector3d(com);
        Vector3d crosshairTarget = JOMLConversion.toJOML(player.getEyePosition().add(player.getLookAngle().scale(Math.max(ServerConfig.INSTANCE.minDistance, distance))));
        Quaterniond initialOrient = new Quaterniond(serverSubLevel.logicalPose().orientation());

        boolean hasSurf = GrabSessionHelper.containsSurfMechanicalBlocks(serverSubLevel);

        GrabSession session = new GrabSession(
                serverSubLevel, distance, localGrabPosJoml, localCenterOfMass,
                crosshairTarget, initialOrient, pipeline, hasSurf
        );

        pipeline.wakeUp(serverSubLevel);
        GrabPhysicsController.rebuildConstraint(session);
        GrabPhysicsController.setGraceTicks(player, 5);

        Services.NETWORK.sendStartGrabbingAnimation(player);
        Services.NETWORK.sendSyncGrabState(player,
                serverSubLevel.getMassTracker().getMass(),
                serverSubLevel.getUniqueId(),
                localGrabPosJoml,
                distance
        );
        ServerGrabManager.registerGrab(player, session);
    }

    public static void assembleAndGrab(Player player, BlockPos pos) {
        Level level = player.level();
        if (level.isClientSide()) return;

        if (!ServerGrabManager.canPlayerGrab(player) || !player.getMainHandItem().isEmpty()) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        SubLevel existingSubLevel = Sable.HELPER.getContaining(level, pos);
        if (existingSubLevel instanceof ServerSubLevel serverSubLevel) {
            if (!ServerConfig.INSTANCE.enableRipOffBlocks) {
                Services.NETWORK.sendStopGrabbingAnimation(player);
                return;
            }
            ripBlockOffAndGrab(player, serverSubLevel, pos);
            return;
        }

        double reach = GrabPhysicsController.getGrabReach(player);
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (reach * reach)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        double maxDist = ServerConfig.INSTANCE.barehandedAssemblyMaxDistance + ServerConfig.INSTANCE.assemblyServerDistanceTolerance;
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > (maxDist * maxDist)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        BlockState mainState = level.getBlockState(pos);
        if (AssemblyBehaviorHelper.isIgnored(level, pos, mainState)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        List<BlockPos> blocks = AssemblyBehaviorHelper.getConnectedBlocks(level, pos);

        if (!BarehandedEvents.fireBeforeAssemble(player, pos, blocks)) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        for (BlockPos bPos : blocks) {
            if (Sable.HELPER.getContaining(level, bPos) != null) {
                Services.NETWORK.sendStopGrabbingAnimation(player);
                return;
            }
        }

        BoundingBox3i bounds = BoundingBox3i.from(blocks);
        SubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel) level, pos, blocks, bounds);

        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            BarehandedEvents.fireOnAssemble(player, serverSubLevel, blocks);

            boolean isFastLift = AssemblyBehaviorHelper.isFastLift(level, pos, mainState);

            if (!isFastLift) {
                net.minecraft.world.level.block.SoundType soundType = mainState.getSoundType();
                level.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(mainState));
            } else {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 0.5f);
            }

            for (BlockPos bPos : blocks) {
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    BlockPos neighbor = bPos.relative(dir);
                    if (!blocks.contains(neighbor)) {
                        BlockState neighborState = level.getBlockState(neighbor);

                        if (neighborState.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
                            level.scheduleTick(neighbor, neighborState.getBlock(), 15);
                        } else {
                            level.neighborChanged(neighbor, net.minecraft.world.level.block.Blocks.AIR, bPos);

                            net.minecraft.world.level.material.FluidState fluid = level.getFluidState(neighbor);
                            if (!fluid.isEmpty()) {
                                level.scheduleTick(neighbor, fluid.getType(), fluid.getType().getTickDelay(level));
                            }
                        }
                    }
                }
            }

            ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);

            if (container != null) {
                container.physicsSystem().getPipeline().wakeUp(serverSubLevel);
            }

            BlockPos localPos = serverSubLevel.getPlot().getCenterBlock();
            forceGrab(player, serverSubLevel, localPos);
        } else {
            Services.NETWORK.sendStopGrabbingAnimation(player);
        }
    }

    private static void ripBlockOffAndGrab(Player player, ServerSubLevel serverSubLevel, BlockPos localPos) {
        ServerLevel subLevelLevel = (ServerLevel) serverSubLevel.getLevel();
        BlockState stateToRip = subLevelLevel.getBlockState(localPos);

        if (stateToRip.isAir()) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        if (DisassembleHandler.getBlockCount(serverSubLevel) <= 1) {
            forceGrab(player, serverSubLevel, localPos);
            return;
        }

        subLevelLevel.setBlock(localPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        player.level().playSound(null, player.blockPosition(), stateToRip.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);

        Vector3d localCenter = new Vector3d(localPos.getX() + 0.5, localPos.getY() + 0.5, localPos.getZ() + 0.5);
        Vector3d globalCenter = serverSubLevel.logicalPose().transformPosition(localCenter);
        Quaterniond originalRot = new Quaterniond(serverSubLevel.logicalPose().orientation());

        BlockPos globalBlockPos = BlockPos.containing(globalCenter.x, globalCenter.y, globalCenter.z);

        ServerLevel worldLevel = (ServerLevel) player.level();
        BlockState originalWorldState = worldLevel.getBlockState(globalBlockPos);

        if (originalWorldState.canBeReplaced()) {
            worldLevel.setBlock(globalBlockPos, stateToRip, 3);
            SubLevel newSubLevel = SubLevelAssemblyHelper.assembleBlocks(worldLevel, globalBlockPos, List.of(globalBlockPos), BoundingBox3i.from(List.of(globalBlockPos)));
            if (newSubLevel instanceof ServerSubLevel newServerSubLevel) {

                BlockPos newLocalPos = DisassembleHandler.getFirstSolidBlockPos(newServerSubLevel);
                if (newLocalPos == null) newLocalPos = newServerSubLevel.getPlot().getCenterBlock();

                Vector3d newLocalCenter = new Vector3d(newLocalPos.getX() + 0.5, newLocalPos.getY() + 0.5, newLocalPos.getZ() + 0.5);

                Vector3d rotPoint = newServerSubLevel.logicalPose().rotationPoint();
                Vector3d offsetFromRot = new Vector3d(newLocalCenter).sub(rotPoint);
                Vector3d rotatedOffset = new Vector3d(offsetFromRot).rotate(originalRot);
                Vector3d targetPos = new Vector3d(globalCenter).sub(rotatedOffset);

                newServerSubLevel.logicalPose().position().set(targetPos);
                newServerSubLevel.logicalPose().orientation().set(originalRot);

                dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline = ((ServerSubLevelContainer) SubLevelContainer.getContainer(worldLevel)).physicsSystem().getPipeline();
                pipeline.teleport(newServerSubLevel, targetPos, originalRot);

                newServerSubLevel.latestLinearVelocity.set(serverSubLevel.latestLinearVelocity);
                newServerSubLevel.latestAngularVelocity.set(serverSubLevel.latestAngularVelocity);

                forceGrab(player, newServerSubLevel, newLocalPos);
                return;
            }
        } else {
            net.minecraft.world.level.block.Block.dropResources(stateToRip, worldLevel, globalBlockPos);
        }
        Services.NETWORK.sendStopGrabbingAnimation(player);
    }

    public static void forceGrab(Player player, ServerSubLevel serverSubLevel, BlockPos localGrabBlock) {
        if (!ServerGrabManager.canPlayerGrab(player)) return;

        dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline = ((ServerSubLevelContainer) SubLevelContainer.getContainer(player.level())).physicsSystem().getPipeline();

        org.joml.Vector3dc com = serverSubLevel.getMassTracker().getCenterOfMass();
        if (com == null || serverSubLevel.getMassTracker().getMass() <= ServerConfig.INSTANCE.minPhysicsMass) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        Vector3d localCenterOfMass = new Vector3d(com);
        Vector3d localGrabPosJoml = new Vector3d(localGrabBlock.getX() + 0.5, localGrabBlock.getY() + 0.5, localGrabBlock.getZ() + 0.5);

        Vector3d globalGrabBlockPos = serverSubLevel.logicalPose().transformPosition(new Vector3d(localGrabPosJoml));
        float distance = (float) player.getEyePosition().distanceTo(new Vec3(globalGrabBlockPos.x, globalGrabBlockPos.y, globalGrabBlockPos.z));

        Vector3d crosshairTarget = JOMLConversion.toJOML(player.getEyePosition().add(player.getLookAngle().scale(Math.max(ServerConfig.INSTANCE.minDistance, distance))));
        Quaterniond initialOrient = new Quaterniond(serverSubLevel.logicalPose().orientation());

        boolean hasSurf = GrabSessionHelper.containsSurfMechanicalBlocks(serverSubLevel);

        GrabSession session = new GrabSession(
                serverSubLevel, distance, localGrabPosJoml, localCenterOfMass,
                crosshairTarget, initialOrient, pipeline, hasSurf
        );

        pipeline.wakeUp(serverSubLevel);
        GrabPhysicsController.rebuildConstraint(session);
        GrabPhysicsController.setGraceTicks(player, 5);

        Services.NETWORK.sendStartGrabbingAnimation(player);
        Services.NETWORK.sendSyncGrabState(player,
                serverSubLevel.getMassTracker().getMass(),
                serverSubLevel.getUniqueId(),
                localGrabPosJoml,
                distance
        );
        ServerGrabManager.registerGrab(player, session);
    }
}