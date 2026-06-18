package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StopGrabbingPacket() implements CustomPacketPayload {

    public static final Type<StopGrabbingPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "stop_grabbing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopGrabbingPacket> CODEC = StreamCodec.unit(new StopGrabbingPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}