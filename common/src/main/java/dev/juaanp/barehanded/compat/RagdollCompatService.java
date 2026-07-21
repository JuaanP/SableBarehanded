package dev.juaanp.barehanded.compat;

import dev.juaanp.barehanded.Constants;

public final class RagdollCompatService {
    
    private static IRagdollCompat instance = IRagdollCompat.NOOP;
    
    private RagdollCompatService() {}
    
    public static IRagdollCompat get() {
        return instance;
    }

    public static void register(IRagdollCompat impl) {
        instance = impl != null ? impl : IRagdollCompat.NOOP;
        Constants.LOG.info("[Barehanded] Ragdoll compat registered: {}", impl != null ? "active" : "noop");
    }
}