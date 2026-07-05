package dev.juaanp.barehanded.client.handler;

import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.ClientInputTracker;
import dev.juaanp.barehanded.client.KeyBindings;
import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;

public class RotationInputHandler {

    public static boolean handleRotation(double dx, double dy) {
        if (ClientGrabSession.isHoldingGrab && KeyBindings.ROTATE_KEY.isDown() && ServerConfig.INSTANCE.enableRotation) {

            if (dx != 0.0 || dy != 0.0) {
                double hSens = ClientConfig.INSTANCE.horizontalRotationSensitivity * 0.01;
                double vSens = ClientConfig.INSTANCE.verticalRotationSensitivity * 0.01;

                double yaw   = -dx * hSens;
                double pitch = -dy * vSens;

                if (ClientConfig.INSTANCE.invertHorizontalRotation) yaw = -yaw;
                if (ClientConfig.INSTANCE.invertVerticalRotation)   pitch = -pitch;

                ClientInputTracker.pendingYaw   += yaw;
                ClientInputTracker.pendingPitch += pitch;
            }

            return true;
        }
        return false;
    }
}