package dev.juaanp.sablebarehanded.client;

import dev.juaanp.sablebarehanded.config.ClientConfig;
import dev.juaanp.sablebarehanded.config.ServerConfig;
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

        if (!ClientGrabSession.isHoldingGrab) {
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
            String hint = "Hold [ " + rotateKey + " ] to rotate ";
            int width = mc.font.width(hint);
            graphics.drawString(mc.font, hint, (screenWidth - width) / 2, textY, 0xAAAAAA, true);
            return;
        }

        boolean isKeyDown = KeyBindings.PIVOT_KEY.isDown();
        boolean isCenter = ClientConfig.INSTANCE.rotateAroundCenter ^ isKeyDown;

        String text = "Rotation Pivot: " + (isCenter ? "CENTER OF MASS " : "GRAB POINT ");
        int color = isCenter ? 0x55FF55 : 0xFFAA00;

        int textWidth = mc.font.width(text);

        int boxLeft = (screenWidth - textWidth) / 2 - 10;
        int boxRight = (screenWidth + textWidth) / 2 + 10;
        int boxTop = textY - 4;
        int boxBottom = textY + mc.font.lineHeight + 4;

        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0x88000000);
        graphics.drawString(mc.font, text, (screenWidth - textWidth) / 2, textY, color, true);

        String action = isKeyDown ? "Release " : "Hold ";
        String target = isCenter ? "Grab Point " : "Center Mass ";
        String hint = action + "[ " + pivotKey + " ] for " + target;

        int hintWidth = mc.font.width(hint);
        graphics.drawString(mc.font, hint, (screenWidth - hintWidth) / 2, hintY, 0xAAAAAA, true);
    }

    private static void renderPlaceToggle(GuiGraphics graphics, Minecraft mc) {
        if (!ClientInputTracker.isPlaceToggleActive()) return;
        if (ClientConfig.INSTANCE.hidePhysicsPlacementOverlay) return;

        String text = "Physics Placement";
        int width = mc.font.width(text);
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = screenWidth - width - 8;
        int y = 4;

        graphics.fill(x - 4, y - 2, x + width + 4, y + mc.font.lineHeight + 2, 0xAA000000);
        graphics.drawString(mc.font,
                Component.literal(text).withStyle(ChatFormatting.AQUA),
                x, y, 0xFFFFFF, true);
    }
}