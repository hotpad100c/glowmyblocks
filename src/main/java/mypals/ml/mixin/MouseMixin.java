package mypals.ml.mixin;

import mypals.ml.config.GlowMyBlocksConfig;
import mypals.ml.wandSystem.WandActionsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mypals.ml.wandSystem.SelectedManager.wand;


@Mixin(Mouse.class)
public class MouseMixin {
    private final MinecraftClient client = MinecraftClient.getInstance();

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void injectOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {

        if (window == this.client.getWindow().getHandle()) {
            if (this.client.player != null) {
                ItemStack mainHand = this.client.player.getMainHandStack();
                if (((mainHand.isOf(wand) || (this.client.player.isSpectator() && GlowMyBlocksConfig.selectInSpectator))
                       && MinecraftClient.getInstance().options.sprintKey.isPressed()))
                {
                    double sensitivity = this.client.options.getMouseWheelSensitivity().getValue();
                    double scrollAmount = (this.client.options.getDiscreteMouseScroll().getValue() ?
                            Math.signum(vertical) : vertical) * sensitivity;

                    if (scrollAmount > 0) {
                        WandActionsManager.switchRenderMod(true);
                    } else if (scrollAmount < 0) {
                        WandActionsManager.switchRenderMod(false);
                    }
                    ci.cancel();
                }
            }
        }
    }

}