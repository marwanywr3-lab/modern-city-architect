package moderncity.item;

import moderncity.builder.TerraformerJob;
import moderncity.engine.StreamingBuilderEngine;
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

public class TerraformerItem extends Item {

    private static final Map<UUID, BlockPos> FIRST_CORNERS = new HashMap<>();

    public TerraformerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        BlockPos clicked = context.getBlockPos();
        BlockPos first = FIRST_CORNERS.get(player.getUuid());
        if (first == null) {
            FIRST_CORNERS.put(player.getUuid(), clicked);
            player.sendMessage(Text.literal("Terraformer Corner 1 set at X:" + clicked.getX() + " Y:" + clicked.getY()
                    + " Z:" + clicked.getZ()).formatted(Formatting.AQUA), true);
        } else {
            FIRST_CORNERS.remove(player.getUuid());
            StreamingBuilderEngine.enqueueJob(new TerraformerJob(
                    (ServerWorld) context.getWorld(), player.getUuid(), first, clicked, first.getY()));
            player.sendMessage(Text.literal("Flattening terrain to Y=" + first.getY() + "...").formatted(Formatting.GREEN), true);
        }
        return ActionResult.SUCCESS;
    }
}
