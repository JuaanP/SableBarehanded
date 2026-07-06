package dev.juaanp.barehanded.physics;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class GrabSession {
    public final ServerSubLevel subLevel;
    public float distance;
    public float targetDistance;
    public final PhysicsPipeline pipeline;
    public PhysicsConstraintHandle constraintHandle;

    public final Vector3d localPivot;
    public final Vector3d localCenterOfMass;

    public boolean isRotating = false;
    public int rotationTicksLeft = 0;
    public boolean rotateAroundCenter = false;

    public int suspendTicksLeft = 0;
    public boolean isAltDown = false;

    public byte lastCollisionMask = -1;
    public boolean hasSyncedGhostState = false;

    public final Vector3d anchorGlobalOrigin = new Vector3d();
    public final Quaterniond baseOrientation = new Quaterniond();
    public final Quaterniond targetGlobalOrientation = new Quaterniond();

    public final Vector3d accumulatedPivotOffset = new Vector3d();

    public int impactTicks = 0;

    public final boolean containsSurfMechanicalBlocks;

    public GrabSession(ServerSubLevel subLevel, float distance, Vector3d localPivot, Vector3d localCenterOfMass,
                       Vector3d initialTarget, Quaterniond initialOrient, PhysicsPipeline pipeline,
                       boolean containsSurfMechanicalBlocks) {
        this.subLevel = subLevel;
        this.distance = distance;
        this.targetDistance = distance;
        this.localPivot = localPivot;
        this.localCenterOfMass = localCenterOfMass;
        this.pipeline = pipeline;
        this.containsSurfMechanicalBlocks = containsSurfMechanicalBlocks;

        this.anchorGlobalOrigin.set(initialTarget);
        this.baseOrientation.set(initialOrient);
        this.targetGlobalOrientation.set(initialOrient);
    }

    public static boolean hasSuperStrength(net.minecraft.world.entity.player.Player player) {
        return (player.isCreative() && dev.juaanp.barehanded.config.ServerConfig.INSTANCE.creativeSuperStrength) ||
                (player.isSpectator() && dev.juaanp.barehanded.config.ServerConfig.INSTANCE.spectatorSuperStrength);
    }
}