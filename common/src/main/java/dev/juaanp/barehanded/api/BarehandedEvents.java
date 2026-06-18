package dev.juaanp.barehanded.api;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class BarehandedEvents {

    @FunctionalInterface
    public interface BeforeAssemble {
        boolean test(Player player, BlockPos targetPos, List<BlockPos> assembledBlocks);
    }

    @FunctionalInterface
    public interface OnAssemble {
        void accept(Player player, ServerSubLevel subLevel, List<BlockPos> assembledBlocks);
    }

    private static final List<BiPredicate<Player, ServerSubLevel>> BEFORE_GRAB_LISTENERS = new ArrayList<>();
    private static final List<BiConsumer<Player, ServerSubLevel>> ON_GRAB_LISTENERS = new ArrayList<>();
    private static final List<BiConsumer<Player, ServerSubLevel>> ON_RELEASE_LISTENERS = new ArrayList<>();
    private static final List<BeforeAssemble> BEFORE_ASSEMBLE_LISTENERS = new ArrayList<>();
    private static final List<OnAssemble> ON_ASSEMBLE_LISTENERS = new ArrayList<>();

    public static void onBeforeGrab(BiPredicate<Player, ServerSubLevel> listener) { BEFORE_GRAB_LISTENERS.add(listener); }
    public static void onGrab(BiConsumer<Player, ServerSubLevel> listener) { ON_GRAB_LISTENERS.add(listener); }
    public static void onRelease(BiConsumer<Player, ServerSubLevel> listener) { ON_RELEASE_LISTENERS.add(listener); }
    public static void onBeforeAssemble(BeforeAssemble listener) { BEFORE_ASSEMBLE_LISTENERS.add(listener); }
    public static void onAssemble(OnAssemble listener) { ON_ASSEMBLE_LISTENERS.add(listener); }

    public static boolean fireBeforeGrab(Player player, ServerSubLevel subLevel) {
        return BEFORE_GRAB_LISTENERS.stream().allMatch(l -> l.test(player, subLevel));
    }

    public static void fireOnGrab(Player player, ServerSubLevel subLevel) { ON_GRAB_LISTENERS.forEach(l -> l.accept(player, subLevel)); }
    public static void fireOnRelease(Player player, ServerSubLevel subLevel) { ON_RELEASE_LISTENERS.forEach(l -> l.accept(player, subLevel)); }

    public static boolean fireBeforeAssemble(Player player, BlockPos targetPos, List<BlockPos> blocks) {
        return BEFORE_ASSEMBLE_LISTENERS.stream().allMatch(l -> l.test(player, targetPos, blocks));
    }

    public static void fireOnAssemble(Player player, ServerSubLevel subLevel, List<BlockPos> blocks) {
        ON_ASSEMBLE_LISTENERS.forEach(l -> l.accept(player, subLevel, blocks));
    }
}