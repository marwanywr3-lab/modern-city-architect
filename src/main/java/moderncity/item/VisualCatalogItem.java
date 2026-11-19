package moderncity.item;

import moderncity.gui.VisualCatalogScreenHandler;
import moderncity.util.BuildingPreviewManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class VisualCatalogItem extends Item {

    public VisualCatalogItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient() && context.getPlayer() instanceof ServerPlayerEntity sp) {
            if (BuildingPreviewManager.hasPreview(sp.getUuid()) && !sp.isSneaking()) {
                BuildingPreviewManager.confirmBuildAt(sp, context.getBlockPos().up());
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && user instanceof ServerPlayerEntity sp) {
            if (BuildingPreviewManager.hasPreview(sp.getUuid()) && !sp.isSneaking()) {
                BuildingPreviewManager.confirmBuild(sp);
            } else {
                sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, player) -> new VisualCatalogScreenHandler(syncId, inv),
                        Text.literal("Modern City Architect - Visual Catalog")
                ));
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
