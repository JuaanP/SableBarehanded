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

/**
 * Public API for interacting with the Barehanded mod.
 *
 * <p>This API allows other mods to query grab state, force interactions,
 * and integrate with Barehanded's physics system.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Check if a player is currently grabbing something
 * if (BarehandedAPI.isPlayerGrabbing(player)) {
 *     ServerSubLevel grabbed = BarehandedAPI.getGrabbedSubLevel(player);
 *     System.out.println("Player is holding: " + grabbed);
 * }
 *
 * // Force a player to drop their grabbed object
 * BarehandedAPI.forceDrop(player);
 *
 * // Force a player to grab a specific block
 * BarehandedAPI.forceGrab(serverPlayer, new BlockPos(0, 64, 0));
 * }</pre>
 *
 * @author JuaanP
 * @since 1.6.0
 */
public class BarehandedAPI {

    /**
     * Checks if a player is currently grabbing a sub-level.
     *
     * @param player The player to check
     * @return true if the player is currently holding a sub-level, false otherwise
     * @throws NullPointerException if player is null
     */
    public static boolean isPlayerGrabbing(@NotNull Player player) {
        return ServerGrabManager.isPlayerGrabbing(player);
    }

    /**
     * Checks if a player is holding a specific sub-level.
     *
     * @param player The player to check
     * @param subLevel The sub-level to check against
     * @return true if the player is holding the specified sub-level, false otherwise
     * @throws NullPointerException if player or subLevel is null
     */
    public static boolean isHoldingSubLevel(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        return ServerGrabManager.isHoldingSubLevel(player, subLevel);
    }

    /**
     * Gets the sub-level that a player is currently grabbing.
     *
     * @param player The player to query
     * @return The grabbed sub-level, or null if the player is not grabbing anything
     * @throws NullPointerException if player is null
     */
    @Nullable
    public static ServerSubLevel getGrabbedSubLevel(@NotNull Player player) {
        return ServerGrabManager.getGrabbedSubLevel(player);
    }

    /**
     * Gets the current grab reach distance for a player.
     * This takes into account attributes, config bonuses, and super strength.
     *
     * @param player The player to query
     * @return The grab reach distance in blocks
     * @throws NullPointerException if player is null
     */
    public static double getGrabReach(@NotNull Player player) {
        return GrabPhysicsController.getGrabReach(player);
    }

    /**
     * Forces a player to immediately drop their grabbed sub-level.
     * This will trigger the release event and clean up physics constraints.
     *
     * <p>If the player is not currently grabbing anything, this method does nothing.</p>
     *
     * @param player The player to force drop
     * @throws NullPointerException if player is null
     */
    public static void forceDrop(@NotNull Player player) {
        ServerGrabManager.stopGrabbing(player.getUUID());
    }

    /**
     * Forces a player to grab a sub-level at the specified position.
     * This bypasses normal grab checks and distance limitations.
     *
     * <p>The player must be a ServerPlayer for this to work correctly.</p>
     *
     * @param serverPlayer The server player to force grab
     * @param targetPos The block position to grab
     * @throws NullPointerException if serverPlayer or targetPos is null
     * @throws IllegalArgumentException if the position does not contain a valid sub-level
     */
    public static void forceGrab(@NotNull ServerPlayer serverPlayer, @NotNull BlockPos targetPos) {
        GrabActionHandler.startGrabbing(serverPlayer, targetPos);
    }

    /**
     * Forces a player to assemble blocks from the world and grab them.
     * This is equivalent to the player performing a sneak+grab action programmatically.
     *
     * @param player The player to force assemble and grab
     * @param targetPos The block position to start assembly from
     * @throws NullPointerException if player or targetPos is null
     */
    public static void forceAssembleAndGrab(@NotNull Player player, @NotNull BlockPos targetPos) {
        GrabActionHandler.assembleAndGrab(player, targetPos);
    }

    /**
     * Applies rotation input to a player's grabbed sub-level.
     * This is equivalent to the player moving their mouse while holding the rotate key.
     *
     * @param player The player whose grabbed object should rotate
     * @param yaw The yaw rotation delta (horizontal)
     * @param pitch The pitch rotation delta (vertical)
     * @param rotateAroundCenter If true, rotate around center of mass; if false, rotate around grab point
     * @throws NullPointerException if player is null
     */
    public static void applyRotation(@NotNull Player player, double yaw, double pitch, boolean rotateAroundCenter) {
        GrabRotationController.applyRotation(player, yaw, pitch, rotateAroundCenter);
    }

    /**
     * Gets the mass of the sub-level a player is currently grabbing.
     *
     * @param player The player to query
     * @return The mass in kilograms, or 0.0 if not grabbing anything
     * @throws NullPointerException if player is null
     */
    public static double getGrabbedMass(@NotNull Player player) {
        ServerSubLevel subLevel = getGrabbedSubLevel(player);
        if (subLevel == null) return 0.0;
        return subLevel.getMassTracker().getMass();
    }

    /**
     * Checks if a player has super strength (Creative mode or Spectator mode with config enabled).
     *
     * @param player The player to check
     * @return true if the player has super strength, false otherwise
     * @throws NullPointerException if player is null
     */
    public static boolean hasSuperStrength(@NotNull Player player) {
        return dev.juaanp.barehanded.physics.GrabSession.hasSuperStrength(player);
    }
}