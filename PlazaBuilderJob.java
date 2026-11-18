package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class PlazaBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum Shape { L, U }
    public enum Layout { RETAIL, OFFICE, OPEN }

    private static final int FLOOR_HEIGHT = 5;
    private static final int SIZE = 48;
    private static final int WING_DEPTH = 16;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos origin;
    private final Shape shape;
    private final Layout layout;
    private final int floors;
    private final int totalHeight;
    private final long totalBlocks;

    private int curX = 0;
    private int curY = 0;
    private int curZ = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public PlazaBuilderJob(ServerWorld world, UUID ownerId, BlockPos origin, Shape shape, Layout layout, int floors) {
        this.world = world;
        this.ownerId = ownerId;
        this.origin = origin;
        this.shape = shape;
        this.layout = layout;
        this.floors = floors;
        this.totalHeight = floors * FLOOR_HEIGHT + 1;
        this.totalBlocks = (long) SIZE * SIZE * totalHeight;
    }

    @Override
    public String getName() {
        return String.format("%s-Shape Boulevard Plaza (%s, %d floors)", shape.name(), layout.name(), floors);
    }

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
            BlockState state = computeBlockAt(curX, curY, curZ);
            if (state != null) {
                mutablePos.set(origin.getX() + curX, origin.getY() + curY, origin.getZ() + curZ);
                StreamingBuilderEngine.placeBlockFast(world, mutablePos, state);
            }
            processed++;
            placedBlocks++;
            curZ++;
            if (curZ >= SIZE) {
                curZ = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curX++;
                    if (curX >= SIZE) finished = true;
                }
            }
        }
        return processed;
    }

    private boolean isInsideBuildingFootprint(int x, int z) {
        if (shape == Shape.L) {
            return (x < WING_DEPTH) || (z < WING_DEPTH);
        } else {
            return (x < WING_DEPTH) || (z < WING_DEPTH) || (z >= SIZE - WING_DEPTH);
        }
    }

    private boolean isOuterWall(int x, int z) {
        if (!isInsideBuildingFootprint(x, z)) return false;
        return !isInsideBuildingFootprint(x - 1, z) || !isInsideBuildingFootprint(x + 1, z)
                || !isInsideBuildingFootprint(x, z - 1) || !isInsideBuildingFootprint(x, z + 1)
                || x == 0 || x == SIZE - 1 || z == 0 || z == SIZE - 1;
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        // Outdoor courtyard boulevard walkway on ground level
        if (!isInsideBuildingFootprint(x, z)) {
            if (y == 0) {
                if ((x % 8 == 0 && z % 8 == 0)) return Blocks.SEA_LANTERN.getDefaultState();
                return Blocks.SMOOTH_QUARTZ.getDefaultState();
            }
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

        if (localY == 0) {
            if (x % 6 == 3 && z % 6 == 3) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return (floorIndex == 0) ? Blocks.BIRCH_PLANKS.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }

        // Stairwell near corner (x: 4..9, z: 4..6)
        if (floorIndex < floors - 1 && z == 5 && x >= 4 && x <= 8) {
            int step = x - 4;
            if (localY == step + 1) return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState();
            if (localY > step + 1) return Blocks.AIR.getDefaultState();
        }

        // Interior partitioning based on chosen layout
        Layout activeLayout = (floorIndex == 0 && layout == Layout.OFFICE) ? Layout.RETAIL : layout;
        if (activeLayout == Layout.RETAIL) {
            if ((x % 12 == 0 || z % 12 == 0)) {
                if (localY <= 3 && (x % 12 == 6 || z % 12 == 6)) return Blocks.AIR.getDefaultState();
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
        } else if (activeLayout == Layout.OFFICE) {
            if ((x % 8 == 0 || z % 8 == 0)) {
                if (localY <= 3 && (x % 8 == 4 || z % 8 == 4)) return Blocks.AIR.getDefaultState();
                return (localY == 4) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }
}
