package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

public class FabricClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KeyBindings.ROTATE_KEY);
        KeyBindingHelper.registerKeyBinding(KeyBindings.PIVOT_KEY);
        KeyBindingHelper.registerKeyBinding(KeyBindings.DISASSEMBLE_KEY);
        KeyBindingHelper.registerKeyBinding(KeyBindings.PLACE_TOGGLE_KEY);

        ClientPlayNetworking.registerGlobalReceiver(StartGrabbingAnimationPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleStartGrabbingAnimation(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(StopGrabbingAnimationPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleStopGrabbingAnimation(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncGhostStatePacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleSyncGhostState(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncConfigPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientConfigSyncHandler.applyServerConfig(payload.configJson()));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncGrabStatePacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleSyncGrabState(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(AssemblyStateS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleAssemblyStateSync(payload));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerConfig.load();
            ClientConfig.load();
        });

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickOrchestrator::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> KeyBindings.clientTick());

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> ClientHudRenderer.renderOverlay(graphics));

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide() && dev.juaanp.barehanded.client.handler.ClientInteractionHandler.shouldCancelInteraction()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() && dev.juaanp.barehanded.client.handler.ClientInteractionHandler.shouldCancelInteraction()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}