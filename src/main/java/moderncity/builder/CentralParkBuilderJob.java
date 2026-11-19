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

public class CentralParkBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int SIZE = 120;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final long totalBlocks = (long) SIZE * SIZE;

    private int curF = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public CentralParkBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        WaypointManager.addWaypoint("Central Park", anchor, 0x16A34A);
    }

    @Override public String getName() { return "Grand Central Park (120x120)"; }
    @Override public ServerWorld getWorld() { return world; }
    @Override public UUID getOwnerId() { return ownerId; }
    @Override public long getTotalBlocks() { return totalBlocks; }
    @Override public long getPlacedBlocks() { return placedBlocks; }
    @Override public boolean isFinished() { return finished; }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        int processed = 0;
        int c = SIZE / 2;
        while (processed < maxBlocks && !finished) {
            if ((processed & 63) == 0 && System.nanoTime() >= deadlineNanos) break;
            int x = curR, z = curF;
            BlockPos p = PlacementUtil.toWorld(anchor, facing, x, 0, z);
            double dist = Math.hypot(x - c, z - c);

            if (dist <= 22) {
                StreamingBuilderEngine.placeBlockFast(world, p.down(), Blocks.SAND.getDefaultState());
                StreamingBuilderEngine.placeBlockFast(world, p, (dist <= 3) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WATER.getDefaultState());
            } else if (Math.abs(x - c) <= 2 || Math.abs(z - c) <= 2 || Math.abs(dist - 25) <= 2) {
                StreamingBuilderEngine.placeBlockFast(world, p, Blocks.SMOOTH_QUARTZ.getDefaultState());
                if ((x % 14 == 0 || z % 14 == 0) && Math.abs(dist - 25) <= 1.5) {
                    StreamingBuilderEngine.placeBlockFast(world, p.up(1), Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, p.up(2), Blocks.SEA_LANTERN.getDefaultState());
                }
            } else {
                StreamingBuilderEngine.placeBlockFast(world, p, Blocks.GRASS_BLOCK.getDefaultState());
                if (x % 12 == 4 && z % 12 == 4) {
                    for (int h = 1; h <= 3; h++) StreamingBuilderEngine.placeBlockFast(world, p.up(h), Blocks.BIRCH_LOG.getDefaultState());
                    for (int lx = -1; lx <= 1; lx++) {
                        for (int lz = -1; lz <= 1; lz++) {
                            StreamingBuilderEngine.placeBlockFast(world, p.add(lx, 4, lz), Blocks.AZALEA_LEAVES.getDefaultState());
                        }
                    }
                }
            }

            processed++;
            placedBlocks++;
            curR++;
            if (curR >= SIZE) {
                curR = 0;
                curF++;
                if (curF >= SIZE) finished = true;
            }
        }
        return processed;
    }
}
