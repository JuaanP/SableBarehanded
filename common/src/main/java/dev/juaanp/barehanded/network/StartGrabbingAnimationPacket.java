package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StartGrabbingAnimationPacket(int entityId) implements CustomPacketPayload {

    public static final Type<StartGrabbingAnimationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "start_grabbing_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartGrabbingAnimationPacket> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT, StartGrabbingAnimationPacket::entityId,
            StartGrabbingAnimationPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}