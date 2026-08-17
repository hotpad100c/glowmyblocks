package mypals.ml.blockOutline;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.math.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static mypals.ml.blockOutline.ChunkDataBuilder.buildMeshesAsync;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class OutlineManager {

    public static Map<BlockPos, Color> blockToRenderer = new HashMap<>();
    public static ArrayList<BlockPos> targetedBlocks = new ArrayList<>();

    public static Map<AreaBox, AreaRenderData> areaVbos = new ConcurrentHashMap<>();

    public static class AreaRenderData {
        public Map<SectionPos, ChunkRenderData> sectionData = new HashMap<>();
    }
    record ChunkBufferData(
            MeshData buffer,
            Map<BlockPos, Color> blockEntities
    ) {}
    public static class ChunkRenderData {
        public GMBVertexBuffer vbo;
        public Map<BlockPos, Color> blockEntities = new HashMap<>();
    }


    public static void buildMeshes(DeltaTracker counter) {

        buildMeshesAsync(counter);
    }

    public static void renderBlocks(PoseStack stack, DeltaTracker counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        GlStateManager._disableDepthTest();

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                if (chunk.vbo != null) renderAreaVbo(chunk.vbo, cameraPos);
            }
        }

        OutlineBufferSource consumer = mc.levelRenderer.renderBuffers.outlineBufferSource();
        RandomSource random = mc.getCameraEntity().getRandom();

        //we need this to keep the outline color bright idk why :((((
        PoseStack tempStack = new PoseStack();
        tempStack.translate(0, 0, 0);
        tempStack.scale(0.0f, 0.0f, 0.0f);
        Minecraft.getInstance().getBlockRenderer().renderBatched(Blocks.STONE.defaultBlockState(),
                new BlockPos(0, 0, 0), Minecraft.getInstance().level,tempStack,
                consumer.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)),true,random );
    }

    public static void renderBlockEntities(PoseStack stack, DeltaTracker counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                if (!chunk.blockEntities.isEmpty()) {
                    renderAreaBlockEntities(stack, chunk.blockEntities, counter, cameraPos);
                }
            }
        }
    }

    private static void renderAreaVbo(GMBVertexBuffer vbo, Vec3 cameraPos) {
        vbo.bind();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().translate(
                (float) -cameraPos.x,
                (float) -cameraPos.y,
                (float) -cameraPos.z
        );
        RenderSystem.applyModelViewMatrix();

        Minecraft.getInstance().levelRenderer.entityOutlineTarget().beginWrite(false);

        vbo.draw(RenderSystem.getModelViewStack(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        GMBVertexBuffer.unbind();

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void renderAreaBlockEntities(PoseStack stack, Map<BlockPos, Color> blockEntities,
                                                DeltaTracker counter, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        BlockEntityRenderDispatcher blockEntityRenderer = mc.getBlockEntityRenderDispatcher();
        OutlineBufferSource outlineProvider = mc.levelRenderer.renderBuffers.outlineBufferSource();
        float delta = counter.getGameTimeDeltaTicks();

        for (Map.Entry<BlockPos, Color> entry : blockEntities.entrySet()) {
            BlockPos blockPos = entry.getKey();
            Color color = entry.getValue();

            double x1 = blockPos.getX() - cameraPos.x;
            double y1 = blockPos.getY() - cameraPos.y;
            double z1 = blockPos.getZ() - cameraPos.z;

            stack.pushPose();
            stack.translate(x1, y1, z1);

            outlineProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 1);
            BlockEntity blockEntity = mc.level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntityRenderer.render(blockEntity, delta, stack, outlineProvider);
            }
            stack.popPose();
        }
    }

    public static boolean isBlockInsideArea(BlockPos pos, AreaBox area) {
        return pos.getX() >= area.minPos.getX() && pos.getX() <= area.maxPos.getX()
                && pos.getY() >= area.minPos.getY() && pos.getY() <= area.maxPos.getY()
                && pos.getZ() >= area.minPos.getZ() && pos.getZ() <= area.maxPos.getZ();
    }

    static void renderBlockOutline(Map.Entry<BlockPos, BlockState> entry, float delta, Camera camera,
                                   PoseStack matrixStack, Color color, RandomSource random,BufferBuilder bufferBuilder) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BlockPos blockPos = entry.getKey();
        BlockState blockState = entry.getValue();

        matrixStack.pushPose();

        if (!blockState.getFluidState().isEmpty()) {
            CustomFluidOutlineRenderer.render(mc.level, blockPos, bufferBuilder,
                    blockState, blockState.getFluidState(), matrixStack,
                    color.getRed(), color.getGreen(), color.getBlue());
        }

        if (blockState.getRenderShape() == RenderShape.MODEL) {
            CustomBlockOutlineRenderer.render(mc.level,
                    dispatcher.getBlockModel(blockState), blockState, blockPos, matrixStack,
                    bufferBuilder, random,
                    blockState.getSeed(blockPos),
                    OverlayTexture.NO_OVERLAY, color.getRed(), color.getGreen(), color.getBlue());
        }
        matrixStack.popPose();
    }

    public static void resolveBlocks() {
        blockToRenderer.clear();
        targetedBlocks.clear();
    }
}
