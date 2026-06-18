package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public class MixinFallingBlock {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void barehanded$preventFallInSubLevels(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (ServerConfig.INSTANCE.preventGravityInSubLevels) {
            if (level.dimension().location().getNamespace().equals("sable") || Sable.HELPER.getContaining(level, pos) != null) {
                ci.cancel();
            }
        }
    }
}