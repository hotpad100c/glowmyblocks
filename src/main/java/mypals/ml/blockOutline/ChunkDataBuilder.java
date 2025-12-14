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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static mypals.ml.blockOutline.OutlineManager.*;
import static mypals.ml.config.GlowModeManager.shouldGlow;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class ChunkDataBuilder {

    private static final AtomicInteger totalBlocksProcessed = new AtomicInteger(0);
    private static final AtomicInteger totalBlocksToProcess = new AtomicInteger(0);
    private static final AtomicLong buildStartTime = new AtomicLong(0);

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
    private static final long REBUILD_DELAY_MS = 20;

    private static final Map<ChunkSectionPos, CompletableFuture<Void>> activeRebuilds =
            new ConcurrentHashMap<>();

    private static final Map<AreaBox, Map<ChunkSectionPos, Long>> pendingRebuilds = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService rebuildScheduler = Executors.newSingleThreadScheduledExecutor();

    private static final ExecutorService WORKER_POOL = Util.getMainWorkerExecutor();

    private static final AtomicBoolean rebuildCheckScheduled = new AtomicBoolean(false);

    private static final AtomicReference<CompletableFuture<Void>> currentBuildTask = new AtomicReference<>();

    public static void buildMeshesAsync(RenderTickCounter counter) {
        GlowMyBlocks.needRebuildOutlineMesh = false;
        CompletableFuture<Void> oldTask = currentBuildTask.get();
        if (oldTask != null) {
            oldTask.cancel(true);
        }

        World world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }

        float delta = counter.getTickDelta(false);
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        List<AreaBox> areasSnapshot = new ArrayList<>(selectedAreas);
        CompletableFuture<Void> newTask = CompletableFuture
                .supplyAsync(() -> collectAllAreaData(world, areasSnapshot), WORKER_POOL)
                .thenApplyAsync(areaDataMap -> buildVertexData(areaDataMap, delta, camera), WORKER_POOL)
                .thenAcceptAsync(ChunkDataBuilder::uploadToGPU, MinecraftClient.getInstance())
                .whenComplete((result, error) -> {
                    if (error != null && !(error.getCause() instanceof CancellationException)) {
                        GlowMyBlocks.LOGGER.error("Failed to build mesh:", error);
                    } else {
                        GlowMyBlocks.needRebuildOutlineMesh = false;
                    }
                });

        currentBuildTask.set(newTask);
    }

    private static Map<AreaBox, List<ChunkBuildResult>> collectAllAreaData(
            World world, List<AreaBox> areas) {

        int totalBlocks = areas.stream()
                .mapToInt(area -> {
                    int dx = area.maxPos.getX() - area.minPos.getX() + 1;
                    int dy = area.maxPos.getY() - area.minPos.getY() + 1;
                    int dz = area.maxPos.getZ() - area.minPos.getZ() + 1;
                    return dx * dy * dz;
                })
                .sum();

        totalBlocksToProcess.set(totalBlocks);
        totalBlocksProcessed.set(0);
        buildStartTime.set(System.currentTimeMillis());

        Map<AreaBox, List<ChunkBuildResult>> result = new ConcurrentHashMap<>();

        areas.parallelStream().forEach(area -> {
            Map<ChunkSectionPos, ChunkBuildResult> sectionMap = new ConcurrentHashMap<>();

            for (int x = area.minPos.getX(); x <= area.maxPos.getX(); x++) {
                for (int y = area.minPos.getY(); y <= area.maxPos.getY(); y++) {
                    for (int z = area.minPos.getZ(); z <= area.maxPos.getZ(); z++) {

                        totalBlocksProcessed.incrementAndGet();

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
    public record BuildProgress(
            int processedBlocks,
            int totalBlocks,
            float percentage,
            int blocksPerSecond,
            int pendingChunks,
            long elapsedMs
    ) {
        public boolean isBuilding() {
            return processedBlocks < totalBlocks && totalBlocks > 0;
        }
        public String getStatusText() {
            if (!isBuilding() && pendingChunks == 0) {
                return "Empty";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Building: %.1f%% ", percentage));
            sb.append(String.format("(%d/%d blocks) ", processedBlocks, totalBlocks));
            sb.append(String.format("[%d blocks/s] ", blocksPerSecond));

            if (pendingChunks > 0) {
                sb.append(String.format("Pending: %d Sections", pendingChunks));
            }

            return sb.toString();
        }
    }
    public static boolean isBuilding() {
        return totalBlocksToProcess.get() > 0
                && totalBlocksProcessed.get() < totalBlocksToProcess.get();
    }
    public static BuildProgress getBuildProgress() {
        int processed = totalBlocksProcessed.get();
        int total = totalBlocksToProcess.get();
        long elapsed = System.currentTimeMillis() - buildStartTime.get();

        float percentage = total > 0 ? (processed * 100.0f / total) : 0;
        int blocksPerSecond = elapsed > 0 ? (int)(processed * 1000L / elapsed) : 0;

        int pendingRebuilds = ChunkDataBuilder.pendingRebuilds.values().stream()
                .mapToInt(Map::size)
                .sum();

        return new BuildProgress(
                processed,
                total,
                percentage,
                blocksPerSecond,
                pendingRebuilds,
                elapsed
        );
    }
    private static Map<AreaBox, Map<ChunkSectionPos, ChunkBufferData>> buildVertexData(
            Map<AreaBox, List<ChunkBuildResult>> areaDataMap,
            float delta,
            Camera camera) {

        Map<AreaBox, Map<ChunkSectionPos, ChunkBufferData>> result = new ConcurrentHashMap<>();

        areaDataMap.entrySet().parallelStream().forEach(entry -> {
            AreaBox area = entry.getKey();
            List<ChunkBuildResult> chunks = entry.getValue();

            Map<ChunkSectionPos, ChunkBufferData> chunkBuffers = new ConcurrentHashMap<>();

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
                    chunkBuffers.put(chunk.sectionPos,
                            new ChunkBufferData(builtBuffer, chunk.blockEntities));
                }
            });

            result.put(area, chunkBuffers);
        });

        return result;
    }

    private static void uploadToGPU(Map<AreaBox, Map<ChunkSectionPos, ChunkBufferData>> areaDataMap) {
        for (AreaRenderData data : areaVbos.values()) {
            for (ChunkRenderData chunk : data.sectionData.values()) {
                if (chunk.vbo != null) chunk.vbo.close();
            }
        }
        areaVbos.clear();

        for (Map.Entry<AreaBox, Map<ChunkSectionPos, ChunkBufferData>> entry : areaDataMap.entrySet()) {
            AreaBox area = entry.getKey();
            Map<ChunkSectionPos, ChunkBufferData> chunkBuffers = entry.getValue();

            AreaRenderData renderData = new AreaRenderData();

            for (Map.Entry<ChunkSectionPos, ChunkBufferData> chunkEntry : chunkBuffers.entrySet()) {
                ChunkSectionPos sectionPos = chunkEntry.getKey();
                ChunkBufferData bufferData = chunkEntry.getValue();

                ChunkRenderData chunkData = new ChunkRenderData();

                VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vbo.bind();
                vbo.upload(bufferData.buffer());
                VertexBuffer.unbind();

                chunkData.vbo = vbo;
                chunkData.blockEntities = bufferData.blockEntities();

                renderData.sectionData.put(sectionPos, chunkData);
            }

            areaVbos.put(area, renderData);
        }
    }

    public static void rebuildChunkSectionAsync(AreaBox area, ChunkSectionPos sectionPos) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;
        CompletableFuture<Void> existingTask = activeRebuilds.get(sectionPos);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(true);
        }

        CompletableFuture<Void> rebuildTask = CompletableFuture
                .supplyAsync(() -> {
                    List<BlockRenderData> blocks = new ArrayList<>();
                    Map<BlockPos, Color> blockEntities = new ConcurrentHashMap<>();

                    int startX = Math.max(sectionPos.getMinX(), area.minPos.getX());
                    int endX = Math.min(sectionPos.getMaxX(), area.maxPos.getX());
                    int startY = Math.max(sectionPos.getMinY(), area.minPos.getY());
                    int endY = Math.min(sectionPos.getMaxY(), area.maxPos.getY());
                    int startZ = Math.max(sectionPos.getMinZ(), area.minPos.getZ());
                    int endZ = Math.min(sectionPos.getMaxZ(), area.maxPos.getZ());

                    for (int x = startX; x <= endX; x++) {
                        for (int y = startY; y <= endY; y++) {
                            for (int z = startZ; z <= endZ; z++) {

                                if (Thread.currentThread().isInterrupted()) {
                                    throw new CancellationException("Task cancelled during data collection");
                                }

                                BlockPos blockPos = new BlockPos(x, y, z);
                                BlockState state = world.getBlockState(blockPos);

                                if (state.isAir() || !shouldGlow(blockPos, state, area)) continue;

                                if (checkVisibleFaces(world, blockPos, area)) {
                                    blocks.add(new BlockRenderData(blockPos, state, area.color, true));
                                }

                                if (state.getBlock() instanceof BlockWithEntity) {
                                    BlockEntity be = world.getBlockEntity(blockPos);
                                    if (be != null) {
                                        blockEntities.put(be.getPos(), area.color);
                                    }
                                }
                            }
                        }
                    }
                    return new ChunkBuildResult(sectionPos, blocks, blockEntities);
                }, WORKER_POOL)

                .thenApplyAsync(chunkResult -> {
                    if (chunkResult.blocks.isEmpty()) return null;

                    MinecraftClient mc = MinecraftClient.getInstance();
                    Camera camera = mc.gameRenderer.getCamera();
                    float delta = mc.getRenderTickCounter().getTickDelta(false);

                    BufferBuilder buffer = new BufferBuilder(new BufferAllocator(2048),
                            VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                    MatrixStack stack = new MatrixStack();

                    for (BlockRenderData blockData : chunkResult.blocks) {
                        stack.loadIdentity();
                        stack.translate(blockData.pos.getX(), blockData.pos.getY(), blockData.pos.getZ());
                        renderBlockOutline(
                                new AbstractMap.SimpleEntry<>(blockData.pos, blockData.state),
                                delta, camera, stack, blockData.color, Random.create(), buffer
                        );
                    }

                    BuiltBuffer builtBuffer = buffer.endNullable();
                    return new ChunkBufferData(builtBuffer, chunkResult.blockEntities);
                }, WORKER_POOL)

                .thenAcceptAsync(bufferData -> {
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

                    if (bufferData != null && bufferData.buffer() != null) {
                        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                        vbo.bind();
                        vbo.upload(bufferData.buffer());
                        VertexBuffer.unbind();
                        chunkData.vbo = vbo;
                        chunkData.blockEntities = bufferData.blockEntities();
                    } else {
                        chunkData.vbo = null;
                        chunkData.blockEntities = new ConcurrentHashMap<>();
                    }
                }, MinecraftClient.getInstance())

                .whenComplete((result, error) -> {
                    activeRebuilds.remove(sectionPos);
                    if (error != null && !(error.getCause() instanceof CancellationException)) {
                        GlowMyBlocks.LOGGER.error("Failed to build mesh:", error);
                    }
                });
        activeRebuilds.put(sectionPos, rebuildTask);
    }

    public static void onBlockStateChange(BlockPos blockPos) {
        long currentTime = System.currentTimeMillis();
        ChunkSectionPos changedSectionPos = ChunkSectionPos.from(blockPos);

        for (AreaBox area : selectedAreas) {
            if (!isBlockInsideArea(blockPos, area)) {
                continue;
            }

            Map<ChunkSectionPos, Long> areaRebuilds = pendingRebuilds.computeIfAbsent(
                    area, k -> new ConcurrentHashMap<>()
            );
            areaRebuilds.put(changedSectionPos, currentTime + REBUILD_DELAY_MS);

            for (Direction direction : Direction.values()) {
                if (isOnChunkSectionBoundary(blockPos, direction)) {
                    BlockPos adjacentPos = blockPos.offset(direction);

                    if (isBlockInsideArea(adjacentPos, area)) {
                        ChunkSectionPos adjacentSectionPos = ChunkSectionPos.from(adjacentPos);

                        if (!adjacentSectionPos.equals(changedSectionPos)) {
                            areaRebuilds.put(adjacentSectionPos, currentTime + REBUILD_DELAY_MS);
                        }
                    }
                }
            }
        }

        scheduleRebuildCheck();
    }
    private static boolean isOnChunkSectionBoundary(BlockPos pos, Direction direction) {
        return switch (direction) {
            case DOWN -> (pos.getY() & 15) == 0;
            case UP -> (pos.getY() & 15) == 15;
            case NORTH -> (pos.getZ() & 15) == 0;
            case SOUTH -> (pos.getZ() & 15) == 15;
            case WEST -> (pos.getX() & 15) == 0;
            case EAST -> (pos.getX() & 15) == 15;
        };
    }

    private static boolean isInSameChunkSection(BlockPos pos1, BlockPos pos2) {
        return ChunkSectionPos.from(pos1).equals(ChunkSectionPos.from(pos2));
    }

    private static void scheduleRebuildCheck() {
        if (rebuildCheckScheduled.compareAndSet(false, true)) {
            rebuildScheduler.schedule(() -> {
                processScheduledRebuilds();
                rebuildCheckScheduled.set(false);
            }, REBUILD_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static void processScheduledRebuilds() {
        long currentTime = System.currentTimeMillis();

        pendingRebuilds.forEach((area, chunks) -> {
            chunks.entrySet().removeIf(entry -> {
                ChunkSectionPos sectionPos = entry.getKey();
                long scheduledTime = entry.getValue();

                if (currentTime >= scheduledTime) {
                    rebuildChunkSectionAsync(area, sectionPos);
                    return true;
                }
                return false;
            });

            if (chunks.isEmpty()) {
                pendingRebuilds.remove(area);
            }
        });

        if (!pendingRebuilds.isEmpty()) {
            scheduleRebuildCheck();
        }
    }

    public static void shutdown() {
        CompletableFuture<Void> task = currentBuildTask.get();
        if (task != null) {
            task.cancel(true);
        }

        rebuildScheduler.shutdownNow();
        pendingRebuilds.clear();
    }
}
