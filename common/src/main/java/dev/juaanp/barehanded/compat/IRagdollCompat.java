package dev.juaanp.barehanded.compat;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public interface IRagdollCompat {
    
    boolean isLoaded();
    
    boolean isPlayerRagdollSubLevel(ServerSubLevel subLevel);
    
    boolean isMobRagdollSubLevel(ServerSubLevel subLevel);
    
    boolean isAnyRagdollSubLevel(ServerSubLevel subLevel);
    
    boolean releaseRagdoll(ServerLevel level, ServerSubLevel subLevel);
    
    boolean isPlayerRidingOwnRagdoll(Player player, ServerSubLevel targetSubLevel);
    
    boolean isLocalPlayerRidingRagdoll(Player player);
    
    IRagdollCompat NOOP = new IRagdollCompat() {
        @Override public boolean isLoaded() { return false; }
        @Override public boolean isPlayerRagdollSubLevel(ServerSubLevel subLevel) { return false; }
        @Override public boolean isMobRagdollSubLevel(ServerSubLevel subLevel) { return false; }
        @Override public boolean isAnyRagdollSubLevel(ServerSubLevel subLevel) { return false; }
        @Override public boolean releaseRagdoll(ServerLevel level, ServerSubLevel subLevel) { return false; }
        @Override public boolean isPlayerRidingOwnRagdoll(Player player, ServerSubLevel targetSubLevel) { return false; }
        @Override public boolean isLocalPlayerRidingRagdoll(Player player) { return false; }
    };
}