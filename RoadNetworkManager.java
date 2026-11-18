package moderncity.road;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RoadNetworkManager {

    public enum CurveType {
        STRAIGHT, CORNER90, SCURVE, UTURN
    }

    private static final double SNAP_RADIUS_SQ = 64.0; // 8-block magnetic Auto-Snap radius
    private static final List<BlockPos> ENDPOINTS = new ArrayList<>();

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
}
