package dev.juaanp.barehanded.physics;

import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.platform.Services;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.UUID;

public class GrabPhysicsController {

    public static void rebuildConstraint(GrabSession grab) {
        if (grab.constraintHandle != null) {
            grab.constraintHandle.remove();
        }
        grab.constraintHandle = grab.pipeline.addConstraint(
                null, grab.subLevel,
                new FreeConstraintConfiguration(grab.anchorGlobalOrigin, grab.localPivot, grab.baseOrientation)
        );
    }

    public static void tickPlayer(Player player) {
        UUID playerId = player.getUUID();
        if (player.level().isClientSide()) return;

        Vec3 vel = player.getDeltaMovement();
        if (Math.abs(vel.x) > ServerConfig.INSTANCE.maxPlayerVelocityXZ || vel.y > ServerConfig.INSTANCE.maxPlayerVelocityYUp || vel.y < ServerConfig.INSTANCE.maxPlayerVelocityYDown || Math.abs(vel.z) > ServerConfig.INSTANCE.maxPlayerVelocityXZ) {
            player.setDeltaMovement(
                    Mth.clamp(vel.x, -ServerConfig.INSTANCE.maxPlayerVelocityXZ, ServerConfig.INSTANCE.maxPlayerVelocityXZ),
                    Mth.clamp(vel.y, ServerConfig.INSTANCE.maxPlayerVelocityYDown, ServerConfig.INSTANCE.maxPlayerVelocityYUp),
                    Mth.clamp(vel.z, -ServerConfig.INSTANCE.maxPlayerVelocityXZ, ServerConfig.INSTANCE.maxPlayerVelocityXZ)
            );
            player.hurtMarked = true;
        }

        GrabSession grab = ServerGrabManager.getGrabSession(player);
        if (grab == null) return;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(player.level());

        if (container == null ||
                grab.subLevel.isRemoved() ||
                container.getSubLevel(grab.subLevel.getUniqueId()) == null ||
                grab.subLevel.getMassTracker().getMass() <= ServerConfig.INSTANCE.minPhysicsMass) {
            ServerGrabManager.stopGrabbing(playerId);
            return;
        }

        if (!player.isAlive()) {
            ServerGrabManager.stopGrabbing(playerId);
            return;
        }

        grab.pipeline.wakeUp(grab.subLevel);

        if (player instanceof ServerPlayer serverPlayer) {
            ImpactDisassembleHandler.checkImpact(serverPlayer, grab.subLevel, grab);
        }

        player.yBodyRot = player.yHeadRot;
        player.yBodyRotO = player.yHeadRotO;

        boolean isCreativeSuper = player.isCreative() && ServerConfig.INSTANCE.creativeSuperStrength;

        double strengthMultiplier = 1.0;
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            if (amplifier == 0) strengthMultiplier = ServerConfig.INSTANCE.strength1Multiplier;
            else if (amplifier >= 1) strengthMultiplier = ServerConfig.INSTANCE.strength2Multiplier;
        }
        double actualMaxForce = ServerConfig.INSTANCE.maxForce * strengthMultiplier;

        if (grab.distance != grab.targetDistance) {
            grab.distance = net.minecraft.util.Mth.lerp(0.15F, grab.distance, grab.targetDistance);

            if (Math.abs(grab.distance - grab.targetDistance) < 0.01F) {
                grab.distance = grab.targetDistance;
            }
        }

        Vector3d currentCameraTarget = JOMLConversion.toJOML(player.getEyePosition().add(player.getLookAngle().scale(Math.max(ServerConfig.INSTANCE.minDistance, grab.distance))));

        boolean isGhostEverything = grab.isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEverything : ServerConfig.INSTANCE.ignoreCollisionsGrabEverything;

        boolean wasRotating = grab.isRotating;
        grab.isRotating = grab.rotationTicksLeft > 0;
        grab.rotationTicksLeft = Math.max(0, grab.rotationTicksLeft - 1);

        if (grab.isRotating && !isGhostEverything) {
            grab.subLevel.latestLinearVelocity.set(0, 0, 0);
        }

        if (!grab.isRotating && wasRotating) {
            if (grab.rotateAroundCenter) {
                Vector3d vectorToCOM = new Vector3d(grab.localCenterOfMass).sub(grab.localPivot);
                Vector3d originalCOM = new Vector3d(vectorToCOM).rotate(grab.baseOrientation);
                Vector3d targetCOM = new Vector3d(vectorToCOM).rotate(grab.targetGlobalOrientation);
                grab.accumulatedPivotOffset.add(new Vector3d(originalCOM).sub(targetCOM));
            }

            Vector3d currentActualPivotPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(grab.localPivot));
            grab.anchorGlobalOrigin.set(currentActualPivotPos);
            grab.baseOrientation.set(grab.subLevel.logicalPose().orientation());
            grab.targetGlobalOrientation.set(grab.baseOrientation);
            if (grab.constraintHandle != null) rebuildConstraint(grab);
        }

        Quaterniond relativeRot = new Quaterniond(grab.baseOrientation).invert().mul(grab.targetGlobalOrientation);

        if (grab.constraintHandle != null && relativeRot.angle() > ServerConfig.INSTANCE.rotationRebuildThreshold) {
            if (grab.rotateAroundCenter) {
                Vector3d vectorToCOM = new Vector3d(grab.localCenterOfMass).sub(grab.localPivot);
                Vector3d originalCOM = new Vector3d(vectorToCOM).rotate(grab.baseOrientation);
                Vector3d targetCOM = new Vector3d(vectorToCOM).rotate(grab.targetGlobalOrientation);
                grab.accumulatedPivotOffset.add(new Vector3d(originalCOM).sub(targetCOM));
            }

            grab.baseOrientation.set(grab.targetGlobalOrientation);
            rebuildConstraint(grab);
            relativeRot.identity();
        }

        if (isGhostEverything) {
            Vector3d pivotReference = grab.rotateAroundCenter ? grab.localCenterOfMass : grab.subLevel.logicalPose().rotationPoint();
            Vector3d localOffsetToGrab = new Vector3d(grab.localPivot).sub(pivotReference);
            Vector3d rotatedOffset = new Vector3d(localOffsetToGrab).rotate(grab.targetGlobalOrientation);

            Vector3d targetPos = new Vector3d(currentCameraTarget).add(grab.accumulatedPivotOffset).sub(rotatedOffset);

            grab.pipeline.teleport(grab.subLevel, targetPos, grab.targetGlobalOrientation);
            grab.subLevel.latestLinearVelocity.set(0, 0, 0);
            grab.subLevel.latestAngularVelocity.set(0, 0, 0);
        }

        if (grab.constraintHandle == null) {
            rebuildConstraint(grab);
        }

        Vector3d targetAnchor = new Vector3d(currentCameraTarget).add(grab.accumulatedPivotOffset);

        if (grab.rotateAroundCenter) {
            Vector3d vectorToCOM = new Vector3d(grab.localCenterOfMass).sub(grab.localPivot);
            Vector3d originalCOM = new Vector3d(vectorToCOM).rotate(grab.baseOrientation);
            Vector3d targetCOM = new Vector3d(vectorToCOM).rotate(grab.targetGlobalOrientation);
            targetAnchor.add(new Vector3d(originalCOM).sub(targetCOM));
        }

        Vector3d currentActualGrabBlockPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(grab.localPivot));
        boolean suspendPhysics = false;
        ServerSubLevel standingSubLevel = (ServerSubLevel) Sable.HELPER.getTrackingSubLevel(player);

        if (standingSubLevel != null && standingSubLevel.equals(grab.subLevel)) {
            grab.suspendTicksLeft = ServerConfig.INSTANCE.standingOnGrabSuspendTicks;
            suspendPhysics = true;
        } else if (grab.suspendTicksLeft > 0) {
            grab.suspendTicksLeft--;
            suspendPhysics = true;
        }

        double eyeDistSq = player.getEyePosition().distanceToSqr(new Vec3(currentActualGrabBlockPos.x, currentActualGrabBlockPos.y, currentActualGrabBlockPos.z));
        double bodyDistSq = player.position().distanceToSqr(currentActualGrabBlockPos.x, currentActualGrabBlockPos.y, currentActualGrabBlockPos.z);

        double eyeSusDistSq = ServerConfig.INSTANCE.grabProximityEyeSuspendDistance * ServerConfig.INSTANCE.grabProximityEyeSuspendDistance;
        double bodySusDistSq = ServerConfig.INSTANCE.grabProximityBodySuspendDistance * ServerConfig.INSTANCE.grabProximityBodySuspendDistance;

        if (eyeDistSq < eyeSusDistSq || bodyDistSq < bodySusDistSq) {
            suspendPhysics = true;
        }

        double tension = currentActualGrabBlockPos.distance(currentCameraTarget);
        double suspendThresh = isCreativeSuper ? ServerConfig.INSTANCE.creativeTensionSuspendThreshold : ServerConfig.INSTANCE.tensionSuspendThreshold;
        double breakThresh = isCreativeSuper ? ServerConfig.INSTANCE.creativeTensionBreakThreshold : ServerConfig.INSTANCE.tensionBreakThreshold;

        GrabTetherSystem.applyPhysicalTether(player, grab, tension, actualMaxForce);

        if (tension > suspendThresh) {
            if (tension > breakThresh) {
                ServerGrabManager.stopGrabbing(playerId);
                return;
            }
            suspendPhysics = true;
        }

        byte currentMask = 0;
        if (isGhostEverything) currentMask |= 1;
        if (grab.isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationSelf : ServerConfig.INSTANCE.ignoreCollisionsGrabSelf) currentMask |= 2;
        if (grab.isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationOtherPlayers : ServerConfig.INSTANCE.ignoreCollisionsGrabOtherPlayers) currentMask |= 4;
        if (grab.isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEntities : ServerConfig.INSTANCE.ignoreCollisionsGrabEntities) currentMask |= 8;

        if (!grab.hasSyncedGhostState || grab.lastCollisionMask != currentMask) {
            grab.lastCollisionMask = currentMask;
            grab.hasSyncedGhostState = true;
            Services.NETWORK.sendGhostStateSync(grab.subLevel, playerId, currentMask);
        }

        Vec3 pVel = player.getDeltaMovement();
        double playerSpeed = pVel.length();

        if (playerSpeed > ServerConfig.INSTANCE.leadVelocityThreshold) {
            double leadX = pVel.x * ServerConfig.INSTANCE.leadPredictionFactor;
            double leadY = pVel.y > 0 ? pVel.y * ServerConfig.INSTANCE.leadPredictionFactor : Math.max(pVel.y * ServerConfig.INSTANCE.leadPredictionFactor, ServerConfig.INSTANCE.leadDownwardClamp);
            double leadZ = pVel.z * ServerConfig.INSTANCE.leadPredictionFactor;

            Vector3d leadOffset = new Vector3d(leadX, leadY, leadZ);
            targetAnchor.add(leadOffset);
        }

        if (grab.constraintHandle != null && !isGhostEverything) {
            Vector3d eulers = new Vector3d();
            relativeRot.getEulerAnglesXYZ(eulers);

            double exponent = ServerConfig.INSTANCE.stabilizationExponent;
            double grabStable = isCreativeSuper ? 1.0 : Math.pow(ServerConfig.INSTANCE.grabStabilization, exponent);
            double rotStable = isCreativeSuper ? 1.0 : Math.pow(ServerConfig.INSTANCE.rotationStabilization, exponent);
            double mass = grab.subLevel.getMassTracker().getMass();

            double horizontalSpeed = Math.sqrt(pVel.x * pVel.x + pVel.z * pVel.z);
            double effectiveSpeed = horizontalSpeed + (pVel.y > 0 ? pVel.y : 0.0);

            double speedMultiplier = 1.0 + (effectiveSpeed * ServerConfig.INSTANCE.speedStiffnessMultiplierFactor);
            speedMultiplier = Math.min(speedMultiplier, ServerConfig.INSTANCE.maxSpeedStiffnessMultiplier);

            double baseStiffness = isCreativeSuper ? ServerConfig.INSTANCE.stiffness * ServerConfig.INSTANCE.creativeStrengthMultiplier : ServerConfig.INSTANCE.stiffness;
            double linearDamping = isCreativeSuper ? ServerConfig.INSTANCE.damping * ServerConfig.INSTANCE.creativeStrengthMultiplier : ServerConfig.INSTANCE.damping;
            double angularDamping = isCreativeSuper ? ServerConfig.INSTANCE.angularDamping * ServerConfig.INSTANCE.creativeStrengthMultiplier : ServerConfig.INSTANCE.angularDamping;

            double baseAngularForce = actualMaxForce * ServerConfig.INSTANCE.baseAngularForceFactor;
            double stableAngularForce = actualMaxForce * (ServerConfig.INSTANCE.stableAngularForceMassBase + mass * ServerConfig.INSTANCE.stableAngularForceMassFactor);

            boolean disableMotors = suspendPhysics;

            double linearMaxForce = disableMotors ? 0.0 : (isCreativeSuper ? ServerConfig.INSTANCE.creativeMaxMotorForce : actualMaxForce);

            Vector3d globalOffset = new Vector3d(targetAnchor).sub(grab.anchorGlobalOrigin);
            Vector3d localOffset = new Vector3d(globalOffset).rotate(new Quaterniond(grab.baseOrientation).invert());

            double currentLinearStiffness = baseStiffness * speedMultiplier;
            double currentLinearDamping = linearDamping * speedMultiplier;

            grab.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_X, localOffset.x, currentLinearStiffness, currentLinearDamping, true, linearMaxForce);
            grab.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, localOffset.y, currentLinearStiffness, currentLinearDamping, true, linearMaxForce);
            grab.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Z, localOffset.z, currentLinearStiffness, currentLinearDamping, true, linearMaxForce);

            if (grab.isRotating) {
                double angularStiffness = baseStiffness * (ServerConfig.INSTANCE.rotatingAngularStiffnessBase + (ServerConfig.INSTANCE.rotatingAngularStiffnessRange * rotStable));
                double angularMaxForce = disableMotors ? 0.0 : (isCreativeSuper ? ServerConfig.INSTANCE.creativeMaxMotorForce : (baseAngularForce + ((stableAngularForce - baseAngularForce) * rotStable)));

                grab.constraintHandle.setMotor(ConstraintJointAxis.ANGULAR_X, eulers.x, angularStiffness, angularDamping, true, angularMaxForce);
                grab.constraintHandle.setMotor(ConstraintJointAxis.ANGULAR_Y, eulers.y, angularStiffness, angularDamping, true, angularMaxForce);
                grab.constraintHandle.setMotor(ConstraintJointAxis.ANGULAR_Z, eulers.z, angularStiffness, angularDamping, true, angularMaxForce);

            } else {
                double swayStiffness = baseStiffness * (ServerConfig.INSTANCE.swayAngularStiffnessBase + (ServerConfig.INSTANCE.swayAngularStiffnessRange * grabStable));
                double angularMaxForce = disableMotors ? 0.0 : (isCreativeSuper ? ServerConfig.INSTANCE.creativeMaxMotorForce : (baseAngularForce + ((stableAngularForce - baseAngularForce) * grabStable)));

                for (ConstraintJointAxis axis : ConstraintJointAxis.ANGULAR) {
                    grab.constraintHandle.setMotor(axis, 0.0, swayStiffness, angularDamping, true, angularMaxForce);
                }
            }

            GrabEncumbranceSystem.applyMovementPenalty(player, grab, tension, actualMaxForce);
            GrabExhaustionSystem.applyExhaustion(player, grab, tension, actualMaxForce, suspendPhysics);
        } else {
            ServerGrabManager.clearPlayerMovementPenalty(player);
        }
    }

    public static final double CREATIVE_REACH = 128.0;

    public static double getGrabReach(Player player) {
        double normalReach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue() + ServerConfig.INSTANCE.grabReachBonus;
        if (player.isCreative() && ServerConfig.INSTANCE.creativeSuperStrength) {
            return Math.max(CREATIVE_REACH, normalReach);
        }
        return normalReach;
    }
}