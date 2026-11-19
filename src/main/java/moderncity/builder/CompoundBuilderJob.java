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

public class CompoundBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum CompoundSize {
        SMALL_12(3, 4, "Modern Compound (12 Villas)"),
        MEDIUM_16(4, 4, "Modern Compound (16 Villas)"),
        LARGE_24(4, 6, "Modern Compound (24 Villas)");

        public final int cols;
        public final int rows;
        public final String label;

        CompoundSize(int cols, int rows, String label) {
            this.cols = cols;
            this.rows = rows;
            this.label = label;
        }
    }

    private static final int PLOT_SIZE = 20;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final CompoundSize size;
    private final int totalW;
    private final int totalD;
    private final int totalH = 12;
    private final long totalBlocks;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public CompoundBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing, CompoundSize size) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.size = size;
        this.totalW = size.cols * PLOT_SIZE + 8;
        this.totalD = size.rows * PLOT_SIZE + 8;
        this.totalBlocks = (long) totalW * totalD * totalH;
    }

    @Override
    public String getName() { return size.label; }

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
            if (y <= 3) return (y == 3) ? Blocks.WHITE_TERRACOTTA.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.AIR.getDefaultState();
        }

        int gx = x - 4;
        int gz = z - 4;
        if (gx < 0 || gz < 0 || gx >= size.cols * PLOT_SIZE || gz >= size.rows * PLOT_SIZE) {
            return (y == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        int lx = gx % PLOT_SIZE;
        int lz = gz % PLOT_SIZE;

        if (lx >= 15 || lz >= 15) {
            if (y == 0) return Blocks.GRAY_CONCRETE.getDefaultState();
            if (lx == 15 && lz == 15) {
                if (y >= 1 && y <= 3) return Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState();
                if (y == 4) return Blocks.SEA_LANTERN.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        if (lx == 0 || lx == 14 || lz == 0 || lz == 14) {
            return (y == 0) ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        if (y == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
        if (y > 10) return Blocks.AIR.getDefaultState();

        // Clean closed white concrete roof (no exposed froglights on top!)
        if (y == 10) {
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }

        int floor = y / 5;
        int ly = y % 5;
        boolean onXEdge = (lx == 1 || lx == 13);
        boolean onZEdge = (lz == 1 || lz == 13);
        boolean villaWall = onXEdge || onZEdge;

        if (villaWall) {
            // Front entrance door
            if (floor == 0 && lz == 1 && lx >= 6 && lx <= 8 && ly <= 3) return Blocks.AIR.getDefaultState();
            // Horizontal slab bands
            if (ly == 0) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            // Corner pillars & mid-wall columns ONLY (so panoramic windows open cleanly!)
            boolean isCorner = onXEdge && onZEdge;
            boolean isMidCol = (onZEdge && (lx == 5 || lx == 9)) || (onXEdge && (lz == 5 || lz == 9));
            if (isCorner || isMidCol) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }

        // Flush staircase inside villa
        boolean inStair = (lx >= 2 && lx <= 3) && (lz >= 7 && lz <= 11);
        if (inStair) {
            int step = lz - 7;
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

        // Hidden ceiling lights along upper corners (ly == 4) so floor stays spotless
        if (ly == 4 && (lx == 2 || lx == 12) && (lz == 2 || lz == 12)) {
            return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
        }

        // Divided interior rooms (Living / Kitchen / Bedrooms)
        if ((lx == 7 || lz == 7) && ly <= 4) {
            if (ly <= 3 && (lx == 10 || lz == 4)) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }
}
