package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OverpassBuilderJob implements StreamingBuilderEngine.BuildJob {

    private record Placement(BlockPos pos, BlockState state) {}

    private final ServerWorld world;
    private final UUID ownerId;
    private final List<Placement> placements = new ArrayList<>();
    private int cursor = 0;

    public OverpassBuilderJob(ServerWorld world, UUID ownerId, BlockPos center) {
        this.world = world;
        this.ownerId = ownerId;
        generateOverpass(center);
    }

    private void generateOverpass(BlockPos center) {
        int deckY = center.getY() + 8;
        int halfLen = 40;
        int halfW = 7;

        // 1. Elevated Overpass Deck along X-axis
        for (int rx = -halfLen; rx <= halfLen; rx++) {
            for (int rz = -halfW; rz <= halfW; rz++) {
                BlockPos p = new BlockPos(center.getX() + rx, deckY, center.getZ() + rz);
                if (Math.abs(rz) == halfW) {
                    placements.add(new Placement(p, Blocks.WHITE_CONCRETE.getDefaultState()));
                    placements.add(new Placement(p.up(), Blocks.STONE_BRICK_WALL.getDefaultState()));
                } else {
                    BlockState s = (rz == 0 || (Math.abs(rz) == 4 && Math.abs(rx) % 6 < 3))
                            ? Blocks.WHITE_CONCRETE.getDefaultState()
                            : Blocks.GRAY_CONCRETE.getDefaultState();
                    placements.add(new Placement(p, s));
                    placements.add(new Placement(p.down(), Blocks.WHITE_CONCRETE.getDefaultState()));
                }
            }
            // Support piers every 16 blocks
            if (Math.abs(rx) % 16 == 0) {
                for (int py = center.getY(); py < deckY - 1; py++) {
                    placements.add(new Placement(new BlockPos(center.getX() + rx, py, center.getZ() - 4), Blocks.WHITE_CONCRETE.getDefaultState()));
                    placements.add(new Placement(new BlockPos(center.getX() + rx, py, center.getZ() + 4), Blocks.WHITE_CONCRETE.getDefaultState()));
                }
            }
        }

        // 2. Circular Voxel Ramp connecting ground (Y) to deckY (Y+8)
        int rampCenterX = center.getX() + 18;
        int rampCenterZ = center.getZ() + 18;
        int rampRadius = 11;
        int steps = 120;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double angle = t * Math.PI * 1.5;
            int ry = center.getY() + (int) Math.round(t * 8.0);
            for (int dr = -2; dr <= 2; dr++) {
                int bx = rampCenterX + (int) Math.round((rampRadius + dr) * Math.cos(angle));
                int bz = rampCenterZ + (int) Math.round((rampRadius + dr) * Math.sin(angle));
                BlockPos rp = new BlockPos(bx, ry, bz);
                BlockState s = (Math.abs(dr) == 2) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
                placements.add(new Placement(rp, s));
                placements.add(new Placement(rp.down(), Blocks.WHITE_CONCRETE.getDefaultState()));
                if (Math.abs(dr) == 2) {
                    placements.add(new Placement(rp.up(), Blocks.STONE_BRICK_WALL.getDefaultState()));
                }
            }
        }
    }

    @Override
    public String getName() { return "Elevated Overpass with Circular Ramp"; }

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
