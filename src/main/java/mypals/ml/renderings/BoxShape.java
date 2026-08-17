package mypals.ml.renderings;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;

public class BoxShape{
    public Vec3 pos;
    public float length;
    public float weigth;
    public float height;
    public float alpha;
    public Color color;
    public boolean seeThrough;
    public BoxShape(Vec3 pos, float length, float weigth, float height,Color color,float alpha,boolean seeThrough){
        this.pos = pos;
        this.length = length;
        this.weigth = weigth;
        this.height = height;
        this.color = color;
        this.alpha = alpha;
        this.seeThrough = seeThrough;
    }
    public void draw(PoseStack matrices) {
        draw(matrices, this.pos, this.length, this.weigth, this.height,0, this.color, this.alpha, this.seeThrough);
    }
    public static void draw(PoseStack matrices, Vec3 pos, float length, float width, float height, float tickDelta, Color color, float alpha,boolean seeThrough) {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (camera.isInitialized() && client.getEntityRenderDispatcher().options != null && client.player != null) {
            matrices.pushPose();
            double lastTickPosX = camera.position().x();
            double lastTickPosY = camera.position().y();
            double lastTickPosZ = camera.position().z();

            float x = (float) (pos.x() - Mth.lerp(tickDelta, lastTickPosX, camera.position().x()));
            float y = (float) (pos.y() - Mth.lerp(tickDelta, lastTickPosY, camera.position().y()));
            float z = (float) (pos.z() - Mth.lerp(tickDelta, lastTickPosZ, camera.position().z()));

            matrices.translate(x, y, z);
            Matrix4f modelViewMatrix = matrices.last().pose();
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.begin(RenderTypes.debugQuads().mode(), RenderTypes.debugQuads().format());

            float xMin = -length / 2;
            float xMax = length / 2;
            float yMin = -height / 2;
            float yMax = height / 2;
            float zMin = -width / 2;
            float zMax = width / 2;

            float red = ((color.getRGB() >> 16) & 0xFF) / 255.0f;
            float green = ((color.getRGB() >> 8) & 0xFF) / 255.0f;
            float blue = (color.getRGB() & 0xFF) / 255.0f;


            buffer.addVertex(modelViewMatrix, xMin, yMax, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMax, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMax, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMax, zMax).setColor(red, green, blue, alpha);


            buffer.addVertex(modelViewMatrix, xMin, yMin, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMin, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMin, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMin, zMin).setColor(red, green, blue, alpha);


            buffer.addVertex(modelViewMatrix, xMin, yMax, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMax, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMin, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMin, zMin).setColor(red, green, blue, alpha);


            buffer.addVertex(modelViewMatrix, xMax, yMin, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMin, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMax, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMax, zMin).setColor(red, green, blue, alpha);


            buffer.addVertex(modelViewMatrix, xMin, yMin, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMin, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMax, zMin).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMax, zMin).setColor(red, green, blue, alpha);


            buffer.addVertex(modelViewMatrix, xMin, yMax, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMax, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMax, yMin, zMax).setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, xMin, yMin, zMax).setColor(red, green, blue, alpha);


            if(seeThrough)
                GlStateManager._disableDepthTest();
            GlStateManager._enableBlend();
            GlStateManager._disableCull();
            MeshData meshData = buffer.buildOrThrow();
            ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(64);
            meshData.sortQuads(byteBufferBuilder,VertexSorting.DISTANCE_TO_ORIGIN);
            RenderTypes.debugQuads().draw(meshData);
            GlStateManager._enableDepthTest();
            GlStateManager._enableCull();
            GlStateManager._disableBlend();
            matrices.popPose();
        }
    }
}
