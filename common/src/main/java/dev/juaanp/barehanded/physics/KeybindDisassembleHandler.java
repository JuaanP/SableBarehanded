package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class KeybindDisassembleHandler {
    public static void attemptDisassemble(ServerPlayer player, ServerSubLevel subLevel, boolean isAltDown) {
        if (!ServerConfig.INSTANCE.enableKeybindDisassemble) return;

        int limit = ServerConfig.INSTANCE.disassembleBlockLimit;
        if (limit > 0 && DisassembleHandler.getBlockCount(subLevel) > limit) {
            if (ServerConfig.INSTANCE.showDisassembleMessages) {
                player.displayClientMessage(
                        Component.literal("Structure is too large to disassemble (Limit: " + limit + ")")
                                .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        ServerSubLevel targetShip = DisassembleHandler.findTargetSubLevel(level, subLevel);

        if (targetShip != null) {
            boolean success = DisassembleHandler.disassembleIntoSubLevel(level, subLevel, targetShip, player);
            if (success) {
                ServerGrabManager.stopGrabbing(player.getUUID());
            }
            return;
        }

        if (isAltDown) {
            if (ServerConfig.INSTANCE.showDisassembleMessages) {
                player.displayClientMessage(Component.literal("Merge Failed: No support detected").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (!DisassembleHandler.isAlignedToGrid(subLevel,
                ServerConfig.INSTANCE.keybindRotationTolerance,
                ServerConfig.INSTANCE.keybindPositionTolerance)) {
            return;
        }

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