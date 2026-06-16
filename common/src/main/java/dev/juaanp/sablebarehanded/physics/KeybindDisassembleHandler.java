package dev.juaanp.sablebarehanded.physics;

import dev.juaanp.sablebarehanded.config.ServerConfig;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class KeybindDisassembleHandler {
    public static void attemptDisassemble(ServerPlayer player, ServerSubLevel subLevel) {
        if (!ServerConfig.INSTANCE.enableKeybindDisassemble) return;

        if (!DisassembleHandler.isAlignedToGrid(subLevel,
                ServerConfig.INSTANCE.keybindRotationTolerance,
                ServerConfig.INSTANCE.keybindPositionTolerance)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        DisassembleHandler.PlacementResult placement = DisassembleHandler.computePlacementAtCurrentPosition(subLevel);
        BlockState placedBlockState = DisassembleHandler.getFirstBlockState(subLevel);

        boolean success = DisassembleHandler.disassemble(
                level, subLevel,
                placement.plotAnchor(), placement.disassemblyGoal(), placement.rotation(),
                null, null, placedBlockState
        );

        if (success) {
            ServerGrabManager.stopGrabbing(player.getUUID());
        } else {
            if (ServerConfig.INSTANCE.showDisassembleMessages) {
                player.displayClientMessage(
                        Component.literal("Cannot place here")
                                .withStyle(ChatFormatting.RED), true);
            }
        }
    }
}