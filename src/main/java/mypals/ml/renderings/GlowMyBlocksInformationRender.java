package mypals.ml.renderings;

import com.mojang.blaze3d.opengl.GlStateManager;
import mypals.ml.wandSystem.AreaBox;
import mypals.ml.wandSystem.WandActionsManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.awt.*;
import java.util.List;

import static mypals.ml.config.GlowMyBlocksConfig.renderSelectionMarker;
import static mypals.ml.config.GlowMyBlocksConfig.selectInSpectator;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;
import static mypals.ml.wandSystem.SelectedManager.wand;
import static mypals.ml.wandSystem.WandActionsManager.deleteMode;
import static mypals.ml.wandSystem.WandActionsManager.getAreasToDelete;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class GlowMyBlocksInformationRender {
    public static double lastTickPosX = 0;
    public static double lastTickPosY = 0;
    public static double lastTickPosZ = 0;

    public static void render(PoseStack matrixStack, DeltaTracker counter){
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().gameRenderer.getMainCamera().isInitialized()) {
            drawSelectedAreas(matrixStack, counter.getGameTimeDeltaPartialTick(false));
        }
    }

    private static void drawSelectedAreas(PoseStack matrixStack, float tickDelta){
        if (Minecraft.getInstance().player != null && (Minecraft.getInstance().player.getMainHandItem().getItem() == wand || (Minecraft.getInstance().player.isSpectator() && selectInSpectator))) {


            GlStateManager._depthMask(false);
            HitResult result = Minecraft.getInstance().hitResult;
            BlockPos lookingAt = BlockPos.containing(result.getLocation());
            if(renderSelectionMarker) {
                CubeShape.drawSingle(matrixStack, lookingAt, 0.01f, 0, deleteMode ? Color.red : Color.white, 0.2f, false);
            }
            if (WandActionsManager.pos1 != null) {
                renderSelectionBox(matrixStack, Minecraft.getInstance().gameRenderer.getMainCamera(), tickDelta);
            }

            if (deleteMode) {
                List<AreaBox> areasToDelete = getAreasToDelete(lookingAt, false);
                for (AreaBox selectedArea : areasToDelete) {
                    selectedArea.draw(matrixStack, Color.red, 0.4f, true);
                }
                for (AreaBox selectedArea : selectedAreas) {
                    if(!areasToDelete.contains(selectedArea)){
                        selectedArea.draw(matrixStack, selectedArea.color, 0.01f, true);
                    }
                }
            }

            GlStateManager._depthMask(true);
        }

    }
    public static void renderSelectionBox(PoseStack matrices, Camera camera, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        BlockPos origin = WandActionsManager.pos1;

        HitResult result = client.getCameraEntity().pick(player.getAbilities().instabuild ? 5.0F : 4.5F, 0, false);
        BlockPos pos = WandActionsManager.pos2 != null ? WandActionsManager.pos2 : (result.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) result).getBlockPos() : BlockPos.containing(result.getLocation()));
        if(pos != null){
            CubeShape.drawSingle(matrices, pos, 0.01f, tickDelta, Color.blue, 0.2f,true);
        }
        if(origin != null){
            CubeShape.drawSingle(matrices, origin, 0.01f, tickDelta, Color.red, 0.2f,true);
        }
        pos = pos.subtract(origin);

        origin = origin.offset(pos.getX() < 0 ? 1 : 0, pos.getY() < 0 ? 1 : 0, pos.getZ() < 0 ? 1 : 0);
        pos = pos.offset(pos.getX() >= 0 ? 1 : -1, pos.getY() >= 0 ? 1 : -1, pos.getZ() >= 0 ? 1 : -1);

        lastTickPosX = camera.position().x();
        lastTickPosY = camera.position().y();
        lastTickPosZ = camera.position().z();
        float x = (float) (origin.getX() - Mth.lerp(tickDelta, lastTickPosX, camera.position().x()));
        float y = (float) (origin.getY() - Mth.lerp(tickDelta, lastTickPosY, camera.position().y()));
        float z = (float) (origin.getZ() - Mth.lerp(tickDelta, lastTickPosZ, camera.position().z()));

        matrices.pushPose();

        VertexConsumer consumer = client.renderBuffers().bufferSource().getBuffer(RenderTypes.lines());
        matrices.translate(x, y, z);

        // 1.21.11 removed ShapeRenderer#renderLineBox (the debug shape helpers moved to Gizmos),
        // so emit the twelve edges ourselves.
        renderLineBox(matrices, consumer, 0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), 1, 1, 1, 1);

        matrices.popPose();
    }

    /**
     * Draws the wireframe of the box spanning (x1,y1,z1)-(x2,y2,z2) into a lines consumer.
     * Replaces {@code ShapeRenderer#renderLineBox}, dropped in 1.21.11.
     */
    private static void renderLineBox(PoseStack matrices, VertexConsumer consumer,
                                      double x1, double y1, double z1,
                                      double x2, double y2, double z2,
                                      float r, float g, float b, float a) {
        PoseStack.Pose pose = matrices.last();
        float minX = (float) x1, minY = (float) y1, minZ = (float) z1;
        float maxX = (float) x2, maxY = (float) y2, maxZ = (float) z2;

        // Edges along X
        line(consumer, pose, minX, minY, minZ, maxX, minY, minZ, 1, 0, 0, r, g, b, a);
        line(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, 1, 0, 0, r, g, b, a);
        line(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ, 1, 0, 0, r, g, b, a);
        line(consumer, pose, minX, maxY, maxZ, maxX, maxY, maxZ, 1, 0, 0, r, g, b, a);
        // Edges along Y
        line(consumer, pose, minX, minY, minZ, minX, maxY, minZ, 0, 1, 0, r, g, b, a);
        line(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, 0, 1, 0, r, g, b, a);
        line(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, 0, 1, 0, r, g, b, a);
        line(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, 0, 1, 0, r, g, b, a);
        // Edges along Z
        line(consumer, pose, minX, minY, minZ, minX, minY, maxZ, 0, 0, 1, r, g, b, a);
        line(consumer, pose, maxX, minY, minZ, maxX, minY, maxZ, 0, 0, 1, r, g, b, a);
        line(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, 0, 0, 1, r, g, b, a);
        line(consumer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, 0, 0, 1, r, g, b, a);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float nx, float ny, float nz,
                             float r, float g, float b, float a) {
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(1);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(1);
    }
}
