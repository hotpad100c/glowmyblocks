package mypals.ml.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mypals.ml.blockOutline.OutlineManager;
import mypals.ml.renderings.GlowMyBlocksInformationRender;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

@Mixin(WorldRenderer.class)
public class GlowMyBlocksWorldRenderMixin {
	@SuppressWarnings({"InvalidInjectorMethodSignature", "MixinAnnotationTarget"})
	@ModifyVariable(
			method = "render",
			at = @At(
					value = "LOAD",
					ordinal = 0
			),
			ordinal = 3
	)
	private boolean blockOutline$forceOutline(boolean bl3) {
		return bl3 || !OutlineManager.targetedBlocks.isEmpty() || !selectedAreas.isEmpty();
	}
	@Inject(method = "render", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/Camera;)V", ordinal = 0))
	private void blockOutline$render(CallbackInfo ci,
						@Local MatrixStack matrixStack,
						@Local(argsOnly = true) RenderTickCounter tickCounter
	) {
		GlowMyBlocksInformationRender.render(matrixStack,tickCounter);
	}
}