package mypals.ml.blockOutline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mypals.ml.GlowMyBlocks;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
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
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static mypals.ml.config.GlowModeManager.shouldGlow;
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
            VertexFormats.POSITION_COLOR,
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
                    .texture(NO_TEXTURE)
                    .build(true)
    );

    public static void buildMeshes(RenderTickCounter counter) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        for (AreaRenderData data : areaVbos.values()) {
            for (ChunkRenderData chunk : data.sectionData.values()) {
                if (chunk.vbo != null) chunk.vbo.close();
            }
        }
        areaVbos.clear();

        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        float delta = counter.getTickDelta(false);

        for (AreaBox area : selectedAreas) {
            AreaRenderData renderData = new AreaRenderData();

            for (int x = area.minPos.getX(); x <= area.maxPos.getX(); x++) {
                for (int y = area.minPos.getY(); y <= area.maxPos.getY(); y++) {
                    for (int z = area.minPos.getZ(); z <= area.maxPos.getZ(); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(blockPos);
                        if (state.isAir()) continue;

                        ChunkSectionPos sectionPos = ChunkSectionPos.from(blockPos);
                        ChunkRenderData chunkData = renderData.sectionData.computeIfAbsent(sectionPos, k -> new ChunkRenderData());

                        if (chunkData.vbo == null) {
                            chunkData.vbo = buildMeshForChunk(area, sectionPos, delta);
                        }

                        if (state.getBlock() instanceof BlockWithEntity) {
                            BlockEntity be = world.getBlockEntity(blockPos);
                            if (shouldGlow(blockPos,world.getBlockState(blockPos),area) && be != null) {
                                chunkData.blockEntities.put(be.getPos(), area.color);
                            }
                        }
                    }
                }
            }

            areaVbos.put(area, renderData);
        }

        GlowMyBlocks.needRebuildOutlineMesh = false;
    }

    private static VertexBuffer buildMeshForChunk(AreaBox area, ChunkSectionPos sectionPos, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        World world = mc.world;
        if (world == null) return null;

        BufferBuilder buffer = Tessellator.getInstance().begin(
                GLOWING_OUTLINE_RENDER.getDrawMode(),
                GLOWING_OUTLINE_RENDER.getVertexFormat()
        );

        int startX = sectionPos.getMinX();
        int endX = sectionPos.getMaxX();
        int startY = sectionPos.getMinY();
        int endY = sectionPos.getMaxY();
        int startZ = sectionPos.getMinZ();
        int endZ = sectionPos.getMaxZ();

        for (int x = startX; x <= endX && x <= area.maxPos.getX(); x++) {
            for (int y = startY; y <= endY && y <= area.maxPos.getY(); y++) {
                for (int z = startZ; z <= endZ && z <= area.maxPos.getZ(); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    if (state.isAir() || !shouldGlow(blockPos,state,area)) continue;

                    for (Direction direction : Direction.values()) {
                        BlockPos offsetPos = blockPos.offset(direction);
                        boolean isSideBlocked =false;
                        if(isBlockInsideArea(offsetPos,area)){
                            isSideBlocked = world.getBlockState(offsetPos).isFullCube(world, offsetPos);
                        }
                        if (!isSideBlocked) {
                            MatrixStack stack = new MatrixStack();
                            stack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                            renderBlockOutline(new AbstractMap.SimpleEntry<>(blockPos, state),
                                    delta, camera, stack, area.color, buffer);
                            break;
                        }
                    }
                }
            }
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer == null) return null;

        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        vbo.bind();
        vbo.upload(builtBuffer);
        VertexBuffer.unbind();
        return vbo;
    }

    public static void onBlockStateChange(BlockPos blockPos) {
        for (AreaBox area : selectedAreas) {
            if (isBlockInsideArea(blockPos,area)) {
                ChunkSectionPos sectionPos = ChunkSectionPos.from(blockPos);
                rebuildChunkSection(area, sectionPos);
            }
        }
    }

    private static void rebuildChunkSection(AreaBox area, ChunkSectionPos sectionPos) {
        AreaRenderData areaData = areaVbos.get(area);
        if (areaData == null) return;

        ChunkRenderData old = areaData.sectionData.get(sectionPos);
        if (old != null && old.vbo != null) old.vbo.close();

        ChunkRenderData newData = new ChunkRenderData();
        newData.vbo = buildMeshForChunk(area, sectionPos, 0);
        newData.blockEntities = collectBlockEntitiesForChunk(sectionPos, area);

        areaData.sectionData.put(sectionPos, newData);
    }

    private static Map<BlockPos, Color> collectBlockEntitiesForChunk(ChunkSectionPos sectionPos, AreaBox area) {
        Map<BlockPos, Color> blockEntities = new HashMap<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return blockEntities;

        int startX = sectionPos.getMinX();
        int endX = sectionPos.getMaxX();
        int startY = sectionPos.getMinY();
        int endY = sectionPos.getMaxY();
        int startZ = sectionPos.getMinZ();
        int endZ = sectionPos.getMaxZ();

        for (int x = startX; x <= endX && x <= area.maxPos.getX(); x++) {
            for (int y = startY; y <= endY && y <= area.maxPos.getY(); y++) {
                for (int z = startZ; z <= endZ && z <= area.maxPos.getZ(); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    if (shouldGlow(blockPos,state,area) && state.getBlock() instanceof BlockWithEntity) {
                        BlockEntity be = world.getBlockEntity(blockPos);
                        if (be != null) blockEntities.put(be.getPos(), area.color);
                    }
                }
            }
        }
        return blockEntities;
    }

    public static void renderBlocks(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                if (chunk.vbo != null) renderAreaVbo(chunk.vbo, cameraPos);
            }
        }
        OutlineVertexConsumerProvider consumer = mc.worldRenderer.bufferBuilders.getOutlineVertexConsumers();
        Random random = mc.getCameraEntity().getRandom();
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

    private static void renderBlockOutline(Map.Entry<BlockPos, BlockState> entry, float delta, Camera camera,
                                           MatrixStack matrixStack, Color color, BufferBuilder bufferBuilder) {
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
                    bufferBuilder, mc.getCameraEntity().getRandom(),
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
