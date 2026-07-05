package dev.juaanp.barehanded.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyBindings {
    public static final KeyMapping ROTATE_KEY = new KeyMapping("Rotate Sub-level", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "Barehanded");
    public static final KeyMapping PIVOT_KEY = new KeyMapping("Modifier Key (Pivot/Priority)", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "Barehanded");
    public static final KeyMapping DISASSEMBLE_KEY = new KeyMapping("Disassemble Sub-level", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, "Barehanded");
    public static final KeyMapping PLACE_TOGGLE_KEY = new KeyMapping("Physics Placement Toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "Barehanded");
    public static final KeyMapping GRAB_KEY = new KeyMapping("Toggle Grab Sub-level", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "Barehanded");

    public static final KeyMapping PULL_KEY = new KeyMapping("Pull Object Closer", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, "Barehanded");
    public static final KeyMapping PUSH_KEY = new KeyMapping("Push Object Further", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, "Barehanded");

    private static final Map<KeyMapping, Boolean> wasPhysicallyDown = new HashMap<>();

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        long window = mc.getWindow().getWindow();
        KeyMapping[] ourKeys = {ROTATE_KEY, PIVOT_KEY, DISASSEMBLE_KEY, PLACE_TOGGLE_KEY, GRAB_KEY, PULL_KEY, PUSH_KEY};

        boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();

        for (KeyMapping ourKey : ourKeys) {
            if (ourKey.isUnbound()) continue;

            InputConstants.Key key = InputConstants.getKey(ourKey.saveString());
            if (key.getValue() != InputConstants.UNKNOWN.getValue()) {
                boolean physical = key.getType() == InputConstants.Type.MOUSE
                        ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                        : GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;

                if (physical) {
                    ourKey.setDown(true);
                    wasPhysicallyDown.put(ourKey, true);
                }
                else if (wasPhysicallyDown.getOrDefault(ourKey, false)) {
                    ourKey.setDown(false);
                    wasPhysicallyDown.put(ourKey, false);
                }
            }

            if (isGrabbing && ourKey != PIVOT_KEY && ourKey.isDown()) {
                for (KeyMapping mapping : mc.options.keyMappings) {
                    if (mapping != ourKey && mapping.same(ourKey)) {
                        mapping.setDown(false);
                    }
                }
            }
        }
    }
}