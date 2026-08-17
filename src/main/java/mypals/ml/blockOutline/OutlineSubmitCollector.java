package mypals.ml.blockOutline;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

/**
 * A {@link SubmitNodeCollector} that forwards everything to a delegate but forces the outline
 * colour on every submission that carries one.
 *
 * <p>This is how block entities glow on 1.21.9+. Entities get their glow from
 * {@code EntityRenderState#outlineColor}, but {@code BlockEntityRenderState} has no equivalent
 * field, so there is nothing to set. Instead we wrap the collector handed to
 * {@code BlockEntityRenderDispatcher#submit} and overwrite the outline colour as the renderer
 * submits its geometry -- the block entity is still submitted exactly once, so it draws normally
 * and additionally feeds the outline target.
 */
@Environment(EnvType.CLIENT)
public class OutlineSubmitCollector implements SubmitNodeCollector {

    private final SubmitNodeCollector delegate;
    private final int outlineColor;

    public OutlineSubmitCollector(SubmitNodeCollector delegate, int outlineColor) {
        this.delegate = delegate;
        this.outlineColor = outlineColor;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        // The ordered view has to keep forcing the colour too, otherwise anything submitted through
        // collector.order(n) would come out without a glow.
        return new Ordered(this.delegate.order(order));
    }

    @Override
    public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> pieces) {
        this.delegate.submitShadow(poseStack, f, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, Vec3 vec3, int i, Component component, boolean bl,
                              int j, double d, CameraRenderState cameraRenderState) {
        this.delegate.submitNameTag(poseStack, vec3, i, component, bl, j, d, cameraRenderState);
    }

    @Override
    public void submitText(PoseStack poseStack, float f, float g, FormattedCharSequence text, boolean bl,
                           Font.DisplayMode displayMode, int i, int j, int k, int l) {
        this.delegate.submitText(poseStack, f, g, text, bl, displayMode, i, j, k, l);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
        this.delegate.submitFlame(poseStack, entityRenderState, quaternionf);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.delegate.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite,
                                int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        this.delegate.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor,
                sprite, this.outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                                int overlayCoords, TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil,
                                int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                int outlineColor) {
        this.delegate.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite,
                sheeted, hasFoil, tintedColor, crumblingOverlay, this.outlineColor);
    }

    @Override
    public void submitBlock(PoseStack poseStack, BlockState blockState, int lightCoords, int overlayCoords,
                            int outlineColor) {
        this.delegate.submitBlock(poseStack, blockState, lightCoords, overlayCoords, this.outlineColor);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        this.delegate.submitMovingBlock(poseStack, movingBlockRenderState);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model,
                                 float r, float g, float b, int lightCoords, int overlayCoords, int outlineColor) {
        this.delegate.submitBlockModel(poseStack, renderType, model, r, g, b, lightCoords, overlayCoords,
                this.outlineColor);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords,
                           int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads,
                           RenderType renderType, ItemStackRenderState.FoilType foilType) {
        this.delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, this.outlineColor,
                tintLayers, quads, renderType, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                     SubmitNodeCollector.CustomGeometryRenderer renderer) {
        this.delegate.submitCustomGeometry(poseStack, renderType, renderer);
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
        this.delegate.submitParticleGroup(renderer);
    }

    /** Same forcing, for the view returned by {@link #order(int)}. */
    private class Ordered implements OrderedSubmitNodeCollector {

        private final OrderedSubmitNodeCollector inner;

        private Ordered(OrderedSubmitNodeCollector inner) {
            this.inner = inner;
        }

        @Override
        public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> pieces) {
            this.inner.submitShadow(poseStack, f, pieces);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, Vec3 vec3, int i, Component component, boolean bl,
                                  int j, double d, CameraRenderState cameraRenderState) {
            this.inner.submitNameTag(poseStack, vec3, i, component, bl, j, d, cameraRenderState);
        }

        @Override
        public void submitText(PoseStack poseStack, float f, float g, FormattedCharSequence text, boolean bl,
                               Font.DisplayMode displayMode, int i, int j, int k, int l) {
            this.inner.submitText(poseStack, f, g, text, bl, displayMode, i, j, k, l);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
            this.inner.submitFlame(poseStack, entityRenderState, quaternionf);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            this.inner.submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                    int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite,
                                    int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
            this.inner.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor,
                    sprite, OutlineSubmitCollector.this.outlineColor, crumblingOverlay);
        }

        @Override
        public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                                    int overlayCoords, TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil,
                                    int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                    int outlineColor) {
            this.inner.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite,
                    sheeted, hasFoil, tintedColor, crumblingOverlay, OutlineSubmitCollector.this.outlineColor);
        }

        @Override
        public void submitBlock(PoseStack poseStack, BlockState blockState, int lightCoords, int overlayCoords,
                                int outlineColor) {
            this.inner.submitBlock(poseStack, blockState, lightCoords, overlayCoords,
                    OutlineSubmitCollector.this.outlineColor);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
            this.inner.submitMovingBlock(poseStack, movingBlockRenderState);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model,
                                     float r, float g, float b, int lightCoords, int overlayCoords, int outlineColor) {
            this.inner.submitBlockModel(poseStack, renderType, model, r, g, b, lightCoords, overlayCoords,
                    OutlineSubmitCollector.this.outlineColor);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords,
                               int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads,
                               RenderType renderType, ItemStackRenderState.FoilType foilType) {
            this.inner.submitItem(poseStack, displayContext, lightCoords, overlayCoords,
                    OutlineSubmitCollector.this.outlineColor, tintLayers, quads, renderType, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                         SubmitNodeCollector.CustomGeometryRenderer renderer) {
            this.inner.submitCustomGeometry(poseStack, renderType, renderer);
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
            this.inner.submitParticleGroup(renderer);
        }
    }
}
