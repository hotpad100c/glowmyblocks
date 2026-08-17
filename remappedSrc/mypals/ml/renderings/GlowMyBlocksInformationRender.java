package mypals.ml.renderings;

import mypals.ml.wandSystem.AreaBox;
import mypals.ml.wandSystem.WandActionsManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
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
            drawSelectedAreas(matrixStack, counter.getTickDelta(false));
        }
    }

    private static void drawSelectedAreas(PoseStack matrixStack, float tickDelta){
        if (Minecraft.getInstance().player != null && (Minecraft.getInstance().player.getMainHandItem().getItem() == wand || (Minecraft.getInstance().player.isSpectator() && selectInSpectator))) {

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
        }

    }
    public static void renderSelectionBox(PoseStack matrices, Camera camera, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        BlockPos origin = WandActionsManager.pos1;

        HitResult result = client.cameraEntity.pick(player.getAbilities().instabuild ? 5.0F : 4.5F, 0, false);
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

        lastTickPosX = camera.getPosition().x();
        lastTickPosY = camera.getPosition().y();
        lastTickPosZ = camera.getPosition().z();
        float x = (float) (origin.getX() - Mth.lerp(tickDelta, lastTickPosX, camera.getPosition().x()));
        float y = (float) (origin.getY() - Mth.lerp(tickDelta, lastTickPosY, camera.getPosition().y()));
        float z = (float) (origin.getZ() - Mth.lerp(tickDelta, lastTickPosZ, camera.getPosition().z()));

        matrices.pushPose();

        VertexConsumer consumer = client.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        matrices.translate(x, y, z);

        LevelRenderer.drawBox(matrices, consumer, 0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), 1, 1, 1, 1, 0, 0, 0);

        matrices.popPose();
    }
}
