package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.handler.MovementInputHandler;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void barehanded$onTravel(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player) {
            if (MovementInputHandler.shouldPreventMovement()) {
                double currentY = player.getDeltaMovement().y;
                player.setDeltaMovement(0.0, currentY, 0.0);
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 barehanded$modifyTravelInput(Vec3 movementInput) {
        if ((Object) this instanceof LocalPlayer player) {
            double encumbrance = ClientGrabSession.getEffectiveEncumbranceRatio(player);
            if (encumbrance > 0.0 && !player.isCreative() && !player.isSpectator()) {
                double movementScale = 1.0 - (encumbrance * ServerConfig.INSTANCE.maxMovementPenalty);
                movementScale = Math.max(movementScale, ServerConfig.INSTANCE.minSpeedWhileGrabbing);

                // Escala los impulsos X y Z (izquierda/derecha y adelante/atrás)
                return new Vec3(
                        movementInput.x * movementScale,
                        movementInput.y,
                        movementInput.z * movementScale
                );
            }
        }
        return movementInput;
    }
}