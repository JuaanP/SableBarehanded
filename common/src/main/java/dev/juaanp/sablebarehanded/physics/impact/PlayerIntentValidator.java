package dev.juaanp.sablebarehanded.physics.impact;

import dev.juaanp.sablebarehanded.config.ServerConfig;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class PlayerIntentValidator {
    private static final double M_S_TO_B_T = 1.0 / 20.0;

    private PlayerIntentValidator() {}

    public static boolean wasIntentionalImpact(ServerPlayer player, ServerSubLevel subLevel, Vector3d previousVelocity) {
        if (!ServerConfig.INSTANCE.impactRequireIntentionalThrow) return true;

        Vec3 playerVelocity = player.getDeltaMovement();
        double playerSpeed = playerVelocity.length();
        double objectSpeed = previousVelocity.length() * M_S_TO_B_T;

        if (objectSpeed < 0.01) return false;

        double speedRatio = objectSpeed / Math.max(playerSpeed, 0.01);
        if (speedRatio >= ServerConfig.INSTANCE.impactThrowSpeedRatio) return true;

        if (playerSpeed < 0.1 && objectSpeed > ServerConfig.INSTANCE.impactMinSpeed) return true;

        return false;
    }

    public static Vec3 getApproachDirection(Vector3d velocity) {
        if (velocity.lengthSquared() < 0.0001) return Vec3.ZERO;
        return new Vec3(velocity.x, velocity.y, velocity.z).normalize();
    }
}