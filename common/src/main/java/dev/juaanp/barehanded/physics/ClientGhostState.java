package dev.juaanp.barehanded.physics;

import java.util.UUID;

public class ClientGhostState {
    public final UUID subLevelId;
    public final boolean ignoreEverything;
    public final boolean ignoreSelf;
    public final boolean ignoreOthers;
    public final boolean ignoreEntities;

    public ClientGhostState(UUID subLevelId, byte mask) {
        this.subLevelId = subLevelId;
        this.ignoreEverything = (mask & 1) != 0;
        this.ignoreSelf = (mask & 2) != 0;
        this.ignoreOthers = (mask & 4) != 0;
        this.ignoreEntities = (mask & 8) != 0;
    }
}