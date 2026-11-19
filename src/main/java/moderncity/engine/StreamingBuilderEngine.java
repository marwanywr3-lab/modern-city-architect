package moderncity.engine;

import moderncity.item.VolumeWandItem;
import moderncity.road.RoadNetworkManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class StreamingBuilderEngine {

    private static final int MAX_BLOCKS_PER_TICK = 3500;
    private static final long MAX_NANOS_PER_TICK = 12_000_000L;
    private static final int MAX_EXACT_UNDO_BLOCKS = 350_000;

    public record UndoBlock(BlockPos pos, BlockState prevState) {}

    public static class GlobalUndoRecord {
        public final String jobName;
        public final ServerWorld world;
        public final List<UndoBlock> blocks = new ArrayList<>();
        public final Set<BlockPos> seen = new HashSet<>();
        public boolean overflowed = false;
        public int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        public int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        public GlobalUndoRecord(String jobName, ServerWorld world) {
            this.jobName = jobName;
            this.world = world;
        }

        public void record(BlockPos pos, BlockState prev) {
            BlockPos imm = pos.toImmutable();
            minX = Math.min(minX, imm.getX());
            minY = Math.min(minY, imm.getY());
            minZ = Math.min(minZ, imm.getZ());
            maxX = Math.max(maxX, imm.getX());
            maxY = Math.max(maxY, imm.getY());
            maxZ = Math.max(maxZ, imm.getZ());

            if (!overflowed) {
                if (blocks.size() < MAX_EXACT_UNDO_BLOCKS) {
                    if (seen.add(imm)) {
                        blocks.add(new UndoBlock(imm, prev));
                    }
                } else {
                    overflowed = true;
                    blocks.clear();
                    seen.clear();
                }
            }
        }
    }

    private static final Queue<BuildJob> JOB_QUEUE = new ArrayDeque<>();
    private static final Deque<GlobalUndoRecord> GLOBAL_UNDO_STACK = new ArrayDeque<>();
    private static BuildJob currentJob = null;
    private static GlobalUndoRecord currentUndoRecord = null;
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
        currentUndoRecord = null;
        return count;
    }

    public static boolean isBusy() {
        return currentJob != null || !JOB_QUEUE.isEmpty();
    }

    public static boolean triggerGlobalUndo(UUID ownerId) {
        if (GLOBAL_UNDO_STACK.isEmpty()) return false;
        GlobalUndoRecord rec = GLOBAL_UNDO_STACK.pop();

        if (!rec.overflowed && !rec.blocks.isEmpty()) {
            enqueueJob(new BuildJob() {
                private int idx = rec.blocks.size() - 1;
                private long done = 0;
                private final long total = rec.blocks.size();

                @Override public String getName() { return "Undo: " + rec.jobName; }
                @Override public ServerWorld getWorld() { return rec.world; }
                @Override public UUID getOwnerId() { return ownerId; }
                @Override public long getTotalBlocks() { return total; }
                @Override public long getPlacedBlocks() { return done; }
                @Override public boolean isFinished() { return idx < 0; }

                @Override
                public int processBatch(int maxBlocks, long deadlineNanos) {
                    int count = 0;
                    while (count < maxBlocks && idx >= 0) {
                        if ((count & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
                        UndoBlock ub = rec.blocks.get(idx--);
                        if (ub.pos().getY() >= rec.world.getBottomY() && ub.pos().getY() < rec.world.getTopY()) {
                            rec.world.setBlockState(ub.pos(), ub.prevState(), Block.NOTIFY_LISTENERS);
                        }
                        done++;
                        count++;
                    }
                    return count;
                }
            });
        } else if (rec.minX <= rec.maxX) {
            // Mega-structure fallback: stream-clear bounding box back to AIR cleanly
            enqueueJob(new BuildJob() {
                private int x = rec.minX, y = rec.minY, z = rec.minZ;
                private long done = 0;
                private boolean fin = false;
                private final long total = (long) (rec.maxX - rec.minX + 1) * (rec.maxY - rec.minY + 1) * (rec.maxZ - rec.minZ + 1);
                private final BlockPos.Mutable m = new BlockPos.Mutable();

                @Override public String getName() { return "Undo Mega: " + rec.jobName; }
                @Override public ServerWorld getWorld() { return rec.world; }
                @Override public UUID getOwnerId() { return ownerId; }
                @Override public long getTotalBlocks() { return total; }
                @Override public long getPlacedBlocks() { return done; }
                @Override public boolean isFinished() { return fin; }

                @Override
                public int processBatch(int maxBlocks, long deadlineNanos) {
                    int count = 0;
                    while (count < maxBlocks && !fin) {
                        if ((count & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
                        m.set(x, y, z);
                        if (y > rec.world.getBottomY() && !rec.world.getBlockState(m).isAir()) {
                            rec.world.setBlockState(m, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                        done++;
                        count++;
                        z++;
                        if (z > rec.maxZ) {
                            z = rec.minZ;
                            y++;
                            if (y > rec.maxY) {
                                y = rec.minY;
                                x++;
                                if (x > rec.maxX) fin = true;
                            }
                        }
                    }
                    return count;
                }
            });
        }
        return true;
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 4 == 0) {
            RoadNetworkManager.tickRoadPreview(server);
            VolumeWandItem.tickWireframePreview(server);
            moderncity.util.BuildingPreviewManager.tickPreviews(server);
            moderncity.block.ModBlocks.tickElevators(server);
        }
        if (tickCounter % 80 == 0) {
            RoadNetworkManager.tickTrafficLights(server);
        }
        if (tickCounter % 100 == 0) {
            RoadNetworkManager.tickNightLighting(server);
        }

        if (currentJob == null) {
            currentJob = JOB_QUEUE.poll();
            if (currentJob == null) return;
            if (!currentJob.getName().startsWith("Undo")) {
                currentUndoRecord = new GlobalUndoRecord(currentJob.getName(), currentJob.getWorld());
            } else {
                currentUndoRecord = null;
            }
            sendToOwner(server, currentJob.getOwnerId(),
                    Text.literal("Started: " + currentJob.getName()).formatted(Formatting.AQUA), false);
        }

        long deadline = System.nanoTime() + MAX_NANOS_PER_TICK;
        currentJob.processBatch(MAX_BLOCKS_PER_TICK, deadline);

        if (tickCounter % 20 == 0 && !currentJob.isFinished()) {
            long placed = currentJob.getPlacedBlocks();
            long total = Math.max(1, currentJob.getTotalBlocks());
            int percent = (int) Math.min(99, (placed * 100L) / total);
            sendToOwner(server, currentJob.getOwnerId(),
                    Text.literal(String.format("%s: %d%% (%d / %d)",
                            currentJob.getName(), percent, placed, total)).formatted(Formatting.GREEN), true);
        }

        if (currentJob.isFinished()) {
            if (currentUndoRecord != null) {
                GLOBAL_UNDO_STACK.push(currentUndoRecord);
                if (GLOBAL_UNDO_STACK.size() > 15) GLOBAL_UNDO_STACK.removeLast();
                currentUndoRecord = null;
            }
            sendToOwner(server, currentJob.getOwnerId(),
                    Text.literal("Completed: " + currentJob.getName() + "!")
                            .formatted(Formatting.GOLD, Formatting.BOLD), false);
            currentJob = null;
        }
    }

    public static void placeBlockFast(ServerWorld world, BlockPos pos, BlockState state) {
        if (pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY()) {
            if (currentUndoRecord != null) {
                currentUndoRecord.record(pos, world.getBlockState(pos));
            }
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
    }

    private static void sendToOwner(MinecraftServer server, UUID ownerId, Text message, boolean actionBar) {
        if (ownerId == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerId);
        if (player != null) {
            player.sendMessage(message, actionBar);
        }
    }
}
