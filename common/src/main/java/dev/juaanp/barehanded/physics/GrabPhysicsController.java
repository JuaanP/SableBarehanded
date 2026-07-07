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
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GrabPhysicsController {
    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();
    private static final Map<UUID, Integer> graceTicksMap = new HashMap<>();

    public static void setGraceTicks(Player player, int ticks) {
        graceTicksMap.put(player.getUUID(), ticks);
    }

    public static void cleanupPlayer(UUID playerId) {
        lastYawMap.remove(playerId);
        lastPitchMap.remove(playerId);
        graceTicksMap.remove(playerId);
    }

    public static void rebuildConstraint(GrabSession grab) {
        if (grab.constraintHandle != null) {
            grab.constraintHandle.remove();
        }
        grab.constraintHandle = grab.pipeline.addConstraint(
                null, grab.subLevel,
                new FreeConstraintConfiguration(grab.anchorGlobalOrigin, grab.localPivot, grab.baseOrientation)
        );
    }

    public static Quaterniond getPlayerYawQuaternion(Player player) {
        return new Quaterniond().rotateY(Math.toRadians(-player.getYRot()));
    }

    private static void applyPlayerRelativeRotation(Player player, GrabSession grab) {
        Quaterniond playerYaw = getPlayerYawQuaternion(player);
        grab.targetGlobalOrientation.set(playerYaw.mul(grab.relativeOrientation, new Quaterniond()));
    }

    private static void applyCameraLockedRotation(Player player, GrabSession grab) {
        UUID playerId = player.getUUID();
        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();

        if (!ServerConfig.INSTANCE.cameraLockedRotationX) {
            lastYawMap.put(playerId, currentYaw);
            lastPitchMap.put(playerId, currentPitch);
            return;
        }

        if (!lastYawMap.containsKey(playerId) || !lastPitchMap.containsKey(playerId)) {
            lastYawMap.put(playerId, currentYaw);
            lastPitchMap.put(playerId, currentPitch);
            return;
        }

        float lastPitch = lastPitchMap.get(playerId);
        float lastYaw = lastYawMap.get(playerId);

        float deltaPitch = currentPitch - lastPitch;

        if (deltaPitch != 0.0f) {
            double pitchDelta = Math.toRadians(deltaPitch);

            Vec3 right = player.calculateViewVector(0.0f, player.getYRot() - 90.0f);
            Vector3d rightAxis = new Vector3d(right.x, right.y, right.z).normalize();

            grab.targetGlobalOrientation.premul(new Quaterniond().rotateAxis(pitchDelta, rightAxis));
            grab.targetGlobalOrientation.normalize();
        }

        lastYawMap.put(playerId, currentYaw);
        lastPitchMap.put(playerId, currentPitch);
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
        if (grab == null) {
            cleanupPlayer(playerId);
            return;
        }

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

        int grace = graceTicksMap.getOrDefault(playerId, 0);
        if (grace > 0) {
            graceTicksMap.put(playerId, grace - 1);
        }

        player.yBodyRot = player.yHeadRot;
        player.yBodyRotO = player.yHeadRotO;

        boolean hasSuperStrength = GrabSession.hasSuperStrength(player);

        double strengthMultiplier = 1.0;
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            if (amplifier == 0) strengthMultiplier = ServerConfig.INSTANCE.strength1Multiplier;
            else if (amplifier >= 1) strengthMultiplier = ServerConfig.INSTANCE.strength2Multiplier;
        }
        double actualMaxForce = ServerConfig.INSTANCE.maxForce * strengthMultiplier;

        double maxScroll = getMaxScrollDistance(player);
        if (grab.targetDistance > maxScroll) {
            grab.targetDistance = (float) maxScroll;
        }
        if (grab.targetDistance < ServerConfig.INSTANCE.scrollMinDistance) {
            grab.targetDistance = (float) ServerConfig.INSTANCE.scrollMinDistance;
        }

        if (grab.distance != grab.targetDistance) {
            double baseLerpSpeed = 0.15F;
            double lerpSpeed = baseLerpSpeed;

            if (!hasSuperStrength && ServerConfig.INSTANCE.enableEncumbrance && ServerConfig.INSTANCE.scrollSpeedReduction > 0.0) {
                double mass = grab.subLevel.getMassTracker().getMass();
                double maxCapacity = ServerConfig.INSTANCE.maxForce * strengthMultiplier;
                double objectWeight = mass * ServerConfig.INSTANCE.physicsGravity;
                double rawRatio = maxCapacity > 0 ? objectWeight / maxCapacity : 0.0;
                double encumbrance = Math.min(Math.pow(rawRatio, 2.0), 1.0);

                double speedReduction = 1.0 - (encumbrance * ServerConfig.INSTANCE.scrollSpeedReduction);
                lerpSpeed = baseLerpSpeed * speedReduction;

                lerpSpeed = Math.max(0.02, lerpSpeed);
            }

            grab.distance = net.minecraft.util.Mth.lerp((float) lerpSpeed, grab.distance, grab.targetDistance);
            if (Math.abs(grab.distance - grab.targetDistance) < 0.01F) {
                grab.distance = grab.targetDistance;
            }
        }

        Vector3d currentCameraTarget = JOMLConversion.toJOML(player.getEyePosition().add(player.getLookAngle().scale(Math.max(ServerConfig.INSTANCE.minDistance, grab.distance))));

        boolean isGhostEverything = grab.isRotating ? ServerConfig.INSTANCE.ignoreCollisionsRotationEverything : ServerConfig.INSTANCE.ignoreCollisionsGrabEverything;
        boolean isPlayerRelativeRotation = !grab.isRotating;
        boolean isCameraLocked = isPlayerRelativeRotation || ServerConfig.INSTANCE.cameraLockedRotationX;

        boolean wasRotating = grab.isRotating;
        grab.isRotating = grab.rotationTicksLeft > 0;
        grab.rotationTicksLeft = Math.max(0, grab.rotationTicksLeft - 1);

        if (isPlayerRelativeRotation) {
            applyPlayerRelativeRotation(player, grab);
            applyCameraLockedRotation(player, grab);
        }

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

            grab.rotateAroundCenter = false;

            Vector3d currentActualPivotPos = grab.subLevel.logicalPose().transformPosition(new Vector3d(grab.localPivot));
            grab.anchorGlobalOrigin.set(currentActualPivotPos);
            grab.baseOrientation.set(grab.subLevel.logicalPose().orientation());
            grab.targetGlobalOrientation.set(grab.baseOrientation);
            grab.syncRelativeOrientationFromTarget(player);
            if (grab.constraintHandle != null) rebuildConstraint(grab);
        }

        Quaterniond relativeRot = new Quaterniond(grab.baseOrientation).invert().mul(grab.targetGlobalOrientation);

        if (grab.isRotating || isCameraLocked) {
            if (grab.isRotating && grab.rotateAroundCenter) {
                Vector3d vectorToCOM = new Vector3d(grab.localCenterOfMass).sub(grab.localPivot);
                Vector3d originalCOM = new Vector3d(vectorToCOM).rotate(grab.baseOrientation);
                Vector3d targetCOM = new Vector3d(vectorToCOM).rotate(grab.targetGlobalOrientation);
                grab.accumulatedPivotOffset.add(new Vector3d(originalCOM).sub(targetCOM));
            }

            grab.baseOrientation.set(grab.targetGlobalOrientation);
            rebuildConstraint(grab);
            relativeRot.identity();

            if (isPlayerRelativeRotation && !isGhostEverything) {
                Vector3d currentPos = new Vector3d(grab.subLevel.logicalPose().position());
                grab.pipeline.teleport(grab.subLevel, currentPos, new Quaterniond(grab.targetGlobalOrientation));
                grab.subLevel.latestAngularVelocity.set(0, 0, 0);
            }
        } else {
            if (grab.constraintHandle != null && relativeRot.angle() > ServerConfig.INSTANCE.rotationRebuildThreshold) {
                if (grab.rotateAroundCenter) {
                    Vector3d vectorToCOM = new Vector3d(grab.localCenterOfMass).sub(grab.localPivot);
                    Vector3d originalCOM = new Vector3d(vectorToCOM).rotate(grab.baseOrientation);
                    Vector3d targetCOM = new Vector3d(vectorToCOM).rotate(grab.targetGlobalOrientation);
                    grab.accumulatedPivotOffset.add(new Vector3d(originalCOM).sub(targetCOM));
                }
                grab.baseOrientation.set(grab.subLevel.logicalPose().orientation());
                rebuildConstraint(grab);
                relativeRot = new Quaterniond(grab.baseOrientation).invert().mul(grab.targetGlobalOrientation);
            }
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

        if (ServerConfig.INSTANCE.preventPropSurfing) {
            Vector3dc com = grab.subLevel.getMassTracker().getCenterOfMass();
            if (com != null) {
                Vector3d globalCom = grab.subLevel.logicalPose().transformPosition(new Vector3d(com));
                double dx = globalCom.x - player.getX();
                double dz = globalCom.z - player.getZ();
                double horizontalDistSqr = dx * dx + dz * dz;

                if (horizontalDistSqr > 0.04) {
                    Vec3 look = player.getLookAngle();
                    double lookLen = Math.sqrt(look.x * look.x + look.z * look.z);
                    double grabLen = Math.sqrt(horizontalDistSqr);

                    if (lookLen > 0.01 && grabLen > 0.01) {
                        double dotXZ = ((dx / grabLen) * (look.x / lookLen)) + ((dz / grabLen) * (look.z / lookLen));

                        if (dotXZ < 0.0) {
                            ServerGrabManager.stopGrabbing(playerId);
                            return;
                        }
                    }
                }
            }
        }

        boolean suspendPhysics = false;

        if (ServerConfig.INSTANCE.preventPropSurfing) {
            ServerSubLevel standingSubLevel = (ServerSubLevel) Sable.HELPER.getTrackingSubLevel(player);

            if (standingSubLevel != null) {
                if (standingSubLevel.equals(grab.subLevel)) {
                    grab.suspendTicksLeft = ServerConfig.INSTANCE.standingOnGrabSuspendTicks;
                    suspendPhysics = true;
                } else if (grab.containsSurfMechanicalBlocks) {
                    grab.suspendTicksLeft = ServerConfig.INSTANCE.standingOnGrabSuspendTicks;
                    suspendPhysics = true;
                }
            } else if (grab.suspendTicksLeft > 0) {
                grab.suspendTicksLeft--;
                suspendPhysics = true;
            }
        }

        double eyeDistSq = player.getEyePosition().distanceToSqr(new Vec3(currentActualGrabBlockPos.x, currentActualGrabBlockPos.y, currentActualGrabBlockPos.z));
        double bodyDistSq = player.position().distanceToSqr(currentActualGrabBlockPos.x, currentActualGrabBlockPos.y, currentActualGrabBlockPos.z);

        double eyeSusDistSq = ServerConfig.INSTANCE.grabProximityEyeSuspendDistance * ServerConfig.INSTANCE.grabProximityEyeSuspendDistance;
        double bodySusDistSq = ServerConfig.INSTANCE.grabProximityBodySuspendDistance * ServerConfig.INSTANCE.grabProximityBodySuspendDistance;

        if (eyeDistSq < eyeSusDistSq || bodyDistSq < bodySusDistSq) {
            suspendPhysics = true;
        }

        double tension = currentActualGrabBlockPos.distance(currentCameraTarget);
        double suspendThresh = hasSuperStrength ? ServerConfig.INSTANCE.creativeTensionSuspendThreshold : ServerConfig.INSTANCE.tensionSuspendThreshold;
        double breakThresh = hasSuperStrength ? ServerConfig.INSTANCE.creativeTensionBreakThreshold : ServerConfig.INSTANCE.tensionBreakThreshold;

        GrabTetherSystem.applyPhysicalTether(player, grab, tension, actualMaxForce);

        if (grab.suspendTicksLeft > 0) {
            suspendPhysics = true;
        } else if (tension > suspendThresh) {
            if (grace == 0) {
                if (tension > breakThresh) {
                    ServerGrabManager.stopGrabbing(playerId);
                    return;
                }
                suspendPhysics = true;
            }
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
            double grabStable = hasSuperStrength ? 1.0 : Math.pow(ServerConfig.INSTANCE.grabStabilization, exponent);
            double rotStable = hasSuperStrength ? 1.0 : Math.pow(ServerConfig.INSTANCE.rotationStabilization, exponent);
            double mass = grab.subLevel.getMassTracker().getMass();

            double horizontalSpeed = Math.sqrt(pVel.x * pVel.x + pVel.z * pVel.z);
            double effectiveSpeed = horizontalSpeed + (pVel.y > 0 ? pVel.y : 0.0);

            double speedMultiplier = 1.0 + (effectiveSpeed * ServerConfig.INSTANCE.speedStiffnessMultiplierFactor);
            speedMultiplier = Math.min(speedMultiplier, ServerConfig.INSTANCE.maxSpeedStiffnessMultiplier);

            double baseStiffness = hasSuperStrength ? ServerConfig.INSTANCE.stiffness * ServerConfig.INSTANCE.creativeStrengthMultiplier : ServerConfig.INSTANCE.stiffness;
            double linearDamping = hasSuperStrength ? ServerConfig.INSTANCE.damping * ServerConfig.INSTANCE.creativeStrengthMultiplier : ServerConfig.INSTANCE.damping;
            double angularDamping = hasSuperStrength ? ServerConfig.INSTANCE.angularDamping * ServerConfig.INSTANCE.creativeStrengthMultiplier : ServerConfig.INSTANCE.angularDamping;

            double massCurve = Math.log1p(mass) * ServerConfig.INSTANCE.heavyObjectMassCurveMultiplier;
            double baseAngularForce = actualMaxForce * ServerConfig.INSTANCE.baseAngularForceFactor;
            double stableAngularForce = actualMaxForce * (ServerConfig.INSTANCE.stableAngularForceMassBase + massCurve * ServerConfig.INSTANCE.stableAngularForceMassFactor);

            boolean disableMotors = suspendPhysics;

            double linearMaxForce = disableMotors ? 0.0 : (hasSuperStrength ? ServerConfig.INSTANCE.creativeMaxMotorForce : actualMaxForce * (1.0 + massCurve * ServerConfig.INSTANCE.heavyObjectMaxForceFactor));

            Vector3d globalOffset = new Vector3d(targetAnchor).sub(grab.anchorGlobalOrigin);
            Vector3d localOffset = new Vector3d(globalOffset).rotate(new Quaterniond(grab.baseOrientation).invert());

            double currentLinearStiffness = disableMotors ? 0.0 : (baseStiffness * speedMultiplier) * (hasSuperStrength ? 1.0 : ServerConfig.INSTANCE.grabElasticityStiffnessFactor);
            double currentLinearDamping = disableMotors ? 0.0 : (linearDamping * speedMultiplier) * (hasSuperStrength ? 1.0 : ServerConfig.INSTANCE.grabElasticityDampingFactor);

            grab.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_X, localOffset.x, currentLinearStiffness, currentLinearDamping, true, linearMaxForce);
            grab.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Y, localOffset.y, currentLinearStiffness, currentLinearDamping, true, linearMaxForce);
            grab.constraintHandle.setMotor(ConstraintJointAxis.LINEAR_Z, localOffset.z, currentLinearStiffness, currentLinearDamping, true, linearMaxForce);

            if (grab.isRotating || isCameraLocked) {
                double angularStiffness = disableMotors ? 0.0 : baseStiffness * (ServerConfig.INSTANCE.rotatingAngularStiffnessBase + (ServerConfig.INSTANCE.rotatingAngularStiffnessRange * rotStable));
                double angularMaxForce = disableMotors ? 0.0 : (hasSuperStrength ? ServerConfig.INSTANCE.creativeMaxMotorForce : (baseAngularForce + ((stableAngularForce - baseAngularForce) * rotStable)));
                double currentAngularDamping = disableMotors ? 0.0 : angularDamping;

                if (isCameraLocked && !disableMotors) {
                    angularStiffness *= 3.0;
                    angularMaxForce *= 3.0;
                }

                for (ConstraintJointAxis angularAxis : ConstraintJointAxis.ANGULAR) {
                    grab.constraintHandle.setMotor(angularAxis, 0.0, angularStiffness, currentAngularDamping, false, angularMaxForce);
                }

            } else {
                eulers = new Vector3d();
                relativeRot.getEulerAnglesXYZ(eulers);

                double freeDamping = disableMotors ? 0.0 : angularDamping * ServerConfig.INSTANCE.freePivotDampingMultiplier;
                double swayStiffness = disableMotors ? 0.0 : baseStiffness * (ServerConfig.INSTANCE.swayAngularStiffnessBase * ServerConfig.INSTANCE.swayStiffnessEdgeFactor + (ServerConfig.INSTANCE.swayAngularStiffnessRange * grabStable * ServerConfig.INSTANCE.swayStiffnessEdgeRangeFactor));
                double angularMaxForce = disableMotors ? 0.0 : (hasSuperStrength ? ServerConfig.INSTANCE.creativeMaxMotorForce : (baseAngularForce + ((stableAngularForce - baseAngularForce) * grabStable)));

                double errorMagnitude = Math.sqrt(eulers.x * eulers.x + eulers.y * eulers.y + eulers.z * eulers.z);
                if (errorMagnitude < ServerConfig.INSTANCE.angularBrakeThreshold) {
                    freeDamping *= ServerConfig.INSTANCE.angularBrakeMultiplier;
                }

                grab.constraintHandle.setMotor(ConstraintJointAxis.ANGULAR_X, eulers.x, swayStiffness, freeDamping, true, angularMaxForce);
                grab.constraintHandle.setMotor(ConstraintJointAxis.ANGULAR_Y, eulers.y, swayStiffness, freeDamping, true, angularMaxForce);
                grab.constraintHandle.setMotor(ConstraintJointAxis.ANGULAR_Z, eulers.z, swayStiffness, freeDamping, true, angularMaxForce);
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
        if (GrabSession.hasSuperStrength(player)) {
            return Math.max(CREATIVE_REACH, normalReach);
        }
        return normalReach;
    }

    public static double getMaxScrollDistance(Player player) {
        if (ServerConfig.INSTANCE.dynamicScrollMaxDistance) {
            return getGrabReach(player);
        }
        return ServerConfig.INSTANCE.scrollMaxDistance;
    }
}