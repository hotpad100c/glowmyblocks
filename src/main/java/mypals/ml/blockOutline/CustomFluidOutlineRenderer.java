package mypals.ml.blockOutline;

import mypals.ml.config.GlowModeManager;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.swing.text.html.BlockView;

import static net.minecraft.client.render.OverlayTexture.DEFAULT_UV;

public class CustomFluidOutlineRenderer {
    private static boolean isSameFluid(FluidState a, FluidState b) {
        return b.getFluid().matchesType(a.getFluid());
    }

    private static boolean isSideCovered(World world, Direction direction, float height, BlockPos pos, BlockState state) {
        if (state.isOpaque()) {
            VoxelShape voxelShape = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, height, 1.0);
            VoxelShape voxelShape2 = state.getCullingShape(world, pos);
            return VoxelShapes.isSideCovered(voxelShape, voxelShape2, direction);
        } else {
            return false;
        }
    }

    private static boolean isSideCovered(World world, BlockPos pos, Direction direction, float maxDeviation, BlockState state) {
        return OutlineManager.blockToRenderer.containsKey(pos.offset(direction)) && isSideCovered(world, direction, maxDeviation, pos.offset(direction), state);
    }

    private static boolean isOppositeSideCovered(World world, BlockPos pos, BlockState state, Direction direction) {
        return isSideCovered(world, direction.getOpposite(), 1.0F, pos, state);
    }

    public static boolean shouldRenderSide(
            World world, BlockPos pos, FluidState fluidState, BlockState blockState, Direction direction, FluidState neighborFluidState
    ) {
        return !OutlineManager.blockToRenderer.containsKey(pos.offset(direction)) ||( !isOppositeSideCovered(world, pos, blockState, direction) && !isSameFluid(fluidState, neighborFluidState));
    }

    public static void render(World world, BlockPos pos, BufferBuilder vertexConsumer,
                              BlockState blockState, FluidState fluidState, MatrixStack matrixStack,
                              float r, float g, float b) {
        Sprite[] lavaSprites = new Sprite[2];
        Sprite[] waterSprites = new Sprite[2];

        matrixStack.push();
        matrixStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());

        lavaSprites[0] = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(Blocks.LAVA.getDefaultState()).getParticleSprite();
        lavaSprites[1] = ModelLoader.LAVA_FLOW.getSprite();
        waterSprites[0] = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(Blocks.WATER.getDefaultState()).getParticleSprite();
        waterSprites[1] = ModelLoader.WATER_FLOW.getSprite();
        Sprite waterOverlaySprite = ModelLoader.WATER_OVERLAY.getSprite();


        boolean bl = fluidState.isIn(FluidTags.LAVA);
        Sprite[] sprites = bl ? lavaSprites : waterSprites;

        BlockState blockState2 = world.getBlockState(pos.offset(Direction.DOWN));
        FluidState fluidState2 = blockState2.getFluidState();
        BlockState blockState3 = world.getBlockState(pos.offset(Direction.UP));
        FluidState fluidState3 = blockState3.getFluidState();
        BlockState blockState4 = world.getBlockState(pos.offset(Direction.NORTH));
        FluidState fluidState4 = blockState4.getFluidState();
        BlockState blockState5 = world.getBlockState(pos.offset(Direction.SOUTH));
        FluidState fluidState5 = blockState5.getFluidState();
        BlockState blockState6 = world.getBlockState(pos.offset(Direction.WEST));
        FluidState fluidState6 = blockState6.getFluidState();
        BlockState blockState7 = world.getBlockState(pos.offset(Direction.EAST));
        FluidState fluidState7 = blockState7.getFluidState();
        boolean bl2 = !isSameFluid(fluidState, fluidState3);
        boolean bl3 = shouldRenderSide(world, pos, fluidState, blockState, Direction.DOWN, fluidState2)
                && !isSideCovered(world, pos, Direction.DOWN, 0.8888889F, blockState2);
        boolean bl4 = shouldRenderSide(world, pos, fluidState, blockState, Direction.NORTH, fluidState4);
        boolean bl5 = shouldRenderSide(world, pos, fluidState, blockState, Direction.SOUTH, fluidState5);
        boolean bl6 = shouldRenderSide(world, pos, fluidState, blockState, Direction.WEST, fluidState6);
        boolean bl7 = shouldRenderSide(world, pos, fluidState, blockState, Direction.EAST, fluidState7);

        if (bl2 || bl3 || bl7 || bl6 || bl4 || bl5) {
            Fluid fluid = fluidState.getFluid();
            float n = getFluidHeight(world, fluid, pos, blockState, fluidState);
            float o;
            float p;
            float q;
            float rr;
            if (n >= 1.0F) {
                o = 1.0F;
                p = 1.0F;
                q = 1.0F;
                rr = 1.0F;
            } else {
                float s = getFluidHeight(world, fluid, pos.north(), blockState4, fluidState4);
                float t = getFluidHeight(world, fluid, pos.south(), blockState5, fluidState5);
                float u = getFluidHeight(world, fluid, pos.east(), blockState7, fluidState7);
                float v = getFluidHeight(world, fluid, pos.west(), blockState6, fluidState6);
                o = calculateFluidHeight(world, fluid, n, s, u, pos.offset(Direction.NORTH).offset(Direction.EAST));
                p = calculateFluidHeight(world, fluid, n, s, v, pos.offset(Direction.NORTH).offset(Direction.WEST));
                q = calculateFluidHeight(world, fluid, n, t, u, pos.offset(Direction.SOUTH).offset(Direction.EAST));
                rr = calculateFluidHeight(world, fluid, n, t, v, pos.offset(Direction.SOUTH).offset(Direction.WEST));
            }

            float s = pos.getX();
            float t = pos.getY();
            float u = pos.getZ();
            float v = 0.001F;
            float w = bl3 ? 0.001F : 0.0F;

            Matrix4f matrix = matrixStack.peek().getPositionMatrix();

            if (bl2 && !isSideCovered(world, pos, Direction.UP, Math.min(Math.min(p, rr), Math.min(q, o)), blockState3)) {
                p -= 0.001F;
                rr -= 0.001F;
                q -= 0.001F;
                o -= 0.001F;
                Vec3d vec3d = fluidState.getVelocity(world, pos);
                float x;
                float z;
                float ab;
                float ad;
                float y;
                float aa;
                float ac;
                float ae;
                if (vec3d.x == 0.0 && vec3d.z == 0.0) {
                    Sprite sprite = sprites[0];
                    x = sprite.getFrameU(0.0F);
                    y = sprite.getFrameV(0.0F);
                    z = x;
                    aa = sprite.getFrameV(1.0F);
                    ab = sprite.getFrameU(1.0F);
                    ac = aa;
                    ad = ab;
                    ae = y;
                } else {
                    Sprite sprite = sprites[1];
                    float af = (float)MathHelper.atan2(vec3d.z, vec3d.x) - (float) (Math.PI / 2);
                    float ag = MathHelper.sin(af) * 0.25F;
                    float ah = MathHelper.cos(af) * 0.25F;
                    float ai = 0.5F;
                    x = sprite.getFrameU(0.5F + (-ah - ag));
                    y = sprite.getFrameV(0.5F + (-ah + ag));
                    z = sprite.getFrameU(0.5F + (-ah + ag));
                    aa = sprite.getFrameV(0.5F + (ah + ag));
                    ab = sprite.getFrameU(0.5F + (ah + ag));
                    ac = sprite.getFrameV(0.5F + (ah - ag));
                    ad = sprite.getFrameU(0.5F + (ah - ag));
                    ae = sprite.getFrameV(0.5F + (-ah - ag));
                }

                float aj = (x + z + ab + ad) / 4.0F;
                float af = (y + aa + ac + ae) / 4.0F;
                float ag = sprites[0].getAnimationFrameDelta();
                x = MathHelper.lerp(ag, x, aj);
                z = MathHelper.lerp(ag, z, aj);
                ab = MathHelper.lerp(ag, ab, aj);
                ad = MathHelper.lerp(ag, ad, aj);
                y = MathHelper.lerp(ag, y, af);
                aa = MathHelper.lerp(ag, aa, af);
                ac = MathHelper.lerp(ag, ac, af);
                ae = MathHelper.lerp(ag, ae, af);
                int ak = getLight(world, pos);
                vertex(vertexConsumer, matrix, s + 0.0F, t + p, u + 0.0F, r, g, b, x, y, ak);
                vertex(vertexConsumer, matrix, s + 0.0F, t + rr, u + 1.0F, r, g, b, z, aa, ak);
                vertex(vertexConsumer, matrix, s + 1.0F, t + q, u + 1.0F, r, g, b, ab, ac, ak);
                vertex(vertexConsumer, matrix, s + 1.0F, t + o, u + 0.0F, r, g, b, ad, ae, ak);
                if (fluidState.canFlowTo(world, pos.up())) {
                    vertex(vertexConsumer, matrix, s + 0.0F, t + p, u + 0.0F, r, g, b, x, y, ak);
                    vertex(vertexConsumer, matrix, s + 1.0F, t + o, u + 0.0F, r, g, b, ad, ae, ak);
                    vertex(vertexConsumer, matrix, s + 1.0F, t + q, u + 1.0F, r, g, b, ab, ac, ak);
                    vertex(vertexConsumer, matrix, s + 0.0F, t + rr, u + 1.0F, r, g, b, z, aa, ak);
                }
            }

            if (bl3) {
                float xx = sprites[0].getMinU();
                float zx = sprites[0].getMaxU();
                float abx = sprites[0].getMinV();
                float adx = sprites[0].getMaxV();
                int an = getLight(world, pos.down());
                vertex(vertexConsumer, matrix, s, t + w, u + 1.0F, r, g, b, xx, adx, an);
                vertex(vertexConsumer, matrix, s, t + w, u, r, g, b, xx, abx, an);
                vertex(vertexConsumer, matrix, s + 1.0F, t + w, u, r, g, b, zx, abx, an);
                vertex(vertexConsumer, matrix, s + 1.0F, t + w, u + 1.0F, r, g, b, zx, adx, an);
            }

            int ao = getLight(world, pos);

            for (Direction direction : Direction.Type.HORIZONTAL) {
                float adx;
                float yx;
                float aax;
                float acx;
                float aex;
                float ap;
                boolean bl8;
                switch (direction) {
                    case NORTH:
                        adx = p;
                        yx = o;
                        aax = s;
                        aex = s + 1.0F;
                        acx = u + 0.001F;
                        ap = u + 0.001F;
                        bl8 = bl4;
                        break;
                    case SOUTH:
                        adx = q;
                        yx = rr;
                        aax = s + 1.0F;
                        aex = s;
                        acx = u + 1.0F - 0.001F;
                        ap = u + 1.0F - 0.001F;
                        bl8 = bl5;
                        break;
                    case WEST:
                        adx = rr;
                        yx = p;
                        aax = s + 0.001F;
                        aex = s + 0.001F;
                        acx = u + 1.0F;
                        ap = u;
                        bl8 = bl6;
                        break;
                    default:
                        adx = o;
                        yx = q;
                        aax = s + 1.0F - 0.001F;
                        aex = s + 1.0F - 0.001F;
                        acx = u;
                        ap = u + 1.0F;
                        bl8 = bl7;
                }

                if (bl8 && !isSideCovered(world, pos, direction, Math.max(adx, yx), world.getBlockState(pos.offset(direction)))) {
                    BlockPos blockPos = pos.offset(direction);
                    Sprite sprite2 = sprites[1];
                    if (!bl) {
                        Block block = world.getBlockState(blockPos).getBlock();
                        if (block instanceof TranslucentBlock || block instanceof LeavesBlock) {
                            sprite2 = waterOverlaySprite;
                        }
                    }

                    float ah = sprite2.getFrameU(0.0F);
                    float ai = sprite2.getFrameU(0.5F);
                    float al = sprite2.getFrameV((1.0F - adx) * 0.5F);
                    float am = sprite2.getFrameV((1.0F - yx) * 0.5F);
                    float aq = sprite2.getFrameV(0.5F);
                    vertex(vertexConsumer, matrix, aax, t + adx, acx, r, g, b, ah, al, ao);
                    vertex(vertexConsumer, matrix, aex, t + yx, ap, r, g, b, ai, am, ao);
                    vertex(vertexConsumer, matrix, aex, t + w, ap, r, g, b, ai, aq, ao);
                    vertex(vertexConsumer, matrix, aax, t + w, acx, r, g, b, ah, aq, ao);
                    if (sprite2 != waterOverlaySprite) {
                        vertex(vertexConsumer, matrix, aax, t + w, acx, r, g, b, ah, aq, ao);
                        vertex(vertexConsumer, matrix, aex, t + w, ap, r, g, b, ai, aq, ao);
                        vertex(vertexConsumer, matrix, aex, t + yx, ap, r, g, b, ai, am, ao);
                        vertex(vertexConsumer, matrix, aax, t + adx, acx, r, g, b, ah, al, ao);
                    }
                }
            }
        }
        matrixStack.pop();
    }

    private static float calculateFluidHeight(BlockRenderView world, Fluid fluid, float originHeight, float northSouthHeight, float eastWestHeight, BlockPos pos) {
        if (!(eastWestHeight >= 1.0F) && !(northSouthHeight >= 1.0F)) {
            float[] fs = new float[2];
            if (eastWestHeight > 0.0F || northSouthHeight > 0.0F) {
                float f = getFluidHeight(world, fluid, pos);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                addHeight(fs, f);
            }

            addHeight(fs, originHeight);
            addHeight(fs, eastWestHeight);
            addHeight(fs, northSouthHeight);
            return fs[0] / fs[1];
        } else {
            return 1.0F;
        }
    }

    private static void addHeight(float[] weightedAverageHeight, float height) {
        if (height >= 0.8F) {
            weightedAverageHeight[0] += height * 10.0F;
            weightedAverageHeight[1] += 10.0F;
        } else if (height >= 0.0F) {
            weightedAverageHeight[0] += height;
            weightedAverageHeight[1]++;
        }
    }

    private static float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return getFluidHeight(world, fluid, pos, blockState, blockState.getFluidState());
    }

    private static float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.matchesType(fluidState.getFluid())) {
            BlockState blockState2 = world.getBlockState(pos.up());
            return fluid.matchesType(blockState2.getFluidState().getFluid()) ? 1.0F : fluidState.getHeight();
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private static void vertex(BufferBuilder vertexConsumer, Matrix4f matrix, float x, float y, float z,
                               float red, float green, float blue, float u, float v, int light) {
        vertexConsumer.vertex(matrix, x, y, z)
                .color(red/255, green/255, blue/255, 1.0F)
                .texture(u, v)
                .light(light)
                .normal(0.0F, 1.0F, 0.0F);
    }

    private static int getLight(BlockRenderView world, BlockPos pos) {
        return LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE;
    }
}