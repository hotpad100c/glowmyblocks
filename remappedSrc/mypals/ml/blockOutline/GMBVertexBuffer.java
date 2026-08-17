package mypals.ml.blockOutline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class GMBVertexBuffer implements AutoCloseable {

    // 1.21.6 用 GpuBuffer 替代旧的 OpenGL VAO/VBO/IBO
    @Nullable
    private GpuBuffer vertexBuffer;
    @Nullable
    private GpuBuffer indexBuffer;

    private int indexCount = 0;
    private boolean usesSharedIndexBuffer = false;
    private VertexFormat.Mode drawMode;

    // -------------------------------------------------------------------------
    // 上传顶点数据
    // -------------------------------------------------------------------------

    /**
     * 将 BuiltBuffer 的内容上传到 GPU，替换之前的数据。
     * 调用后 builtBuffer 会被自动关闭。
     */
    public void upload(MeshData builtBuffer) {
        // 先释放旧资源
        close();

        RenderSystem.assertOnRenderThread();

        try {
            MeshData.DrawState params = builtBuffer.drawState();
            this.drawMode   = params.mode();
            this.indexCount = params.indexCount();

            var gpuDevice = RenderSystem.getDevice();

            // ------------------------------------------------------------------
            // 上传顶点缓冲
            // flag 40 对应 BufferType.VERTICES（参见 BufferedVertexBuilder）
            // ------------------------------------------------------------------
            var rawVertex = builtBuffer.vertexBuffer();
            if (rawVertex != null) {
                this.vertexBuffer = gpuDevice.createBuffer(
                        () -> "GMBVertexBuffer/vertex",
                        40,
                        rawVertex
                );
            }

            // ------------------------------------------------------------------
            // 上传索引缓冲
            // flag 72 对应 BufferType.INDICES
            // 若 BuiltBuffer 没有自定义索引，则借用 AutoStorageIndexBuffer（共享）
            // ------------------------------------------------------------------
            var rawIndex = builtBuffer.indexBuffer();
            if (rawIndex != null) {
                this.indexBuffer = gpuDevice.createBuffer(
                        () -> "GMBVertexBuffer/index",
                        72,
                        rawIndex
                );
                this.usesSharedIndexBuffer = false;
            } else {
                // 共享的顺序索引缓冲，不需要我们自己创建，也不需要我们关闭
                RenderSystem.AutoStorageIndexBuffer autoIndex =
                        RenderSystem.getSequentialBuffer(params.mode());
                this.indexBuffer = autoIndex.getBuffer(params.indexCount());
                this.usesSharedIndexBuffer = true;
            }

        } finally {
            builtBuffer.close();
        }
    }

    // -------------------------------------------------------------------------
    // 绘制
    // -------------------------------------------------------------------------

    /**
     * 使用指定 RenderLayer（即 RenderType）的 pipeline 绘制缓冲区内容。
     *
     * @param viewMatrix  ModelView 矩阵（已含相机平移）
     * @param renderLayer 提供渲染管线、输出目标等状态
     */
    public void draw(Matrix4f viewMatrix, RenderType renderLayer) {
        if (isClosed()) return;

        RenderSystem.assertOnRenderThread();

        // ------------------------------------------------------------------
        // 写入动态 Uniform（变换矩阵、颜色等）
        // 与 BufferedVertexBuilder 保持一致
        // ------------------------------------------------------------------
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                viewMatrix,
                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(),
                RenderSystem.getTextureMatrix(),
                RenderSystem.getShaderLineWidth()
        );

        // ------------------------------------------------------------------
        // 从 RenderLayer.state 取出输出目标与渲染管线
        // ------------------------------------------------------------------
        var state = renderLayer.state;
        RenderTarget renderTarget = state.outputState.getRenderTarget();

        var colorView = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride
                : renderTarget.getColorTextureView();

        var depthView = renderTarget.useDepth
                ? (RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride
                : renderTarget.getDepthTextureView())
                : null;

        RenderSystem.AutoStorageIndexBuffer autoIndex =
                RenderSystem.getSequentialBuffer(drawMode);

        // ------------------------------------------------------------------
        // 创建 RenderPass 并提交绘制指令
        // ------------------------------------------------------------------
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "RenderPass_GMBVertexBuffer/" + renderLayer,
                        colorView,
                        OptionalInt.empty(),
                        depthView,
                        OptionalDouble.empty()
                )) {

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setPipeline(state.renderPipeline);

            renderPass.setVertexBuffer(0, vertexBuffer);

            // Scissor
            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                renderPass.enableScissor(
                        scissorState.x(), scissorState.y(),
                        scissorState.width(), scissorState.height()
                );
            }

            // 索引缓冲：共享时用 autoIndex 实时取，自定义时用自己的
            GpuBuffer ib = usesSharedIndexBuffer
                    ? autoIndex.getBuffer(indexCount)
                    : indexBuffer;
            renderPass.setIndexBuffer(ib, autoIndex.type());
            renderPass.drawIndexed(0, 0, indexCount, 1);
        }
    }

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    @Override
    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        // 共享的 indexBuffer 由 RenderSystem 管理，不能由我们关闭
        if (!usesSharedIndexBuffer && indexBuffer != null) {
            indexBuffer.close();
        }
        indexBuffer = null;
        indexCount  = 0;
    }

    public boolean isClosed() {
        return vertexBuffer == null;
    }

    public VertexFormat.Mode getDrawMode() {
        return drawMode;
    }
}