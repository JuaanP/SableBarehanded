package dev.juaanp.barehanded;

import com.google.gson.Gson;
import dev.juaanp.barehanded.client.ClientPayloadHandler;
import dev.juaanp.barehanded.client.NeoForgeClient;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.network.*;
import dev.juaanp.barehanded.physics.GrabPhysicsController;
import dev.juaanp.barehanded.physics.ServerGrabManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public class BarehandedNeoForge {
    private static final Gson GSON = new Gson();

    public BarehandedNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ServerConfig.load();

        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onStartTracking);

        if (FMLEnvironment.dist.isClient()) {
            NeoForgeClient.init(modEventBus, modContainer);
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GrabPhysicsController.tickPlayer(serverPlayer);
        }
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SyncConfigPacket packet = new SyncConfigPacket(GSON.toJson(ServerConfig.INSTANCE));
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerGrabManager.onPlayerLoggedOut(serverPlayer);
        }
    }

    private void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerGrabManager.onPlayerDeath(serverPlayer);
        }
    }

    private void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player targetPlayer) {
            if (ServerGrabManager.isPlayerGrabbing(targetPlayer)) {
                if (event.getEntity() instanceof ServerPlayer tracker) {
                    PacketDistributor.sendToPlayer(tracker, new StartGrabbingAnimationPacket(targetPlayer.getId()));
                }
            }
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToServer(RequestGrabPacket.TYPE, RequestGrabPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleRequestGrab((ServerPlayer) context.player(), payload)));

        registrar.playToServer(AssembleGrabPacket.TYPE, AssembleGrabPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleAssembleGrab((ServerPlayer) context.player(), payload)));

        registrar.playToServer(StopGrabbingPacket.TYPE, StopGrabbingPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleStopGrabbing((ServerPlayer) context.player(), payload)));

        registrar.playToServer(RotateGrabPacket.TYPE, RotateGrabPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleRotateGrab((ServerPlayer) context.player(), payload)));

        registrar.playToServer(DisassembleRequestPacket.TYPE, DisassembleRequestPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleDisassembleRequest((ServerPlayer) context.player(), payload)));

        registrar.playToServer(PhysicsPlaceRequestPacket.TYPE, PhysicsPlaceRequestPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handlePhysicsPlaceRequest((ServerPlayer) context.player(), payload)));

        registrar.playToServer(UpdateServerConfigPacket.TYPE, UpdateServerConfigPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleUpdateServerConfig((ServerPlayer) context.player(), payload)));

        registrar.playToServer(AssemblyStateC2SPacket.TYPE, AssemblyStateC2SPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleAssemblyState((ServerPlayer) context.player(), payload)));

        registrar.playToServer(AdjustDistancePacket.TYPE, AdjustDistancePacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerPayloadHandler.handleAdjustDistance((ServerPlayer) context.player(), payload)));

        registrar.playToClient(StartGrabbingAnimationPacket.TYPE, StartGrabbingAnimationPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.handleStartGrabbingAnimation(payload)));

        registrar.playToClient(StopGrabbingAnimationPacket.TYPE, StopGrabbingAnimationPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.handleStopGrabbingAnimation(payload)));

        registrar.playToClient(SyncGhostStatePacket.TYPE, SyncGhostStatePacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.handleSyncGhostState(payload)));

        registrar.playToClient(SyncGrabStatePacket.TYPE, SyncGrabStatePacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.handleSyncGrabState(payload)));

        registrar.playToClient(SyncConfigPacket.TYPE, SyncConfigPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientConfigSyncHandler.applyServerConfig(payload.configJson())));

        registrar.playToClient(AssemblyStateS2CPacket.TYPE, AssemblyStateS2CPacket.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.handleAssemblyStateSync(payload)));
    }
}