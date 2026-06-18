package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestGrabPacket(BlockPos blockPos) implements CustomPacketPayload {

    public static final Type<RequestGrabPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "request_grab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestGrabPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RequestGrabPacket::blockPos,
            RequestGrabPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}