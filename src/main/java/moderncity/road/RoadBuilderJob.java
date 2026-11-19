package moderncity.road;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RoadBuilderJob implements StreamingBuilderEngine.BuildJob {

    private record Placement(BlockPos pos, BlockState state) {}
    private record SamplePt(double x, double z, double arcLen) {}

    private final ServerWorld world;
    private final UUID ownerId;
    private final RoadClass roadClass;
    private final RoadNetworkManager.DirectionMode directionMode;
    private final RoadNetworkManager.CurveType curveType;
    private final List<Placement> placements = new ArrayList<>();
    private final List<RoadNetworkManager.UndoEntry> undoEntries = new ArrayList<>();
    private final Set<BlockPos> recordedSet = new HashSet<>();
    private int cursor = 0;

    public RoadBuilderJob(ServerWorld world, UUID ownerId, BlockPos start, BlockPos end,
                          RoadClass roadClass, RoadNetworkManager.DirectionMode directionMode,
                          RoadNetworkManager.CurveType curveType) {
        this(world, ownerId, start, end, List.of(), Direction.NORTH, roadClass, directionMode, curveType);
    }

    public RoadBuilderJob(ServerWorld world, UUID ownerId, BlockPos start, BlockPos end,
                          List<BlockPos> waypoints, Direction startFacing,
                          RoadClass roadClass, RoadNetworkManager.DirectionMode directionMode,
                          RoadNetworkManager.CurveType curveType) {
        this.world = world;
        this.ownerId = ownerId;
        this.roadClass = roadClass;
        this.directionMode = directionMode;
        this.curveType = curveType;
        generateRoadDistanceField(start, end, waypoints, startFacing);
        RoadNetworkManager.registerEndpoints(start, end);
        RoadNetworkManager.recordRoadSegment(new RoadNetworkManager.RoadSegmentRecord(
                start, end, roadClass, directionMode, curveType, undoEntries
        ));
    }

    private void addPlacement(BlockPos pos, BlockState state) {
        BlockPos imm = pos.toImmutable();
        if (recordedSet.add(imm)) {
            undoEntries.add(new RoadNetworkManager.UndoEntry(imm, world.getBlockState(imm)));
        }
        placements.add(new Placement(imm, state));
    }

    private void generateRoadDistanceField(BlockPos start, BlockPos end, List<BlockPos> waypoints, Direction startFacing) {
        List<SamplePt> curve = sampleCenterline(start, end, waypoints, startFacing);
        int baseY = start.getY();

        boolean isTwoWay = (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY);
        double medianHalf = isTwoWay ? 2.2 : -1.0;
        double asphaltHalf = isTwoWay ? (2.2 + roadClass.roadWidth) : (roadClass.roadWidth / 2.0);
        double totalHalf = asphaltHalf + roadClass.sidewalkWidth;
        double totalHalfSq = (totalHalf + 0.6) * (totalHalf + 0.6);

        // Compute tight bounding box around curve
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (SamplePt pt : curve) {
            minX = Math.min(minX, (int) Math.floor(pt.x() - totalHalf - 2));
            maxX = Math.max(maxX, (int) Math.ceil(pt.x() + totalHalf + 2));
            minZ = Math.min(minZ, (int) Math.floor(pt.z() - totalHalf - 2));
            maxZ = Math.max(maxZ, (int) Math.ceil(pt.z() + totalHalf + 2));
        }

        double totalArc = curve.get(curve.size() - 1).arcLen();

        // Spatial grid bucketing for ultra-fast nearest-curve lookup
        int cellSize = 8;
        Map<Long, List<SamplePt>> grid = new HashMap<>();
        for (SamplePt pt : curve) {
            int cx = (int) Math.floor(pt.x() / cellSize);
            int cz = (int) Math.floor(pt.z() / cellSize);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    long key = (((long) (cx + dx)) << 32) | ((cz + dz) & 0xFFFFFFFFL);
                    grid.computeIfAbsent(key, k -> new ArrayList<>()).add(pt);
                }
            }
        }

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                long key = (((long) (int) Math.floor((double) bx / cellSize)) << 32)
                        | (((int) Math.floor((double) bz / cellSize)) & 0xFFFFFFFFL);
                List<SamplePt> nearby = grid.get(key);
                if (nearby == null) continue;

                double bestDistSq = Double.MAX_VALUE;
                SamplePt nearest = null;
                for (SamplePt pt : nearby) {
                    double dx = bx - pt.x();
                    double dz = bz - pt.z();
                    double dSq = dx * dx + dz * dz;
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        nearest = pt;
                    }
                }
                if (nearest == null || bestDistSq > totalHalfSq) continue;

                double d = Math.sqrt(bestDistSq);
                double s = nearest.arcLen();

                BlockPos checkPos = new BlockPos((int) Math.round(nearest.x()), baseY, (int) Math.round(nearest.z()));
                boolean isBridge = world.getFluidState(checkPos).isIn(FluidTags.WATER)
                        || world.getFluidState(checkPos.down()).isIn(FluidTags.WATER);
                boolean isTunnel = !isBridge && world.getBlockState(checkPos.up(4)).isSolidBlock(world, checkPos.up(4));
                boolean isCrosswalk = !isBridge && !isTunnel && ((s >= 2.0 && s <= 4.5) || (s >= totalArc - 4.5 && s <= totalArc - 2.0));

                BlockState surface;
                if (isTwoWay && d <= medianHalf) {
                    surface = (d >= 1.2)
                            ? Blocks.SMOOTH_QUARTZ.getDefaultState()
                            : (isBridge ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
                } else if (d <= asphaltHalf) {
                    if (isCrosswalk && ((int) Math.round(d)) % 2 == 0) {
                        surface = Blocks.WHITE_CONCRETE.getDefaultState();
                    } else {
                        double laneDist = isTwoWay ? (d - medianHalf) : (d + roadClass.roadWidth / 2.0);
                        boolean isEdge = Math.abs(d - asphaltHalf) < 0.6 || (isTwoWay && Math.abs(d - medianHalf) < 0.6);
                        boolean isDashedLine = !isEdge && roadClass.roadWidth >= 7
                                && Math.abs(laneDist - Math.round(laneDist / 4.0) * 4.0) < 0.45
                                && ((int) s) % 8 < 4;
                        if (isEdge && roadClass.isHighway()) surface = Blocks.YELLOW_CONCRETE.getDefaultState();
                        else if (isDashedLine) surface = Blocks.WHITE_CONCRETE.getDefaultState();
                        else surface = Blocks.GRAY_CONCRETE.getDefaultState();
                    }
                } else {
                    surface = Blocks.SMOOTH_QUARTZ.getDefaultState();
                }

                BlockPos groundPos = new BlockPos(bx, baseY, bz);
                addPlacement(groundPos, surface);

                if (isBridge) {
                    addPlacement(groundPos.down(), Blocks.WHITE_CONCRETE.getDefaultState());
                    if (d >= totalHalf - 0.6) {
                        addPlacement(groundPos.up(), Blocks.STONE_BRICK_WALL.getDefaultState());
                    } else {
                        for (int cy = 1; cy <= 5; cy++) addPlacement(groundPos.up(cy), Blocks.AIR.getDefaultState());
                    }
                    if (((int) s) % 14 == 0 && (d < 0.8 || Math.abs(d - asphaltHalf) < 0.6)) {
                        for (int py = 2; py <= 24; py++) {
                            BlockPos pillarPos = groundPos.down(py);
                            if (world.getBlockState(pillarPos).isSolidBlock(world, pillarPos)) break;
                            addPlacement(pillarPos, Blocks.GRAY_CONCRETE.getDefaultState());
                        }
                    }
                } else if (isTunnel) {
                    for (int cy = 1; cy <= 5; cy++) {
                        addPlacement(groundPos.up(cy), (d >= totalHalf - 0.6)
                                ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.AIR.getDefaultState());
                    }
                    addPlacement(groundPos.up(6), (d < 0.8 && ((int) s) % 8 == 0)
                            ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
                } else {
                    for (int cy = 1; cy <= 6; cy++) addPlacement(groundPos.up(cy), Blocks.AIR.getDefaultState());
                }
            }
        }

        // Place clean Streetlights along the curve every 16 blocks
        for (int i = 0; i < curve.size(); i += 32) {
            SamplePt pt = curve.get(i);
            BlockPos base = new BlockPos((int) Math.round(pt.x()), baseY, (int) Math.round(pt.z()));
            if (isTwoWay) {
                placeStreetlight(base, roadClass.isHighway() ? 5 : 4, true);
            } else {
                SamplePt next = curve.get(Math.min(curve.size() - 1, i + 2));
                double dx = next.x() - pt.x(), dz = next.z() - pt.z();
                double len = Math.max(0.001, Math.hypot(dx, dz));
                int lx = (int) Math.round(pt.x() + (-dz / len) * (asphaltHalf + 1));
                int lz = (int) Math.round(pt.z() + (dx / len) * (asphaltHalf + 1));
                placeStreetlight(new BlockPos(lx, baseY, lz), 4, false);
            }
        }
    }

    private void placeStreetlight(BlockPos base, int height, boolean doubleHead) {
        addPlacement(base.up(1), Blocks.SMOOTH_QUARTZ.getDefaultState());
        for (int h = 2; h <= height; h++) addPlacement(base.up(h), Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState());
        addPlacement(base.up(height + 1), Blocks.WHITE_CONCRETE.getDefaultState());
        BlockState lamp = RoadNetworkManager.isManualLitState()
                ? Blocks.SEA_LANTERN.getDefaultState()
                : Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, false);
        if (doubleHead) {
            addPlacement(base.up(height + 1).east(), Blocks.WHITE_CONCRETE.getDefaultState());
            addPlacement(base.up(height + 1).west(), Blocks.WHITE_CONCRETE.getDefaultState());
            BlockPos p1 = base.up(height).east();
            BlockPos p2 = base.up(height).west();
            addPlacement(p1, lamp);
            addPlacement(p2, lamp);
            RoadNetworkManager.registerStreetLamp(p1);
            RoadNetworkManager.registerStreetLamp(p2);
        } else {
            BlockPos p = base.up(height + 1);
            addPlacement(p, lamp);
            RoadNetworkManager.registerStreetLamp(p);
        }
    }

    private List<SamplePt> sampleCenterline(BlockPos start, BlockPos end, List<BlockPos> waypoints, Direction startFacing) {
        List<double[]> raw = new ArrayList<>();

        if (curveType == RoadNetworkManager.CurveType.FOLLOW_PATH && !waypoints.isEmpty()) {
            List<BlockPos> ctrl = new ArrayList<>();
            ctrl.add(start);
            ctrl.addAll(waypoints);
            ctrl.add(end);
            for (int seg = 0; seg < ctrl.size() - 1; seg++) {
                BlockPos p0 = ctrl.get(Math.max(0, seg - 1));
                BlockPos p1 = ctrl.get(seg);
                BlockPos p2 = ctrl.get(seg + 1);
                BlockPos p3 = ctrl.get(Math.min(ctrl.size() - 1, seg + 2));
                double segDist = Math.hypot(p2.getX() - p1.getX(), p2.getZ() - p1.getZ());
                int steps = Math.max(8, (int) Math.ceil(segDist * 2.5));
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps, t2 = t * t, t3 = t2 * t;
                    double x = 0.5 * ((2 * p1.getX()) + (-p0.getX() + p2.getX()) * t
                            + (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t2
                            + (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t3);
                    double z = 0.5 * ((2 * p1.getZ()) + (-p0.getZ() + p2.getZ()) * t
                            + (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t2
                            + (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t3);
                    raw.add(new double[]{x, z});
                }
            }
        } else {
            double dist = Math.hypot(end.getX() - start.getX(), end.getZ() - start.getZ());
            int steps = Math.max(8, (int) Math.ceil(dist * 2.5));
            double sx = start.getX(), sz = start.getZ();
            double ex = end.getX(), ez = end.getZ();
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x, z;
                if (curveType == RoadNetworkManager.CurveType.CORNER90) {
                    boolean alongX = (startFacing == Direction.EAST || startFacing == Direction.WEST);
                    double cx = alongX ? ex : sx, cz = alongX ? sz : ez;
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
                raw.add(new double[]{x, z});
            }
        }

        List<SamplePt> pts = new ArrayList<>();
        double arc = 0.0;
        for (int i = 0; i < raw.size(); i++) {
            if (i > 0) {
                arc += Math.hypot(raw.get(i)[0] - raw.get(i - 1)[0], raw.get(i)[1] - raw.get(i - 1)[1]);
            }
            pts.add(new SamplePt(raw.get(i)[0], raw.get(i)[1], arc));
        }
        return pts;
    }

    @Override public String getName() { return roadClass.displayName + " (" + directionMode.name() + ")"; }
    @Override public ServerWorld getWorld() { return world; }
    @Override public UUID getOwnerId() { return ownerId; }
    @Override public long getTotalBlocks() { return placements.size(); }
    @Override public long getPlacedBlocks() { return cursor; }
    @Override public boolean isFinished() { return cursor >= placements.size(); }

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
