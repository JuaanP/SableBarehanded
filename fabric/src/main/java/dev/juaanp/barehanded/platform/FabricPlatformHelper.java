package dev.juaanp.barehanded.platform;

import dev.juaanp.barehanded.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean fireBlockBreakEvent(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, be);
        if (allowed) {
            if (player.level() == level) {
                player.gameMode.destroyBlock(pos);
            } else {
                level.destroyBlock(pos, true, player);
            }
            PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, be);
        }
        return allowed;
    }
}