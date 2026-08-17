package mypals.ml.blockOutline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * A persistent GPU buffer for a chunk section's outline mesh.
 *
 * <p>1.21.6+ removed the old VAO/VBO {@code VertexBuffer}; geometry is now recorded into a
 * {@link RenderPass}. Vanilla's {@link RenderType#draw(MeshData)} re-uploads the mesh on every
 * frame, which is wasteful for outlines that only change when the world does, so this class
 * mirrors that method exactly but keeps the vertex/index buffers alive across frames.
 *
 * <p>Keeping it a faithful mirror matters: the pipeline, output target, texture bindings and
 * uniforms all have to agree with what the {@link RenderType} declares, or the draw silently
 * produces nothing.
 */
@Environment(EnvType.CLIENT)
public class GMBVertexBuffer implements AutoCloseable {

    // Same usage flags vanilla's VertexFormat#uploadImmediateVertexBuffer / #uploadImmediateIndexBuffer use.
    private static final int VERTEX_USAGE = GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST;
    private static final int INDEX_USAGE = GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST;

    @Nullable
    private GpuBuffer vertexBuffer;
    /** Null when the mesh uses the shared sequential index buffer, which we must not own or close. */
    @Nullable
    private GpuBuffer indexBuffer;

    private int indexCount;
    private VertexFormat.Mode drawMode = VertexFormat.Mode.QUADS;
    private VertexFormat.IndexType indexType = VertexFormat.IndexType.SHORT;

    /**
     * Uploads {@code mesh} to the GPU, replacing any previous contents, and closes it.
     *
     * <p>The mesh must have been built with {@code renderType.pipeline().getVertexFormat()};
     * a mismatch here draws nothing at all rather than failing loudly.
     */
    public void upload(MeshData mesh) {
        RenderSystem.assertOnRenderThread();
        close();

        try {
            MeshData.DrawState drawState = mesh.drawState();
            this.drawMode = drawState.mode();
            this.indexCount = drawState.indexCount();

            var device = RenderSystem.getDevice();

            ByteBuffer vertices = mesh.vertexBuffer();
            if (vertices != null) {
                this.vertexBuffer = device.createBuffer(() -> "GMBVertexBuffer/vertex", VERTEX_USAGE, vertices);
            }

            ByteBuffer indices = mesh.indexBuffer();
            if (indices != null) {
                // The mesh carries its own (e.g. sorted) indices, so they have to be kept too.
                this.indexBuffer = device.createBuffer(() -> "GMBVertexBuffer/index", INDEX_USAGE, indices);
                this.indexType = drawState.indexType();
            } else {
                // Plain sequential indices: reuse RenderSystem's shared buffer at draw time.
                this.indexBuffer = null;
                this.indexType = RenderSystem.getSequentialBuffer(this.drawMode).type();
            }
        } finally {
            mesh.close();
        }
    }

    /**
     * Draws the buffered mesh with {@code renderType}, in the caller's current model-view state.
     *
     * <p>This is {@link RenderType#draw(MeshData)} with the upload step hoisted out.
     */
    public void draw(RenderType renderType) {
        if (isClosed()) return;

        RenderSystem.assertOnRenderThread();

        RenderSetup setup = renderType.state;
        RenderPipeline pipeline = renderType.pipeline();

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        // Most layers have no layering transform at all, in which case getModifier() is null and
        // vanilla skips the push/pop entirely -- so the pop below is guarded on the same value.
        Consumer<Matrix4fStack> layering = setup.layeringTransform.getModifier();
        if (layering != null) {
            modelViewStack.pushMatrix();
            layering.accept(modelViewStack);
        }

        try {
            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                    new Vector3f(),
                    setup.textureTransform.getMatrix()
            );

            RenderTarget renderTarget = setup.outputTarget.getRenderTarget();

            GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride
                    : renderTarget.getColorTextureView();

            GpuTextureView depthView = null;
            if (renderTarget.useDepth) {
                depthView = RenderSystem.outputDepthTextureOverride != null
                        ? RenderSystem.outputDepthTextureOverride
                        : renderTarget.getDepthTextureView();
            }

            GpuBuffer indices = this.indexBuffer != null
                    ? this.indexBuffer
                    : RenderSystem.getSequentialBuffer(this.drawMode).getBuffer(this.indexCount);

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "GMBVertexBuffer/" + renderType,
                    colorView,
                    OptionalInt.empty(),
                    depthView,
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(pipeline);

                ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissor.enabled()) {
                    pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setVertexBuffer(0, this.vertexBuffer);

                for (Map.Entry<String, RenderSetup.TextureAndSampler> texture : setup.getTextures().entrySet()) {
                    pass.bindTexture(texture.getKey(), texture.getValue().textureView(), texture.getValue().sampler());
                }

                pass.setIndexBuffer(indices, this.indexType);
                pass.drawIndexed(0, 0, this.indexCount, 1);
            }
        } finally {
            if (layering != null) {
                modelViewStack.popMatrix();
            }
        }
    }

    @Override
    public void close() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
        // A null indexBuffer means we were borrowing RenderSystem's shared sequential buffer.
        if (this.indexBuffer != null) {
            this.indexBuffer.close();
            this.indexBuffer = null;
        }
        this.indexCount = 0;
    }

    public boolean isClosed() {
        return this.vertexBuffer == null || this.indexCount == 0;
    }

    public VertexFormat.Mode getDrawMode() {
        return this.drawMode;
    }
}
