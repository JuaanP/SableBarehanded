package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;

import java.util.UUID;

public record SyncGrabStatePacket(int entityId, double mass, UUID subLevelId, Vector3d localPivot, double distance) implements CustomPacketPayload {

    public static final Type<SyncGrabStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_grab_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncGrabStatePacket> CODEC = StreamCodec.ofMember(
            SyncGrabStatePacket::write,
            SyncGrabStatePacket::new
    );

    public SyncGrabStatePacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readDouble(), buf.readUUID(), new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeDouble(this.mass);
        buf.writeUUID(this.subLevelId);
        buf.writeDouble(this.localPivot.x);
        buf.writeDouble(this.localPivot.y);
        buf.writeDouble(this.localPivot.z);
        buf.writeDouble(this.distance);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}