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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RoadBuilderJob implements StreamingBuilderEngine.BuildJob {

    private record Placement(BlockPos pos, BlockState state) {}

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
        generateRoad(start, end, waypoints, startFacing);
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

    private void generateRoad(BlockPos start, BlockPos end, List<BlockPos> waypoints, Direction startFacing) {
        List<BlockPos> centerline = sampleCenterline(start, end, waypoints, startFacing);
        int baseY = start.getY();

        int medianHalf = (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY) ? 2 : -1;
        int asphaltHalf = (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY)
                ? (2 + roadClass.roadWidth)
                : (roadClass.roadWidth / 2);
        int totalHalf = asphaltHalf + roadClass.sidewalkWidth;

        for (int i = 0; i < centerline.size(); i++) {
            BlockPos c = centerline.get(i);
            BlockPos prev = centerline.get(Math.max(0, i - 2));
            BlockPos next = centerline.get(Math.min(centerline.size() - 1, i + 2));

            double dx = next.getX() - prev.getX();
            double dz = next.getZ() - prev.getZ();
            double len = Math.hypot(dx, dz);
            if (len < 0.001) { dx = 1; dz = 0; len = 1; }
            double perpX = -dz / len;
            double perpZ = dx / len;

            BlockPos checkPos = new BlockPos(c.getX(), baseY, c.getZ());
            boolean isBridge = world.getFluidState(checkPos).isIn(FluidTags.WATER)
                    || world.getFluidState(checkPos.down()).isIn(FluidTags.WATER);
            boolean isTunnel = !isBridge && world.getBlockState(checkPos.up(4)).isSolidBlock(world, checkPos.up(4));
            boolean isCrosswalk = !isBridge && !isTunnel && (i == 4 || i == centerline.size() - 5);

            // Half-block cross-section stepping eliminates any diagonal/curve gaps!
            for (double offD = -totalHalf; offD <= totalHalf; offD += 0.5) {
                int offset = (int) Math.round(offD);
                int bx = (int) Math.round(c.getX() + perpX * offD);
                int bz = (int) Math.round(c.getZ() + perpZ * offD);
                int absOff = Math.abs(offset);

                BlockState surface;
                boolean isMedianCenter = (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY && offset == 0);
                boolean isMedian = (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY && absOff <= medianHalf);

                if (isMedian) {
                    if (absOff == medianHalf) {
                        surface = Blocks.SMOOTH_QUARTZ.getDefaultState();
                    } else {
                        surface = isBridge ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
                    }
                } else if (absOff <= asphaltHalf) {
                    if (isCrosswalk && (absOff % 2 == 0)) {
                        surface = Blocks.WHITE_CONCRETE.getDefaultState();
                    } else {
                        int distInCarriageway = (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY)
                                ? (absOff - medianHalf - 1)
                                : (offset + asphaltHalf);
                        boolean isEdge = (absOff == asphaltHalf) || (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY && absOff == medianHalf + 1);
                        boolean isLaneLine = !isEdge && roadClass.roadWidth >= 7 && (distInCarriageway % 4 == 0) && ((i / 2) % 6 < 3);
                        if (isEdge && roadClass.isHighway()) {
                            surface = Blocks.YELLOW_CONCRETE.getDefaultState();
                        } else if (isLaneLine) {
                            surface = Blocks.WHITE_CONCRETE.getDefaultState();
                        } else {
                            surface = Blocks.GRAY_CONCRETE.getDefaultState();
                        }
                    }
                } else {
                    surface = Blocks.SMOOTH_QUARTZ.getDefaultState();
                }

                BlockPos groundPos = new BlockPos(bx, baseY, bz);
                addPlacement(groundPos, surface);

                if (isBridge) {
                    addPlacement(groundPos.down(), Blocks.WHITE_CONCRETE.getDefaultState());
                    if (absOff == totalHalf) {
                        addPlacement(groundPos.up(), Blocks.STONE_BRICK_WALL.getDefaultState());
                    } else {
                        for (int clearY = 1; clearY <= 5; clearY++) addPlacement(groundPos.up(clearY), Blocks.AIR.getDefaultState());
                    }
                    if (i % 24 == 0 && (absOff == asphaltHalf || offset == 0)) {
                        for (int py = 2; py <= 24; py++) {
                            BlockPos pillarPos = groundPos.down(py);
                            if (world.getBlockState(pillarPos).isSolidBlock(world, pillarPos)) break;
                            addPlacement(pillarPos, Blocks.GRAY_CONCRETE.getDefaultState());
                        }
                    }
                } else if (isTunnel) {
                    for (int clearY = 1; clearY <= 5; clearY++) {
                        addPlacement(groundPos.up(clearY), (absOff == totalHalf)
                                ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.AIR.getDefaultState());
                    }
                    BlockPos roofPos = groundPos.up(6);
                    addPlacement(roofPos, (offset == 0 && i % 12 == 0)
                            ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
                } else {
                    for (int clearY = 1; clearY <= 6; clearY++) addPlacement(groundPos.up(clearY), Blocks.AIR.getDefaultState());
                    if (i % 28 == 0 && Math.abs(offD - offset) < 0.01) {
                        if (directionMode == RoadNetworkManager.DirectionMode.TWO_WAY && isMedianCenter) {
                            placeStreetlight(groundPos, roadClass.isHighway() ? 5 : 4, true);
                        } else if (directionMode == RoadNetworkManager.DirectionMode.ONE_WAY && offset == asphaltHalf + 1) {
                            placeStreetlight(groundPos, 4, false);
                        }
                    }
                }
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

    private List<BlockPos> sampleCenterline(BlockPos start, BlockPos end, List<BlockPos> waypoints, Direction startFacing) {
        List<BlockPos> pts = new ArrayList<>();

        if (curveType == RoadNetworkManager.CurveType.FOLLOW_PATH && !waypoints.isEmpty()) {
            List<BlockPos> ctrl = new ArrayList<>();
            ctrl.add(start);
            ctrl.addAll(waypoints);
            ctrl.add(end);

            // Smooth Catmull-Rom spline through all recorded turn points
            for (int seg = 0; seg < ctrl.size() - 1; seg++) {
                BlockPos p0 = ctrl.get(Math.max(0, seg - 1));
                BlockPos p1 = ctrl.get(seg);
                BlockPos p2 = ctrl.get(seg + 1);
                BlockPos p3 = ctrl.get(Math.min(ctrl.size() - 1, seg + 2));

                double segDist = Math.hypot(p2.getX() - p1.getX(), p2.getZ() - p1.getZ());
                int steps = Math.max(4, (int) Math.ceil(segDist * 2.0));
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    double t2 = t * t;
                    double t3 = t2 * t;
                    double x = 0.5 * ((2 * p1.getX()) + (-p0.getX() + p2.getX()) * t
                            + (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t2
                            + (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t3);
                    double z = 0.5 * ((2 * p1.getZ()) + (-p0.getZ() + p2.getZ()) * t
                            + (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t2
                            + (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t3);
                    pts.add(new BlockPos((int) Math.round(x), start.getY(), (int) Math.round(z)));
                }
            }
            return pts;
        }

        double dist = Math.hypot(end.getX() - start.getX(), end.getZ() - start.getZ());
        int steps = Math.max(4, (int) Math.ceil(dist * 2.0));
        double sx = start.getX(), sz = start.getZ();
        double ex = end.getX(), ez = end.getZ();

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x, z;
            if (curveType == RoadNetworkManager.CurveType.CORNER90) {
                // Turn cleanly using player's starting facing direction!
                boolean alongX = (startFacing == Direction.EAST || startFacing == Direction.WEST);
                double cx = alongX ? ex : sx;
                double cz = alongX ? sz : ez;
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
