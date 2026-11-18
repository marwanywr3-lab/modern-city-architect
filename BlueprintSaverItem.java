package moderncity.item;

import moderncity.blueprint.BlueprintManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class BlueprintSaverItem extends Item {

    public BlueprintSaverItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        BlockPos pos = context.getBlockPos();
        if (player.isSneaking()) {
            BlueprintManager.setCorner2(player.getUuid(), pos);
            player.sendMessage(Text.literal("Blueprint Corner 2 set! Run /blueprint save <name>").formatted(Formatting.GREEN), true);
        } else {
            BlueprintManager.setCorner1(player.getUuid(), pos);
            player.sendMessage(Text.literal("Blueprint Corner 1 set! Sneak+Right-Click for Corner 2.").formatted(Formatting.AQUA), true);
        }
        return ActionResult.SUCCESS;
    }
}
