package mypals.ml.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import mypals.ml.blockOutline.OutlineManager;
import mypals.ml.blockOutline.OutlineSubmitCollector;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes the block entities inside a selected area glow.
 *
 * <p>Entities carry their glow colour on their render state, but {@code BlockEntityRenderState}
 * has no such field, so there is nothing to set ahead of time. Instead we intercept the submit
 * call and swap in a collector that stamps our colour onto every submission the block entity's
 * renderer makes. The block entity is still submitted exactly once, so it renders normally and
 * additionally lands in the outline target.
 */
@Mixin(LevelRenderer.class)
public class GlowMyBlocksBlockEntityOutlineMixin {

	@WrapOperation(
			method = "submitBlockEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;"
							+ "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"
							+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
							+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
							+ "Lnet/minecraft/client/renderer/state/CameraRenderState;)V"
			)
	)
	private void blockOutline$glowBlockEntities(BlockEntityRenderDispatcher dispatcher,
												BlockEntityRenderState state,
												PoseStack poseStack,
												SubmitNodeCollector collector,
												CameraRenderState cameraRenderState,
												Operation<Void> original) {
		int outlineColor = OutlineManager.outlineColorFor(state.blockPos);
		if (outlineColor != OutlineManager.NO_OUTLINE) {
			collector = new OutlineSubmitCollector(collector, outlineColor);
		}
		original.call(dispatcher, state, poseStack, collector, cameraRenderState);
	}
}
