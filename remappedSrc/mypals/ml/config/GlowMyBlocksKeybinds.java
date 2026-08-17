package mypals.ml.config;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;

import com.mojang.blaze3d.platform.InputConstants;

public class GlowMyBlocksKeybinds {
    public static KeyMapping addOutlineArea;
    public static KeyMapping deleteOutlineArea;
    public static KeyMapping openConfigKey;
    public static KeyMapping openColorPickerKey;

    public static void init() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockOutline.config",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_F8,
                "category.blockOutline"
        ));
        addOutlineArea = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockOutline.addSelection",
                InputConstants.Type.KEYSYM,
                GLFW_KEY_RIGHT_ALT,
                "category.blockOutline"
        ));
        deleteOutlineArea = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockOutline.removeSelection",
                InputConstants.Type.KEYSYM,
                GLFW_KEY_RIGHT_CONTROL,
                "category.blockOutline"
        ));

        openColorPickerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockOutline.color_picker",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.blockOutline"
        ));
    }
}
