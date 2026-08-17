package mypals.ml.blockOutline;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static mypals.ml.blockOutline.ChunkDataBuilder.buildMeshesAsync;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class OutlineManager {

    public static Map<BlockPos, Color> blockToRenderer = new HashMap<>();
    public static ArrayList<BlockPos> targetedBlocks = new ArrayList<>();

    /**
     * The layer the outline meshes are built for and drawn with. Both sides must agree: since
     * 1.21.6 the vertex layout comes from the layer's pipeline, and a mesh built with a different
     * format draws nothing rather than erroring.
     *
     * <p>This is vanilla's own outline layer, which is what makes the meshes glow instead of
     * showing up as flat geometry: it renders into {@code OutputTarget.OUTLINE_TARGET} with
     * {@code OutlineProperty.IS_OUTLINE}, and the entity-outline post chain turns that buffer into
     * the glow. It also already carries NO_DEPTH_TEST and no depth write, so outlines draw through
     * walls for free -- no custom pipeline needed.
     *
     * <p>Its vertex format is {@code POSITION_TEX_COLOR}, which is what the mesh builder produces.
     */
    public static RenderType outlineLayer() {
        return RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS);
    }

    public static Map<AreaBox, AreaRenderData> areaVbos = new ConcurrentHashMap<>();

    public static class AreaRenderData {
        public Map<SectionPos, ChunkRenderData> sectionData = new HashMap<>();
    }
    record ChunkBufferData(
            MeshData buffer,
            Map<BlockPos, Color> blockEntities
    ) {}
    public static class ChunkRenderData {
        public GMBVertexBuffer vbo;
        public Map<BlockPos, Color> blockEntities = new HashMap<>();
    }


    public static void buildMeshes(DeltaTracker counter) {

        buildMeshesAsync(counter);
    }

    public static void renderBlocks(PoseStack stack, DeltaTracker counter, Matrix4f projectionMatrix) {
        if (selectedAreas.isEmpty() && blockToRenderer.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        // Drawing through walls is a property of the layer's pipeline now (see GMBRenderTypes),
        // not of global GL state -- the GlStateManager depth toggles that used to wrap this loop
        // have had no effect since 1.21.6.
        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                if (chunk.vbo != null) renderAreaVbo(chunk.vbo, cameraPos);
            }
        }


        /*
        OutlineBufferSource consumer = mc.levelRenderer.renderBuffers.outlineBufferSource();
        RandomSource random = mc.getCameraEntity().getRandom();

        //we need this to keep the outline color bright idk why :((((
        PoseStack tempStack = new PoseStack();
        tempStack.translate(0, 0, 0);
        tempStack.scale(0.0f, 0.0f, 0.0f);
        Minecraft.getInstance().getBlockRenderer().renderBatched(Blocks.STONE.defaultBlockState(),
                new BlockPos(0, 0, 0), Minecraft.getInstance().level,tempStack,
                consumer.getBuffer(RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS)),true,
                new ArrayList<>());

         */
    }

    /**
     * Flat position -> packed ARGB index of the block entities that should glow.
     *
     * <p>Read once per block entity per frame from {@code GlowMyBlocksBlockEntityOutlineMixin},
     * so it is kept flat rather than walking {@link #areaVbos} section by section.
     */
    private static final Map<BlockPos, Integer> outlinedBlockEntities = new ConcurrentHashMap<>();

    /** Rebuilds {@link #outlinedBlockEntities} from the current section data. */
    public static void refreshBlockEntityIndex() {
        outlinedBlockEntities.clear();
        for (AreaRenderData areaData : areaVbos.values()) {
            for (ChunkRenderData chunk : areaData.sectionData.values()) {
                for (Map.Entry<BlockPos, Color> entry : chunk.blockEntities.entrySet()) {
                    outlinedBlockEntities.put(entry.getKey(), entry.getValue().getRGB());
                }
            }
        }
    }

    /**
     * The glow colour for the block entity at {@code pos}, or {@link #NO_OUTLINE} if it should
     * render normally.
     */
    public static int outlineColorFor(BlockPos pos) {
        return outlinedBlockEntities.getOrDefault(pos, NO_OUTLINE);
    }

    public static final int NO_OUTLINE = 0;

    private static void renderAreaVbo(GMBVertexBuffer vbo, Vec3 cameraPos) {
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().translate(
                (float) -cameraPos.x,
                (float) -cameraPos.y,
                (float) -cameraPos.z
        );

        vbo.draw(outlineLayer());

        RenderSystem.getModelViewStack().popMatrix();
    }


    public static boolean isBlockInsideArea(BlockPos pos, AreaBox area) {
        return pos.getX() >= area.minPos.getX() && pos.getX() <= area.maxPos.getX()
                && pos.getY() >= area.minPos.getY() && pos.getY() <= area.maxPos.getY()
                && pos.getZ() >= area.minPos.getZ() && pos.getZ() <= area.maxPos.getZ();
    }

    /**
     * @param visibleFaces bit per {@link net.minecraft.core.Direction#ordinal()}; faces whose bit
     *                     is clear are hidden by another block of the same selection and skipped.
     */
    static void renderBlockOutline(Map.Entry<BlockPos, BlockState> entry, float delta, Camera camera,
                                   PoseStack matrixStack, Color color, RandomSource random,
                                   BufferBuilder bufferBuilder, int visibleFaces) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BlockPos blockPos = entry.getKey();
        BlockState blockState = entry.getValue();

        matrixStack.pushPose();

        if (!blockState.getFluidState().isEmpty()) {
            CustomFluidOutlineRenderer.render(mc.level, blockPos, bufferBuilder,
                    blockState, blockState.getFluidState(), matrixStack,
                    color.getRed(), color.getGreen(), color.getBlue());
        }

        if (blockState.getRenderShape() == RenderShape.MODEL) {
            CustomBlockOutlineRenderer.render(mc.level,
                    dispatcher.getBlockModel(blockState), blockState, blockPos, matrixStack,
                    bufferBuilder, random,
                    blockState.getSeed(blockPos),
                    OverlayTexture.NO_OVERLAY, color.getRed(), color.getGreen(), color.getBlue(),
                    visibleFaces);
        }
        matrixStack.popPose();
    }

    public static void resolveBlocks() {
        blockToRenderer.clear();
        targetedBlocks.clear();
    }
}
