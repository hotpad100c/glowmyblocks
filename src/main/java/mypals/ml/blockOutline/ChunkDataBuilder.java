package mypals.ml.blockOutline;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import mypals.ml.GlowMyBlocks;
import mypals.ml.blockOutline.OutlineManager.AreaRenderData;
import mypals.ml.blockOutline.OutlineManager.ChunkBufferData;
import mypals.ml.blockOutline.OutlineManager.ChunkRenderData;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
            /** Bit per {@link Direction#ordinal()}; a set bit means that face is not occluded. */
            int visibleFaces
    ) {}

    private record ChunkBuildResult(
            SectionPos sectionPos,
            List<BlockRenderData> blocks,
            Map<BlockPos, Color> blockEntities
    ) {}
    private static final long REBUILD_DELAY_MS = 20;

    /** Identifies an in-flight rebuild. Keyed by area too: two areas can share a section, and
     *  keying on the section alone made each one cancel the other's rebuild. */
    private record RebuildKey(AreaBox area, SectionPos sectionPos) {}

    private static final Map<RebuildKey, CompletableFuture<Void>> activeRebuilds =
            new ConcurrentHashMap<>();

    private static final Map<AreaBox, Map<SectionPos, Long>> pendingRebuilds = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService rebuildScheduler = Executors.newSingleThreadScheduledExecutor();

    private static final ExecutorService WORKER_POOL = Util.backgroundExecutor().service();

    private static final AtomicBoolean rebuildCheckScheduled = new AtomicBoolean(false);

    private static final AtomicReference<CompletableFuture<Void>> currentBuildTask = new AtomicReference<>();

    public static void buildMeshesAsync(DeltaTracker counter) {
        GlowMyBlocks.needRebuildOutlineMesh = false;
        CompletableFuture<Void> oldTask = currentBuildTask.get();
        if (oldTask != null) {
            oldTask.cancel(true);
        }

        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        float delta = counter.getGameTimeDeltaPartialTick(false);
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        List<AreaBox> areasSnapshot = new ArrayList<>(selectedAreas);
        CompletableFuture<Void> newTask = CompletableFuture
                .supplyAsync(() -> collectAllAreaData(world, areasSnapshot), WORKER_POOL)
                .thenApplyAsync(areaDataMap -> buildVertexData(areaDataMap, delta, camera), WORKER_POOL)
                .thenAcceptAsync(ChunkDataBuilder::uploadToGPU, Minecraft.getInstance())
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
            Level world, List<AreaBox> areas) {

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
            Map<SectionPos, ChunkBuildResult> sectionMap = new ConcurrentHashMap<>();

            for (int x = area.minPos.getX(); x <= area.maxPos.getX(); x++) {
                for (int y = area.minPos.getY(); y <= area.maxPos.getY(); y++) {
                    for (int z = area.minPos.getZ(); z <= area.maxPos.getZ(); z++) {

                        totalBlocksProcessed.incrementAndGet();

                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(blockPos);

                        if (state.isAir()) continue;

                        SectionPos sectionPos = SectionPos.of(blockPos);
                        ChunkBuildResult chunkResult = sectionMap.computeIfAbsent(
                                sectionPos,
                                k -> new ChunkBuildResult(k, new CopyOnWriteArrayList<>(), new ConcurrentHashMap<>())
                        );

                        if (shouldGlow(blockPos, state, area)) {
                            int visibleFaces = checkVisibleFaces(world, blockPos, area);

                            if (visibleFaces != 0) {
                                chunkResult.blocks.add(new BlockRenderData(
                                        blockPos, state, area.color, visibleFaces
                                ));
                            }

                            if (state.getBlock() instanceof BaseEntityBlock) {
                                BlockEntity be = world.getBlockEntity(blockPos);
                                if (be != null) {
                                    chunkResult.blockEntities.put(be.getBlockPos(), area.color);
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

    /**
     * Returns a bitmask of the faces of {@code blockPos} that are not hidden by another block of
     * the same selection, one bit per {@link Direction#ordinal()}. Zero means the block is fully
     * enclosed and contributes no geometry at all.
     *
     * <p>Only faces pointing at a neighbour that is itself selected and a full block get culled --
     * a face pointing out of the selection stays, so the selection keeps a closed shell.
     */
    private static int checkVisibleFaces(Level world, BlockPos blockPos, AreaBox area) {
        int visible = 0;
        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = blockPos.relative(direction);

            // The neighbour must actually be drawn by us to hide this face. Testing only "inside the
            // area and a full block" would punch holes in the shell under the selective glow modes,
            // where a neighbour can be inside the area yet filtered out and never rendered.
            boolean isSideBlocked = false;
            if (isBlockInsideArea(offsetPos, area)) {
                BlockState neighbour = world.getBlockState(offsetPos);
                isSideBlocked = neighbour.isCollisionShapeFullBlock(world, offsetPos)
                        && shouldGlow(offsetPos, neighbour, area);
            }

            if (!isSideBlocked) {
                visible |= 1 << direction.ordinal();
            }
        }
        return visible;
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
    private static Map<AreaBox, Map<SectionPos, ChunkBufferData>> buildVertexData(
            Map<AreaBox, List<ChunkBuildResult>> areaDataMap,
            float delta,
            Camera camera) {

        Map<AreaBox, Map<SectionPos, ChunkBufferData>> result = new ConcurrentHashMap<>();

        areaDataMap.entrySet().parallelStream().forEach(entry -> {
            AreaBox area = entry.getKey();
            List<ChunkBuildResult> chunks = entry.getValue();

            Map<SectionPos, ChunkBufferData> chunkBuffers = new ConcurrentHashMap<>();

            chunks.parallelStream().forEach(chunk -> {
                if (chunk.blocks.isEmpty()) return;

                RenderType layer = OutlineManager.outlineLayer();
                BufferBuilder buffer = new BufferBuilder(new ByteBufferBuilder(2048), layer.mode(), layer.format());

                PoseStack stack = new PoseStack();

                for (BlockRenderData blockData : chunk.blocks) {
                    stack.setIdentity();
                    stack.translate(
                            blockData.pos.getX(),
                            blockData.pos.getY(),
                            blockData.pos.getZ()
                    );

                    renderBlockOutline(
                            new AbstractMap.SimpleEntry<>(blockData.pos, blockData.state),
                            delta, camera, stack, blockData.color(), RandomSource.create(), buffer,
                            blockData.visibleFaces()
                    );
                }

                MeshData builtBuffer = buffer.build();
                if (builtBuffer != null) {
                    chunkBuffers.put(chunk.sectionPos,
                            new ChunkBufferData(builtBuffer, chunk.blockEntities));
                }
            });

            result.put(area, chunkBuffers);
        });

        return result;
    }

    private static void uploadToGPU(Map<AreaBox, Map<SectionPos, ChunkBufferData>> areaDataMap) {
        for (AreaRenderData data : areaVbos.values()) {
            for (ChunkRenderData chunk : data.sectionData.values()) {
                if (chunk.vbo != null) chunk.vbo.close();
            }
        }
        areaVbos.clear();

        for (Map.Entry<AreaBox, Map<SectionPos, ChunkBufferData>> entry : areaDataMap.entrySet()) {
            AreaBox area = entry.getKey();
            Map<SectionPos, ChunkBufferData> chunkBuffers = entry.getValue();

            AreaRenderData renderData = new AreaRenderData();

            for (Map.Entry<SectionPos, ChunkBufferData> chunkEntry : chunkBuffers.entrySet()) {
                SectionPos sectionPos = chunkEntry.getKey();
                ChunkBufferData bufferData = chunkEntry.getValue();

                ChunkRenderData chunkData = new ChunkRenderData();

                GMBVertexBuffer vbo = new GMBVertexBuffer();

                vbo.upload(bufferData.buffer());
                //TODO
                chunkData.vbo = vbo;
                chunkData.blockEntities = bufferData.blockEntities();

                renderData.sectionData.put(sectionPos, chunkData);
            }

            areaVbos.put(area, renderData);
        }

        OutlineManager.refreshBlockEntityIndex();
    }

    public static void rebuildChunkSectionAsync(AreaBox area, SectionPos sectionPos) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;
        RebuildKey rebuildKey = new RebuildKey(area, sectionPos);
        CompletableFuture<Void> existingTask = activeRebuilds.get(rebuildKey);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(true);
        }

        CompletableFuture<Void> rebuildTask = CompletableFuture
                .supplyAsync(() -> {
                    List<BlockRenderData> blocks = new ArrayList<>();
                    Map<BlockPos, Color> blockEntities = new ConcurrentHashMap<>();

                    int startX = Math.max(sectionPos.minBlockX(), area.minPos.getX());
                    int endX = Math.min(sectionPos.maxBlockX(), area.maxPos.getX());
                    int startY = Math.max(sectionPos.minBlockY(), area.minPos.getY());
                    int endY = Math.min(sectionPos.maxBlockY(), area.maxPos.getY());
                    int startZ = Math.max(sectionPos.minBlockZ(), area.minPos.getZ());
                    int endZ = Math.min(sectionPos.maxBlockZ(), area.maxPos.getZ());

                    for (int x = startX; x <= endX; x++) {
                        for (int y = startY; y <= endY; y++) {
                            for (int z = startZ; z <= endZ; z++) {

                                if (Thread.currentThread().isInterrupted()) {
                                    throw new CancellationException("Task cancelled during data collection");
                                }

                                BlockPos blockPos = new BlockPos(x, y, z);
                                BlockState state = world.getBlockState(blockPos);

                                if (state.isAir() || !shouldGlow(blockPos, state, area)) continue;

                                int visibleFaces = checkVisibleFaces(world, blockPos, area);
                                if (visibleFaces != 0) {
                                    blocks.add(new BlockRenderData(blockPos, state, area.color, visibleFaces));
                                }

                                if (state.getBlock() instanceof BaseEntityBlock) {
                                    BlockEntity be = world.getBlockEntity(blockPos);
                                    if (be != null) {
                                        blockEntities.put(be.getBlockPos(), area.color);
                                    }
                                }
                            }
                        }
                    }
                    return new ChunkBuildResult(sectionPos, blocks, blockEntities);
                }, WORKER_POOL)

                .thenApplyAsync(chunkResult -> {
                    if (chunkResult.blocks.isEmpty()) return null;

                    Minecraft mc = Minecraft.getInstance();
                    Camera camera = mc.gameRenderer.getMainCamera();
                    float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

                    RenderType layer = OutlineManager.outlineLayer();
                    BufferBuilder buffer = new BufferBuilder(new ByteBufferBuilder(2048), layer.mode(), layer.format());

                    PoseStack stack = new PoseStack();

                    for (BlockRenderData blockData : chunkResult.blocks) {
                        stack.setIdentity();
                        stack.translate(blockData.pos.getX(), blockData.pos.getY(), blockData.pos.getZ());
                        renderBlockOutline(
                                new AbstractMap.SimpleEntry<>(blockData.pos, blockData.state),
                                delta, camera, stack, blockData.color, RandomSource.create(), buffer,
                                blockData.visibleFaces()
                        );
                    }

                    MeshData builtBuffer = buffer.build();
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
                        GMBVertexBuffer vbo = new GMBVertexBuffer();

                        vbo.upload(bufferData.buffer());
                        chunkData.vbo = vbo;
                        chunkData.blockEntities = bufferData.blockEntities();
                    } else {
                        chunkData.vbo = null;
                        chunkData.blockEntities = new ConcurrentHashMap<>();
                    }

                    OutlineManager.refreshBlockEntityIndex();
                }, Minecraft.getInstance())

                .whenComplete((result, error) -> {
                    activeRebuilds.remove(rebuildKey);
                    if (error != null && !(error.getCause() instanceof CancellationException)) {
                        GlowMyBlocks.LOGGER.error("Failed to build mesh:", error);
                    }
                });
        activeRebuilds.put(rebuildKey, rebuildTask);
    }

    public static void onBlockStateChange(BlockPos blockPos) {
        long currentTime = System.currentTimeMillis();
        SectionPos changedSectionPos = SectionPos.of(blockPos);

        for (AreaBox area : selectedAreas) {
            if (!isBlockInsideArea(blockPos, area)) {
                continue;
            }

            Map<SectionPos, Long> areaRebuilds = pendingRebuilds.computeIfAbsent(
                    area, k -> new ConcurrentHashMap<>()
            );
            areaRebuilds.put(changedSectionPos, currentTime + REBUILD_DELAY_MS);

            for (Direction direction : Direction.values()) {
                if (isOnChunkSectionBoundary(blockPos, direction)) {
                    BlockPos adjacentPos = blockPos.relative(direction);

                    if (isBlockInsideArea(adjacentPos, area)) {
                        SectionPos adjacentSectionPos = SectionPos.of(adjacentPos);

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
        return SectionPos.of(pos1).equals(SectionPos.of(pos2));
    }

    private static void scheduleRebuildCheck() {
        if (rebuildCheckScheduled.compareAndSet(false, true)) {
            rebuildScheduler.schedule(() -> {
                try {
                    processScheduledRebuilds();
                } catch (Throwable t) {
                    GlowMyBlocks.LOGGER.error("Failed to process scheduled rebuilds:", t);
                } finally {
                    // Must be cleared before re-arming below, otherwise that call's CAS fails and
                    // any section that was not yet due is stranded until the next block change.
                    rebuildCheckScheduled.set(false);
                }

                if (!pendingRebuilds.isEmpty()) {
                    scheduleRebuildCheck();
                }
            }, REBUILD_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static void processScheduledRebuilds() {
        long currentTime = System.currentTimeMillis();

        pendingRebuilds.forEach((area, chunks) -> {
            chunks.entrySet().removeIf(entry -> {
                SectionPos sectionPos = entry.getKey();
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
        // Re-arming happens in scheduleRebuildCheck's task, after the guard flag is cleared.
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
