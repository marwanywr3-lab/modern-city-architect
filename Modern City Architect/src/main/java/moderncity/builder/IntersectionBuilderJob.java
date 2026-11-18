package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class IntersectionBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum Type { FOUR_WAY, T_JUNCTION, Y_JUNCTION, ROUNDABOUT }

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos center;
    private final Type type;
    private final RoadClass roadClass;

    private final int radius;
    private final int size;
    private final long totalBlocks;

    private int curX = 0;
    private int curY = 0;
    private int curZ = 0;
    private long placedBlocks = 0;
    private boolean finished = false;
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public IntersectionBuilderJob(ServerWorld world, UUID ownerId, BlockPos center, Type type, RoadClass roadClass) {
        this.world = world;
        this.ownerId = ownerId;
        this.center = center;
        this.type = type;
        this.roadClass = roadClass;
        int halfRoad = roadClass.roadWidth / 2 + 2;
        this.radius = (type == Type.ROUNDABOUT) ? (halfRoad + 14) : (halfRoad + 8);
        this.size = radius * 2 + 1;
        this.totalBlocks = (long) size * size * 7;

        RoadNetworkManager.registerEndpoints(center.add(-radius, 0, 0), center.add(radius, 0, 0));
        RoadNetworkManager.registerEndpoints(center.add(0, 0, -radius), center.add(0, 0, radius));
    }

    @Override
    public String getName() { return type.name() + " Intersection (" + roadClass.name() + ")"; }

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
            int rx = curX - radius;
            int rz = curZ - radius;
            BlockState state = computeBlock(rx, curY, rz);
            if (state != null) {
                mutablePos.set(center.getX() + rx, center.getY() + curY, center.getZ() + rz);
                StreamingBuilderEngine.placeBlockFast(world, mutablePos, state);
            }
            processed++;
            placedBlocks++;
            curZ++;
            if (curZ >= size) {
                curZ = 0;
                curY++;
                if (curY >= 7) {
                    curY = 0;
                    curX++;
                    if (curX >= size) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int rx, int y, int rz) {
        int roadW = roadClass.roadWidth / 2 + 1;
        int totalW = roadW + roadClass.sidewalkWidth;

        if (type == Type.ROUNDABOUT) {
            double dist = Math.hypot(rx, rz);
            int islandR = 7;
            int outerRoadR = islandR + roadW + 3;
            boolean onArm = (Math.abs(rx) <= totalW || Math.abs(rz) <= totalW);

            if (dist <= islandR) {
                if (y == 0) {
                    if (dist <= 2.5) return Blocks.WATER.getDefaultState();
                    if (dist >= islandR - 1) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                    return Blocks.GRASS_BLOCK.getDefaultState();
                }
                if (y == 1 && dist <= 1.2) return Blocks.SEA_LANTERN.getDefaultState();
                return Blocks.AIR.getDefaultState();
            }

            if (dist <= outerRoadR || onArm) {
                if (y == 0) {
                    if (dist <= outerRoadR && dist > outerRoadR - 1.5) return Blocks.WHITE_CONCRETE.getDefaultState();
                    if (dist <= outerRoadR) return Blocks.GRAY_CONCRETE.getDefaultState();
                    if (Math.abs(rx) <= roadW || Math.abs(rz) <= roadW) return Blocks.GRAY_CONCRETE.getDefaultState();
                    return Blocks.SMOOTH_QUARTZ.getDefaultState();
                }
                return Blocks.AIR.getDefaultState();
            }
            return null;
        }

        boolean inXArm = Math.abs(rz) <= totalW;
        boolean inZArm = Math.abs(rx) <= totalW;
        if (type == Type.T_JUNCTION && rz > totalW) inZArm = false;
        if (type == Type.Y_JUNCTION) {
            if (rz > totalW) inZArm = false;
            if (rz < 0 && (Math.abs(rx - rz) <= totalW || Math.abs(rx + rz) <= totalW)) inXArm = true;
        }
        if (!inXArm && !inZArm) return null;

        if (y == 0) {
            boolean onAsphalt = (inXArm && Math.abs(rz) <= roadW) || (inZArm && Math.abs(rx) <= roadW);
            boolean onCrosswalk = onAsphalt && ((Math.abs(rx) == roadW + 2 && Math.abs(rz) % 2 == 0)
                    || (Math.abs(rz) == roadW + 2 && Math.abs(rx) % 2 == 0));
            if (onCrosswalk) return Blocks.WHITE_CONCRETE.getDefaultState();
            return onAsphalt ? Blocks.GRAY_CONCRETE.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }

        boolean isCorner = Math.abs(rx) == roadW + 1 && Math.abs(rz) == roadW + 1;
        if (isCorner) {
            if (y >= 1 && y <= 3) return Blocks.BLACK_CONCRETE.getDefaultState();
            if (y == 4) return Blocks.RED_CONCRETE.getDefaultState();
            if (y == 5) return Blocks.YELLOW_CONCRETE.getDefaultState();
            if (y == 6) return Blocks.LIME_CONCRETE.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }
}
