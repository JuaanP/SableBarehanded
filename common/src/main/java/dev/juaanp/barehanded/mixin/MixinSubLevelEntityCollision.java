package dev.juaanp.barehanded.mixin;

import dev.juaanp.barehanded.handler.CollisionFilterHandler;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SubLevelEntityCollision.class, remap = false)
public class MixinSubLevelEntityCollision {

    @Inject(method = "collide", at = @At("HEAD"))
    private static void onCollide(Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj, LevelReusedVectors sink, CallbackInfoReturnable<SubLevelEntityCollision.CollisionInfo> cir) {
        CollisionFilterHandler.setProcessingEntity(entity);
    }

    @Redirect(method = "collide", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;add(Ljava/lang/Object;)Z"))
    private static boolean onAddSubLevel(ObjectSet<SubLevel> instance, Object e) {
        SubLevel subLevel = (SubLevel) e;

        if (CollisionFilterHandler.shouldFilterSubLevel(subLevel)) {
            return false;
        }

        return instance.add(subLevel);
    }

    @Inject(method = "collide", at = @At("RETURN"))
    private static void onCollideReturn(Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj, LevelReusedVectors sink, CallbackInfoReturnable<SubLevelEntityCollision.CollisionInfo> cir) {
        CollisionFilterHandler.clearProcessingEntity();
    }
}