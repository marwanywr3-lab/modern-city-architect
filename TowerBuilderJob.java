package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class TowerBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum Tier {
        MID(30, 22, 6, "Mid-Rise Tower (30b)"),
        HIGH(80, 28, 16, "High-Rise Glass Tower (80b)"),
        SUPERTALL(120, 34, 23, "Supertall Skyscraper (120b)");

        public final int height;
        public final int footprint;
        public final int floors;
        public final String label;

        Tier(int height, int footprint, int floors, String label) {
            this.height = height;
            this.footprint = footprint;
            this.floors = floors;
            this.label = label;
        }
    }

    private static final int FLOOR_HEIGHT = 5;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos origin;
    private final Tier tier;

    private final int size;
    private final int totalHeight;
    private final long totalBlocks;

    private int curX = 0;
    private int curY = 0;
    private int curZ = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public TowerBuilderJob(ServerWorld world, UUID ownerId, BlockPos origin, Tier tier) {
        this.world = world;
        this.ownerId = ownerId;
        this.origin = origin;
        this.tier = tier;
        this.size = tier.footprint;
        this.totalHeight = tier.height;
        this.totalBlocks = (long) size * size * totalHeight;
    }

    @Override
    public String getName() { return tier.label; }

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
            if (curZ >= size) {
                curZ = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curX++;
                    if (curX >= size) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        int mainTopY = tier.floors * FLOOR_HEIGHT;
        int center = size / 2;

        int setback = 0;
        if (tier == Tier.SUPERTALL && y >= 85) setback = 3;
        else if (tier == Tier.HIGH && y >= 55) setback = 2;

        int minB = setback;
        int maxB = size - 1 - setback;

        if (y > mainTopY) {
            int crownY = y - mainTopY;
            if (tier == Tier.SUPERTALL) {
                if (crownY == 1) {
                    int dx = Math.abs(x - center);
                    int dz = Math.abs(z - center);
                    int padRadius = 7;
                    if (dx <= padRadius && dz <= padRadius) {
                        if ((dx == padRadius || dz == padRadius) && (dx + dz) % 2 == 0) {
                            return Blocks.SEA_LANTERN.getDefaultState();
                        }
                        boolean isH = (dz >= -3 && dz <= 3 && dx == 2) || (dx >= -2 && dx <= 2 && dz == 0);
                        if (isH) return Blocks.YELLOW_CONCRETE.getDefaultState();
                        return Blocks.GRAY_CONCRETE.getDefaultState();
                    }
                    if (x == minB || x == maxB || z == minB || z == maxB) {
                        return Blocks.SEA_LANTERN.getDefaultState();
                    }
                    return Blocks.AIR.getDefaultState();
                }
                if (crownY <= 4 && ((x == minB && z == minB) || (x == maxB && z == maxB)
                        || (x == minB && z == maxB) || (x == maxB && z == minB))) {
                    return (crownY == 4) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
                }
                return Blocks.AIR.getDefaultState();
            }
            if (crownY == 1 && (x == minB || x == maxB || z == minB || z == maxB)) {
                return Blocks.WHITE_CONCRETE.getDefaultState();
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
        boolean isFacade = (x == minB || x == maxB || z == minB || z == maxB);

        if (isFacade) {
            if (floorIndex == 0 && z == minB && Math.abs(x - center) <= 2 && localY >= 1 && localY <= 3) {
                return Blocks.AIR.getDefaultState();
            }
            if (localY == 0) {
                return (floorIndex % 3 == 0) ? Blocks.WHITE_TERRACOTTA.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            }
            if ((x == minB && z == minB) || (x == maxB && z == maxB) || (x == minB && z == maxB) || (x == maxB && z == minB)
                    || (x % 5 == 0 && (z == minB || z == maxB)) || (z % 5 == 0 && (x == minB || x == maxB))) {
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            if (localY == 4 && (Math.abs(x - minB) <= 1 || Math.abs(x - maxB) <= 1)
                    && (Math.abs(z - minB) <= 1 || Math.abs(z - maxB) <= 1)) {
                return Blocks.SEA_LANTERN.getDefaultState();
            }
            return (floorIndex == 0) ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.TINTED_GLASS.getDefaultState();
        }

        if (localY == 0) {
            if (Math.abs(x - center) <= 1 && Math.abs(z - center) <= 1 && floorIndex > 0) return Blocks.AIR.getDefaultState();
            if (x % 6 == 3 && z % 6 == 3) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return (floorIndex == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        int dx = Math.abs(x - center);
        int dz = Math.abs(z - center);
        if (dx <= 2 && dz <= 2 && (dx == 2 || dz == 2)) {
            if (z == center - 2 && x == center && localY <= 3) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_TERRACOTTA.getDefaultState();
        }

        if (floorIndex > 0) {
            boolean onCrossPartition = (x == center || z == center) && (dx > 2 || dz > 2);
            if (onCrossPartition) {
                if ((dx == 4 || dz == 4) && localY <= 3) return Blocks.AIR.getDefaultState();
                if (localY == 4) return Blocks.WHITE_CONCRETE.getDefaultState();
                return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }
}
