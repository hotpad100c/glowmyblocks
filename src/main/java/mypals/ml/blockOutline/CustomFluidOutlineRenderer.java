package mypals.ml.blockOutline;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
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
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class CustomFluidOutlineRenderer {

    public static void render(BlockRenderView world, BlockPos pos, BufferBuilder vertexConsumer,
                              BlockState blockState, FluidState fluidState, MatrixStack matrixStack,
                              float r, float g, float b) {
        matrixStack.push();
        matrixStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());

        BlockState downState = world.getBlockState(pos.offset(Direction.DOWN));
        FluidState downFluidState = downState.getFluidState();
        BlockState upState = world.getBlockState(pos.offset(Direction.UP));
        FluidState upFluidState = upState.getFluidState();
        BlockState northState = world.getBlockState(pos.offset(Direction.NORTH));
        FluidState northFluidState = northState.getFluidState();
        BlockState southState = world.getBlockState(pos.offset(Direction.SOUTH));
        FluidState southFluidState = southState.getFluidState();
        BlockState westState = world.getBlockState(pos.offset(Direction.WEST));
        FluidState westFluidState = westState.getFluidState();
        BlockState eastState = world.getBlockState(pos.offset(Direction.EAST));
        FluidState eastFluidState = eastState.getFluidState();

        boolean renderUp = !isSameFluid(fluidState, upFluidState);
        boolean renderDown = shouldRenderSide(world, pos, fluidState, blockState, Direction.DOWN, downFluidState)
                && !isSideCovered(world, pos, Direction.DOWN, 0.8888889F, downState);
        boolean renderNorth = shouldRenderSide(world, pos, fluidState, blockState, Direction.NORTH, northFluidState);
        boolean renderSouth = shouldRenderSide(world, pos, fluidState, blockState, Direction.SOUTH, southFluidState);
        boolean renderWest = shouldRenderSide(world, pos, fluidState, blockState, Direction.WEST, westFluidState);
        boolean renderEast = shouldRenderSide(world, pos, fluidState, blockState, Direction.EAST, eastFluidState);

        if (renderUp || renderDown || renderEast || renderWest || renderNorth || renderSouth) {
            Fluid fluid = fluidState.getFluid();
            float fluidHeight = getFluidHeight(world, fluid, pos, blockState, fluidState);
            float hNW, hSW, hSE, hNE;

            if (fluidHeight >= 1.0F) {
                hNW = hSW = hSE = hNE = 1.0F;
            } else {
                float northHeight = getFluidHeight(world, fluid, pos.north(), northState, northFluidState);
                float southHeight = getFluidHeight(world, fluid, pos.south(), southState, southFluidState);
                float eastHeight = getFluidHeight(world, fluid, pos.east(), eastState, eastFluidState);
                float westHeight = getFluidHeight(world, fluid, pos.west(), westState, westFluidState);
                hNW = calculateFluidHeight(fluidHeight, northHeight, westHeight);
                hNE = calculateFluidHeight(fluidHeight, northHeight, eastHeight);
                hSE = calculateFluidHeight(fluidHeight, southHeight, eastHeight);
                hSW = calculateFluidHeight(fluidHeight, southHeight, westHeight);
            }

            float x = pos.getX();
            float y = pos.getY();
            float z = pos.getZ();
            float offset = 0.001F;
            float downOffset = renderDown ? 0.001F : 0.0F;

            if (renderUp && !isSideCovered(world, pos, Direction.UP, Math.min(Math.min(hNW, hSW), Math.min(hSE, hNE)), upState)) {
                hNW -= 0.001F;
                hSW -= 0.001F;
                hSE -= 0.001F;
                hNE -= 0.001F;
                vertex(vertexConsumer, matrixStack, x, y + hNW, z, r, g, b);
                vertex(vertexConsumer, matrixStack, x, y + hSW, z + 1.0F, r, g, b);
                vertex(vertexConsumer, matrixStack, x + 1.0F, y + hSE, z + 1.0F, r, g, b);
                vertex(vertexConsumer, matrixStack, x + 1.0F, y + hNE, z, r, g, b);

                if (fluidState.canFlowTo(world, pos.up())) {
                    vertex(vertexConsumer, matrixStack, x, y + hNW, z, r, g, b);
                    vertex(vertexConsumer, matrixStack, x + 1.0F, y + hNE, z, r, g, b);
                    vertex(vertexConsumer, matrixStack, x + 1.0F, y + hSE, z + 1.0F, r, g, b);
                    vertex(vertexConsumer, matrixStack, x, y + hSW, z + 1.0F, r, g, b);
                }
            }

            if (renderDown) {
                vertex(vertexConsumer, matrixStack, x, y + downOffset, z + 1.0F, r, g, b);
                vertex(vertexConsumer, matrixStack, x, y + downOffset, z, r, g, b);
                vertex(vertexConsumer, matrixStack, x + 1.0F, y + downOffset, z, r, g, b);
                vertex(vertexConsumer, matrixStack, x + 1.0F, y + downOffset, z + 1.0F, r, g, b);
            }
            for (Direction direction : Direction.Type.HORIZONTAL) {
                float h1, h2, x1, x2, z1, z2;
                boolean shouldRenderSide;
                switch (direction) {
                    case NORTH:
                        h1 = hNW; h2 = hNE;
                        x1 = x; x2 = x + 1.0F;
                        z1 = z + offset; z2 = z + offset;
                        shouldRenderSide = renderNorth;
                        break;
                    case SOUTH:
                        h1 = hSE; h2 = hSW;
                        x1 = x + 1.0F; x2 = x;
                        z1 = z + 1.0F - offset; z2 = z + 1.0F - offset;
                        shouldRenderSide = renderSouth;
                        break;
                    case WEST:
                        h1 = hSW; h2 = hNW;
                        x1 = x + offset; x2 = x + offset;
                        z1 = z + 1.0F; z2 = z;
                        shouldRenderSide = renderWest;
                        break;
                    default: // EAST
                        h1 = hNE; h2 = hSE;
                        x1 = x + 1.0F - offset; x2 = x + 1.0F - offset;
                        z1 = z; z2 = z + 1.0F;
                        shouldRenderSide = renderEast;
                        break;
                }

                if (shouldRenderSide && !isSideCovered(world, pos, direction, Math.max(h1, h2), world.getBlockState(pos.offset(direction)))) {
                    vertex(vertexConsumer, matrixStack, x1, y + h1, z1, r, g, b);
                    vertex(vertexConsumer, matrixStack, x2, y + h2, z2, r, g, b);
                    vertex(vertexConsumer, matrixStack, x2, y + downOffset, z2, r, g, b);
                    vertex(vertexConsumer, matrixStack, x1, y + downOffset, z1, r, g, b);
                }
            }
        }
        matrixStack.pop();
    }

    private static void vertex(BufferBuilder vertexConsumer, MatrixStack matrixStack, float x, float y, float z, float r, float g, float b) {
        vertexConsumer.vertex(matrixStack.peek().getPositionMatrix(), x, y, z).color((float) r /255, (float) g /255, (float) b /255, 1.0F);
    }

    private static float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.matchesType(fluidState.getFluid())) {
            return fluid.matchesType(world.getBlockState(pos.up()).getFluidState().getFluid()) ? 1.0F : fluidState.getHeight();
        }
        return !blockState.isSolid() ? 0.0F : -1.0F;
    }

    private static float calculateFluidHeight(float originHeight, float height1, float height2) {
        if (height1 >= 1.0F || height2 >= 1.0F) return 1.0F;
        float sum = 0.0F, count = 0.0F;
        if (height1 > 0.0F) { sum += height1; count++; }
        if (height2 > 0.0F) { sum += height2; count++; }
        if (originHeight > 0.0F) { sum += originHeight; count++; }
        return count > 0.0F ? sum / count : originHeight;
    }

    private static boolean shouldRenderSide(BlockRenderView world, BlockPos pos, FluidState fluidState, BlockState blockState, Direction direction, FluidState neighborFluidState) {
        return !isSameFluid(fluidState, neighborFluidState) ||
                !blockState.isSideSolidFullSquare(world, pos, direction) ||
                !OutlineManager.blockToRenderer.containsKey(pos.offset(direction));
    }

    private static boolean isSameFluid(FluidState fluidState, FluidState other) {
        return fluidState.getFluid().matchesType(other.getFluid());
    }

    private static boolean isSideCovered(BlockRenderView world, BlockPos pos, Direction direction, float height, BlockState neighborState) {
        return neighborState.isSideSolidFullSquare(world, pos.offset(direction), direction.getOpposite()) && height >= 1.0F;
    }
}