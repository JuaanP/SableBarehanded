package dev.juaanp.sablebarehanded.network;

import dev.juaanp.sablebarehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DisassembleRequestPacket() implements CustomPacketPayload {
    public static final Type<DisassembleRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "disassemble_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DisassembleRequestPacket> CODEC =
            StreamCodec.unit(new DisassembleRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}