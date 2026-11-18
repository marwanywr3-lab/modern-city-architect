package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class PlazaBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum Shape { L, U }
    public enum Layout { RETAIL, OFFICE, OPEN }

    private static final int FLOOR_HEIGHT = 5;
    private static final int SIZE = 48;
    private static final int WING_DEPTH = 16;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final Shape shape;
    private final Layout layout;
    private final int floors;
    private final int totalHeight;
    private final long totalBlocks;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public PlazaBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing,
                           Shape shape, Layout layout, int floors) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.shape = shape;
        this.layout = layout;
        this.floors = floors;
        this.totalHeight = floors * FLOOR_HEIGHT + 1;
        this.totalBlocks = (long) SIZE * SIZE * totalHeight;
    }

    public PlazaBuilderJob(ServerWorld world, UUID ownerId, BlockPos origin, Shape shape, Layout layout, int floors) {
        this(world, ownerId, origin, Direction.NORTH, shape, layout, floors);
    }

    @Override
    public String getName() { return shape.name() + "-Shape Plaza (" + layout.name() + ", " + floors + "F)"; }

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
        int processed = 0;
        while (processed < maxBlocks && !finished) {
            if ((processed & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
            BlockState state = computeBlockAt(curR, curY, curF);
            if (state != null) {
                BlockPos wp = PlacementUtil.toWorld(anchor, facing, curR, curY, curF);
                StreamingBuilderEngine.placeBlockFast(world, wp, state);
            }
            processed++;
            placedBlocks++;
            curR++;
            if (curR >= SIZE) {
                curR = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curF++;
                    if (curF >= SIZE) finished = true;
                }
            }
        }
        return processed;
    }

    private boolean isInsideBuildingFootprint(int x, int z) {
        if (shape == Shape.L) return (x < WING_DEPTH) || (z < WING_DEPTH);
        return (x < WING_DEPTH) || (z < WING_DEPTH) || (x >= SIZE - WING_DEPTH);
    }

    private boolean isOuterWall(int x, int z) {
        if (!isInsideBuildingFootprint(x, z)) return false;
        return !isInsideBuildingFootprint(x - 1, z) || !isInsideBuildingFootprint(x + 1, z)
                || !isInsideBuildingFootprint(x, z - 1) || !isInsideBuildingFootprint(x, z + 1)
                || x == 0 || x == SIZE - 1 || z == 0 || z == SIZE - 1;
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        if (!isInsideBuildingFootprint(x, z)) {
            if (y == 0) return (x % 8 == 0 && z % 8 == 0) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
            return Blocks.AIR.getDefaultState();
        }

        if (y == totalHeight - 1) {
            if (isOuterWall(x, z)) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (x % 6 == 0 && z % 6 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        int floorIndex = y / FLOOR_HEIGHT;
        int localY = y % FLOOR_HEIGHT;

        if (isOuterWall(x, z)) {
            if (localY == 0) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            if (floorIndex == 0 && localY <= 3 && (x % 10 == 5 || z % 10 == 5)) return Blocks.AIR.getDefaultState();
            if (x % 6 == 0 || z % 6 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (floorIndex == 0) ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.TINTED_GLASS.getDefaultState();
        }

        boolean inStair = (x >= 4 && x <= 5) && (z >= 4 && z <= 8);
        if (inStair) {
            int step = z - 4; // 0..4
            for (int f = 0; f < floors - 1; f++) {
                int stairY = f * FLOOR_HEIGHT + step + 1;
                if (y == stairY) {
                    return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                            .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
                }
                if (y > stairY && y <= stairY + 4) return Blocks.AIR.getDefaultState();
            }
        }
        if (localY == 0) {
            if (x % 6 == 3 && z % 6 == 3) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return (floorIndex == 0) ? Blocks.BIRCH_PLANKS.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }

        // Ground floor Cafe & Retail divisions + Upper floor distinct Company Offices with reception counters
        Layout activeLayout = (floorIndex == 0 && layout == Layout.OFFICE) ? Layout.RETAIL : layout;
        if (activeLayout == Layout.RETAIL) {
            if ((x % 12 == 0 || z % 12 == 0)) {
                if (localY <= 3 && (x % 12 == 6 || z % 12 == 6)) return Blocks.AIR.getDefaultState();
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            // Cafe service counter inside each ground-floor unit
            if (floorIndex == 0 && localY == 1 && (x % 12 >= 3 && x % 12 <= 7) && (z % 12 == 4)) {
                return Blocks.SMOOTH_QUARTZ.getDefaultState();
            }
        } else if (activeLayout == Layout.OFFICE) {
            if ((x % 10 == 0 || z % 10 == 0)) {
                if (localY <= 3 && (x % 10 == 5 || z % 10 == 5)) return Blocks.AIR.getDefaultState();
                return (localY == 4) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
            // Mini reception desk in each office suite
            if (localY == 1 && (x % 10 >= 4 && x % 10 <= 6) && (z % 10 == 2)) {
                return Blocks.BIRCH_PLANKS.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }
}
