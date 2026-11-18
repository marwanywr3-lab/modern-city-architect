package moderncity.blueprint;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlueprintManager {

    public record RelativeBlock(int dx, int dy, int dz, BlockState state) {}

    private static final Map<UUID, BlockPos[]> SELECTIONS = new HashMap<>();
    private static final Map<String, List<RelativeBlock>> BLUEPRINTS = new HashMap<>();

    public static void setCorner1(UUID playerId, BlockPos pos) {
        BlockPos[] sel = SELECTIONS.computeIfAbsent(playerId, id -> new BlockPos[2]);
        sel[0] = pos.toImmutable();
    }

    public static void setCorner2(UUID playerId, BlockPos pos) {
        BlockPos[] sel = SELECTIONS.computeIfAbsent(playerId, id -> new BlockPos[2]);
        sel[1] = pos.toImmutable();
    }

    public static int saveSelection(UUID playerId, ServerWorld world, String name) {
        BlockPos[] sel = SELECTIONS.get(playerId);
        if (sel == null || sel[0] == null || sel[1] == null) return -1;

        int minX = Math.min(sel[0].getX(), sel[1].getX());
        int maxX = Math.max(sel[0].getX(), sel[1].getX());
        int minY = Math.min(sel[0].getY(), sel[1].getY());
        int maxY = Math.max(sel[0].getY(), sel[1].getY());
        int minZ = Math.min(sel[0].getZ(), sel[1].getZ());
        int maxZ = Math.max(sel[0].getZ(), sel[1].getZ());

        List<RelativeBlock> blocks = new ArrayList<>();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    m.set(x, y, z);
                    BlockState state = world.getBlockState(m);
                    if (!state.isAir()) {
                        blocks.add(new RelativeBlock(x - minX, y - minY, z - minZ, state));
                    }
                }
            }
        }
        BLUEPRINTS.put(name.toLowerCase(), blocks);
        return blocks.size();
    }

    public static boolean pasteBlueprint(UUID playerId, ServerWorld world, BlockPos origin, String name) {
        List<RelativeBlock> blocks = BLUEPRINTS.get(name.toLowerCase());
        if (blocks == null) return false;

        StreamingBuilderEngine.enqueueJob(new StreamingBuilderEngine.BuildJob() {
            private int cursor = 0;

            @Override
            public String getName() { return "Blueprint: " + name; }

            @Override
            public ServerWorld getWorld() { return world; }

            @Override
            public UUID getOwnerId() { return playerId; }

            @Override
            public long getTotalBlocks() { return blocks.size(); }

            @Override
            public long getPlacedBlocks() { return cursor; }

            @Override
            public boolean isFinished() { return cursor >= blocks.size(); }

            @Override
            public int processBatch(int maxBlocks, long deadlineNanos) {
                int count = 0;
                while (count < maxBlocks && cursor < blocks.size()) {
                    if ((count & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
                    RelativeBlock rb = blocks.get(cursor++);
                    StreamingBuilderEngine.placeBlockFast(world, origin.add(rb.dx(), rb.dy(), rb.dz()), rb.state());
                    count++;
                }
                return count;
            }
        });
        return true;
    }
}
