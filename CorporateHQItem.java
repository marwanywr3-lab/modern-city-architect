package moderncity.item;

import moderncity.gui.CorporateHQScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CorporateHQItem extends Item {

    public CorporateHQItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new CorporateHQScreenHandler(syncId, inv),
                    Text.literal("Corporate HQ Architect")
            ));
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
