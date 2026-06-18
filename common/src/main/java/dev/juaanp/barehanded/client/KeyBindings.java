package dev.juaanp.barehanded.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping ROTATE_KEY = new KeyMapping(
            "Rotate Sub-level",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
            "Barehanded"
    );

    public static final KeyMapping PIVOT_KEY = new KeyMapping(
            "Alternative Rotation Pivot",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT,
            "Barehanded"
    );

    public static final KeyMapping DISASSEMBLE_KEY = new KeyMapping(
            "Disassemble Sub-level",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q,
            "Barehanded"
    );

    public static final KeyMapping PLACE_TOGGLE_KEY = new KeyMapping(
            "Physics Placement Toggle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X,
            "Barehanded"
    );

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        long window = mc.getWindow().getWindow();
        KeyMapping[] ourKeys = {ROTATE_KEY, PIVOT_KEY, DISASSEMBLE_KEY, PLACE_TOGGLE_KEY};

        boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();

        for (KeyMapping ourKey : ourKeys) {
            InputConstants.Key key = InputConstants.getKey(ourKey.saveString());
            if (key != InputConstants.UNKNOWN) {
                boolean isPhysicallyDown = key.getType() == InputConstants.Type.MOUSE
                        ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                        : GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;

                ourKey.setDown(isPhysicallyDown);

                if (isGrabbing && ourKey != PIVOT_KEY) {
                    for (KeyMapping mapping : mc.options.keyMappings) {
                        if (mapping != ourKey && mapping.same(ourKey)) {
                            mapping.setDown(false);
                            while (mapping.consumeClick()) {}
                        }
                    }
                }
            }
        }

        if (ClientGrabSession.isHoldingGrab && !ClientGrabSession.isWaitingForGrabSync) {
            boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            if (!leftDown && !rightDown && !ClientGrabSession.pendingStopGrab) {
                dev.juaanp.barehanded.platform.Services.NETWORK.sendStopGrabbingRequest();
                ClientGrabSession.isHoldingGrab = false;
                ClientGrabSession.pendingStopGrab = true;
            }
        }
    }
}