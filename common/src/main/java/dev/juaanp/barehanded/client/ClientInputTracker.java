package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;

public class ClientInputTracker {
    public static boolean preventRegrabUntilRelease = false;
    public static boolean wasHoldingGrabLastTick = false;
    public static int keysReleasedTicks = 0;

    public static double pendingYaw = 0.0;
    public static double pendingPitch = 0.0;

    private static boolean placeToggleActive = false;
    private static boolean lastPlaceToggleDown = false;

    public static boolean grabToggleActive = false;
    private static boolean lastGrabKeyDown = false;

    public static boolean suppressMiningUntilRelease = false;

    public static void tickDebounce(boolean eitherDown, boolean bothDown, boolean grabKeyPressed) {

        if (grabKeyPressed && !lastGrabKeyDown) {
            grabToggleActive = !grabToggleActive;
        }
        lastGrabKeyDown = grabKeyPressed;

        boolean anyInputActive = eitherDown || grabToggleActive;

        if (!anyInputActive) {
            keysReleasedTicks++;
        } else {
            keysReleasedTicks = 0;
        }

        if (keysReleasedTicks >= ClientConfig.INSTANCE.regrabDebounceTicks) {
            preventRegrabUntilRelease = false;
        }

        if (wasHoldingGrabLastTick && !ClientGrabSession.isHoldingGrab && anyInputActive) {
            preventRegrabUntilRelease = true;
            keysReleasedTicks = 0;
            grabToggleActive = false;
        }

        wasHoldingGrabLastTick = ClientGrabSession.isHoldingGrab;

        tickPlaceToggle();
    }

    private static void tickPlaceToggle() {
        boolean isDown = KeyBindings.PLACE_TOGGLE_KEY.isDown();
        if (isDown && !lastPlaceToggleDown) {
            placeToggleActive = !placeToggleActive;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && ClientConfig.INSTANCE.showPhysicsPlacementMessage) {
                String state = placeToggleActive ? "Physics" : "Normal";
                net.minecraft.ChatFormatting color = placeToggleActive ? net.minecraft.ChatFormatting.AQUA : net.minecraft.ChatFormatting.WHITE;
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Block Placement: ")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)
                                .append(net.minecraft.network.chat.Component.literal(state).withStyle(color)),
                        true);
            }
        }
        lastPlaceToggleDown = isDown;
    }

    public static boolean isPlaceToggleActive() { return placeToggleActive; }

    public static boolean canInitiateGrab() {
        return !preventRegrabUntilRelease;
    }
}