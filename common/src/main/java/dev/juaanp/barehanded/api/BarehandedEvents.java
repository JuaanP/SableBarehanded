package dev.juaanp.barehanded.api;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class BarehandedEvents {

    @FunctionalInterface
    public interface BeforeAssemble {

        boolean test(@NotNull Player player, @NotNull BlockPos targetPos, @NotNull List<BlockPos> assembledBlocks);
    }

    @FunctionalInterface
    public interface OnAssemble {

        void accept(@NotNull Player player, @NotNull ServerSubLevel subLevel, @NotNull List<BlockPos> assembledBlocks);
    }

    private static final List<BiPredicate<Player, ServerSubLevel>> BEFORE_GRAB_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<Player, ServerSubLevel>> ON_GRAB_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<Player, ServerSubLevel>> ON_RELEASE_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BeforeAssemble> BEFORE_ASSEMBLE_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<OnAssemble> ON_ASSEMBLE_LISTENERS = new CopyOnWriteArrayList<>();

    public static void onBeforeGrab(@NotNull BiPredicate<Player, ServerSubLevel> listener) {
        BEFORE_GRAB_LISTENERS.add(listener);
    }

    public static void onGrab(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        ON_GRAB_LISTENERS.add(listener);
    }

    public static void onRelease(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        ON_RELEASE_LISTENERS.add(listener);
    }

    public static void onBeforeAssemble(@NotNull BeforeAssemble listener) {
        BEFORE_ASSEMBLE_LISTENERS.add(listener);
    }

    public static void onAssemble(@NotNull OnAssemble listener) {
        ON_ASSEMBLE_LISTENERS.add(listener);
    }

    public static boolean removeBeforeGrabListener(@NotNull BiPredicate<Player, ServerSubLevel> listener) {
        return BEFORE_GRAB_LISTENERS.remove(listener);
    }

    public static boolean removeOnGrabListener(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        return ON_GRAB_LISTENERS.remove(listener);
    }

    public static boolean removeOnReleaseListener(@NotNull BiConsumer<Player, ServerSubLevel> listener) {
        return ON_RELEASE_LISTENERS.remove(listener);
    }

    public static boolean removeBeforeAssembleListener(@NotNull BeforeAssemble listener) {
        return BEFORE_ASSEMBLE_LISTENERS.remove(listener);
    }

    public static boolean removeOnAssembleListener(@NotNull OnAssemble listener) {
        return ON_ASSEMBLE_LISTENERS.remove(listener);
    }

    public static boolean fireBeforeGrab(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        return BEFORE_GRAB_LISTENERS.stream().allMatch(l -> l.test(player, subLevel));
    }

    public static void fireOnGrab(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        ON_GRAB_LISTENERS.forEach(l -> l.accept(player, subLevel));
    }

    public static void fireOnRelease(@NotNull Player player, @NotNull ServerSubLevel subLevel) {
        ON_RELEASE_LISTENERS.forEach(l -> l.accept(player, subLevel));
    }

    public static boolean fireBeforeAssemble(@NotNull Player player, @NotNull BlockPos targetPos, @NotNull List<BlockPos> blocks) {
        return BEFORE_ASSEMBLE_LISTENERS.stream().allMatch(l -> l.test(player, targetPos, blocks));
    }

    public static void fireOnAssemble(@NotNull Player player, @NotNull ServerSubLevel subLevel, @NotNull List<BlockPos> blocks) {
        ON_ASSEMBLE_LISTENERS.forEach(l -> l.accept(player, subLevel, blocks));
    }
}