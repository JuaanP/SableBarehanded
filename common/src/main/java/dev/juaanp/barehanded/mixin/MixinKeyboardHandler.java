package dev.juaanp.barehanded.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.juaanp.barehanded.client.ClientAssemblyTracker;
import dev.juaanp.barehanded.client.ClientGrabSession;
import dev.juaanp.barehanded.client.KeyBindings;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void barehanded$onKeyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) return;

        boolean isGrabbing = ClientGrabSession.isHoldingGrab || ClientAssemblyTracker.isActive();
        if (!isGrabbing) return;

        KeyMapping[] ourKeys = {
                KeyBindings.ROTATE_KEY,
                KeyBindings.PIVOT_KEY,
                KeyBindings.DISASSEMBLE_KEY,
                KeyBindings.PLACE_TOGGLE_KEY
        };

        for (KeyMapping ourKey : ourKeys) {
            InputConstants.Key boundKey = InputConstants.getKey(ourKey.saveString());
            if (boundKey.getType() == InputConstants.Type.KEYSYM && boundKey.getValue() == key) {
                ci.cancel();
                return;
            }
        }
    }
}