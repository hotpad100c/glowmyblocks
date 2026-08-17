package mypals.ml.blockOutline;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public class CustomBlockOutlineRenderer {
    public static void render(BlockAndTintGetter world, BlockStateModel model, BlockState state, BlockPos pos,
                              PoseStack matrices, BufferBuilder vertexConsumer, RandomSource random,
                              long seed, int overlay, int r, int g, int b) {
        Vec3 offset = state.getOffset(pos);
        matrices.translate(offset.x, offset.y, offset.z);

        random.setSeed(seed);
        List<BlockModelPart> quads = model.collectParts(random);
        if (!quads.isEmpty()) {
            for (BlockModelPart quad : quads) {
                for (Direction direction : Direction.values()){
                    for(BakedQuad bakedQuad : quad.getQuads(direction)){
                        vertexConsumer.putBulkData(matrices.last(), bakedQuad, new float[]{1, 1, 1, 1}, (float) r /255, (float) g /255, (float) b /255, 1.0F, new int[]{1, 1, 1, 1}, overlay, true);
                    }
                }
                for(BakedQuad bakedQuad : quad.getQuads(null)){
                    vertexConsumer.putBulkData(matrices.last(), bakedQuad, new float[]{1, 1, 1, 1}, (float) r /255, (float) g /255, (float) b /255, 1.0F, new int[]{1, 1, 1, 1}, overlay, true);
                }
            }
        }
    }
}
