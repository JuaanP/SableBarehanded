package dev.juaanp.barehanded.api;

import dev.juaanp.barehanded.physics.GrabActionHandler;
import dev.juaanp.barehanded.physics.GrabPhysicsController;
import dev.juaanp.barehanded.physics.GrabRotationController;
import dev.juaanp.barehanded.physics.ServerGrabManager;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class BarehandedAPI {

    public static boolean isPlayerGrabbing(Player player) {
        return ServerGrabManager.isPlayerGrabbing(player);
    }

    public static boolean isHoldingSubLevel(Player player, ServerSubLevel subLevel) {
        return ServerGrabManager.isHoldingSubLevel(player, subLevel);
    }

    @Nullable
    public static ServerSubLevel getGrabbedSubLevel(Player player) {
        return ServerGrabManager.getGrabbedSubLevel(player);
    }

    public static double getGrabReach(Player player) {
        return GrabPhysicsController.getGrabReach(player);
    }

    public static void forceDrop(Player player) {
        ServerGrabManager.stopGrabbing(player.getUUID());
    }

    public static void forceGrab(ServerPlayer serverPlayer, BlockPos targetPos) {
        GrabActionHandler.startGrabbing(serverPlayer, targetPos);
    }

    public static void forceAssembleAndGrab(Player player, BlockPos targetPos) {
        GrabActionHandler.assembleAndGrab(player, targetPos);
    }

    public static void applyRotation(Player player, double yaw, double pitch, boolean rotateAroundCenter) {
        GrabRotationController.applyRotation(player, yaw, pitch, rotateAroundCenter);
    }
}