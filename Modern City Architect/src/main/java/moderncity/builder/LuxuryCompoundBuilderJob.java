package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.map.WaypointManager;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class LuxuryCompoundBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int COLS = 3;
    private static final int ROWS = 4;
    private static final int PLOT_SIZE = 32;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final int totalW = COLS * PLOT_SIZE + 8;
    private final int totalD = ROWS * PLOT_SIZE + 8;
    private final int totalH = 12;
    private final long totalBlocks = (long) totalW * totalD * totalH;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public LuxuryCompoundBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        WaypointManager.addWaypoint("Luxury Villa Compound", anchor, 0x10B981);
    }

    @Override public String getName() { return "Luxury Villa Compound (12 Large 4BR Villas)"; }
    @Override public ServerWorld getWorld() { return world; }
    @Override public UUID getOwnerId() { return ownerId; }
    @Override public long getTotalBlocks() { return totalBlocks; }
    @Override public long getPlacedBlocks() { return placedBlocks; }
    @Override public boolean isFinished() { return finished; }

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
            if (curR >= totalW) {
                curR = 0;
                curY++;
                if (curY >= totalH) {
                    curY = 0;
                    curF++;
                    if (curF >= totalD) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int x, int y, int z) {
        boolean onWall = (x == 0 || x == totalW - 1 || z == 0 || z == totalD - 1);
        if (onWall) {
            if (z == 0 && Math.abs(x - totalW / 2) <= 4) {
                if (y == 0) return Blocks.GRAY_CONCRETE.getDefaultState();
                if (y <= 4) return Blocks.AIR.getDefaultState();
                if (y == 5) return Blocks.WHITE_CONCRETE.getDefaultState();
                return Blocks.AIR.getDefaultState();
            }
            return (y <= 3) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        int gx = x - 4;
        int gz = z - 4;
        if (gx < 0 || gz < 0 || gx >= COLS * PLOT_SIZE || gz >= ROWS * PLOT_SIZE) {
            return (y == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        int lx = gx % PLOT_SIZE;
        int lz = gz % PLOT_SIZE;

        if (lx >= 27 || lz >= 27) {
            if (y == 0) return Blocks.GRAY_CONCRETE.getDefaultState();
            if (lx == 27 && lz == 27) {
                if (y >= 1 && y <= 3) return Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState();
                if (y == 4) return Blocks.SEA_LANTERN.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        if (lx == 0 || lx == 26 || lz == 0 || lz == 26) {
            return (y == 0) ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        if (y == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
        if (y > 10) return Blocks.AIR.getDefaultState();
        if (y == 10) return Blocks.WHITE_CONCRETE.getDefaultState();

        int floor = y / 5;
        int ly = y % 5;
        boolean onX = (lx == 1 || lx == 25);
        boolean onZ = (lz == 1 || lz == 25);

        if (onX || onZ) {
            if (floor == 0 && lz == 1 && (lx >= 11 && lx <= 14) && ly <= 3) return Blocks.AIR.getDefaultState();
            if (ly == 0) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            if ((onX && onZ) || (onZ && lx % 6 == 1) || (onX && lz % 6 == 1)) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }

        boolean inStair = (lx >= 12 && lx <= 13) && (lz >= 18 && lz <= 22);
        if (inStair) {
            int step = lz - 18;
            int stairY = step + 1;
            if (y == stairY) {
                return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                        .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
            }
            if (y > stairY && y <= stairY + 4) return Blocks.AIR.getDefaultState();
        }

        if (ly == 0) {
            return (floor == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.BIRCH_PLANKS.getDefaultState();
        }
        if (ly == 4 && lx % 6 == 0 && lz % 6 == 0) {
            return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
        }

        if (floor == 0) {
            if (lx == 13 || lz == 13) {
                if (ly <= 3 && (lx == 6 || lx == 19 || lz == 6 || lz == 19)) return Blocks.AIR.getDefaultState();
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            if (lx >= 3 && lx <= 8 && lz == 23 && ly == 1) return Blocks.SMOOTH_QUARTZ.getDefaultState();
        } else {
            boolean mainCross = (lx == 13 || lz == 13);
            boolean masterBathWall = (lx <= 7 && lz == 7) || (lz <= 7 && lx == 7);
            boolean bath2Wall = (lx >= 19 && lz == 6) || (lz <= 6 && lx == 19);
            boolean bath3Wall = (lx <= 6 && lz == 19) || (lz >= 19 && lx == 6);

            if (mainCross || masterBathWall || bath2Wall || bath3Wall) {
                if (ly <= 3 && (lx == 4 || lx == 10 || lx == 21 || lz == 4 || lz == 10 || lz == 16 || lz == 21)) {
                    return Blocks.AIR.getDefaultState();
                }
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }
}
