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

public class ApartmentTowerJob implements StreamingBuilderEngine.BuildJob {

    private static final int FLOOR_HEIGHT = 5;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final int width;
    private final int depth;
    private final int floors;
    private final int totalHeight;
    private final long totalBlocks;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public ApartmentTowerJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing,
                             int width, int depth, int floors) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.width = width;
        this.depth = depth;
        this.floors = floors;
        this.totalHeight = floors * FLOOR_HEIGHT + 1;
        this.totalBlocks = (long) width * depth * totalHeight;
    }

    @Override
    public String getName() { return String.format("Apartment Tower (%dx%d, %dF)", width, depth, floors); }

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
            BlockState state = computeBlock(curR, curY, curF);
            if (state != null) {
                BlockPos wp = PlacementUtil.toWorld(anchor, facing, curR, curY, curF);
                StreamingBuilderEngine.placeBlockFast(world, wp, state);
            }
            processed++;
            placedBlocks++;
            curR++;
            if (curR >= width) {
                curR = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curF++;
                    if (curF >= depth) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int x, int y, int z) {
        int topY = floors * FLOOR_HEIGHT;
        if (y == topY) {
            if (x == 0 || x == width - 1 || z == 0 || z == depth - 1) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (x % 6 == 0 && z % 6 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        int floor = y / FLOOR_HEIGHT;
        int ly = y % FLOOR_HEIGHT;
        int centerZ = depth / 2;
        boolean outer = (x == 0 || x == width - 1 || z == 0 || z == depth - 1);

        if (outer) {
            if (floor == 0 && z == 0 && Math.abs(x - width / 2) <= 2 && ly >= 1 && ly <= 3) return Blocks.AIR.getDefaultState();
            if (ly == 0) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            if (x % 6 == 0 || z % 6 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.TINTED_GLASS.getDefaultState();
        }

        boolean inStair = (x >= 2 && x <= 3) && (z >= centerZ - 2 && z <= centerZ + 2);
        if (inStair) {
            int step = z - (centerZ - 2); // 0..4
            for (int f = 0; f < floors - 1; f++) {
                int stairY = f * FLOOR_HEIGHT + step + 1;
                if (y == stairY) {
                    return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                            .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
                }
                if (y > stairY && y <= stairY + 4) return Blocks.AIR.getDefaultState();
            }
        }
        if (ly == 0) {
            if (x % 5 == 2 && z % 5 == 2) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return (Math.abs(z - centerZ) <= 1) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.BIRCH_PLANKS.getDefaultState();
        }

        // Central hallway walls + Apartment unit dividers + Room dividers inside each apartment
        if (Math.abs(z - centerZ) == 2) {
            if (ly <= 3 && (x % 12 == 6)) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }
        if (Math.abs(z - centerZ) > 2) {
            // Between apartments
            if (x % 12 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            // Inside each apartment (living / bedroom / bath)
            if (x % 12 == 6 || Math.abs(z - centerZ) == 7) {
                if (ly <= 3 && (x % 12 == 3 || Math.abs(z - centerZ) == 5)) return Blocks.AIR.getDefaultState();
                return Blocks.WHITE_TERRACOTTA.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }
}
