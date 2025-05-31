package mypals.ml.blockOutline;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import static net.minecraft.client.render.WorldRenderer.DIRECTIONS;

public class CustomBlockOutlineRenderer {
    public static void render(BlockRenderView world, BakedModel model, BlockState state,
                       BlockPos pos, MatrixStack matrices, BufferBuilder vertexConsumer, Random random, long seed,
                              int overlay,int r,int g,int b) {
        Vec3d vec3d = state.getModelOffset(world, pos);
        matrices.translate(vec3d.x, vec3d.y, vec3d.z);

        try {
            renderFlat(world, model, state, pos,
                    matrices, vertexConsumer, random, seed, overlay,r,g,b);

        } catch (Throwable throwable) {
        }
    }
    public static void renderFlat(BlockRenderView world, BakedModel model,
                                  BlockState state, BlockPos pos, MatrixStack matrices,
                                  BufferBuilder vertexConsumer, Random random,
                                  long seed, int overlay,int r,int g,int b) {
        BlockPos.Mutable mutable = pos.mutableCopy();

        for(Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random);
            if (!list.isEmpty()) {
                mutable.set(pos, direction);
                boolean shouldRender = Block.shouldDrawSide(state, world, pos, direction, mutable)
                        || !OutlineManager.blockToRenderer.containsKey(pos.offset(direction));
                if (shouldRender) {
                    renderQuadsFlat(matrices,vertexConsumer,list, overlay,r,g,b);
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list2 = model.getQuads(state, (Direction)null, random);
        if (!list2.isEmpty()) {
            renderQuadsFlat( matrices,vertexConsumer,list2, overlay,r,g,b);
        }

    }

    private static void renderQuadsFlat(MatrixStack matrices, BufferBuilder vertexConsumer, List<BakedQuad> quads,int overlay, int r, int g, int b) {
        for(BakedQuad bakedQuad : quads) {
            renderQuad(vertexConsumer, matrices.peek(), bakedQuad,overlay, r,g,b);
        }

    }
    private static void renderQuad( BufferBuilder builder, MatrixStack.Entry matrixEntry, BakedQuad quad,int overlay, int r, int g, int b) {
        builder.quad(matrixEntry, quad, new float[]{1, 1, 1, 1}, r, g, b,
                1.0F, new int[]{1, 1, 1, 1}, overlay, true);
    }
    private static void getQuadDimensions(BlockRenderView world, BlockState state, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] box, BitSet flags) {
        float f = 32.0F;
        float g = 32.0F;
        float h = 32.0F;
        float i = -32.0F;
        float j = -32.0F;
        float k = -32.0F;

        for(int l = 0; l < 4; ++l) {
            float m = Float.intBitsToFloat(vertexData[l * 8]);
            float n = Float.intBitsToFloat(vertexData[l * 8 + 1]);
            float o = Float.intBitsToFloat(vertexData[l * 8 + 2]);
            f = Math.min(f, m);
            g = Math.min(g, n);
            h = Math.min(h, o);
            i = Math.max(i, m);
            j = Math.max(j, n);
            k = Math.max(k, o);
        }

        if (box != null) {
            box[Direction.WEST.getId()] = f;
            box[Direction.EAST.getId()] = i;
            box[Direction.DOWN.getId()] = g;
            box[Direction.UP.getId()] = j;
            box[Direction.NORTH.getId()] = h;
            box[Direction.SOUTH.getId()] = k;
            int l = DIRECTIONS.length;
            box[Direction.WEST.getId() + l] = 1.0F - f;
            box[Direction.EAST.getId() + l] = 1.0F - i;
            box[Direction.DOWN.getId() + l] = 1.0F - g;
            box[Direction.UP.getId() + l] = 1.0F - j;
            box[Direction.NORTH.getId() + l] = 1.0F - h;
            box[Direction.SOUTH.getId() + l] = 1.0F - k;
        }

        float p = 1.0E-4F;
        float m = 0.9999F;
        switch (face) {
            case DOWN:
                flags.set(1, f >= 1.0E-4F || h >= 1.0E-4F || i <= 0.9999F || k <= 0.9999F);
                flags.set(0, g == j && (g < 1.0E-4F || state.isFullCube(world, pos)));
                break;
            case UP:
                flags.set(1, f >= 1.0E-4F || h >= 1.0E-4F || i <= 0.9999F || k <= 0.9999F);
                flags.set(0, g == j && (j > 0.9999F || state.isFullCube(world, pos)));
                break;
            case NORTH:
                flags.set(1, f >= 1.0E-4F || g >= 1.0E-4F || i <= 0.9999F || j <= 0.9999F);
                flags.set(0, h == k && (h < 1.0E-4F || state.isFullCube(world, pos)));
                break;
            case SOUTH:
                flags.set(1, f >= 1.0E-4F || g >= 1.0E-4F || i <= 0.9999F || j <= 0.9999F);
                flags.set(0, h == k && (k > 0.9999F || state.isFullCube(world, pos)));
                break;
            case WEST:
                flags.set(1, g >= 1.0E-4F || h >= 1.0E-4F || j <= 0.9999F || k <= 0.9999F);
                flags.set(0, f == i && (f < 1.0E-4F || state.isFullCube(world, pos)));
                break;
            case EAST:
                flags.set(1, g >= 1.0E-4F || h >= 1.0E-4F || j <= 0.9999F || k <= 0.9999F);
                flags.set(0, f == i && (i > 0.9999F || state.isFullCube(world, pos)));
        }

    }
    public static boolean shouldDraw(BlockPos pos){
        ClientWorld world = MinecraftClient.getInstance().world;
        for(Direction direction : DIRECTIONS) {
            BlockPos offset = pos.offset(direction);
            boolean shouldRender = !Block.isShapeFullCube(world.getBlockState(offset).getCollisionShape(world,offset))
                    || !OutlineManager.blockToRenderer.containsKey(pos.offset(direction));
            if (shouldRender) {
                return true;
            }
        }
        return false;
    }
}
