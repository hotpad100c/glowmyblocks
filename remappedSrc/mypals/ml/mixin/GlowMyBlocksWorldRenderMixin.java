package mypals.ml.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import mypals.ml.blockOutline.OutlineManager;
import mypals.ml.renderings.GlowMyBlocksInformationRender;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mypals.ml.GlowMyBlocks.renderBlockEntitiesOutlines;
import static mypals.ml.GlowMyBlocks.renderBlockOutlines;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

@Mixin(LevelRenderer.class)
public class GlowMyBlocksWorldRenderMixin {
	@Shadow @Nullable public PostChain entityOutlinePostProcessor;

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
						@Local PoseStack matrixStack,
						@Local(argsOnly = true) DeltaTracker tickCounter,
									 @Local(ordinal = 0, argsOnly = true) Matrix4f matrix4f2
	) {
		GlowMyBlocksInformationRender.render(matrixStack,tickCounter);
	}
	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
	private void blockOutline$draw(
			DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
		renderBlockOutlines(new PoseStack(), Minecraft.getInstance().getDeltaTracker(), new Matrix4f());
	}
}