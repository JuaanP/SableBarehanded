package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.Constants;
import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClient {

    public static void init(net.neoforged.bus.api.IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (client, parent) -> dev.juaanp.barehanded.client.BarehandedConfigScreen.create(parent));
        modEventBus.addListener(NeoForgeClient::registerKeyMappings);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.ROTATE_KEY);
        event.register(KeyBindings.PIVOT_KEY);
        event.register(KeyBindings.DISASSEMBLE_KEY);
        event.register(KeyBindings.PLACE_TOGGLE_KEY);
        event.register(KeyBindings.GRAB_KEY);
        event.register(KeyBindings.PULL_KEY);
        event.register(KeyBindings.PUSH_KEY);
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ClientTickOrchestrator.tick(mc);
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        KeyBindings.clientTick();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ClientHudRenderer.renderOverlay(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerConfig.load();
        ClientConfig.load();
    }
}