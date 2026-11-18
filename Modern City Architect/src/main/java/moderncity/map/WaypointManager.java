package moderncity.map;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class WaypointManager {

    public record Waypoint(String name, BlockPos pos, int color) {}

    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();

    public static synchronized void addWaypoint(String name, BlockPos pos, int color) {
        WAYPOINTS.add(new Waypoint(name, pos.toImmutable(), color));
    }

    public static synchronized List<Waypoint> getWaypoints() {
        return new ArrayList<>(WAYPOINTS);
    }
}
