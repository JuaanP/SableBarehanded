package dev.juaanp.barehanded.client.handler;

import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.KeyBindings;
import dev.juaanp.barehanded.config.ClientConfig;

public class MovementInputHandler {

    public static boolean shouldPreventMovement() {
        return ClientGrabSession.isHoldingGrab &&
               KeyBindings.ROTATE_KEY.isDown() &&
               ClientConfig.INSTANCE.preventMovementWhileRotating;
    }
}