package dev.juaanp.barehanded;

import com.google.gson.Gson;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.network.*;
import dev.juaanp.barehanded.physics.GrabPhysicsController;
import dev.juaanp.barehanded.physics.ServerGrabManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class BarehandedFabric implements ModInitializer {
    private static final Gson GSON = new Gson();

    @Override
    public void onInitialize() {
        ServerConfig.load();

        PayloadTypeRegistry.playC2S().register(RequestGrabPacket.TYPE, RequestGrabPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AssembleGrabPacket.TYPE, AssembleGrabPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(StopGrabbingPacket.TYPE, StopGrabbingPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RotateGrabPacket.TYPE, RotateGrabPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DisassembleRequestPacket.TYPE, DisassembleRequestPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PhysicsPlaceRequestPacket.TYPE, PhysicsPlaceRequestPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateServerConfigPacket.TYPE, UpdateServerConfigPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AssemblyStateC2SPacket.TYPE, AssemblyStateC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AdjustDistancePacket.TYPE, AdjustDistancePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AltStateC2SPacket.TYPE, AltStateC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(StartGrabbingAnimationPacket.TYPE, StartGrabbingAnimationPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(StopGrabbingAnimationPacket.TYPE, StopGrabbingAnimationPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGhostStatePacket.TYPE, SyncGhostStatePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncConfigPacket.TYPE, SyncConfigPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGrabStatePacket.TYPE, SyncGrabStatePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(AssemblyStateS2CPacket.TYPE, AssemblyStateS2CPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestGrabPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleRequestGrab(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(AssembleGrabPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleAssembleGrab(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(StopGrabbingPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleStopGrabbing(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(RotateGrabPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleRotateGrab(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(DisassembleRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleDisassembleRequest(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(PhysicsPlaceRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handlePhysicsPlaceRequest(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdateServerConfigPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleUpdateServerConfig(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(AssemblyStateC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleAssemblyState(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(AdjustDistancePacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleAdjustDistance(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(AltStateC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleAltState(payload, context.player()));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                GrabPhysicsController.tickPlayer(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SyncConfigPacket syncPacket = new SyncConfigPacket(GSON.toJson(ServerConfig.INSTANCE));
            ServerPlayNetworking.send(handler.getPlayer(), syncPacket);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerGrabManager.onPlayerLoggedOut(handler.getPlayer());
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Player player) {
                ServerGrabManager.onPlayerDeath(player);
            }
        });

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, trackingPlayer) -> {
            if (trackedEntity instanceof Player targetPlayer) {
                if (ServerGrabManager.isPlayerGrabbing(targetPlayer)) {
                    StartGrabbingAnimationPacket packet = new StartGrabbingAnimationPacket(targetPlayer.getId());
                    ServerPlayNetworking.send(trackingPlayer, packet);
                }
            }
        });
    }
}