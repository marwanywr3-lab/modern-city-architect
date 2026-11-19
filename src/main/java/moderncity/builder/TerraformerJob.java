package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class TerraformerJob implements StreamingBuilderEngine.BuildJob {

    private final ServerWorld world;
    private final UUID ownerId;
    private final int minX, maxX, minZ, maxZ, targetY;
    private final int clearTopY = 40;
    private final long totalBlocks;

    private int curX, curZ, curYOffset;
    private long placedBlocks = 0;
    private boolean finished = false;
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public TerraformerJob(ServerWorld world, UUID ownerId, BlockPos p1, BlockPos p2, int targetY) {
        this.world = world;
        this.ownerId = ownerId;
        this.minX = Math.min(p1.getX(), p2.getX());
        this.maxX = Math.max(p1.getX(), p2.getX());
        this.minZ = Math.min(p1.getZ(), p2.getZ());
        this.maxZ = Math.max(p1.getZ(), p2.getZ());
        this.targetY = targetY;

        this.curX = minX;
        this.curZ = minZ;
        this.curYOffset = -6;
        this.totalBlocks = (long) (maxX - minX + 1) * (maxZ - minZ + 1) * (clearTopY + 7);
    }

    @Override
    public String getName() { return "Mega-Terraformer (" + (maxX - minX + 1) + "x" + (maxZ - minZ + 1) + ")"; }

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

            mutablePos.set(curX, targetY + curYOffset, curZ);
            BlockState current = world.getBlockState(mutablePos);

            if (curYOffset < 0) {
                if (current.isAir() || !current.isSolidBlock(world, mutablePos)) {
                    StreamingBuilderEngine.placeBlockFast(world, mutablePos, Blocks.DIRT.getDefaultState());
                }
            } else if (curYOffset == 0) {
                if (!current.isOf(Blocks.GRASS_BLOCK)) {
                    StreamingBuilderEngine.placeBlockFast(world, mutablePos, Blocks.GRASS_BLOCK.getDefaultState());
                }
            } else {
                if (!current.isAir()) {
                    StreamingBuilderEngine.placeBlockFast(world, mutablePos, Blocks.AIR.getDefaultState());
                }
            }

            processed++;
            placedBlocks++;
            curYOffset++;
            if (curYOffset > clearTopY) {
                curYOffset = -6;
                curZ++;
                if (curZ > maxZ) {
                    curZ = minZ;
                    curX++;
                    if (curX > maxX) finished = true;
                }
            }
        }
        return processed;
    }
}
