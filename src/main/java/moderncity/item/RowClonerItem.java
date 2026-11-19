package moderncity.item;

import moderncity.blueprint.BlueprintManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RowClonerItem extends Item {

    private static final Map<UUID, String> LAST_BP = new HashMap<>();

    public RowClonerItem(Settings settings) {
        super(settings);
    }

    public static void setTargetBlueprint(UUID playerId, String name) {
        LAST_BP.put(playerId, name);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        String name = LAST_BP.get(player.getUuid());
        if (name == null) {
            player.sendMessage(Text.literal("Save a blueprint first (/blueprint save <name>) or run /rowclone <name>!").formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }
        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos start = context.getBlockPos().up();
        Direction right = player.getHorizontalFacing().rotateYClockwise();
        for (int i = 0; i < 4; i++) {
            BlueprintManager.pasteBlueprint(player.getUuid(), world, start.offset(right, i * 24), name);
        }
        player.sendMessage(Text.literal("Row Cloner queued 4x '" + name + "' along street!").formatted(Formatting.GREEN), true);
        return ActionResult.SUCCESS;
    }
}
