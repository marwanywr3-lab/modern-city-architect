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
            user.sendMessage(Text.literal("====== Modern City Architect - User Guide ======").formatted(Formatting.GOLD, Formatting.BOLD), false);
            user.sendMessage(Text.literal("1. Visual City Catalog: Right-click in air (or /catalog) to build Malls, Towers, Plazas, Facilities & Roads with 1 click.").formatted(Formatting.AQUA), false);
            user.sendMessage(Text.literal("2. Smart Road Wand: Right-click Point A then Point B to build roads (Auto-Bridges & Tunnels!). Sneak+Right-Click to switch Highway / Avenue / Local.").formatted(Formatting.GREEN), false);
            user.sendMessage(Text.literal("3. City Controller: Right-click to Upgrade, Undo, Repair roads, or Toggle Auto Night Lighting. Use /citycontroller sign <name> for street signs.").formatted(Formatting.YELLOW), false);
            user.sendMessage(Text.literal("4. Mega-Terraformer: Right-click Corner 1 then Corner 2 to flatten terrain (or /terraform <radius>).").formatted(Formatting.LIGHT_PURPLE), false);
            user.sendMessage(Text.literal("5. Blueprint Saver: Right-click Corner 1, Sneak+Right-Click Corner 2, then /blueprint save <name> & /blueprint paste <name>.").formatted(Formatting.WHITE), false);
            user.sendMessage(Text.literal("Commands: /buildmall <L> <W> <F> | /buildtower <mid|high|supertall> | /buildplaza <l|u> <retail|office|open> <F> | /buildfacility <type>").formatted(Formatting.GRAY), false);
            user.sendMessage(Text.literal("================================================").formatted(Formatting.GOLD, Formatting.BOLD), false);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
