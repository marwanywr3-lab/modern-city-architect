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

public class AirportBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int WIDTH = 80;
    private static final int LENGTH = 140;
    private static final int HEIGHT = 32;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final long totalBlocks = (long) WIDTH * LENGTH * HEIGHT;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public AirportBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        WaypointManager.addWaypoint("City Airport", anchor, 0xF59E0B);
    }

    @Override
    public String getName() { return "Modern City Airport (80x140)"; }

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
            if (curR >= WIDTH) {
                curR = 0;
                curY++;
                if (curY >= HEIGHT) {
                    curY = 0;
                    curF++;
                    if (curF >= LENGTH) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int x, int y, int z) {
        // 1. Illuminated Runway (x: 45..75, z: 5..135)
        if (x >= 45 && x <= 75 && z >= 5 && z <= 135) {
            if (y == 0) {
                if ((x == 46 || x == 74) && z % 8 == 0) return Blocks.SEA_LANTERN.getDefaultState();
                if (x == 60 && z % 10 < 5) return Blocks.WHITE_CONCRETE.getDefaultState();
                if (x == 47 || x == 73) return Blocks.YELLOW_CONCRETE.getDefaultState();
                return Blocks.GRAY_CONCRETE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        // 2. Modern Passenger Terminal (x: 4..32, z: 20..100, height 12)
        if (x >= 4 && x <= 32 && z >= 20 && z <= 100 && y <= 12) {
            boolean wall = (x == 4 || x == 32 || z == 20 || z == 100);
            if (y == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
            if (y == 12) return Blocks.WHITE_CONCRETE.getDefaultState();
            if (wall) {
                if (x == 4 && Math.abs(z - 60) <= 4 && y <= 4) return Blocks.AIR.getDefaultState();
                if (y == 1 || y == 11 || z % 10 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
                return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
            if (y == 11 && x % 8 == 0 && z % 8 == 0) return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            return Blocks.AIR.getDefaultState();
        }

        // 3. Glass Air Traffic Control Tower (x: 10..20, z: 106..116, height 30)
        if (x >= 10 && x <= 20 && z >= 106 && z <= 116) {
            if (y == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
            boolean inShaft = (x >= 13 && x <= 17 && z >= 109 && z <= 113 && y < 24);
            if (inShaft) {
                return (x == 13 || x == 17 || z == 109 || z == 113)
                        ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.AIR.getDefaultState();
            }
            if (y >= 24 && y <= 29) {
                if (y == 24 || y == 29) return Blocks.WHITE_CONCRETE.getDefaultState();
                if (x == 10 || x == 20 || z == 106 || z == 116) return Blocks.TINTED_GLASS.getDefaultState();
                return Blocks.AIR.getDefaultState();
            }
            if (y == 30 && x == 15 && z == 111) return Blocks.SEA_LANTERN.getDefaultState();
            return Blocks.AIR.getDefaultState();
        }

        // 4. Parked Voxel Airplane (near terminal gate z: 35..55, x: 35..43)
        if (z >= 36 && z <= 54 && x >= 35 && x <= 43 && y >= 2 && y <= 5) {
            if (x == 39 && y == 3) return Blocks.WHITE_CONCRETE.getDefaultState();
            if (z == 45 && y == 3) return Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
        }

        return (y == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.AIR.getDefaultState();
    }
}
