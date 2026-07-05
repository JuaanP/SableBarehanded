package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AltStateC2SPacket(boolean isAltDown) implements CustomPacketPayload {
    public static final Type<AltStateC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "alt_state_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AltStateC2SPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AltStateC2SPacket::isAltDown,
            AltStateC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}