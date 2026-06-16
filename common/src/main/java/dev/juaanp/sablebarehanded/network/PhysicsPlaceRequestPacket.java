package dev.juaanp.sablebarehanded.network;

import dev.juaanp.sablebarehanded.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhysicsPlaceRequestPacket(BlockPos pos, Direction face, boolean isMainHand) implements CustomPacketPayload {

    public static final Type<PhysicsPlaceRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "physics_place"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsPlaceRequestPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PhysicsPlaceRequestPacket::pos,
            Direction.STREAM_CODEC, PhysicsPlaceRequestPacket::face,
            ByteBufCodecs.BOOL, PhysicsPlaceRequestPacket::isMainHand,
            PhysicsPlaceRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}