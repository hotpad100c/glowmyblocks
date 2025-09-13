package mypals.ml.blockOutline;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import java.util.List;

public class CustomBlockOutlineRenderer {
    public static void render(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos,
                              MatrixStack matrices, BufferBuilder vertexConsumer, Random random,
                              long seed, int overlay, int r, int g, int b) {
        Vec3d offset = state.getModelOffset(world, pos);
        matrices.translate(offset.x, offset.y, offset.z);

        BlockPos.Mutable mutable = pos.mutableCopy();
        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            List<BakedQuad> quads = model.getQuads(state, direction, random);
            if (!quads.isEmpty()) {
                mutable.set(pos, direction);
                if (Block.shouldDrawSide(state, world, pos, direction, mutable) || !OutlineManager.blockToRenderer.containsKey(pos.offset(direction))) {
                    for (BakedQuad quad : quads) {
                        vertexConsumer.quad(matrices.peek(), quad, new float[]{1, 1, 1, 1},
                                (float) r /255, (float) g /255, (float) b /255, 1.0F, new int[]{1, 1, 1, 1}, overlay, true);
                    }
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            for (BakedQuad quad : quads) {
                vertexConsumer.quad(matrices.peek(), quad, new float[]{1, 1, 1, 1}, (float) r /255, (float) g /255, (float) b /255, 1.0F, new int[]{1, 1, 1, 1}, overlay, true);
            }
        }
    }
}
