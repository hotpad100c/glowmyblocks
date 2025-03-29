package mypals.ml.config;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import static org.lwjgl.glfw.GLFW.*;

public class Keybinds {
    public static KeyBinding addArea;
    public static KeyBinding deleteArea;

    public static void init() {

        addArea = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockOutline.addSelection",
                InputUtil.Type.KEYSYM,
                GLFW_KEY_EQUAL,
                "category.blockOutline"
        ));
        deleteArea = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockOutline.removeSelection",
                InputUtil.Type.KEYSYM,
                GLFW_KEY_MINUS,
                "category.blockOutline"
        ));
    }
}
