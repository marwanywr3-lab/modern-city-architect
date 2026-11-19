package moderncity.item;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BridgeRiverItem extends Item {

    private static final Map<UUID, BlockPos> FIRST = new HashMap<>();

    public BridgeRiverItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        BlockPos clicked = context.getBlockPos();
        BlockPos start = FIRST.remove(player.getUuid());
        if (start == null) {
            FIRST.put(player.getUuid(), clicked);
            player.sendMessage(Text.literal("Suspension Bridge Point A set! Right-Click Point B.").formatted(Formatting.AQUA), true);
        } else {
            ServerWorld world = (ServerWorld) context.getWorld();
            double dist = Math.hypot(clicked.getX() - start.getX(), clicked.getZ() - start.getZ());
            int steps = Math.max(4, (int) Math.ceil(dist * 1.5));
            double dx = (clicked.getX() - start.getX()) / (double) steps;
            double dz = (clicked.getZ() - start.getZ()) / (double) steps;

            StreamingBuilderEngine.enqueueJob(new StreamingBuilderEngine.BuildJob() {
                private int i = 0;
                @Override public String getName() { return "Suspension Bridge"; }
                @Override public ServerWorld getWorld() { return world; }
                @Override public UUID getOwnerId() { return player.getUuid(); }
                @Override public long getTotalBlocks() { return steps * 9L; }
                @Override public long getPlacedBlocks() { return i * 9L; }
                @Override public boolean isFinished() { return i > steps; }
                @Override
                public int processBatch(int maxBlocks, long deadlineNanos) {
                    int c = 0;
                    while (c < maxBlocks && i <= steps) {
                        double cx = start.getX() + dx * i;
                        double cz = start.getZ() + dz * i;
                        double len = Math.max(0.001, Math.hypot(dx, dz));
                        double px = -dz / len, pz = dx / len;
                        double archY = Math.sin((double) i / steps * Math.PI) * 10.0;
                        for (int off = -4; off <= 4; off++) {
                            int bx = (int) Math.round(cx + px * off);
                            int bz = (int) Math.round(cz + pz * off);
                            BlockPos dp = new BlockPos(bx, start.getY() + (int) Math.round(archY * 0.25), bz);
                            StreamingBuilderEngine.placeBlockFast(world, dp, Math.abs(off) == 4
                                    ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState());
                            if (Math.abs(off) == 4) {
                                int cableH = (int) Math.round(archY) + 2;
                                StreamingBuilderEngine.placeBlockFast(world, dp.up(cableH), Blocks.SEA_LANTERN.getDefaultState());
                                if (i % 6 == 0) {
                                    for (int h = 1; h < cableH; h++) StreamingBuilderEngine.placeBlockFast(world, dp.up(h), Blocks.IRON_BARS.getDefaultState());
                                }
                            }
                        }
                        i++;
                        c += 9;
                    }
                    return c;
                }
            });
        }
        return ActionResult.SUCCESS;
    }
}
