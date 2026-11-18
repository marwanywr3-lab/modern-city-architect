package moderncity.road;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class RoadNetworkManager {

    public enum CurveType {
        STRAIGHT, CORNER90, SCURVE, UTURN
    }

    public record RoadSegmentRecord(
            BlockPos start,
            BlockPos end,
            RoadClass roadClass,
            CurveType curveType,
            List<BlockPos> placedPositions
    ) {}

    private static final double SNAP_RADIUS_SQ = 64.0;
    private static final List<BlockPos> ENDPOINTS = new ArrayList<>();
    private static final Deque<RoadSegmentRecord> ROAD_HISTORY = new ArrayDeque<>();
    private static final List<BlockPos> STREET_LAMPS = new ArrayList<>();

    private static boolean autoNightLighting = true;
    private static Boolean lastLitState = null;

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
        if (ROAD_HISTORY.size() > 25) {
            ROAD_HISTORY.removeLast();
        }
    }

    public static synchronized void registerStreetLamp(BlockPos pos) {
        STREET_LAMPS.add(pos.toImmutable());
    }

    public static synchronized boolean undoLastRoad(ServerWorld world) {
        if (ROAD_HISTORY.isEmpty()) return false;
        RoadSegmentRecord last = ROAD_HISTORY.pop();
        for (BlockPos pos : last.placedPositions()) {
            StreamingBuilderEngine.placeBlockFast(world, pos, Blocks.AIR.getDefaultState());
        }
        return true;
    }

    public static synchronized boolean upgradeLastRoad(ServerWorld world, java.util.UUID ownerId) {
        if (ROAD_HISTORY.isEmpty()) return false;
        RoadSegmentRecord last = ROAD_HISTORY.peek();
        RoadClass nextClass = switch (last.roadClass()) {
            case LOCAL -> RoadClass.AVENUE;
            case AVENUE -> RoadClass.HIGHWAY;
            case HIGHWAY -> RoadClass.HIGHWAY;
        };
        if (nextClass == last.roadClass()) return false;
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId, last.start(), last.end(), nextClass, last.curveType()
        ));
        return true;
    }

    public static synchronized boolean repairLastRoad(ServerWorld world, java.util.UUID ownerId) {
        if (ROAD_HISTORY.isEmpty()) return false;
        RoadSegmentRecord last = ROAD_HISTORY.peek();
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId, last.start(), last.end(), last.roadClass(), last.curveType()
        ));
        return true;
    }

    public static boolean isAutoNightLighting() {
        return autoNightLighting;
    }

    public static boolean toggleAutoNightLighting(MinecraftServer server) {
        autoNightLighting = !autoNightLighting;
        lastLitState = null;
        tickNightLighting(server);
        return autoNightLighting;
    }

    public static void forceLightingState(MinecraftServer server, boolean lit) {
        autoNightLighting = false;
        applyLampState(server.getOverworld(), lit);
        lastLitState = lit;
    }

    public static void tickNightLighting(MinecraftServer server) {
        if (!autoNightLighting || STREET_LAMPS.isEmpty()) return;
        ServerWorld world = server.getOverworld();
        if (world == null) return;
        long timeOfDay = world.getTimeOfDay() % 24000L;
        boolean shouldBeLit = timeOfDay >= 12500L && timeOfDay <= 23500L;
        if (lastLitState == null || lastLitState != shouldBeLit) {
            applyLampState(world, shouldBeLit);
            lastLitState = shouldBeLit;
        }
    }

    private static synchronized void applyLampState(ServerWorld world, boolean lit) {
        BlockState lampState = Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, lit);
        for (BlockPos pos : STREET_LAMPS) {
            if (world.isChunkLoaded(pos)) {
                BlockState current = world.getBlockState(pos);
                if (current.isOf(Blocks.REDSTONE_LAMP) || current.isOf(Blocks.SEA_LANTERN)) {
                    StreamingBuilderEngine.placeBlockFast(world, pos, lampState);
                }
            }
        }
    }
}
