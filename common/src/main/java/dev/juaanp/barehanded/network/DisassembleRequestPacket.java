package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DisassembleRequestPacket(boolean isAltDown) implements CustomPacketPayload {
    public static final Type<DisassembleRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "disassemble_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DisassembleRequestPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DisassembleRequestPacket::isAltDown,
            DisassembleRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}