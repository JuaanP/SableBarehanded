package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

public class GrabEncumbranceSystem {

    public static void applyMovementPenalty(Player player, GrabSession grab, double tension, double actualMaxForce) {
        if (!ServerConfig.INSTANCE.enableEncumbrance || player.isCreative() || player.isSpectator()) {
            ServerGrabManager.clearPlayerMovementPenalty(player);
            return;
        }

        double mass = grab.subLevel.getMassTracker().getMass();
        double objectWeight = mass * ServerConfig.INSTANCE.physicsGravity;
        double weightRatio = Mth.clamp(objectWeight / actualMaxForce, 0.0, 1.0);

        double weightPenalty = weightRatio * ServerConfig.INSTANCE.weightPenaltyMultiplier;

        double tensionPenalty = 0.0;
        if (tension > grab.distance + ServerConfig.INSTANCE.tensionPenaltyStartOffset) {
            double tensionRatio = Mth.clamp((tension - grab.distance) / ServerConfig.INSTANCE.tensionPenaltyMaxDistance, 0.0, 1.0);
            tensionPenalty = tensionRatio * ServerConfig.INSTANCE.tensionPenaltyMultiplier;
        }

        Vector3d blockVel = new Vector3d(grab.subLevel.latestLinearVelocity);
        double blockSpeed = blockVel.length();
        double kineticRatio = Mth.clamp(blockSpeed / ServerConfig.INSTANCE.kineticPenaltyReferenceSpeed, 0.0, 1.0);
        double kineticPenalty = kineticRatio * ServerConfig.INSTANCE.kineticPenaltyMultiplier;

        double totalPenalty = ServerConfig.INSTANCE.baseMovementPenalty + weightPenalty + tensionPenalty + kineticPenalty;
        totalPenalty = Mth.clamp(totalPenalty, 0.0, ServerConfig.INSTANCE.maxMovementPenalty);
        totalPenalty = Math.min(totalPenalty, 1.0 - ServerConfig.INSTANCE.minSpeedWhileGrabbing);

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(ServerGrabManager.getMovementPenaltyId());

            if (totalPenalty > 0.01) {
                AttributeModifier penaltyModifier = new AttributeModifier(
                        ServerGrabManager.getMovementPenaltyId(),
                        -totalPenalty,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                moveSpeed.addTransientModifier(penaltyModifier);
            }
        }
    }
}