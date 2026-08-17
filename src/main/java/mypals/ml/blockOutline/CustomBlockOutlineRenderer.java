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
    /**
     * @param visibleFaces bit per {@link Direction#ordinal()}. Direction-culled quads are only
     *                     emitted for faces whose bit is set, so interior faces between two
     *                     selected blocks cost nothing. Quads with no cull face (null direction)
     *                     are always emitted -- they are the ones a model can't cull anyway.
     */
    public static void render(BlockAndTintGetter world, BlockStateModel model, BlockState state, BlockPos pos,
                              PoseStack matrices, BufferBuilder vertexConsumer, RandomSource random,
                              long seed, int overlay, int r, int g, int b, int visibleFaces) {
        Vec3 offset = state.getOffset(pos);
        matrices.translate(offset.x, offset.y, offset.z);

        random.setSeed(seed);
        List<BlockModelPart> quads = model.collectParts(random);
        if (!quads.isEmpty()) {
            for (BlockModelPart quad : quads) {
                for (Direction direction : Direction.values()){
                    if ((visibleFaces & (1 << direction.ordinal())) == 0) continue;
                    for(BakedQuad bakedQuad : quad.getQuads(direction)){
                        vertexConsumer.putBulkData(matrices.last(), bakedQuad, new float[]{1, 1, 1, 1}, (float) r /255, (float) g /255, (float) b /255, 1.0F, new int[]{1, 1, 1, 1}, overlay);
                    }
                }
                for(BakedQuad bakedQuad : quad.getQuads(null)){
                    vertexConsumer.putBulkData(matrices.last(), bakedQuad, new float[]{1, 1, 1, 1}, (float) r /255, (float) g /255, (float) b /255, 1.0F, new int[]{1, 1, 1, 1}, overlay);
                }
            }
        }
    }
}
