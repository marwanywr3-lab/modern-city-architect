package moderncity.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ArchitectGuideItem extends Item {

    public ArchitectGuideItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            user.sendMessage(Text.literal("====== Modern City Architect v2.0 - User Guide ======").formatted(Formatting.GOLD, Formatting.BOLD), false);
            user.sendMessage(Text.literal("Placement Rule: All buildings generate from Front-Left Anchor -> Right, Forward & Up, facing you!").formatted(Formatting.WHITE), false);
            user.sendMessage(Text.literal("1. Visual Catalog (/catalog): 1-click Malls, 5 Tower Styles (up to 480b), Compounds (12/16/24), Apartments & Facilities.").formatted(Formatting.AQUA), false);
            user.sendMessage(Text.literal("2. Smart Road Wand: Shift+Right-Click in air for GUI (5/7/12/8/16b, One-Way/Two-Way Grass+Quartz Median, Curves & Undo).").formatted(Formatting.GREEN), false);
            user.sendMessage(Text.literal("3. Volume Wand: Right-Click Corner 1 -> Fly to size -> Right-Click Lock -> Right-Click Build (Left-Click extends!). Shift+Right-Click block copies it.").formatted(Formatting.LIGHT_PURPLE), false);
            user.sendMessage(Text.literal("4. Corporate HQ Tool: 2 HQ Towers (up to 200b), 2 Low Offices, or /hq claim <CompanyName> for reception + 2 chairs!").formatted(Formatting.YELLOW), false);
            user.sendMessage(Text.literal("Commands: /buildtower <style> <height> | /buildcompound <12|16|24> | /buildapartments <W> <D> <F> | /hq claim <Name>").formatted(Formatting.GRAY), false);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
