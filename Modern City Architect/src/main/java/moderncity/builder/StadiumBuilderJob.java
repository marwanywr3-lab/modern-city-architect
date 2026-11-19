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

public class StadiumBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int WIDTH = 110;
    private static final int LENGTH = 140;
    private static final int HEIGHT = 30;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final long totalBlocks = (long) WIDTH * LENGTH * HEIGHT;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public StadiumBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        WaypointManager.addWaypoint("International Stadium", anchor, 0x22C55E);
    }

    @Override public String getName() { return "Modern International Stadium (110x140)"; }
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
        double cx = WIDTH / 2.0;
        double cz = LENGTH / 2.0;
        double normDist = Math.hypot((x - cx) / (WIDTH / 2.0 - 2), (z - cz) / (LENGTH / 2.0 - 2));

        if (normDist > 1.0) return Blocks.AIR.getDefaultState();

        // Football Pitch in center (x: 25..85, z: 25..115)
        boolean inPitch = (x >= 25 && x <= 85 && z >= 25 && z <= 115);
        if (inPitch) {
            if (y == 0) {
                boolean line = (x == 25 || x == 85 || z == 25 || z == 115 || z == 70);
                if (line) return Blocks.WHITE_CONCRETE.getDefaultState();
                return (z % 10 < 5) ? Blocks.GREEN_CONCRETE.getDefaultState() : Blocks.LIME_CONCRETE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        // Stadium Tiered Stands & Roof Canopy
        double standDist = normDist;
        int tierHeight = (int) Math.round((standDist - 0.55) * 38.0);
        if (tierHeight < 1) tierHeight = 1;

        // Outer Stadium Facade
        if (normDist >= 0.96 && y <= 24) {
            return (y % 4 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }

        // Cantilevered Roof Ring & Floodlights at y=25..26
        if (normDist >= 0.62 && (y == 25 || y == 26)) {
            if (y == 25 && normDist <= 0.66 && (x + z) % 6 == 0) return Blocks.SEA_LANTERN.getDefaultState();
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }

        if (y <= tierHeight && y <= 20) {
            return (y == tierHeight) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }
}
