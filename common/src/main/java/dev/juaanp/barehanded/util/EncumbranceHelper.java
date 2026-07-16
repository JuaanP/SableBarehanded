package dev.juaanp.barehanded.util;

import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class EncumbranceHelper {

    public static double getStrengthMultiplier(Player player) {
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            return 1.0 + ((amplifier + 1) * ServerConfig.INSTANCE.strengthLevelMultiplier);
        }
        return 1.0;
    }

    public static double getMaxCapacity(Player player) {
        return ServerConfig.INSTANCE.maxForce * getStrengthMultiplier(player);
    }

    public static double getActualMaxForce(Player player) {
        return ServerConfig.INSTANCE.maxForce * getStrengthMultiplier(player);
    }

    public static double getEncumbranceRatio(double mass, Player player) {
        if (mass <= 0.0) return 0.0;

        double maxCapacity = getMaxCapacity(player);
        if (maxCapacity <= 0.0) return 1.0;

        double objectWeight = mass * ServerConfig.INSTANCE.physicsGravity;
        double rawRatio = objectWeight / maxCapacity;

        return Math.min(Math.pow(rawRatio, 2.0), 1.0);
    }

    public static double getEffectiveEncumbranceRatio(double mass, Player player, boolean enableEncumbrance) {
        if (!enableEncumbrance) return 0.0;
        if (player.isCreative() || player.isSpectator()) return 0.0;

        return getEncumbranceRatio(mass, player);
    }
}