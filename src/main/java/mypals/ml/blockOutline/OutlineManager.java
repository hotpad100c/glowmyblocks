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
import java.util.concurrent.ThreadLocalRandom;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;
import static net.minecraft.client.render.RenderPhase.*;

public class OutlineManager {

    public static Map<BlockPos, Color> blockToRenderer = new HashMap<>();
    public static ArrayList<BlockPos> targetedBlocks = new ArrayList<>();

    public static Map<AreaBox, AreaRenderData> areaVbos = new ConcurrentHashMap<>();

    public static class AreaRenderData {
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
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }

        for (AreaRenderData data : areaVbos.values()) {
            if (data.vbo != null) data.vbo.close();
        }
        areaVbos.clear();

        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        float delta = counter.getTickDelta(false);

        for (AreaBox area : selectedAreas) {
            AreaRenderData renderData = new AreaRenderData();
            renderData.vbo = buildMeshForArea(area, delta);
            renderData.blockEntities = collectBlockEntitiesForArea(area);
            areaVbos.put(area, renderData);
        }

        GlowMyBlocks.needRebuildOutlineMesh = false;
    }

    private static VertexBuffer buildMeshForArea(AreaBox area, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        World world = mc.world;
        if (world == null) return null;

        BufferBuilder buffer = Tessellator.getInstance().begin(
                GLOWING_OUTLINE_RENDER.getDrawMode(),
                GLOWING_OUTLINE_RENDER.getVertexFormat()
        );

        for (int x = area.minPos.getX(); x <= area.maxPos.getX(); x++) {
            for (int y = area.minPos.getY(); y <= area.maxPos.getY(); y++) {
                for (int z = area.minPos.getZ(); z <= area.maxPos.getZ(); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    if (state.isAir()) continue;

                    for (Direction direction : Direction.values()) {
                        BlockPos offsetPos = blockPos.offset(direction);
                        boolean isFullyBlocked = !world.getBlockState(offsetPos).isSideSolidFullSquare(world, offsetPos, direction.getOpposite())
                                && isBlockInsideArea(offsetPos, area);
                        if (!isFullyBlocked) {
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
    private static Map<BlockPos, Color> collectBlockEntitiesForArea(AreaBox area) {
        Map<BlockPos, Color> blockEntities = new HashMap<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return blockEntities;

        for (int x = area.minPos.getX(); x <= area.maxPos.getX(); x++) {
            for (int y = area.minPos.getY(); y <= area.maxPos.getY(); y++) {
                for (int z = area.minPos.getZ(); z <= area.maxPos.getZ(); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    if (state.getBlock() instanceof BlockWithEntity) {
                        BlockEntity blockEntity = world.getBlockEntity(blockPos);
                        if (blockEntity != null) {
                            blockEntities.put(blockEntity.getPos(), area.color);
                        }
                    }
                }
            }
        }
        return blockEntities;
    }

    public static void renderBlocks(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Vec3d cameraPos = camera.getPos();

        OutlineVertexConsumerProvider provider = MinecraftClient.getInstance().getBufferBuilders()
                .getOutlineVertexConsumers();
        VertexConsumer consumer = provider.getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

        Random random = Random.create();

        //We need this to make it work idk why but i cant do anything :(


        for (AreaRenderData data : areaVbos.values()) {
            if (data.vbo != null) {
                renderAreaVbo(data.vbo, cameraPos);
            }
        }
        MatrixStack tempStack = new MatrixStack();
        tempStack.translate(0, 0, 0);
        tempStack.scale(0.0f, 0.0f, 0.0f);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(Blocks.STONE.getDefaultState(),
                new BlockPos(0, 0, 0), (BlockRenderView) MinecraftClient.getInstance().world,tempStack,
                consumer,true,random );
    }
    public static void renderBlockEntities(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Vec3d cameraPos = camera.getPos();

        for (AreaRenderData data : areaVbos.values()) {
            if (!data.blockEntities.isEmpty()) {
                renderAreaBlockEntities(stack, data.blockEntities, counter, cameraPos);
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

    public static void onBlockStateChange(BlockPos blockPos) {
        for (AreaBox area : selectedAreas) {
            if (isBlockInsideArea(blockPos, area)) {
                rebuildArea(area);
                break;
            }
        }
    }

    private static boolean isBlockInsideArea(BlockPos pos, AreaBox area) {
        return pos.getX() >= area.minPos.getX() && pos.getX() <= area.maxPos.getX()
                && pos.getY() >= area.minPos.getY() && pos.getY() <= area.maxPos.getY()
                && pos.getZ() >= area.minPos.getZ() && pos.getZ() <= area.maxPos.getZ();
    }

    private static void rebuildArea(AreaBox area) {
        AreaRenderData old = areaVbos.get(area);
        if (old != null && old.vbo != null) {
            old.vbo.close();
        }

        AreaRenderData renderData = new AreaRenderData();
        renderData.vbo = buildMeshForArea(area, 0);
        renderData.blockEntities = collectBlockEntitiesForArea(area);
        VertexBuffer.unbind();

        areaVbos.put(area, renderData);
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

            double x1 = blockPos.getX() - cameraPos.getX();
            double y1 = blockPos.getY() - cameraPos.getY();
            double z1 = blockPos.getZ() - cameraPos.getZ();

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
