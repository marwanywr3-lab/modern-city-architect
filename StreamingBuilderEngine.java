package moderncity.engine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

public class StreamingBuilderEngine {

    private static final int MAX_BLOCKS_PER_TICK = 3000;
    private static final long MAX_NANOS_PER_TICK = 12_000_000L;

    private static final Queue<BuildJob> JOB_QUEUE = new ArrayDeque<>();
    private static BuildJob currentJob = null;
    private static int tickCounter = 0;

    public interface BuildJob {
        String getName();
        ServerWorld getWorld();
        UUID getOwnerId();
        long getTotalBlocks();
        long getPlacedBlocks();
        boolean isFinished();
        int processBatch(int maxBlocks, long deadlineNanos);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(StreamingBuilderEngine::onServerTick);
    }

    public static void enqueueJob(BuildJob job) {
        JOB_QUEUE.add(job);
    }

    public static int cancelAllJobs() {
        int count = JOB_QUEUE.size() + (currentJob != null ? 1 : 0);
        JOB_QUEUE.clear();
        currentJob = null;
        return count;
    }

    public static boolean isBusy() {
        return currentJob != null || !JOB_QUEUE.isEmpty();
    }

    private static void onServerTick(MinecraftServer server) {
        if (currentJob == null) {
            currentJob = JOB_QUEUE.poll();
            if (currentJob == null) {
                return;
            }
            sendToOwner(server, currentJob.getOwnerId(),
                    Text.literal("Started building: " + currentJob.getName()).formatted(Formatting.AQUA),
                    false);
        }

        long startTime = System.nanoTime();
        long deadline = startTime + MAX_NANOS_PER_TICK;

        currentJob.processBatch(MAX_BLOCKS_PER_TICK, deadline);
        tickCounter++;

        if (tickCounter % 20 == 0 && !currentJob.isFinished()) {
            long placed = currentJob.getPlacedBlocks();
            long total = Math.max(1, currentJob.getTotalBlocks());
            int percent = (int) Math.min(99, (placed * 100L) / total);
            sendToOwner(server, currentJob.getOwnerId(),
                    Text.literal(String.format("Building %s: %d%% (%d / %d blocks)",
                            currentJob.getName(), percent, placed, total)).formatted(Formatting.GREEN),
                    true);
        }

        if (currentJob.isFinished()) {
            sendToOwner(server, currentJob.getOwnerId(),
                    Text.literal("Completed building: " + currentJob.getName() + "!")
                            .formatted(Formatting.GOLD, Formatting.BOLD),
                    false);
            currentJob = null;
        }
    }

    public static void placeBlockFast(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    private static void sendToOwner(MinecraftServer server, UUID ownerId, Text message, boolean actionBar) {
        if (ownerId == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerId);
        if (player != null) {
            player.sendMessage(message, actionBar);
        }
    }
}
