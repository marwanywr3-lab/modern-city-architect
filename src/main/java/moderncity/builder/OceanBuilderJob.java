package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.map.WaypointManager;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class OceanBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int DEPTH = 50;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final int width;
    private final int length;
    private final long totalBlocks;

    private int curF = 0;
    private int curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public OceanBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing, int width, int length) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.width = width;
        this.length = length;
        this.totalBlocks = (long) width * length * (DEPTH + 2);
        WaypointManager.addWaypoint("Ocean Corniche", anchor, 0x0EA5E9);
    }

    @Override
    public String getName() { return String.format("Ocean & Corniche (%dx%d, Depth %d)", width, length, DEPTH); }

    @Override
    public ServerWorld getWorld() { return world; }

    @Override
    public UUID getOwnerId() { return ownerId; }

    @Override
    public long getTotalBlocks() { return totalBlocks; }

    @Override
    public long getPlacedBlocks() { return placedBlocks; }

    @Override
    public boolean isFinished() { return finished; }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        // High-speed column batching for massive ocean dimensions (up to 8x speed)
        int colBudget = Math.max(200, maxBlocks / 4);
        int processed = 0;
        int minY = Math.max(world.getBottomY() + 1, anchor.getY() - DEPTH);
        int seaLevel = anchor.getY();

        while (processed < colBudget && !finished) {
            if ((processed & 63) == 0 && System.nanoTime() >= deadlineNanos) break;

            // Beach & Quartz Corniche along front edge (curF: 0..10)
            if (curF <= 4) {
                BlockPos p = PlacementUtil.toWorld(anchor, facing, curR, 0, curF);
                StreamingBuilderEngine.placeBlockFast(world, p, Blocks.SMOOTH_QUARTZ.getDefaultState());
                if (curF == 1 && curR % 16 == 0) {
                    StreamingBuilderEngine.placeBlockFast(world, p.up(1), Blocks.SMOOTH_QUARTZ.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, p.up(2), Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, p.up(3), Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, p.up(4), Blocks.SEA_LANTERN.getDefaultState());
                }
            } else if (curF <= 12) {
                int slopeDepth = curF - 4;
                for (int y = seaLevel - slopeDepth; y <= seaLevel; y++) {
                    BlockPos p = PlacementUtil.toWorld(anchor, facing, curR, y - seaLevel, curF);
                    BlockState s = (y == seaLevel - slopeDepth) ? Blocks.SAND.getDefaultState() : Blocks.WATER.getDefaultState();
                    StreamingBuilderEngine.placeBlockFast(world, p, s);
                }
            } else {
                for (int y = minY; y <= seaLevel; y++) {
                    BlockPos p = PlacementUtil.toWorld(anchor, facing, curR, y - seaLevel, curF);
                    BlockState s = (y == minY) ? Blocks.SAND.getDefaultState() : Blocks.WATER.getDefaultState();
                    StreamingBuilderEngine.placeBlockFast(world, p, s);
                }
            }

            processed++;
            placedBlocks += (DEPTH + 2);
            curR++;
            if (curR >= width) {
                curR = 0;
                curF++;
                if (curF >= length) finished = true;
            }
        }
        return processed * (DEPTH + 2);
    }
}
