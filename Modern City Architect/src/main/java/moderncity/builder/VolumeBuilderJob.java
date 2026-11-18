package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VolumeBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum FillMode {
        SOLID("Solid (Filled)"),
        HOLLOW("Hollow (Box Shell)"),
        WALLS("Walls Only"),
        FLOOR("Floor Only"),
        REPLACE_ALL("Replace Existing Blocks");

        public final String label;
        FillMode(String label) { this.label = label; }
    }

    public record UndoBlock(BlockPos pos, BlockState prevState) {}

    private final ServerWorld world;
    private final UUID ownerId;
    private final int minX, maxX, minY, maxY, minZ, maxZ;
    private final BlockState blockState;
    private final FillMode fillMode;
    private final long totalBlocks;
    private final List<UndoBlock> undoHistory = new ArrayList<>();

    private int curX, curY, curZ;
    private long placedBlocks = 0;
    private boolean finished = false;
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public VolumeBuilderJob(ServerWorld world, UUID ownerId, BlockPos c1, BlockPos c2,
                            BlockState blockState, FillMode fillMode) {
        this.world = world;
        this.ownerId = ownerId;
        this.minX = Math.min(c1.getX(), c2.getX());
        this.maxX = Math.max(c1.getX(), c2.getX());
        this.minY = Math.min(c1.getY(), c2.getY());
        this.maxY = (fillMode == FillMode.FLOOR) ? minY : Math.max(c1.getY(), c2.getY());
        this.minZ = Math.min(c1.getZ(), c2.getZ());
        this.maxZ = Math.max(c1.getZ(), c2.getZ());
        this.blockState = blockState;
        this.fillMode = fillMode;

        this.curX = minX;
        this.curY = minY;
        this.curZ = minZ;
        this.totalBlocks = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public List<UndoBlock> getUndoHistory() { return undoHistory; }

    @Override
    public String getName() {
        return String.format("Volume %s (%dx%dx%d)", fillMode.name(),
                maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

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

            mutablePos.set(curX, curY, curZ);
            BlockState existing = world.getBlockState(mutablePos);

            boolean shouldPlace = switch (fillMode) {
                case SOLID, FLOOR -> true;
                case HOLLOW -> (curX == minX || curX == maxX || curY == minY || curY == maxY || curZ == minZ || curZ == maxZ);
                case WALLS -> (curX == minX || curX == maxX || curZ == minZ || curZ == maxZ);
                case REPLACE_ALL -> !existing.isAir();
            };

            if (shouldPlace) {
                if (undoHistory.size() < 250_000) {
                    undoHistory.add(new UndoBlock(mutablePos.toImmutable(), existing));
                }
                StreamingBuilderEngine.placeBlockFast(world, mutablePos, blockState);
            }

            processed++;
            placedBlocks++;
            curZ++;
            if (curZ > maxZ) {
                curZ = minZ;
                curY++;
                if (curY > maxY) {
                    curY = minY;
                    curX++;
                    if (curX > maxX) finished = true;
                }
            }
        }
        return processed;
    }
}
