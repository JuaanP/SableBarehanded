package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AdjustDistancePacket(double amount) implements CustomPacketPayload {
    public static final Type<AdjustDistancePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "adjust_distance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdjustDistancePacket> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.DOUBLE, AdjustDistancePacket::amount,
            AdjustDistancePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}