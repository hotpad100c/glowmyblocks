package mypals.ml.renderings;

import mypals.ml.wandSystem.AreaBox;
import mypals.ml.wandSystem.WandActionsManager;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.List;

import static mypals.ml.config.GlowMyBlocksConfig.renderSelectionMarker;
import static mypals.ml.config.GlowMyBlocksConfig.selectInSpectator;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;
import static mypals.ml.wandSystem.SelectedManager.wand;
import static mypals.ml.wandSystem.WandActionsManager.deleteMode;
import static mypals.ml.wandSystem.WandActionsManager.getAreasToDelete;

public class GlowMyBlocksInformationRender {
    public static double lastTickPosX = 0;
    public static double lastTickPosY = 0;
    public static double lastTickPosZ = 0;

    public static void render(MatrixStack matrixStack, RenderTickCounter counter){
        if(MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().gameRenderer.getCamera().isReady()) {
            drawSelectedAreas(matrixStack, counter.getTickDelta(false));
        }
    }

    private static void drawSelectedAreas(MatrixStack matrixStack, float tickDelta){
        if (MinecraftClient.getInstance().player != null && (MinecraftClient.getInstance().player.getMainHandStack().getItem() == wand || (MinecraftClient.getInstance().player.isSpectator() && selectInSpectator))) {

            HitResult result = MinecraftClient.getInstance().crosshairTarget;
            BlockPos lookingAt = BlockPos.ofFloored(result.getPos());
            if(renderSelectionMarker) {
                CubeShape.drawSingle(matrixStack, lookingAt, 0.01f, 0, deleteMode ? Color.red : Color.white, 0.2f, false);
            }
            if (WandActionsManager.pos1 != null) {
                renderSelectionBox(matrixStack, MinecraftClient.getInstance().gameRenderer.getCamera(), tickDelta);
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
    public static void renderSelectionBox(MatrixStack matrices, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        BlockPos origin = WandActionsManager.pos1;

        HitResult result = client.cameraEntity.raycast(player.getAbilities().creativeMode ? 5.0F : 4.5F, 0, false);
        BlockPos pos = WandActionsManager.pos2 != null ? WandActionsManager.pos2 : (result.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) result).getBlockPos() : BlockPos.ofFloored(result.getPos()));
        if(pos != null){
            CubeShape.drawSingle(matrices, pos, 0.01f, tickDelta, Color.blue, 0.2f,true);
        }
        if(origin != null){
            CubeShape.drawSingle(matrices, origin, 0.01f, tickDelta, Color.red, 0.2f,true);
        }
        pos = pos.subtract(origin);

        origin = origin.add(pos.getX() < 0 ? 1 : 0, pos.getY() < 0 ? 1 : 0, pos.getZ() < 0 ? 1 : 0);
        pos = pos.add(pos.getX() >= 0 ? 1 : -1, pos.getY() >= 0 ? 1 : -1, pos.getZ() >= 0 ? 1 : -1);

        lastTickPosX = camera.getPos().getX();
        lastTickPosY = camera.getPos().getY();
        lastTickPosZ = camera.getPos().getZ();
        float x = (float) (origin.getX() - MathHelper.lerp(tickDelta, lastTickPosX, camera.getPos().getX()));
        float y = (float) (origin.getY() - MathHelper.lerp(tickDelta, lastTickPosY, camera.getPos().getY()));
        float z = (float) (origin.getZ() - MathHelper.lerp(tickDelta, lastTickPosZ, camera.getPos().getZ()));

        matrices.push();

        VertexConsumer consumer = client.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayer.getLines());
        matrices.translate(x, y, z);

        WorldRenderer.drawBox(matrices, consumer, 0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), 1, 1, 1, 1, 0, 0, 0);

        matrices.pop();
    }
}
