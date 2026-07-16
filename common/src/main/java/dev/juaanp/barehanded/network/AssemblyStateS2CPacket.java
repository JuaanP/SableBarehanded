package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssemblyStateS2CPacket(int entityId, boolean active) implements CustomPacketPayload {
    public static final Type<AssemblyStateS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "assembly_state_s2c"));

    public static final StreamCodec<FriendlyByteBuf, AssemblyStateS2CPacket> CODEC = StreamCodec.of(
            (buf, packet) -> { buf.writeInt(packet.entityId()); buf.writeBoolean(packet.active()); },
            buf -> new AssemblyStateS2CPacket(buf.readInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}