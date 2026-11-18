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

public class TowerBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum Style { CLASSIC, SETBACK, CURVED, CANTILEVER, TWIN }

    public enum Tier {
        MID(30, 24, Style.CLASSIC, "Mid-Rise Tower (30b)"),
        HIGH(80, 28, Style.SETBACK, "High-Rise Setback Tower (80b)"),
        SUPERTALL(120, 34, Style.CURVED, "Supertall Curved Tower (120b)");

        public final int height;
        public final int footprint;
        public final Style defaultStyle;
        public final String label;

        Tier(int height, int footprint, Style defaultStyle, String label) {
            this.height = height;
            this.footprint = footprint;
            this.defaultStyle = defaultStyle;
            this.label = label;
        }
    }

    private static final int FLOOR_HEIGHT = 5;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final Style style;
    private final int size;
    private final int totalHeight;
    private final int floors;
    private final long totalBlocks;

    private int curF = 0;
    private int curY = 0;
    private int curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public TowerBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing, Style style, int requestedHeight) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.style = style;

        int maxAllowed = Math.max(20, world.getTopY() - anchor.getY() - 2);
        this.totalHeight = Math.min(480, Math.min(requestedHeight, maxAllowed));
        this.size = (totalHeight >= 150) ? 38 : (totalHeight >= 80 ? 30 : 24);
        this.floors = Math.max(4, (totalHeight - 6) / FLOOR_HEIGHT);
        this.totalBlocks = (long) size * size * totalHeight;
    }

    public TowerBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Tier tier) {
        this(world, ownerId, anchor, Direction.NORTH, tier.defaultStyle, tier.height);
    }

    @Override
    public String getName() { return style.name() + " Tower (" + totalHeight + "b)"; }

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
            if (curR >= size) {
                curR = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curF++;
                    if (curF >= size) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        int mainTopY = floors * FLOOR_HEIGHT;
        int center = size / 2;

        // Twin Skybridge Tower split logic
        if (style == Style.TWIN) {
            int gapHalf = 3;
            boolean inGap = Math.abs(x - center) < gapHalf;
            boolean inSkybridge = (y >= mainTopY / 2 - 2 && y <= mainTopY / 2 + 3) && Math.abs(z - center) <= 4;
            if (inGap && !inSkybridge) {
                return (y == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.AIR.getDefaultState();
            }
        }

        int setback = 0;
        if (style == Style.SETBACK) {
            setback = Math.min(6, (y / Math.max(15, mainTopY / 4)) * 2);
        } else if (style == Style.CANTILEVER) {
            // Subtle architectural shift every 6 floors
            int shift = ((y / 30) % 2 == 1) ? 2 : 0;
            if (x < shift || z < shift) return Blocks.AIR.getDefaultState();
        }

        int minB = setback;
        int maxB = size - 1 - setback;

        // Curved glass corner chamfer
        if (style == Style.CURVED) {
            int cornerDist = Math.min(Math.abs(x - minB), Math.abs(x - maxB))
                    + Math.min(Math.abs(z - minB), Math.abs(z - maxB));
            if (cornerDist <= 2) return Blocks.AIR.getDefaultState();
        }

        // Crown & Helipad above mainTopY
        if (y > mainTopY) {
            int crownY = y - mainTopY;
            if (totalHeight >= 100 && crownY == 1) {
                int dx = Math.abs(x - center);
                int dz = Math.abs(z - center);
                if (dx <= 6 && dz <= 6) {
                    if ((dx == 6 || dz == 6) && (dx + dz) % 2 == 0) return Blocks.SEA_LANTERN.getDefaultState();
                    boolean isH = (dz >= -3 && dz <= 3 && dx == 2) || (dx >= -2 && dx <= 2 && dz == 0);
                    return isH ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
                }
            }
            if (crownY <= 5 && ((x == minB && z == minB) || (x == maxB && z == maxB)
                    || (x == minB && z == maxB) || (x == maxB && z == minB))) {
                return (crownY == 5) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        if (x < minB || x > maxB || z < minB || z > maxB) return Blocks.AIR.getDefaultState();

        if (y == mainTopY) {
            if (x == minB || x == maxB || z == minB || z == maxB) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (x % 6 == 0 && z % 6 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }

        int floorIndex = y / FLOOR_HEIGHT;
        int localY = y % FLOOR_HEIGHT;
        boolean isFacade = (x == minB || x == maxB || z == minB || z == maxB)
                || (style == Style.CURVED && (Math.min(Math.abs(x - minB), Math.abs(x - maxB))
                + Math.min(Math.abs(z - minB), Math.abs(z - maxB)) == 3));

        // 1. Exterior Layered Glass Curtain Wall
        if (isFacade) {
            // Grand Lobby Entrance facing player (z == minB)
            if (floorIndex == 0 && z == minB && Math.abs(x - center) <= 3) {
                if (localY >= 1 && localY <= 3) return Blocks.AIR.getDefaultState();
                if (localY == 4) return Blocks.SEA_LANTERN.getDefaultState();
            }
            if (localY == 0) {
                return (floorIndex % 2 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState();
            }
            if ((x % 5 == 0) || (z % 5 == 0)) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (localY == 2) ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.TINTED_GLASS.getDefaultState();
        }

        // 2. Flush Stairwell (top step sits flush inside upper floor slab with 4-block headroom)
        boolean inStairBox = (x >= center - 4 && x <= center - 3) && (z >= center - 2 && z <= center + 2);
        if (inStairBox) {
            int step = z - (center - 2); // 0..4
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
            if (Math.abs(x - center) <= 1 && Math.abs(z - center) <= 1 && floorIndex > 0) return Blocks.AIR.getDefaultState();
            if (x % 6 == 3 && z % 6 == 3) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return (floorIndex == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        // 3. Central Elevator Shaft & Divided Office Rooms
        int dx = Math.abs(x - center);
        int dz = Math.abs(z - center);
        if (dx <= 2 && dz <= 2 && (dx == 2 || dz == 2)) {
            if (z == center - 2 && x == center && localY <= 3) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_TERRACOTTA.getDefaultState();
        }

        if (floorIndex > 0 && ((x == center || z == center) && (dx > 2 || dz > 2))) {
            if ((dx == 5 || dz == 5) && localY <= 3) return Blocks.AIR.getDefaultState();
            return (localY == 4) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }
}
