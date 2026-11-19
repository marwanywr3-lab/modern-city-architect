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

public class HotelBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum HotelTier {
        SMALL(44, 32, 26, "Boutique Hotel (44x32x26)"),
        MEDIUM(80, 50, 46, "Grand Hotel (80x50x46)"),
        TALL(110, 64, 90, "Luxury Sky Hotel (110x64x90)"),
        MEGA(1000, 300, 70, "Mega Resort Hotel (1000x300x70)");

        public final int length;
        public final int width;
        public final int height;
        public final String label;

        HotelTier(int length, int width, int height, String label) {
            this.length = length;
            this.width = width;
            this.height = height;
            this.label = label;
        }
    }

    private static final int FLOOR_HEIGHT = 5;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final int length;
    private final int width;
    private final int height;
    private final int floors;
    private final long totalBlocks;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public HotelBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing,
                           int length, int width, int height) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.length = length;
        this.width = width;
        int maxAllowed = Math.max(20, world.getTopY() - anchor.getY() - 2);
        this.height = Math.min(height, maxAllowed);
        this.floors = Math.max(3, (this.height - 1) / FLOOR_HEIGHT);
        this.totalBlocks = (long) length * width * this.height;
        WaypointManager.addWaypoint("Luxury Hotel", anchor, 0xEC4899);
    }

    @Override
    public String getName() { return String.format("Modern Hotel (%dx%dx%d)", length, width, height); }

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
                if (curY >= height) {
                    curY = 0;
                    curF++;
                    if (curF >= length) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int x, int y, int z) {
        int topY = floors * FLOOR_HEIGHT;
        if (y > topY) return Blocks.AIR.getDefaultState();
        if (y == topY) return Blocks.WHITE_CONCRETE.getDefaultState();

        int floor = y / FLOOR_HEIGHT;
        int ly = y % FLOOR_HEIGHT;
        int centerX = width / 2;
        int centerZ = length / 2;
        boolean outer = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

        // 1. Exterior Facade & Grand Hotel Entrance
        if (outer) {
            if (floor == 0 && z == 0 && Math.abs(x - centerX) <= 5) {
                if (ly >= 1 && ly <= 4) return Blocks.AIR.getDefaultState();
            }
            if (ly == 0) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            if (x % 6 == 0 || z % 6 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.TINTED_GLASS.getDefaultState();
        }

        // 2. Giant Swimming Pool Zone in Central Courtyard / Resort Deck
        int poolHalfW = Math.min(24, width / 5);
        int poolHalfL = Math.min(35, length / 6);
        boolean inPoolZone = (Math.abs(x - centerX) <= poolHalfW) && (Math.abs(z - centerZ) <= poolHalfL);
        if (floor == 0 && inPoolZone) {
            if (y == 0) {
                boolean rim = (Math.abs(x - centerX) == poolHalfW || Math.abs(z - centerZ) == poolHalfL);
                if (rim) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                return (x % 6 == 0 && z % 6 == 0) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WATER.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        // 3. Flush Staircases every 120 blocks along length
        int zCycle = z % 120;
        boolean inStair = (x >= centerX - 2 && x <= centerX - 1) && (zCycle >= 10 && zCycle <= 14);
        if (inStair) {
            int step = zCycle - 10;
            for (int f = 0; f < floors - 1; f++) {
                int stairY = f * FLOOR_HEIGHT + step + 1;
                if (y == stairY) {
                    return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                            .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
                }
                if (y > stairY && y <= stairY + 4) return Blocks.AIR.getDefaultState();
            }
        }

        // 4. Floor Slabs & Hidden Ceiling Lights
        if (ly == 0) {
            return (floor == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.BIRCH_PLANKS.getDefaultState();
        }
        if (ly == 4 && x % 8 == 0 && z % 8 == 0) {
            return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
        }

        // 5. Ground Floor: Grand Lobby + Conference Halls + Exhibition Showrooms
        if (floor == 0) {
            // Reception desk near front lobby
            if (z == 8 && Math.abs(x - centerX) <= 4 && ly == 1) {
                return Blocks.SMOOTH_QUARTZ.getDefaultState();
            }
            // Divided Conference Halls (Left Wing) & Exhibition Showrooms (Right Wing)
            if (z > 14 && (x == width / 3 || x == (width * 2) / 3 || z % 40 == 0)) {
                if (ly <= 3 && (z % 20 >= 8 && z % 20 <= 12 || x % 20 >= 8 && x % 20 <= 12)) {
                    return Blocks.AIR.getDefaultState();
                }
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        // 6. Upper Floors: Divided Hotel Rooms along Central & Side Hallways (Ready for Furnishing!)
        boolean inMainHallway = Math.abs(x - centerX) <= 2;
        if (inMainHallway) return Blocks.AIR.getDefaultState();

        // Hallway walls with room doorways every 10 blocks
        if (Math.abs(x - centerX) == 3) {
            if (ly <= 3 && (z % 10 == 5 || z % 10 == 6)) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }
        // Dividers between individual hotel rooms (10 blocks wide) & en-suite bathroom corner
        if (z % 10 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
        if (z % 10 == 4 && Math.abs(x - centerX) <= 7) {
            if (ly <= 3 && Math.abs(x - centerX) == 5) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_TERRACOTTA.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }
}
