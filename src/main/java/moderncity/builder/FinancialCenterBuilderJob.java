package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.map.WaypointManager;
import moderncity.road.RoadBuilderJob;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class FinancialCenterBuilderJob implements StreamingBuilderEngine.BuildJob {

    public static final int SIZE = 3000;
    private static final int CORE_PARK_SIZE = 280; // Scenic central lake, fountains, parks & cafe promenade

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final long totalBlocks = (long) CORE_PARK_SIZE * CORE_PARK_SIZE;

    private int curF = 0;
    private int curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;
    private boolean subJobsQueued = false;

    public FinancialCenterBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        WaypointManager.addWaypoint("Financial CENTER", PlacementUtil.toWorld(anchor, facing, 140, 0, 140), 0xF59E0B);
    }

    @Override public String getName() { return "Financial CENTER (3000x3000 Masterplan)"; }
    @Override public ServerWorld getWorld() { return world; }
    @Override public UUID getOwnerId() { return ownerId; }
    @Override public long getTotalBlocks() { return totalBlocks; }
    @Override public long getPlacedBlocks() { return placedBlocks; }
    @Override public boolean isFinished() { return finished; }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        if (!subJobsQueued) {
            subJobsQueued = true;
            queueFinancialDistrictMasterplan();
        }

        int processed = 0;
        int colBudget = Math.max(250, maxBlocks / 2);
        int center = CORE_PARK_SIZE / 2;

        while (processed < colBudget && !finished) {
            if ((processed & 63) == 0 && System.nanoTime() >= deadlineNanos) break;

            int x = curR;
            int z = curF;
            BlockPos ground = PlacementUtil.toWorld(anchor, facing, x, 0, z);
            double dist = Math.hypot(x - center, z - center);

            // Central Scenic Lake, Fountains, Parks & Quartz Promenades
            if (dist <= 48) {
                if (dist <= 6) {
                    // Central Illuminated Fountain Island
                    StreamingBuilderEngine.placeBlockFast(world, ground, Blocks.SMOOTH_QUARTZ.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(1), Blocks.SEA_LANTERN.getDefaultState());
                    if (dist <= 2) {
                        StreamingBuilderEngine.placeBlockFast(world, ground.up(2), Blocks.QUARTZ_PILLAR.getDefaultState());
                        StreamingBuilderEngine.placeBlockFast(world, ground.up(3), Blocks.WATER.getDefaultState());
                    }
                } else {
                    // Deep Scenic Lake with Sandy/Quartz bed
                    StreamingBuilderEngine.placeBlockFast(world, ground.down(2), Blocks.SAND.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground.down(1), Blocks.WATER.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground, Blocks.WATER.getDefaultState());
                }
            } else if (dist <= 62) {
                // Smooth Quartz Waterfront Promenade with coastal lights
                StreamingBuilderEngine.placeBlockFast(world, ground, Blocks.SMOOTH_QUARTZ.getDefaultState());
                if ((x % 16 == 0 || z % 16 == 0) && Math.abs(dist - 55) <= 2) {
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(1), Blocks.SMOOTH_QUARTZ.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(2), Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(3), Blocks.SEA_LANTERN.getDefaultState());
                }
            } else if (dist <= 115) {
                // Lush Green Gardens, Trees & Winding Quartz Walkways
                boolean onPath = (Math.abs(x - center) <= 3 || Math.abs(z - center) <= 3);
                BlockState surface = onPath ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
                StreamingBuilderEngine.placeBlockFast(world, ground, surface);
                if (!onPath && x % 14 == 0 && z % 14 == 0) {
                    // Modern landscaped tree
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(1), Blocks.BIRCH_LOG.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(2), Blocks.BIRCH_LOG.getDefaultState());
                    StreamingBuilderEngine.placeBlockFast(world, ground.up(3), Blocks.BIRCH_LOG.getDefaultState());
                    for (int lx = -1; lx <= 1; lx++) {
                        for (int lz = -1; lz <= 1; lz++) {
                            StreamingBuilderEngine.placeBlockFast(world, ground.add(lx, 4, lz), Blocks.AZALEA_LEAVES.getDefaultState());
                        }
                    }
                }
            } else {
                StreamingBuilderEngine.placeBlockFast(world, ground, Blocks.SMOOTH_QUARTZ.getDefaultState());
            }

            processed++;
            placedBlocks++;
            curR++;
            if (curR >= CORE_PARK_SIZE) {
                curR = 0;
                curF++;
                if (curF >= CORE_PARK_SIZE) finished = true;
            }
        }
        return processed;
    }

    private void queueFinancialDistrictMasterplan() {
        // 1. Grand 3000-block Cross Boulevards framing the 3000x3000 Financial CENTER
        BlockPos mid = PlacementUtil.toWorld(anchor, facing, 140, 0, 140);
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId,
                PlacementUtil.toWorld(anchor, facing, 0, 0, 140),
                PlacementUtil.toWorld(anchor, facing, SIZE, 0, 140),
                RoadClass.HIGHWAY_16, RoadNetworkManager.DirectionMode.TWO_WAY, RoadNetworkManager.CurveType.STRAIGHT));
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId,
                PlacementUtil.toWorld(anchor, facing, 140, 0, 0),
                PlacementUtil.toWorld(anchor, facing, 140, 0, SIZE),
                RoadClass.HIGHWAY_16, RoadNetworkManager.DirectionMode.TWO_WAY, RoadNetworkManager.CurveType.STRAIGHT));

        // 2. Iconic Skyscraper Cluster (KAFD-style skyline surrounding the central lake & park!)
        int[][] towerSpots = {
                {15, 15, 140}, {65, 15, 190}, {190, 15, 220}, {235, 15, 150},
                {15, 65, 175}, {235, 65, 240},
                {15, 190, 210}, {235, 190, 180},
                {15, 235, 130}, {65, 235, 200}, {190, 235, 160}, {235, 235, 120}
        };
        TowerBuilderJob.Style[] styles = TowerBuilderJob.Style.values();
        for (int i = 0; i < towerSpots.length; i++) {
            int r = towerSpots[i][0];
            int f = towerSpots[i][1];
            int h = towerSpots[i][2];
            BlockPos tAnchor = PlacementUtil.toWorld(anchor, facing, r, 0, f);
            StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, ownerId, tAnchor, facing, styles[i % styles.length], h));
        }

        // 3. Corporate HQ Towers & Cafe/Restaurant Plazas around the waterfront
        StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(
                world, ownerId, PlacementUtil.toWorld(anchor, facing, 115, 0, 15), facing,
                CorporateHQBuilderJob.HQType.HQ_TOWER_ATRIUM, 180));
        StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(
                world, ownerId, PlacementUtil.toWorld(anchor, facing, 115, 0, 235), facing,
                CorporateHQBuilderJob.HQType.HQ_TOWER_TWIN_FIN, 160));
        StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(
                world, ownerId, PlacementUtil.toWorld(anchor, facing, 15, 0, 115), facing,
                PlazaBuilderJob.Shape.U, PlazaBuilderJob.Layout.RETAIL, 3));
        StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(
                world, ownerId, PlacementUtil.toWorld(anchor, facing, 220, 0, 115), facing,
                PlazaBuilderJob.Shape.L, PlazaBuilderJob.Layout.RETAIL, 3));
    }
}
