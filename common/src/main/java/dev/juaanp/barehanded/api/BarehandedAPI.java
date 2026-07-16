package dev.juaanp.barehanded.api;

import dev.juaanp.barehanded.physics.GrabActionHandler;
import dev.juaanp.barehanded.physics.GrabPhysicsController;
import dev.juaanp.barehanded.physics.GrabRotationController;
import dev.juaanp.barehanded.physics.ServerGrabManager;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BarehandedAPI {

    public static boolean isPlayerGrabbing(@NotNull Player player) {
        return ServerGrabManager.isPlayerGrabbing(player);
    }

    public static boolean isHoldingSubLevel(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        return ServerGrabManager.isHoldingSubLevel(player, subLevel);
    }

    @Nullable
    public static ServerSubLevel getGrabbedSubLevel(@NotNull Player player) {
        return ServerGrabManager.getGrabbedSubLevel(player);
    }

    public static double getGrabReach(@NotNull Player player) {
        return GrabPhysicsController.getGrabReach(player);
    }

    public static void forceDrop(@NotNull Player player) {
        ServerGrabManager.stopGrabbing(player.getUUID());
    }

    public static void forceGrab(@NotNull ServerPlayer serverPlayer, @NotNull BlockPos targetPos) {
        GrabActionHandler.startGrabbing(serverPlayer, targetPos);
    }

    public static void forceAssembleAndGrab(@NotNull Player player, @NotNull BlockPos targetPos) {
        GrabActionHandler.assembleAndGrab(player, targetPos);
    }

    public static void applyRotation(@NotNull Player player, double yaw, double pitch, boolean rotateAroundCenter) {
        GrabRotationController.applyRotation(player, yaw, pitch, rotateAroundCenter);
    }

    public static double getGrabbedMass(@NotNull Player player) {
        ServerSubLevel subLevel = getGrabbedSubLevel(player);
        if (subLevel == null) return 0.0;
        return subLevel.getMassTracker().getMass();
    }

    public static boolean hasSuperStrength(@NotNull Player player) {
        return dev.juaanp.barehanded.physics.GrabSession.hasSuperStrength(player);
    }
}