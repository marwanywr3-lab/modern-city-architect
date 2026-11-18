package moderncity.road;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoadBuilderJob implements StreamingBuilderEngine.BuildJob {

    private record Placement(BlockPos pos, BlockState state) {}

    private final ServerWorld world;
    private final UUID ownerId;
    private final RoadClass roadClass;
    private final RoadNetworkManager.CurveType curveType;
    private final List<Placement> placements = new ArrayList<>();
    private int cursor = 0;

    public RoadBuilderJob(ServerWorld world, UUID ownerId, BlockPos start, BlockPos end,
                          RoadClass roadClass, RoadNetworkManager.CurveType curveType) {
        this.world = world;
        this.ownerId = ownerId;
        this.roadClass = roadClass;
        this.curveType = curveType;
        generateRoad(start, end);
        RoadNetworkManager.registerEndpoints(start, end);
    }

    private void generateRoad(BlockPos start, BlockPos end) {
        List<BlockPos> centerline = sampleCenterline(start, end);
        int baseY = start.getY();

        for (int i = 0; i < centerline.size(); i++) {
            BlockPos c = centerline.get(i);
            BlockPos prev = centerline.get(Math.max(0, i - 1));
            BlockPos next = centerline.get(Math.min(centerline.size() - 1, i + 1));

            double dx = next.getX() - prev.getX();
            double dz = next.getZ() - prev.getZ();
            double len = Math.hypot(dx, dz);
            if (len < 0.001) { dx = 1; dz = 0; len = 1; }
            double perpX = -dz / len;
            double perpZ = dx / len;

            int totalHalf = roadClass.halfWidth + roadClass.sidewalkWidth;
            for (int offset = -totalHalf; offset <= totalHalf; offset++) {
                int bx = (int) Math.round(c.getX() + perpX * offset);
                int bz = (int) Math.round(c.getZ() + perpZ * offset);
                int absOff = Math.abs(offset);

                BlockState surface;
                if (absOff <= roadClass.medianHalfWidth && roadClass != RoadClass.LOCAL) {
                    surface = (roadClass == RoadClass.AVENUE)
                            ? Blocks.GRASS_BLOCK.getDefaultState()
                            : Blocks.WHITE_CONCRETE.getDefaultState();
                } else if (absOff <= roadClass.halfWidth) {
                    // Asphalt with dashed white lane lines every 4 blocks
                    boolean isLaneDivider = (absOff == 4 || absOff == 8) && (i % 6 < 3);
                    surface = isLaneDivider ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
                } else {
                    surface = Blocks.SMOOTH_QUARTZ.getDefaultState();
                }

                BlockPos groundPos = new BlockPos(bx, baseY, bz);
                placements.add(new Placement(groundPos, surface));
                // Clear 6 blocks above road
                for (int clearY = 1; clearY <= 6; clearY++) {
                    placements.add(new Placement(groundPos.up(clearY), Blocks.AIR.getDefaultState()));
                }

                // Streetlights & Median Trees every 14 blocks
                if (i % 14 == 0) {
                    if (roadClass == RoadClass.HIGHWAY && offset == 0) {
                        placeDoubleStreetlight(groundPos, 5);
                    } else if (roadClass == RoadClass.AVENUE && offset == 0) {
                        placePalmTree(groundPos);
                    } else if (absOff == roadClass.halfWidth + 1 && (offset > 0 || roadClass == RoadClass.AVENUE)) {
                        placeSingleStreetlight(groundPos, 4);
                    }
                }
            }
        }
    }

    private void placeDoubleStreetlight(BlockPos base, int height) {
        for (int h = 1; h <= height; h++) {
            placements.add(new Placement(base.up(h), Blocks.IRON_BARS.getDefaultState()));
        }
        placements.add(new Placement(base.up(height).east(), Blocks.SEA_LANTERN.getDefaultState()));
        placements.add(new Placement(base.up(height).west(), Blocks.SEA_LANTERN.getDefaultState()));
    }

    private void placeSingleStreetlight(BlockPos base, int height) {
        for (int h = 1; h <= height; h++) {
            placements.add(new Placement(base.up(h), Blocks.IRON_BARS.getDefaultState()));
        }
        placements.add(new Placement(base.up(height + 1), Blocks.SEA_LANTERN.getDefaultState()));
    }

    private void placePalmTree(BlockPos base) {
        for (int h = 1; h <= 4; h++) {
            placements.add(new Placement(base.up(h), Blocks.BIRCH_LOG.getDefaultState()));
        }
        BlockPos top = base.up(5);
        placements.add(new Placement(top, Blocks.AZALEA_LEAVES.getDefaultState()));
        placements.add(new Placement(top.north(), Blocks.AZALEA_LEAVES.getDefaultState()));
        placements.add(new Placement(top.south(), Blocks.AZALEA_LEAVES.getDefaultState()));
        placements.add(new Placement(top.east(), Blocks.AZALEA_LEAVES.getDefaultState()));
        placements.add(new Placement(top.west(), Blocks.AZALEA_LEAVES.getDefaultState()));
    }

    private List<BlockPos> sampleCenterline(BlockPos start, BlockPos end) {
        List<BlockPos> pts = new ArrayList<>();
        double dist = Math.hypot(end.getX() - start.getX(), end.getZ() - start.getZ());
        int steps = Math.max(2, (int) Math.ceil(dist * 1.5));

        double sx = start.getX(), sz = start.getZ();
        double ex = end.getX(), ez = end.getZ();

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x, z;
            if (curveType == RoadNetworkManager.CurveType.CORNER90) {
                double cx = ex, cz = sz;
                x = (1 - t) * (1 - t) * sx + 2 * (1 - t) * t * cx + t * t * ex;
                z = (1 - t) * (1 - t) * sz + 2 * (1 - t) * t * cz + t * t * ez;
            } else if (curveType == RoadNetworkManager.CurveType.SCURVE) {
                double midX = (sx + ex) / 2.0;
                x = (1 - t) * (1 - t) * (1 - t) * sx + 3 * (1 - t) * (1 - t) * t * midX
                        + 3 * (1 - t) * t * t * midX + t * t * t * ex;
                z = (1 - t) * (1 - t) * (1 - t) * sz + 3 * (1 - t) * (1 - t) * t * sz
                        + 3 * (1 - t) * t * t * ez + t * t * t * ez;
            } else if (curveType == RoadNetworkManager.CurveType.UTURN) {
                double ctrlX = Math.max(sx, ex) + Math.max(12.0, dist * 0.6);
                x = (1 - t) * (1 - t) * (1 - t) * sx + 3 * (1 - t) * (1 - t) * t * ctrlX
                        + 3 * (1 - t) * t * t * ctrlX + t * t * t * ex;
                z = (1 - t) * (1 - t) * (1 - t) * sz + 3 * (1 - t) * (1 - t) * t * sz
                        + 3 * (1 - t) * t * t * ez + t * t * t * ez;
            } else {
                x = sx + (ex - sx) * t;
                z = sz + (ez - sz) * t;
            }
            pts.add(new BlockPos((int) Math.round(x), start.getY(), (int) Math.round(z)));
        }
        return pts;
    }

    @Override
    public String getName() { return roadClass.displayName + " (" + curveType.name() + ")"; }

    @Override
    public ServerWorld getWorld() { return world; }

    @Override
    public UUID getOwnerId() { return ownerId; }

    @Override
    public long getTotalBlocks() { return placements.size(); }

    @Override
    public long getPlacedBlocks() { return cursor; }

    @Override
    public boolean isFinished() { return cursor >= placements.size(); }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        int count = 0;
        while (count < maxBlocks && cursor < placements.size()) {
            if ((count & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
            Placement p = placements.get(cursor++);
            StreamingBuilderEngine.placeBlockFast(world, p.pos(), p.state());
            count++;
        }
        return count;
    }
}
