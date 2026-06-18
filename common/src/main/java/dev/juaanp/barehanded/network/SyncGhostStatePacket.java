package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record SyncGhostStatePacket(UUID subLevelId, UUID grabberId, byte collisionMask) implements CustomPacketPayload {

    public static final Type<SyncGhostStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_ghost_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGhostStatePacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SyncGhostStatePacket::subLevelId,
            UUIDUtil.STREAM_CODEC, SyncGhostStatePacket::grabberId,
            ByteBufCodecs.BYTE, SyncGhostStatePacket::collisionMask,
            SyncGhostStatePacket::new
    );

    public boolean ignoreEverything() { return (collisionMask & 1) != 0; }
    public boolean ignoreSelf()       { return (collisionMask & 2) != 0; }
    public boolean ignoreOthers()     { return (collisionMask & 4) != 0; }
    public boolean ignoreEntities()   { return (collisionMask & 8) != 0; }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}