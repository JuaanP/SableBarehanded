package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StopGrabbingAnimationPacket(int entityId) implements CustomPacketPayload {

    public static final Type<StopGrabbingAnimationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "stop_grabbing_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopGrabbingAnimationPacket> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT, StopGrabbingAnimationPacket::entityId,
            StopGrabbingAnimationPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}