package mypals.ml.blockOutline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mypals.ml.GlowMyBlocks;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;
import static net.minecraft.client.render.RenderPhase.*;

public class OutlineManager {
    public static Map<BlockPos, Color> blockToRenderer = new HashMap<>();
    public static ArrayList<BlockPos> targetedBlocks = new ArrayList<>();
    public static VertexBuffer vertexBuffer = null;
    public static Map<BlockPos, Color> blockEntityList = new HashMap<>();

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

    public static void buildMesh(MatrixStack stack1, RenderTickCounter counter) {
        MatrixStack stack = new MatrixStack();
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }

        blockEntityList.clear();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        float delta = counter.getTickDelta(false);


        BufferBuilder buffer = Tessellator.getInstance().begin(
                GLOWING_OUTLINE_RENDER.getDrawMode(), GLOWING_OUTLINE_RENDER.getVertexFormat());

        World world = MinecraftClient.getInstance().world;
        for (AreaBox selectedArea : selectedAreas) {
            for (int x = selectedArea.minPos.getX(); x <= selectedArea.maxPos.getX(); x++) {
                for (int y = selectedArea.minPos.getY(); y <= selectedArea.maxPos.getY(); y++) {
                    for (int z = selectedArea.minPos.getZ(); z <= selectedArea.maxPos.getZ(); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        if(world == null) {
                            return;
                        }
                        BlockState state = world.getBlockState(blockPos);
                        if(state.isAir()){
                            continue;
                        }
                        for(Direction direction : Direction.values()) {
                            BlockPos offsetPos = blockPos.offset(direction);
                            if (!world.getBlockState(offsetPos).isSideSolidFullSquare(world, offsetPos, direction.getOpposite())) {
                                double x1 = blockPos.getX();
                                double y1 = blockPos.getY();
                                double z1 = blockPos.getZ();

                                stack.push();
                                stack.translate(x1, y1 , z1 );

                                onRenderOutline(new HashMap.SimpleEntry<>(blockPos,
                                                MinecraftClient.getInstance().world.getBlockState(blockPos)),
                                        delta, camera, stack, selectedArea.color, buffer);
                                stack.pop();
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (vertexBuffer != null) vertexBuffer.close();

        BuiltBuffer builtBuffer = buffer.endNullable();
        if(builtBuffer != null){
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            vertexBuffer.bind();
            vertexBuffer.upload(builtBuffer);
            VertexBuffer.unbind();

            GlowMyBlocks.needRebuildOutlineMesh = false;
        }
    }

    public static void onRenderWorldLast(MatrixStack stack, RenderTickCounter counter,  Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        BlockEntityRenderDispatcher blockEntityRenderer = mc.getBlockEntityRenderDispatcher();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        OutlineVertexConsumerProvider outlineVertexConsumerProvider = mc.worldRenderer.bufferBuilders.getOutlineVertexConsumers();
        float delta = counter.getTickDelta(false);

        if (vertexBuffer == null || vertexBuffer.drawMode == null || selectedAreas.isEmpty()) return;


        vertexBuffer.bind();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Vec3d cameraPos = camera.getPos();

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().translate(
                (float) -cameraPos.x,
                (float) -cameraPos.y,
                (float) -cameraPos.z
        );
        RenderSystem.applyModelViewMatrix();
        mc.worldRenderer.entityOutlinesFramebuffer.beginWrite(false);
        vertexBuffer.draw(RenderSystem.getModelViewStack(),
                projectionMatrix, RenderSystem.getShader());
        //mc.worldRenderer.entityOutlinesFramebuffer.endWrite();
        VertexBuffer.unbind();


        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        for (Map.Entry<BlockPos, Color> entry : blockEntityList.entrySet()){
            BlockPos blockPos = entry.getKey();
            Color color = entry.getValue();

            double x1 = blockPos.getX() - cameraPos.getX();
            double y1 = blockPos.getY() - cameraPos.getY();
            double z1 = blockPos.getZ() - cameraPos.getZ();

            stack.push();
            stack.translate(x1, y1, z1);

            outlineVertexConsumerProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 1);
            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntityRenderer.render(blockEntity, delta, stack, outlineVertexConsumerProvider);
            }
            stack.pop();
        }
    }

    public static void onRenderOutline(Map.Entry<BlockPos, BlockState> entry, float delta, Camera camera,
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

        if (blockState.getBlock() instanceof BlockWithEntity) {
            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntityList.put(blockEntity.getPos(), color);
            }
        }

    }

    public static void resolveBlocks() {
        blockToRenderer.clear();
        targetedBlocks.clear();
    }
}