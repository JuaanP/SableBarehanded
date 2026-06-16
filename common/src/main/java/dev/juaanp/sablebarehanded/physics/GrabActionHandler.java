package dev.juaanp.sablebarehanded.physics;

import dev.juaanp.sablebarehanded.api.SableBarehandedEvents;
import dev.juaanp.sablebarehanded.config.ServerConfig;
import dev.juaanp.sablebarehanded.platform.Services;
import dev.juaanp.sablebarehanded.util.AssemblyBehaviorHelper;
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

        // 1. pos proviene del cliente y pertenece a las coordenadas del Plot interno.
        // Lo centramos añadiendo 0.5 a cada eje.
        Vector3d localGrabPosJoml = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // 2. Transformamos esa coordenada local a una coordenada global (Mundo real)
        Vector3d globalGrabBlockPos = serverSubLevel.logicalPose().transformPosition(new Vector3d(localGrabPosJoml));

        // 3. Calculamos la distancia usando la posición global del objeto
        float distance = (float) player.getEyePosition().distanceTo(new Vec3(globalGrabBlockPos.x, globalGrabBlockPos.y, globalGrabBlockPos.z));
        double reach = GrabPhysicsController.getGrabReach(player);

        if (distance > reach) {
            Services.NETWORK.sendStopGrabbingAnimation(player);
            return;
        }

        if (!SableBarehandedEvents.fireBeforeGrab(player, serverSubLevel)) {
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
        Quaterniond initialOrient = serverSubLevel.logicalPose().orientation();

        GrabSession session = new GrabSession(serverSubLevel, distance, localGrabPosJoml, localCenterOfMass, crosshairTarget, initialOrient, pipeline);

        pipeline.wakeUp(serverSubLevel);

        GrabPhysicsController.rebuildConstraint(session);
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

        if (!SableBarehandedEvents.fireBeforeAssemble(player, pos, blocks)) {
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
            SableBarehandedEvents.fireOnAssemble(player, serverSubLevel, blocks);

            boolean isFastLift = AssemblyBehaviorHelper.isFastLift(level, pos, mainState);

            if (!isFastLift) {
                net.minecraft.world.level.block.SoundType soundType = mainState.getSoundType();
                level.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(mainState));
            } else {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 0.5f);
            }

            for (BlockPos bPos : blocks) {
                level.updateNeighborsAt(bPos, net.minecraft.world.level.block.Blocks.AIR);
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    BlockPos neighbor = bPos.relative(dir);
                    level.neighborChanged(neighbor, net.minecraft.world.level.block.Blocks.AIR, bPos);

                    net.minecraft.world.level.material.FluidState fluid = level.getFluidState(neighbor);
                    if (!fluid.isEmpty()) {
                        level.scheduleTick(neighbor, fluid.getType(), fluid.getType().getTickDelay(level));
                    }

                    net.minecraft.world.level.block.state.BlockState neighborState = level.getBlockState(neighbor);
                    if (neighborState.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
                        level.scheduleTick(neighbor, neighborState.getBlock(), 2);
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
        Quaterniond initialOrient = serverSubLevel.logicalPose().orientation();

        GrabSession session = new GrabSession(serverSubLevel, distance, localGrabPosJoml, localCenterOfMass, crosshairTarget, initialOrient, pipeline);

        pipeline.wakeUp(serverSubLevel);
        GrabPhysicsController.rebuildConstraint(session);

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