package mypals.ml.config;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import static org.lwjgl.glfw.GLFW.*;

public class GlowMyBlocksKeybinds {
    public static KeyBinding addOutlineArea;
    public static KeyBinding deleteOutlineArea;

    public static void init() {
        addOutlineArea = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockOutline.addSelection",
                InputUtil.Type.KEYSYM,
                GLFW_KEY_RIGHT_ALT,
                "category.blockOutline"
        ));
        deleteOutlineArea = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockOutline.removeSelection",
                InputUtil.Type.KEYSYM,
                GLFW_KEY_RIGHT_CONTROL,
                "category.blockOutline"
        ));
    }
}
