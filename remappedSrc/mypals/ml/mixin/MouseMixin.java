package mypals.ml.mixin;

import mypals.ml.config.GlowMyBlocksConfig;
import mypals.ml.wandSystem.WandActionsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mypals.ml.wandSystem.SelectedManager.wand;


@Mixin(MouseHandler.class)
public class MouseMixin {
    private final Minecraft client = Minecraft.getInstance();

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void injectOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {

        if (window == this.client.getWindow().getWindow()) {
            if (this.client.player != null) {
                ItemStack mainHand = this.client.player.getMainHandItem();
                if (((mainHand.is(wand) || (this.client.player.isSpectator() && GlowMyBlocksConfig.selectInSpectator))
                       && Minecraft.getInstance().options.keySprint.isDown()))
                {
                    double sensitivity = this.client.options.mouseWheelSensitivity().get();
                    double scrollAmount = (this.client.options.discreteMouseScroll().get() ?
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