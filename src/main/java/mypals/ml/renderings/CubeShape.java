package mypals.ml.renderings;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CubeShape {
    public BlockPos pos;
    public float alpha;
    public Color color;

    public boolean seeThrough;
    public CubeShape(BlockPos pos, float alpha, Color color, boolean seeThrough) {
        this.pos = pos;
        this.alpha = alpha;
        this.color = color;
        this.seeThrough = seeThrough;
    }
    public static void drawCubes(PoseStack matrices, Map<BlockPos,CubeShape> cubes, float sizeAdd, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || client.player == null) {
            return;
        }

        matrices.pushPose();
        Vec3 cameraPos = camera.position();
        float lastTickPosX = (float) cameraPos.x();
        float lastTickPosY = (float) cameraPos.y();
        float lastTickPosZ = (float) cameraPos.z();




        Set<BlockPos> cubePositions = cubes.keySet();

        java.util.List<CubeShape> opaqueCubes = cubes.values().stream().filter(cube -> !cube.seeThrough).collect(Collectors.toList());
        java.util.List<CubeShape> seeThroughCubes = cubes.values().stream().filter(cube -> cube.seeThrough).collect(Collectors.toList());

        if (!opaqueCubes.isEmpty()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            drawCubes(bufferBuilder, matrices, opaqueCubes, sizeAdd, tickDelta, cameraPos, lastTickPosX, lastTickPosY, lastTickPosZ, cubePositions);
            GlStateManager._enableDepthTest();

            RenderTypes.debugQuads().draw(bufferBuilder.buildOrThrow());
        }

        if (!seeThroughCubes.isEmpty()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            drawCubes(bufferBuilder, matrices, seeThroughCubes, sizeAdd, tickDelta, cameraPos, lastTickPosX, lastTickPosY, lastTickPosZ, cubePositions);
            GlStateManager._disableDepthTest();
            RenderTypes.debugQuads().draw(bufferBuilder.buildOrThrow());
            GlStateManager._enableDepthTest();
        }

        matrices.popPose();
    }

    private static void drawCubes(BufferBuilder bufferBuilder, PoseStack matrices, java.util.List<CubeShape> cubes, float sizeAdd, float tickDelta,
                                  Vec3 cameraPos, float lastTickPosX, float lastTickPosY, float lastTickPosZ, Set<BlockPos> cubePositions) {
        float minOffset = -0.001f - sizeAdd;
        float maxOffset = 1.001f + sizeAdd;

        for (CubeShape cube : cubes) {
            float x = (float) (cube.pos.getX() - Mth.lerp(tickDelta, lastTickPosX, cameraPos.x()));
            float y = (float) (cube.pos.getY() - Mth.lerp(tickDelta, lastTickPosY, cameraPos.y()));
            float z = (float) (cube.pos.getZ() - Mth.lerp(tickDelta, lastTickPosZ, cameraPos.z()));

            matrices.pushPose();
            matrices.translate(x, y, z);
            Matrix4f modelViewMatrix = matrices.last().pose();

            float red = ((cube.color.getRGB() >> 16) & 0xFF) / 255.0f;
            float green = ((cube.color.getRGB() >> 8) & 0xFF) / 255.0f;
            float blue = (cube.color.getRGB() & 0xFF) / 255.0f;

            BlockPos pos = cube.pos;
            boolean hasUp = cubePositions.contains(pos.above());
            boolean hasDown = cubePositions.contains(pos.below());
            boolean hasNorth = cubePositions.contains(pos.north());
            boolean hasSouth = cubePositions.contains(pos.south());
            boolean hasWest = cubePositions.contains(pos.west());
            boolean hasEast = cubePositions.contains(pos.east());

            if (!hasUp) {
                bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, maxOffset).setColor(red, green, blue, cube.alpha);
            }

            if (!hasDown) {
                bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, minOffset).setColor(red, green, blue, cube.alpha);
            }

            if (!hasWest) {
                bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, minOffset).setColor(red, green, blue, cube.alpha);
            }

            if (!hasEast) {
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, minOffset).setColor(red, green, blue, cube.alpha);
            }

            if (!hasNorth) {
                bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, minOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, minOffset).setColor(red, green, blue, cube.alpha);
            }

            if (!hasSouth) {
                bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, maxOffset).setColor(red, green, blue, cube.alpha);
                bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, maxOffset).setColor(red, green, blue, cube.alpha);
            }

            matrices.popPose();
        }

    }
    public static void drawSingle(PoseStack matrices,BlockPos pos, float sizeAdd, float tickDelta,Color color,float alpha, boolean seeThrough) {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || client.player == null) {
            return;
        }

        matrices.pushPose();
        Vec3 cameraPos = camera.position();
        float x = (float) (pos.getX() - Mth.lerp(tickDelta, cameraPos.x(), cameraPos.x()));
        float y = (float) (pos.getY() - Mth.lerp(tickDelta, cameraPos.y(), cameraPos.y()));
        float z = (float) (pos.getZ() - Mth.lerp(tickDelta, cameraPos.z(), cameraPos.z()));

        matrices.translate(x, y, z);
        Matrix4f modelViewMatrix = matrices.last().pose();


        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float minOffset = -0.001f - sizeAdd;
        float maxOffset = 1.001f + sizeAdd;

        float red = ((color.getRGB() >> 16) & 0xFF) / 255.0f;
        float green = ((color.getRGB() >> 8) & 0xFF) / 255.0f;
        float blue = (color.getRGB() & 0xFF) / 255.0f;

        bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, minOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, maxOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, maxOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, maxOffset, minOffset, maxOffset).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(modelViewMatrix, minOffset, minOffset, maxOffset).setColor(red, green, blue, alpha);

        if (seeThrough) GlStateManager._disableDepthTest();

        MeshData meshData = bufferBuilder.buildOrThrow();
        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(64);
        meshData.sortQuads(byteBufferBuilder,VertexSorting.DISTANCE_TO_ORIGIN);
        RenderTypes.debugQuads().draw(meshData);
        GlStateManager._enableDepthTest();
        matrices.popPose();
    }
}
