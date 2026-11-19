package moderncity.road;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.item.ModItems;
import moderncity.item.RoadWandItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class RoadNetworkManager {

    public enum CurveType {
        FOLLOW_PATH, STRAIGHT, CORNER90, SCURVE, UTURN
    }

    public enum DirectionMode {
        ONE_WAY("One-Way (Sidewalk Lamps)"),
        TWO_WAY("Two-Way (Grass + Quartz Median)");

        public final String label;
        DirectionMode(String label) { this.label = label; }
    }

    public record UndoEntry(BlockPos pos, BlockState previousState) {}

    public record RoadSegmentRecord(
            BlockPos start,
            BlockPos end,
            RoadClass roadClass,
            DirectionMode directionMode,
            CurveType curveType,
            List<UndoEntry> undoEntries
    ) {}

    private static final double SNAP_RADIUS_SQ = 64.0;
    private static final List<BlockPos> ENDPOINTS = new ArrayList<>();
    private static final Deque<RoadSegmentRecord> ROAD_HISTORY = new ArrayDeque<>();
    private static final List<BlockPos> STREET_LAMPS = new ArrayList<>();
    private static final List<BlockPos> TRAFFIC_LIGHT_CENTERS = new ArrayList<>();
    private static int trafficPhase = 0;

    private static boolean autoNightLighting = false;
    private static boolean manualLitState = true;
    private static Boolean lastAppliedLit = null;

    public static synchronized BlockPos autoSnap(BlockPos target) {
        BlockPos best = target;
        double bestDistSq = SNAP_RADIUS_SQ;
        for (BlockPos ep : ENDPOINTS) {
            if (Math.abs(ep.getY() - target.getY()) <= 4) {
                double dSq = ep.getSquaredDistance(target.getX(), ep.getY(), target.getZ());
                if (dSq < bestDistSq) {
                    bestDistSq = dSq;
                    best = new BlockPos(ep.getX(), target.getY(), ep.getZ());
                }
            }
        }
        return best;
    }

    public static synchronized void registerEndpoints(BlockPos start, BlockPos end) {
        ENDPOINTS.add(start.toImmutable());
        ENDPOINTS.add(end.toImmutable());
    }

    public static synchronized void recordRoadSegment(RoadSegmentRecord record) {
        ROAD_HISTORY.push(record);
        if (ROAD_HISTORY.size() > 25) ROAD_HISTORY.removeLast();
    }

    public static synchronized void registerTrafficLightCenter(BlockPos center) { TRAFFIC_LIGHT_CENTERS.add(center.toImmutable()); }

    public static synchronized void tickTrafficLights(MinecraftServer server) {
        ServerWorld w = server.getOverworld();
        if (w == null || TRAFFIC_LIGHT_CENTERS.isEmpty()) return;
        trafficPhase = (trafficPhase + 1) % 3;
        BlockState active = switch (trafficPhase) {
            case 0 -> Blocks.LIME_CONCRETE.getDefaultState();
            case 1 -> Blocks.YELLOW_CONCRETE.getDefaultState();
            default -> Blocks.RED_CONCRETE.getDefaultState();
        };
        for (BlockPos c : TRAFFIC_LIGHT_CENTERS) {
            if (w.isChunkLoaded(c)) {
                for (int dx = -15; dx <= 15; dx++) {
                    for (int dz = -15; dz <= 15; dz++) {
                        BlockPos p = c.add(dx, 5, dz);
                        BlockState cur = w.getBlockState(p);
                        if (cur.isOf(Blocks.RED_CONCRETE) || cur.isOf(Blocks.YELLOW_CONCRETE) || cur.isOf(Blocks.LIME_CONCRETE)) {
                            StreamingBuilderEngine.placeBlockFast(w, p, active);
                        }
                    }
                }
            }
        }
    }

    public static synchronized void registerStreetLamp(BlockPos pos) {
        STREET_LAMPS.add(pos.toImmutable());
    }

    public static synchronized boolean undoLastRoad(ServerWorld world) {
        if (ROAD_HISTORY.isEmpty()) return false;
        RoadSegmentRecord last = ROAD_HISTORY.pop();
        List<UndoEntry> entries = last.undoEntries();
        for (int i = entries.size() - 1; i >= 0; i--) {
            UndoEntry e = entries.get(i);
            StreamingBuilderEngine.placeBlockFast(world, e.pos(), e.previousState());
        }
        return true;
    }

    public static synchronized boolean upgradeLastRoad(ServerWorld world, UUID ownerId) {
        if (ROAD_HISTORY.isEmpty()) return false;
        RoadSegmentRecord last = ROAD_HISTORY.peek();
        RoadClass nextClass = last.roadClass().next();
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId, last.start(), last.end(), nextClass, last.directionMode(), last.curveType()
        ));
        return true;
    }

    public static synchronized boolean repairLastRoad(ServerWorld world, UUID ownerId) {
        if (ROAD_HISTORY.isEmpty()) return false;
        RoadSegmentRecord last = ROAD_HISTORY.peek();
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId, last.start(), last.end(), last.roadClass(), last.directionMode(), last.curveType()
        ));
        return true;
    }

    public static boolean isAutoNightLighting() { return autoNightLighting; }
    public static boolean isManualLitState() { return manualLitState; }

    public static boolean toggleManualLights(MinecraftServer server) {
        autoNightLighting = false;
        manualLitState = !manualLitState;
        applyLampState(server.getOverworld(), manualLitState);
        lastAppliedLit = manualLitState;
        return manualLitState;
    }

    public static boolean toggleAutoNightLighting(MinecraftServer server) {
        autoNightLighting = !autoNightLighting;
        lastAppliedLit = null;
        if (autoNightLighting) {
            tickNightLighting(server);
        } else {
            applyLampState(server.getOverworld(), manualLitState);
            lastAppliedLit = manualLitState;
        }
        return autoNightLighting;
    }

    public static void forceLightingState(MinecraftServer server, boolean lit) {
        autoNightLighting = false;
        manualLitState = lit;
        applyLampState(server.getOverworld(), lit);
        lastAppliedLit = lit;
    }

    public static void tickNightLighting(MinecraftServer server) {
        if (!autoNightLighting || STREET_LAMPS.isEmpty()) return;
        ServerWorld world = server.getOverworld();
        if (world == null) return;
        long timeOfDay = world.getTimeOfDay() % 24000L;
        boolean shouldBeLit = timeOfDay >= 12500L && timeOfDay <= 23500L;
        if (lastAppliedLit == null || lastAppliedLit != shouldBeLit) {
            applyLampState(world, shouldBeLit);
            lastAppliedLit = shouldBeLit;
        }
    }

    private static synchronized void applyLampState(ServerWorld world, boolean lit) {
        if (world == null) return;
        BlockState targetState = lit
                ? Blocks.SEA_LANTERN.getDefaultState()
                : Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, false);
        for (BlockPos pos : STREET_LAMPS) {
            if (world.isChunkLoaded(pos)) {
                BlockState current = world.getBlockState(pos);
                if (current.isOf(Blocks.REDSTONE_LAMP) || current.isOf(Blocks.SEA_LANTERN)) {
                    StreamingBuilderEngine.placeBlockFast(world, pos, targetState);
                }
            }
        }
    }

    public static void tickRoadPreview(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.getMainHandStack().isOf(ModItems.ROAD_WAND)) continue;
            RoadWandItem.WandState state = RoadWandItem.getState(player.getUuid());
            if (state.firstPoint == null) continue;

            // Record smooth path control points as the player walks/flies & turns left/right
            BlockPos currentPos = new BlockPos(player.getBlockX(), state.firstPoint.getY(), player.getBlockZ());
            if (state.curveType == CurveType.FOLLOW_PATH) {
                BlockPos lastPt = state.pathPoints.isEmpty() ? state.firstPoint : state.pathPoints.get(state.pathPoints.size() - 1);
                if (lastPt.getSquaredDistance(currentPos) >= 64.0) { // every 8 blocks
                    state.pathPoints.add(currentPos);
                }
            }

            HitResult hit = player.raycast(60.0, 0.0f, false);
            BlockPos target = (hit.getType() == HitResult.Type.BLOCK)
                    ? ((BlockHitResult) hit).getBlockPos()
                    : BlockPos.ofFloored(hit.getPos());
            target = autoSnap(target);

            ServerWorld world = player.getServerWorld();
            List<BlockPos> previewPts = new ArrayList<>();
            previewPts.add(state.firstPoint);
            if (state.curveType == CurveType.FOLLOW_PATH) {
                previewPts.addAll(state.pathPoints);
            }
            previewPts.add(target);

            for (int seg = 0; seg < previewPts.size() - 1; seg++) {
                BlockPos a = previewPts.get(seg);
                BlockPos b = previewPts.get(seg + 1);
                double dx = b.getX() - a.getX();
                double dz = b.getZ() - a.getZ();
                int steps = Math.max(2, (int) (Math.hypot(dx, dz) / 2.5));
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    world.spawnParticles(player, ParticleTypes.END_ROD, true,
                            a.getX() + dx * t + 0.5, state.firstPoint.getY() + 1.3, a.getZ() + dz * t + 0.5,
                            1, 0, 0, 0, 0);
                }
            }
        }
    }
}
