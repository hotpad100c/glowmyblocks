package mypals.ml.blockOutline;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static mypals.ml.blockOutline.ChunkDataBuilder.buildMeshesAsync;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;
import static net.minecraft.client.render.RenderPhase.*;

public class OutlineManager {

    public static Map<BlockPos, Color> blockToRenderer = new HashMap<>();
    public static ArrayList<BlockPos> targetedBlocks = new ArrayList<>();

    public static Map<AreaBox, AreaRenderData> areaVbos = new ConcurrentHashMap<>();

    public static class AreaRenderData {
        public Map<ChunkSectionPos, ChunkRenderData> sectionData = new HashMap<>();
    }

    public static class ChunkRenderData {
        public VertexBuffer vbo;
        public Map<BlockPos, Color> blockEntities = new HashMap<>();
    }

    private static final RenderPhase.Transparency STO = new RenderPhase.Transparency(
            "sto",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                        GlStateManager.SrcFactor.SRC_ALPHA,
                        GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
                );
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    private static final RenderLayer GLOWING_OUTLINE_RENDER = RenderLayer.of(
            "block_glow_outline",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            false, false,
            RenderLayer.MultiPhaseParameters.builder()
                    .transparency(STO)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(DISABLE_CULLING)
                    .program(COLOR_PROGRAM)
                    .lightmap(DISABLE_LIGHTMAP)
                    .writeMaskState(ALL_MASK)
                    .texture(BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static void buildMeshes(RenderTickCounter counter) {

        buildMeshesAsync(counter);
    }

    public static void renderBlocks(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        RenderSystem.disableDepthTest();

        RenderSystem.setShader(GameRenderer::getRenderTypeOutlineProgram);
        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                if (chunk.vbo != null) renderAreaVbo(chunk.vbo, cameraPos);
            }
        }

        OutlineVertexConsumerProvider consumer = mc.worldRenderer.bufferBuilders.getOutlineVertexConsumers();
        Random random = mc.getCameraEntity().getRandom();

        //we need this to keep the outline color bright idk why :((((
        MatrixStack tempStack = new MatrixStack();
        tempStack.translate(0, 0, 0);
        tempStack.scale(0.0f, 0.0f, 0.0f);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(Blocks.STONE.getDefaultState(),
                new BlockPos(0, 0, 0), (BlockRenderView) MinecraftClient.getInstance().world,tempStack,
                consumer.getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),true,random );
    }

    public static void renderBlockEntities(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                if (!chunk.blockEntities.isEmpty()) {
                    renderAreaBlockEntities(stack, chunk.blockEntities, counter, cameraPos);
                }
            }
        }
    }

    private static void renderAreaVbo(VertexBuffer vbo, Vec3d cameraPos) {
        vbo.bind();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().translate(
                (float) -cameraPos.x,
                (float) -cameraPos.y,
                (float) -cameraPos.z
        );
        RenderSystem.applyModelViewMatrix();

        MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer().beginWrite(false);

        vbo.draw(RenderSystem.getModelViewStack(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        VertexBuffer.unbind();

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void renderAreaBlockEntities(MatrixStack stack, Map<BlockPos, Color> blockEntities,
                                                RenderTickCounter counter, Vec3d cameraPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockEntityRenderDispatcher blockEntityRenderer = mc.getBlockEntityRenderDispatcher();
        OutlineVertexConsumerProvider outlineProvider = mc.worldRenderer.bufferBuilders.getOutlineVertexConsumers();
        float delta = counter.getTickDelta(false);

        for (Map.Entry<BlockPos, Color> entry : blockEntities.entrySet()) {
            BlockPos blockPos = entry.getKey();
            Color color = entry.getValue();

            double x1 = blockPos.getX() - cameraPos.x;
            double y1 = blockPos.getY() - cameraPos.y;
            double z1 = blockPos.getZ() - cameraPos.z;

            stack.push();
            stack.translate(x1, y1, z1);

            outlineProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 1);
            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntityRenderer.render(blockEntity, delta, stack, outlineProvider);
            }
            stack.pop();
        }
    }

    public static boolean isBlockInsideArea(BlockPos pos, AreaBox area) {
        return pos.getX() >= area.minPos.getX() && pos.getX() <= area.maxPos.getX()
                && pos.getY() >= area.minPos.getY() && pos.getY() <= area.maxPos.getY()
                && pos.getZ() >= area.minPos.getZ() && pos.getZ() <= area.maxPos.getZ();
    }

    static void renderBlockOutline(Map.Entry<BlockPos, BlockState> entry, float delta, Camera camera,
                                   MatrixStack matrixStack, Color color, Random random,BufferBuilder bufferBuilder) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager dispatcher = mc.getBlockRenderManager();
        BlockPos blockPos = entry.getKey();
        BlockState blockState = entry.getValue();

        matrixStack.push();

        if (!blockState.getFluidState().isEmpty()) {
            CustomFluidOutlineRenderer.render(mc.world, blockPos, bufferBuilder,
                    blockState, blockState.getFluidState(), matrixStack,
                    color.getRed(), color.getGreen(), color.getBlue());
        }

        if (blockState.getRenderType() == BlockRenderType.MODEL) {
            CustomBlockOutlineRenderer.render((BlockRenderView) mc.world,
                    dispatcher.getModel(blockState), blockState, blockPos, matrixStack,
                    bufferBuilder, random,
                    blockState.getRenderingSeed(blockPos),
                    OverlayTexture.DEFAULT_UV, color.getRed(), color.getGreen(), color.getBlue());
        }
        matrixStack.pop();
    }

    public static void resolveBlocks() {
        blockToRenderer.clear();
        targetedBlocks.clear();
    }
}
