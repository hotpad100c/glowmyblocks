package mypals.ml.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import mypals.ml.blockOutline.OutlineManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mypals.ml.GlowMyBlocks.renderBlockOutlines;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

@Mixin(LevelRenderer.class)
public class GlowMyBlocksWorldRenderMixin {

	/**
	 * Keeps the entity-outline pass alive while we have outlines to draw.
	 *
	 * <p>Up to 1.21.6 this was a {@code @ModifyVariable} on a local boolean inside renderLevel,
	 * pinned by {@code ordinal}. 1.21.9 moved the flag onto {@link LevelRenderState}, which is both
	 * a stabler target and the actual gate: renderLevel and addMainPass read
	 * {@code haveGlowingEntities} to decide whether to allocate the outline target and run the
	 * post chain, and extractVisibleEntities is the last thing to write it.
	 */
	@Inject(method = "extractVisibleEntities", at = @At("TAIL"))
	private void blockOutline$forceOutline(Camera camera, Frustum frustum, DeltaTracker deltaTracker,
										   LevelRenderState levelRenderState, CallbackInfo ci) {
		if (!OutlineManager.targetedBlocks.isEmpty() || !selectedAreas.isEmpty()) {
			levelRenderState.haveGlowingEntities = true;
		}
	}

	/**
	 * Draws the outline meshes inside renderLevel's entity/outline pass lambda, right where vanilla
	 * finishes its own outline batch.
	 *
	 * <p>method_62214 keeps that intermediary name on 1.21.11, but its parameters changed: the
	 * DeltaTracker/Camera/Frustum triple was folded into a single LevelRenderState.
	 */
	@Inject(method = "method_62214", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
	private void blockOutline$draw(
			GpuBufferSlice gpuBufferSlice, LevelRenderState levelRenderState, ProfilerFiller profilerFiller,
			Matrix4f matrix4f, ResourceHandle resourceHandle, ResourceHandle resourceHandle2, boolean bl,
			ResourceHandle resourceHandle3, ResourceHandle resourceHandle4, CallbackInfo ci) {
		renderBlockOutlines(new PoseStack(), Minecraft.getInstance().getDeltaTracker(), new Matrix4f());
	}
}
