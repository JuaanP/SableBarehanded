package dev.juaanp.barehanded.network;

import dev.juaanp.barehanded.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssembleGrabPacket(BlockPos blockPos) implements CustomPacketPayload {

    public static final Type<AssembleGrabPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "assemble_grab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssembleGrabPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AssembleGrabPacket::blockPos,
            AssembleGrabPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}