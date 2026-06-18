package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssemblyStateC2SPacket(boolean active) implements CustomPacketPayload {
    public static final Type<AssemblyStateC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "assembly_state_c2s"));
    
    public static final StreamCodec<FriendlyByteBuf, AssemblyStateC2SPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.active()),
            buf -> new AssemblyStateC2SPacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}