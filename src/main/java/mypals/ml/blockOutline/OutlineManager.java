package mypals.ml.blockOutline;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class OutlineManager {
    public static Map<BlockPos, BlockState> blockToRenderer = new HashMap<>();
    public static ArrayList<BlockPos> targetedBlocks = new ArrayList<>();
    public static VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    public static Map<BlockEntity, Color> blockEntityList = new HashMap<>();

    public static void buildMesh(MatrixStack stack, RenderTickCounter counter) {
        vertexBuffer.close();
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }
        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        blockEntityList.clear();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        float delta = counter.getTickDelta(false);

        for (AreaBox selectedArea : selectedAreas) {
            for (int x = selectedArea.minPos.getX(); x <= selectedArea.maxPos.getX(); x++) {
                for (int y = selectedArea.minPos.getY(); y <= selectedArea.maxPos.getY(); y++) {
                    for (int z = selectedArea.minPos.getZ(); z <= selectedArea.maxPos.getZ(); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        onRenderOutline(new HashMap.SimpleEntry<>(blockPos, MinecraftClient.getInstance().world.getBlockState(blockPos)),
                                delta, camera, stack, selectedArea.color, buffer);
                    }
                }
            }
        }

        for (var entry : blockToRenderer.entrySet()) {
            onRenderOutline(entry, delta, camera, stack, Color.orange, buffer);
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer == null) return;
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);
        VertexBuffer.unbind();
    }

    public static void onRenderWorldLast(MatrixStack stack, RenderTickCounter counter) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        BlockEntityRenderDispatcher blockEntityRenderer = mc.getBlockEntityRenderDispatcher();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        OutlineVertexConsumerProvider outlineVertexConsumerProvider = mc.worldRenderer.bufferBuilders.getOutlineVertexConsumers();
        float delta = counter.getTickDelta(false);

        for (Map.Entry<BlockEntity, Color> entry : blockEntityList.entrySet()) {
            BlockPos blockPos = entry.getKey().getPos();
            double x = blockPos.getX() - MathHelper.lerp(delta, camera.getPos().getX(), camera.getPos().getX());
            double y = blockPos.getY() - MathHelper.lerp(delta, camera.getPos().getY(), camera.getPos().getY());
            double z = blockPos.getZ() - MathHelper.lerp(delta, camera.getPos().getZ(), camera.getPos().getZ());

            stack.push();
            stack.translate(x + 0.01, y + 0.01, z + 0.01);
            stack.scale(0.98f, 0.98f, 0.98f);
            Color color = entry.getValue();
            outlineVertexConsumerProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            blockEntityRenderer.get(entry.getKey()).render(entry.getKey(), 0, stack, outlineVertexConsumerProvider, 111, 0);
            stack.pop();
        }

        if (vertexBuffer.drawMode == null) return;
        vertexBuffer.bind();
        mc.worldRenderer.entityOutlinesFramebuffer.beginWrite(false);
        vertexBuffer.draw(stack.peek().getPositionMatrix(), stack.peek().getPositionMatrix(), GameRenderer.getPositionColorProgram());
        VertexBuffer.unbind();
        RenderSystem.disableBlend();
    }

    public static void onRenderOutline(Map.Entry<BlockPos, BlockState> entry, float delta, Camera camera, MatrixStack matrixStack, Color color, BufferBuilder bufferBuilder) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager dispatcher = mc.getBlockRenderManager();
        BlockPos blockPos = entry.getKey();
        BlockState blockState = entry.getValue();

        double x = blockPos.getX() - MathHelper.lerp(delta, camera.getPos().getX(), camera.getPos().getX());
        double y = blockPos.getY() - MathHelper.lerp(delta, camera.getPos().getY(), camera.getPos().getY());
        double z = blockPos.getZ() - MathHelper.lerp(delta, camera.getPos().getZ(), camera.getPos().getZ());

        matrixStack.push();
        matrixStack.translate(x, y, z);
        matrixStack.scale(1.001f, 1.001f, 1.001f);

        if (!blockState.getFluidState().isEmpty()) {
            CustomFluidOutlineRenderer.render(mc.world, blockPos, bufferBuilder, blockState, blockState.getFluidState(), matrixStack,
                    color.getRed(), color.getGreen(), color.getBlue());
        }

        if (blockState.getRenderType() == BlockRenderType.MODEL) {
            CustomBlockOutlineRenderer.render((BlockRenderView) mc.world, dispatcher.getModel(blockState), blockState, blockPos, matrixStack,
                    bufferBuilder, mc.getCameraEntity().getRandom(), blockState.getRenderingSeed(blockPos),
                    OverlayTexture.DEFAULT_UV, color.getRed(), color.getGreen(), color.getBlue());
        }

        if (blockState.getBlock() instanceof BlockWithEntity) {
            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntityList.put(blockEntity, color);
            }
        }

        matrixStack.pop();
    }

    public static void resolveBlocks() {
        World world = MinecraftClient.getInstance().world;
        blockToRenderer.clear();
        for (BlockPos pos : targetedBlocks) {
            blockToRenderer.put(pos, world.getBlockState(pos));
        }
        targetedBlocks.clear();
    }
}