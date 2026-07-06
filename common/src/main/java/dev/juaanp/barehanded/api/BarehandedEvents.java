package dev.juaanp.barehanded.api;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Event system for Barehanded mod interactions.
 *
 * <p>This class provides hooks for other mods to intercept, monitor, and react to
 * player grab/assemble actions.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Cancel grab if object is too heavy
 * BarehandedEvents.onBeforeGrab((player, subLevel) -> {
 *     if (subLevel.getMassTracker().getMass() > 50000) {
 *         player.sendSystemMessage(Component.literal("Too heavy!"));
 *         return false; // Cancel the grab
 *     }
 *     return true; // Allow the grab
 * });
 *
 * // Log when players grab objects
 * BarehandedEvents.onGrab((player, subLevel) -> {
 *     System.out.println(player.getName().getString() + " grabbed a sub-level!");
 * });
 *
 * // Prevent assembly in protected areas
 * BarehandedEvents.onBeforeAssemble((player, targetPos, blocks) -> {
 *     if (isProtectedArea(targetPos)) {
 *         player.sendSystemMessage(Component.literal("Cannot build here!"));
 *         return false; // Cancel assembly
 *     }
 *     return true; // Allow assembly
 * });
 * }</pre>
 *
 * @author JuaanP
 * @since 1.6.0
 */
public class BarehandedEvents {

    /**
     * Functional interface for before-assemble event.
     */
    @FunctionalInterface
    public interface BeforeAssemble {
        /**
         * Called before blocks are assembled into a sub-level.
         *
         * @param player The player attempting to assemble
         * @param targetPos The position where assembly is being attempted
         * @param assembledBlocks The list of blocks that would be assembled
         * @return true to allow assembly, false to cancel
         */
        boolean test(@NotNull Player player, @NotNull BlockPos targetPos, @NotNull List<BlockPos> assembledBlocks);
    }

    /**
     * Functional interface for on-assemble event.
     */
    @FunctionalInterface
    public interface OnAssemble {
        /**
         * Called after blocks have been successfully assembled into a sub-level.
         *
         * @param player The player who assembled the blocks
         * @param subLevel The newly created sub-level
         * @param assembledBlocks The list of blocks that were assembled
         */
        void accept(@NotNull Player player, @NotNull ServerSubLevel subLevel, @NotNull List<BlockPos> assembledBlocks);
    }

    // Use CopyOnWriteArrayList for thread-safety during iteration
    private static final List<BiPredicate<Player, ServerSubLevel>> BEFORE_GRAB_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<Player, ServerSubLevel>> ON_GRAB_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<Player, ServerSubLevel>> ON_RELEASE_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BeforeAssemble> BEFORE_ASSEMBLE_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<OnAssemble> ON_ASSEMBLE_LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * Registers a listener that is called before a player grabs a sub-level.
     *
     * <p>The listener can return false to cancel the grab action.</p>
     * <p>If multiple listeners are registered, ALL must return true for the grab to proceed.</p>
     *
     * @param listener The listener to register
     */
    public static void onBeforeGrab(@NotNull BiPredicate<Player, ServerSubLevel> listener) {
        BEFORE_GRAB_LISTENERS.add(listener);
    }

    /**
     * Registers a listener that is called after a player successfully grabs a sub-level.
     *
     * @param listener The listener to register
     */
    public static void onGrab(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        ON_GRAB_LISTENERS.add(listener);
    }

    /**
     * Registers a listener that is called when a player releases a grabbed sub-level.
     *
     * @param listener The listener to register
     */
    public static void onRelease(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        ON_RELEASE_LISTENERS.add(listener);
    }

    /**
     * Registers a listener that is called before blocks are assembled into a sub-level.
     *
     * <p>The listener can return false to cancel the assembly action.</p>
     * <p>If multiple listeners are registered, ALL must return true for the assembly to proceed.</p>
     *
     * @param listener The listener to register
     */
    public static void onBeforeAssemble(@NotNull BeforeAssemble listener) {
        BEFORE_ASSEMBLE_LISTENERS.add(listener);
    }

    /**
     * Registers a listener that is called after blocks have been successfully assembled.
     *
     * @param listener The listener to register
     */
    public static void onAssemble(@NotNull OnAssemble listener) {
        ON_ASSEMBLE_LISTENERS.add(listener);
    }

    /**
     * Removes a before-grab listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it was not registered
     */
    public static boolean removeBeforeGrabListener(@NotNull BiPredicate<Player, ServerSubLevel> listener) {
        return BEFORE_GRAB_LISTENERS.remove(listener);
    }

    /**
     * Removes an on-grab listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it was not registered
     */
    public static boolean removeOnGrabListener(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        return ON_GRAB_LISTENERS.remove(listener);
    }

    /**
     * Removes an on-release listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it was not registered
     */
    public static boolean removeOnReleaseListener(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        return ON_RELEASE_LISTENERS.remove(listener);
    }

    /**
     * Removes a before-assemble listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it was not registered
     */
    public static boolean removeBeforeAssembleListener(@NotNull BeforeAssemble listener) {
        return BEFORE_ASSEMBLE_LISTENERS.remove(listener);
    }

    /**
     * Removes an on-assemble listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it was not registered
     */
    public static boolean removeOnAssembleListener(@NotNull OnAssemble listener) {
        return ON_ASSEMBLE_LISTENERS.remove(listener);
    }

    /**
     * Fires the before-grab event.
     * Internal use only - do not call from external mods.
     *
     * @param player The player attempting to grab
     * @param subLevel The sub-level being grabbed
     * @return true if all listeners allow the grab, false if any listener cancels it
     */
    public static boolean fireBeforeGrab(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        return BEFORE_GRAB_LISTENERS.stream().allMatch(l -> l.test(player, subLevel));
    }

    /**
     * Fires the on-grab event.
     * Internal use only - do not call from external mods.
     *
     * @param player The player who grabbed
     * @param subLevel The sub-level that was grabbed
     */
    public static void fireOnGrab(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        ON_GRAB_LISTENERS.forEach(l -> l.accept(player, subLevel));
    }

    /**
     * Fires the on-release event.
     * Internal use only - do not call from external mods.
     *
     * @param player The player who released
     * @param subLevel The sub-level that was released
     */
    public static void fireOnRelease(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        ON_RELEASE_LISTENERS.forEach(l -> l.accept(player, subLevel));
    }

    /**
     * Fires the before-assemble event.
     * Internal use only - do not call from external mods.
     *
     * @param player The player attempting to assemble
     * @param targetPos The target position for assembly
     * @param blocks The list of blocks to be assembled
     * @return true if all listeners allow the assembly, false if any listener cancels it
     */
    public static boolean fireBeforeAssemble(@NotNull Player player, @NotNull BlockPos targetPos, @NotNull List<BlockPos> blocks) {
        return BEFORE_ASSEMBLE_LISTENERS.stream().allMatch(l -> l.test(player, targetPos, blocks));
    }

    /**
     * Fires the on-assemble event.
     * Internal use only - do not call from external mods.
     *
     * @param player The player who assembled
     * @param subLevel The sub-level that was created
     * @param blocks The list of blocks that were assembled
     */
    public static void fireOnAssemble(@NotNull Player player, @NotNull ServerSubLevel subLevel, @NotNull List<BlockPos> blocks) {
        ON_ASSEMBLE_LISTENERS.forEach(l -> l.accept(player, subLevel, blocks));
    }
}