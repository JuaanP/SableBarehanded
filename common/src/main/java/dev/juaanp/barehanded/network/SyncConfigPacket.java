package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncConfigPacket(String configJson) implements CustomPacketPayload {

    public static final Type<SyncConfigPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncConfigPacket> CODEC = StreamCodec.ofMember(
            SyncConfigPacket::write,
            SyncConfigPacket::new
    );

    public SyncConfigPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(262144));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.configJson, 262144);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}