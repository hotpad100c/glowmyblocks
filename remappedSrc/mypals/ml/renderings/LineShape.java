package mypals.ml.renderings;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

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
            double x =  (start.x() - camera.getPosition().x());
            double y =  (start.y() - camera.getPosition().y());
            double z =  (start.z() - camera.getPosition().z());
            float red = ((color.getRGB() >> 16) & 0xFF) / 255.0f;
            float green = ((color.getRGB() >> 8) & 0xFF) / 255.0f;
            float blue = (color.getRGB() & 0xFF) / 255.0f;

            float normalX = 0.0F;
            float normalY = 1.0F;
            float normalZ = 0.0F;
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            matrixStack.pushPose();
            matrixStack.translate(x, y, z);
            Matrix4f modelViewMatrix = matrixStack.last().pose();

            buffer.addVertex(modelViewMatrix, 0.0F, 0.0F, 0.0F)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(modelViewMatrix, (float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z))
                    .setColor(red, green, blue, alpha);

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(2f);
            RenderSystem.getShaderLineWidth();
            if(seeThrough)
                RenderSystem.disableDepthTest();
            BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrixStack.popPose();
        }
    }
}
