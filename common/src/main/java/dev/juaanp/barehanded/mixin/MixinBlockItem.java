package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.physics.GrabActionHandler;
import dev.juaanp.barehanded.physics.ServerGrabManager;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(BlockItem.class)
public class MixinBlockItem {

    @Inject(method = "place", at = @At("RETURN"))
    private void barehanded$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!cir.getReturnValue().consumesAction()) return;

        Player player = context.getPlayer();
        if (player == null || !ServerConfig.INSTANCE.enablePhysicsBlockPlacement) return;

        if (ServerGrabManager.hasPendingPhysicsPlacement(player)) {
            ServerLevel level = (ServerLevel) context.getLevel();
            BlockPos targetPos = context.getClickedPos();

            if (dev.ryanhcode.sable.Sable.HELPER.getContaining(level, targetPos) != null) {
                return;
            }

            List<BlockPos> blocks = new ArrayList<>();
            blocks.add(targetPos);

            BoundingBox3i bounds = new BoundingBox3i(
                    targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                    targetPos.getX(), targetPos.getY(), targetPos.getZ()
            );

            ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, targetPos, blocks, bounds);

            if (subLevel != null) {
                ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
                if (container != null) {
                    container.physicsSystem().getPipeline().wakeUp(subLevel);
                }

                BlockPos localPos = subLevel.getPlot().getCenterBlock();
                GrabActionHandler.forceGrab(player, subLevel, localPos);
            }
        }
    }
}