package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class FacilityBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum FacilityType {
        PARKING(36, 26, 21, "Multi-Story Parking Garage"),
        METRO(32, 20, 14, "Futuristic Metro Station"),
        SHOWROOM(30, 22, 12, "Glass Car Showroom"),
        GAS_STATION(28, 22, 9, "Modern Gas Station"),
        VILLA(26, 24, 12, "Contemporary Modern Villa"),
        MOSQUE(34, 30, 24, "Contemporary Modern Mosque"),
        HOSPITAL(42, 34, 28, "Modern City Hospital");

        public final int length;
        public final int width;
        public final int height;
        public final String label;

        FacilityType(int length, int width, int height, String label) {
            this.length = length;
            this.width = width;
            this.height = height;
            this.label = label;
        }
    }

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos origin;
    private final FacilityType type;
    private final long totalBlocks;

    private int curX = 0;
    private int curY = 0;
    private int curZ = 0;
    private long placedBlocks = 0;
    private boolean finished = false;
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public FacilityBuilderJob(ServerWorld world, UUID ownerId, BlockPos origin, FacilityType type) {
        this.world = world;
        this.ownerId = ownerId;
        this.origin = (type == FacilityType.METRO) ? origin.down(5) : origin;
        this.type = type;
        this.totalBlocks = (long) type.length * type.width * type.height;
    }

    @Override
    public String getName() { return type.label; }

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
            BlockState state = computeBlock(curX, curY, curZ);
            if (state != null) {
                mutablePos.set(origin.getX() + curX, origin.getY() + curY, origin.getZ() + curZ);
                StreamingBuilderEngine.placeBlockFast(world, mutablePos, state);
            }
            processed++;
            placedBlocks++;
            curZ++;
            if (curZ >= type.width) {
                curZ = 0;
                curY++;
                if (curY >= type.height) {
                    curY = 0;
                    curX++;
                    if (curX >= type.length) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int x, int y, int z) {
        int L = type.length;
        int W = type.width;
        int H = type.height;
        boolean outer = (x == 0 || x == L - 1 || z == 0 || z == W - 1);

        return switch (type) {
            case PARKING -> computeParking(x, y, z, L, W, H, outer);
            case METRO -> computeMetro(x, y, z, L, W, H, outer);
            case SHOWROOM -> computeShowroom(x, y, z, L, W, H, outer);
            case GAS_STATION -> computeGasStation(x, y, z, L, W, H);
            case VILLA -> computeVilla(x, y, z, L, W, H);
            case MOSQUE -> computeMosque(x, y, z, L, W, H);
            case HOSPITAL -> computeHospital(x, y, z, L, W, H, outer);
        };
    }

    private BlockState computeParking(int x, int y, int z, int L, int W, int H, boolean outer) {
        int floor = y / 5;
        int ly = y % 5;
        if (y == H - 1) return outer ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.AIR.getDefaultState();
        if (ly == 0) {
            if (x >= L - 7 && z >= 2 && z <= 6 && floor > 0) return Blocks.AIR.getDefaultState();
            if (x % 6 == 3 && z % 6 == 3) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return (z % 4 == 0 && x > 3 && x < L - 8) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
        }
        if (outer) {
            if (floor == 0 && x == 0 && z >= W / 2 - 3 && z <= W / 2 + 3 && ly <= 3) return Blocks.AIR.getDefaultState();
            if (ly == 1) return Blocks.WHITE_CONCRETE.getDefaultState();
            if (x % 6 == 0 || z % 6 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.AIR.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState computeMetro(int x, int y, int z, int L, int W, int H, boolean outer) {
        // y=0..4 underground platform, y=5 ground level, y=6..13 glass canopy
        if (y == 0) {
            if (Math.abs(z - W / 2) <= 2) return Blocks.BLACK_CONCRETE.getDefaultState();
            return (x % 6 == 0) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }
        if (y < 5) {
            if (outer) return Blocks.WHITE_CONCRETE.getDefaultState();
            // Stairs down from street level
            if (x >= 2 && x <= 7 && Math.abs(z - W / 2) <= 2 && y == 5 - (x - 2)) {
                return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }
        if (y == 5) {
            if (x >= 2 && x <= 8 && Math.abs(z - W / 2) <= 3) return Blocks.AIR.getDefaultState();
            return Blocks.SMOOTH_QUARTZ.getDefaultState();
        }
        // Futuristic curved glass canopy above ground
        int canopyY = y - 5;
        int dz = Math.abs(z - W / 2);
        int targetH = Math.max(1, 7 - dz / 2);
        if (x >= 2 && x <= L - 3 && dz <= 7) {
            if (canopyY == targetH) {
                return (x % 6 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
            if (canopyY < targetH && (x == 2 || x == L - 3) && dz > 3) {
                return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState computeShowroom(int x, int y, int z, int L, int W, int H, boolean outer) {
        if (y == 0) return (x % 5 == 0 && z % 5 == 0) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        if (y == H - 1) return (x % 5 == 2 && z % 5 == 2) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        if (outer) {
            if (x == 0 && Math.abs(z - W / 2) <= 3 && y <= 4) return Blocks.AIR.getDefaultState();
            if (x % 6 == 0 || z % 6 == 0 || y == H - 2) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState computeGasStation(int x, int y, int z, int L, int W, int H) {
        if (y == 0) return Blocks.GRAY_CONCRETE.getDefaultState();
        // Sleek modern canopy in front (x: 4..18, z: 4..W-5)
        boolean inCanopy = x >= 4 && x <= 18 && z >= 4 && z <= W - 5;
        if (inCanopy) {
            if (y == 6 || y == 7) {
                if (y == 6 && x % 4 == 0 && z % 4 == 0) return Blocks.SEA_LANTERN.getDefaultState();
                return (y == 7) ? Blocks.WHITE_TERRACOTTA.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            }
            if ((x == 6 || x == 16) && (z == 6 || z == W - 7) && y < 6) {
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
        }
        // Convenience shop shell at back (x: 20..L-2, z: 4..W-5)
        if (x >= 20 && x <= L - 2 && z >= 4 && z <= W - 5 && y <= 5) {
            boolean shopWall = (x == 20 || x == L - 2 || z == 4 || z == W - 5);
            if (y == 5) return (x % 4 == 0 && z % 4 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            if (shopWall) {
                if (x == 20 && Math.abs(z - W / 2) <= 1 && y <= 3) return Blocks.AIR.getDefaultState();
                return (x == 20) ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            }
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState computeVilla(int x, int y, int z, int L, int W, int H) {
        // Pool in front yard (x: 2..9, z: 3..10)
        if (x >= 2 && x <= 9 && z >= 3 && z <= 10 && y == 0) {
            if (x == 2 || x == 9 || z == 3 || z == 10) return Blocks.SMOOTH_QUARTZ.getDefaultState();
            return Blocks.WATER.getDefaultState();
        }
        if (y == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();

        // Two-story cantilevered villa body (x: 11..L-2, z: 2..W-3)
        boolean inBody = x >= 11 && x <= L - 2 && z >= 2 && z <= W - 3;
        if (!inBody) return Blocks.AIR.getDefaultState();

        int floor = y / 5;
        int ly = y % 5;
        if (y == H - 1) return Blocks.WHITE_CONCRETE.getDefaultState();

        boolean wall = (x == 11 || x == L - 2 || z == 2 || z == W - 3);
        if (wall) {
            if (floor == 0 && x == 11 && Math.abs(z - W / 2) <= 1 && ly <= 3) return Blocks.AIR.getDefaultState();
            if (ly == 0) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            if (x % 5 == 0 || z % 5 == 0) return Blocks.BIRCH_PLANKS.getDefaultState();
            return Blocks.TINTED_GLASS.getDefaultState();
        }
        if (ly == 0) {
            return (x % 4 == 0 && z % 4 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.BIRCH_PLANKS.getDefaultState();
        }
        // Interior room partition
        if ((x == 18 || z == W / 2) && ly <= 4) {
            if (ly <= 3 && (z == W / 2 - 3 || x == 15)) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState computeMosque(int x, int y, int z, int L, int W, int H) {
        int cx = L / 2;
        int cz = W / 2;
        // Contemporary Minaret at front-left corner (x: 2..5, z: 2..5)
        if (x >= 2 && x <= 5 && z >= 2 && z <= 5) {
            if (y == H - 1 || y == H - 2) return Blocks.SEA_LANTERN.getDefaultState();
            boolean minWall = (x == 2 || x == 5 || z == 2 || z == 5);
            return minWall ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        // Main prayer hall (x: 7..L-2, z: 3..W-4, height 12)
        boolean inHall = x >= 7 && x <= L - 2 && z >= 3 && z <= W - 4;
        if (!inHall) {
            return (y == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.AIR.getDefaultState();
        }

        if (y == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
        if (y > 12) {
            // Contemporary stepped central dome
            double d = Math.hypot(x - (cx + 2), z - cz);
            int domeR = 7 - (y - 12);
            if (domeR >= 0 && d <= domeR) {
                return (y == 18) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }
        if (y == 12) {
            return (x % 5 == 0 && z % 5 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }
        boolean wall = (x == 7 || x == L - 2 || z == 3 || z == W - 4);
        if (wall) {
            if (x == 7 && Math.abs(z - cz) <= 2 && y <= 4) return Blocks.AIR.getDefaultState();
            if (y == 1 || y == 11 || x % 4 == 0 || z % 4 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState computeHospital(int x, int y, int z, int L, int W, int H, boolean outer) {
        int floor = y / 5;
        int ly = y % 5;
        if (y == H - 1) {
            // Rooftop helipad
            int dx = Math.abs(x - L / 2);
            int dz = Math.abs(z - W / 2);
            if (dx <= 6 && dz <= 6) {
                if ((dx == 6 || dz == 6) && (dx + dz) % 2 == 0) return Blocks.SEA_LANTERN.getDefaultState();
                boolean isH = (dz >= -2 && dz <= 2 && dx == 2) || (dx >= -2 && dx <= 2 && dz == 0);
                return isH ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
            }
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }
        if (outer) {
            if (floor == 0 && x == 0 && Math.abs(z - W / 2) <= 3 && ly <= 3) return Blocks.AIR.getDefaultState();
            // Red cross emblem on front facade
            if (x == 0 && y >= 10 && y <= 18 && Math.abs(z - W / 2) <= 4) {
                if (Math.abs(z - W / 2) <= 1 || (y >= 13 && y <= 15)) return Blocks.RED_CONCRETE.getDefaultState();
            }
            if (ly == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            if (x % 6 == 0 || z % 6 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }
        if (ly == 0) {
            return (x % 5 == 0 && z % 5 == 0) ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }
        // Divided wards along central hallway
        if (Math.abs(z - W / 2) == 3 || (Math.abs(z - W / 2) > 3 && x % 8 == 0)) {
            if (ly <= 3 && (x % 8 == 4 || Math.abs(z - W / 2) == 6)) return Blocks.AIR.getDefaultState();
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }
}
