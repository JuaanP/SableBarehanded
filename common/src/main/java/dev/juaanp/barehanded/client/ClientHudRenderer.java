package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ClientHudRenderer {

    public static void renderOverlay(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientConfig.INSTANCE.hideGrabHud && ClientGrabSession.isHoldingGrab) {
            return;
        }

        if (!ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync) {
            renderPlaceToggle(graphics, mc);
            return;
        }

        if (!ServerConfig.INSTANCE.enableRotation) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        renderPlaceToggle(graphics, mc);

        String rotateKey = KeyBindings.ROTATE_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String pivotKey = KeyBindings.PIVOT_KEY.getTranslatedKeyMessage().getString().toUpperCase();

        int textY = screenHeight - 65;
        int hintY = textY - 14;

        if (!KeyBindings.ROTATE_KEY.isDown()) {
            Component hintComp = Component.translatable("hud.barehanded.hold_to_rotate", rotateKey);
            int width = mc.font.width(hintComp);
            graphics.drawString(mc.font, hintComp, (screenWidth - width) / 2, textY, 0xAAAAAA, true);
            return;
        }

        boolean isKeyDown = KeyBindings.PIVOT_KEY.isDown();
        boolean isCenter = ClientConfig.INSTANCE.rotateAroundCenter ^ isKeyDown;

        Component pivotTarget = Component.translatable(isCenter ? "hud.barehanded.pivot.center_of_mass" : "hud.barehanded.pivot.grab_point");
        Component textComp = Component.translatable("hud.barehanded.rotation_pivot", pivotTarget);
        int color = isCenter ? 0x55FF55 : 0xFFAA00;

        int textWidth = mc.font.width(textComp);

        int boxLeft = (screenWidth - textWidth) / 2 - 10;
        int boxRight = (screenWidth + textWidth) / 2 + 10;
        int boxTop = textY - 4;
        int boxBottom = textY + mc.font.lineHeight + 4;

        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0x88000000);
        graphics.drawString(mc.font, textComp, (screenWidth - textWidth) / 2, textY, color, true);

        Component targetComp = Component.translatable(isCenter ? "hud.barehanded.pivot.grab_point_short" : "hud.barehanded.pivot.center_mass_short");
        Component hintComp = Component.translatable(isKeyDown ? "hud.barehanded.hint.release" : "hud.barehanded.hint.hold", pivotKey, targetComp);

        int hintWidth = mc.font.width(hintComp);
        graphics.drawString(mc.font, hintComp, (screenWidth - hintWidth) / 2, hintY, 0xAAAAAA, true);
    }

    private static void renderPlaceToggle(GuiGraphics graphics, Minecraft mc) {
        if (!ClientInputTracker.isPlaceToggleActive()) return;
        if (ClientConfig.INSTANCE.hidePhysicsPlacementOverlay) return;

        Component textComp = Component.translatable("hud.barehanded.physics_placement");
        int width = mc.font.width(textComp);
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = screenWidth - width - 8;
        int y = 4;

        graphics.fill(x - 4, y - 2, x + width + 4, y + mc.font.lineHeight + 2, 0xAA000000);
        graphics.drawString(mc.font,
                textComp.copy().withStyle(ChatFormatting.AQUA),
                x, y, 0xFFFFFF, true);
    }
}