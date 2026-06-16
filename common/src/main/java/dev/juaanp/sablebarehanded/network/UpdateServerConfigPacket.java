package dev.juaanp.sablebarehanded.network;

import dev.juaanp.sablebarehanded.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateServerConfigPacket(String json) implements CustomPacketPayload {
    public static final Type<UpdateServerConfigPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_server_config"));

    public static final StreamCodec<FriendlyByteBuf, UpdateServerConfigPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.json()),
            buf -> new UpdateServerConfigPacket(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}