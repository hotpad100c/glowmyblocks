package mypals.ml.blockOutline;

import mypals.ml.GlowMyBlocks;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.awt.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static mypals.ml.blockOutline.OutlineManager.*;
import static mypals.ml.config.GlowModeManager.shouldGlow;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class ChunkDataBuilder {
    private record BlockRenderData(
            BlockPos pos,
            BlockState state,
            Color color,
            boolean hasVisibleFace
    ) {}

    private record ChunkBuildResult(
            ChunkSectionPos sectionPos,
            List<BlockRenderData> blocks,
            Map<BlockPos, Color> blockEntities
    ) {}

    private static final ExecutorService WORKER_POOL = Util.getMainWorkerExecutor();

    private static CompletableFuture<Void> currentBuildTask = null;

    private static final AtomicBoolean isBuilding = new AtomicBoolean(false);

    public static void buildMeshesAsync(RenderTickCounter counter) {
        if (!isBuilding.compareAndSet(false, true)) {
            return;
        }

        if (currentBuildTask != null && !currentBuildTask.isDone()) {
            currentBuildTask.cancel(false);
        }

        World world = MinecraftClient.getInstance().world;
        if (world == null) {
            isBuilding.set(false);
            return;
        }

        float delta = counter.getTickDelta(false);
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        List<AreaBox> areasSnapshot = new ArrayList<>(selectedAreas);

        currentBuildTask = CompletableFuture
                .supplyAsync(() -> collectAllAreaData(world, areasSnapshot), WORKER_POOL)

                .thenApplyAsync(areaDataMap -> buildVertexData(areaDataMap, delta, camera), WORKER_POOL)

                .thenAcceptAsync(ChunkDataBuilder::uploadToGPU, MinecraftClient.getInstance())

                .whenComplete((result, error) -> {
                    isBuilding.set(false);
                    if (error != null) {
                        GlowMyBlocks.LOGGER.error("Failed to build mesh:", error);
                    } else {
                        GlowMyBlocks.needRebuildOutlineMesh = false;
                    }
                });
    }

    private static Map<AreaBox, List<ChunkBuildResult>> collectAllAreaData(
            World world, List<AreaBox> areas) {

        Map<AreaBox, List<ChunkBuildResult>> result = new ConcurrentHashMap<>();

        areas.parallelStream().forEach(area -> {
            Map<ChunkSectionPos, ChunkBuildResult> sectionMap = new ConcurrentHashMap<>();

            for (int x = area.minPos.getX(); x <= area.maxPos.getX(); x++) {
                for (int y = area.minPos.getY(); y <= area.maxPos.getY(); y++) {
                    for (int z = area.minPos.getZ(); z <= area.maxPos.getZ(); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(blockPos);

                        if (state.isAir()) continue;

                        ChunkSectionPos sectionPos = ChunkSectionPos.from(blockPos);
                        ChunkBuildResult chunkResult = sectionMap.computeIfAbsent(
                                sectionPos,
                                k -> new ChunkBuildResult(k, new CopyOnWriteArrayList<>(), new ConcurrentHashMap<>())
                        );

                        if (shouldGlow(blockPos, state, area)) {
                            boolean hasVisibleFace = checkVisibleFaces(world, blockPos, area);

                            if (hasVisibleFace) {
                                chunkResult.blocks.add(new BlockRenderData(
                                        blockPos, state, area.color, true
                                ));
                            }

                            if (state.getBlock() instanceof BlockWithEntity) {
                                BlockEntity be = world.getBlockEntity(blockPos);
                                if (be != null) {
                                    chunkResult.blockEntities.put(be.getPos(), area.color);
                                }
                            }
                        }
                    }
                }
            }

            result.put(area, new ArrayList<>(sectionMap.values()));
        });

        return result;
    }

    private static boolean checkVisibleFaces(World world, BlockPos blockPos, AreaBox area) {
        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = blockPos.offset(direction);

            boolean isSideBlocked = isBlockInsideArea(offsetPos, area)
                    && world.getBlockState(offsetPos).isFullCube(world, offsetPos);

            if (!isSideBlocked) {
                return true;
            }
        }
        return false;
    }
    private static Map<AreaBox, Map<ChunkSectionPos, BuiltBuffer>> buildVertexData(
            Map<AreaBox, List<ChunkBuildResult>> areaDataMap,
            float delta,
            Camera camera) {

        Map<AreaBox, Map<ChunkSectionPos, BuiltBuffer>> result = new ConcurrentHashMap<>();

        areaDataMap.entrySet().parallelStream().forEach(entry -> {
            AreaBox area = entry.getKey();
            List<ChunkBuildResult> chunks = entry.getValue();

            Map<ChunkSectionPos, BuiltBuffer> chunkBuffers = new ConcurrentHashMap<>();

            chunks.parallelStream().forEach(chunk -> {
                if (chunk.blocks.isEmpty()) return;

                BufferBuilder buffer = new BufferBuilder(new BufferAllocator(2048),
                        VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                MatrixStack stack = new MatrixStack();

                for (BlockRenderData blockData : chunk.blocks) {
                    stack.loadIdentity();
                    stack.translate(
                            blockData.pos.getX(),
                            blockData.pos.getY(),
                            blockData.pos.getZ()
                    );

                    renderBlockOutline(
                            new AbstractMap.SimpleEntry<>(blockData.pos, blockData.state),
                            delta, camera, stack, blockData.color(), Random.create(), buffer
                    );
                }

                BuiltBuffer builtBuffer = buffer.endNullable();
                if (builtBuffer != null) {
                    chunkBuffers.put(chunk.sectionPos, builtBuffer);
                }
            });

            result.put(area, chunkBuffers);
        });

        return result;
    }

    private static void uploadToGPU(Map<AreaBox, Map<ChunkSectionPos, BuiltBuffer>> areaDataMap) {
        for (AreaRenderData data : areaVbos.values()) {
            for (ChunkRenderData chunk : data.sectionData.values()) {
                if (chunk.vbo != null) chunk.vbo.close();
            }
        }
        areaVbos.clear();

        for (Map.Entry<AreaBox, Map<ChunkSectionPos, BuiltBuffer>> entry : areaDataMap.entrySet()) {
            AreaBox area = entry.getKey();
            Map<ChunkSectionPos, BuiltBuffer> chunkBuffers = entry.getValue();

            AreaRenderData renderData = new AreaRenderData();

            for (Map.Entry<ChunkSectionPos, BuiltBuffer> chunkEntry : chunkBuffers.entrySet()) {
                ChunkSectionPos sectionPos = chunkEntry.getKey();
                BuiltBuffer builtBuffer = chunkEntry.getValue();

                ChunkRenderData chunkData = new ChunkRenderData();

                VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vbo.bind();
                vbo.upload(builtBuffer);
                VertexBuffer.unbind();

                chunkData.vbo = vbo;
                renderData.sectionData.put(sectionPos, chunkData);
            }

            areaVbos.put(area, renderData);
        }
    }

    public static void rebuildChunkSectionAsync(AreaBox area, ChunkSectionPos sectionPos) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        CompletableFuture
                .supplyAsync(() -> {
                    List<BlockRenderData> blocks = new ArrayList<>();

                    int startX = Math.max(sectionPos.getMinX(), area.minPos.getX());
                    int endX = Math.min(sectionPos.getMaxX(), area.maxPos.getX());
                    int startY = Math.max(sectionPos.getMinY(), area.minPos.getY());
                    int endY = Math.min(sectionPos.getMaxY(), area.maxPos.getY());
                    int startZ = Math.max(sectionPos.getMinZ(), area.minPos.getZ());
                    int endZ = Math.min(sectionPos.getMaxZ(), area.maxPos.getZ());

                    for (int x = startX; x <= endX; x++) {
                        for (int y = startY; y <= endY; y++) {
                            for (int z = startZ; z <= endZ; z++) {
                                BlockPos blockPos = new BlockPos(x, y, z);
                                BlockState state = world.getBlockState(blockPos);

                                if (state.isAir() || !shouldGlow(blockPos, state, area)) continue;

                                if (checkVisibleFaces(world, blockPos, area)) {
                                    blocks.add(new BlockRenderData(blockPos, state, area.color, true));
                                }
                            }
                        }
                    }
                    return blocks;
                }, WORKER_POOL)

                .thenApplyAsync(blocks -> {
                    if (blocks.isEmpty()) return null;

                    MinecraftClient mc = MinecraftClient.getInstance();
                    Camera camera = mc.gameRenderer.getCamera();
                    float delta = mc.getRenderTickCounter().getTickDelta(false);

                    BufferBuilder buffer = new BufferBuilder(new BufferAllocator(2048),VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                    MatrixStack stack = new MatrixStack();

                    for (BlockRenderData blockData : blocks) {
                        stack.loadIdentity();
                        stack.translate(blockData.pos.getX(), blockData.pos.getY(), blockData.pos.getZ());
                        renderBlockOutline(
                                new AbstractMap.SimpleEntry<>(blockData.pos, blockData.state),
                                delta, camera, stack, blockData.color,Random.create(), buffer
                        );
                    }

                    return buffer.endNullable();
                }, WORKER_POOL)

                .thenAcceptAsync(builtBuffer -> {
                    AreaRenderData renderData = areaVbos.get(area);
                    if (renderData == null) return;

                    ChunkRenderData chunkData = renderData.sectionData.get(sectionPos);
                    if (chunkData == null) {
                        chunkData = new ChunkRenderData();
                        renderData.sectionData.put(sectionPos, chunkData);
                    }

                    if (chunkData.vbo != null) {
                        chunkData.vbo.close();
                    }

                    if (builtBuffer != null) {
                        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                        vbo.bind();
                        vbo.upload(builtBuffer);
                        VertexBuffer.unbind();
                        chunkData.vbo = vbo;
                    } else {
                        chunkData.vbo = null;
                    }
                }, MinecraftClient.getInstance())

                .exceptionally(error -> {
                    GlowMyBlocks.LOGGER.error("Failed to build mesh:", error);
                    return null;
                });
    }

    public static void onBlockStateChange(BlockPos blockPos) {
        for (AreaBox area : selectedAreas) {

            for(Direction direction : Direction.values()) {
                BlockPos adjacentPos = blockPos.offset(direction);
                if (isBlockInsideArea(adjacentPos, area)) {
                    ChunkSectionPos adjacentSectionPos = ChunkSectionPos.from(adjacentPos);
                    rebuildChunkSectionAsync(area, adjacentSectionPos);
                }
            }
        }
    }

    public static void shutdown() {
        if (currentBuildTask != null) {
            currentBuildTask.cancel(true);
        }
    }
}
