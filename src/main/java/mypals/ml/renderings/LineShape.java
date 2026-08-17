package mypals.ml.renderings;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;

public class LineShape {
    private static double lastTickPosX,lastTickPosY,lastTickPosZ;
    public Vec3 start;
    public Vec3 end;
    public float alpha;
    public Color color;
    public boolean seeThrough;
    public LineShape(Vec3 start, Vec3 end, Color color, float alpha, boolean seeThrough) {
        this.start = start;
        this.end = end;
        this.alpha = alpha;
        this.color = color;
        this.seeThrough = seeThrough;
    }
    public static void draw(PoseStack matrixStack, Vec3 start, Vec3 end, Color color, float alpha, boolean seeThrough) {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (camera.isInitialized() && client.getEntityRenderDispatcher().options != null && client.player != null) {
            double x =  (start.x() - camera.position().x());
            double y =  (start.y() - camera.position().y());
            double z =  (start.z() - camera.position().z());
            float red = ((color.getRGB() >> 16) & 0xFF) / 255.0f;
            float green = ((color.getRGB() >> 8) & 0xFF) / 255.0f;
            float blue = (color.getRGB() & 0xFF) / 255.0f;

            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.begin(RenderTypes.lines().mode(), RenderTypes.lines().format());
            matrixStack.pushPose();
            matrixStack.translate(x, y, z);
            Matrix4f modelViewMatrix = matrixStack.last().pose();

            buffer.addVertex(modelViewMatrix, 0.0F, 0.0F, 0.0F)
                    .setNormal((float) (end.x - start.x()), (float) (end.y - start.y()), (float) (end.z - start.z()))
                    .setColor(red, green, blue, alpha).setLineWidth(1);
            buffer.addVertex(modelViewMatrix, (float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z))
                    .setNormal((float) (end.x - start.x()), (float) (end.y - start.y()), (float) (end.z - start.z()))
                    .setColor(red, green, blue, alpha).setLineWidth(1);

            if(seeThrough)
                GlStateManager._disableDepthTest();
            RenderTypes.lines().draw(buffer.buildOrThrow());
            GlStateManager._enableDepthTest();
            matrixStack.popPose();
        }
    }
}
