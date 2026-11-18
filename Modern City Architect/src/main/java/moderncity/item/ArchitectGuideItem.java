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
            user.sendMessage(Text.literal("====== Modern City Architect v2.2 - User Guide ======").formatted(Formatting.GOLD, Formatting.BOLD), false);
            user.sendMessage(Text.literal("Minimap & Teleport: Press [M] to open Full City Map! Right-Click anywhere on the map to Teleport.").formatted(Formatting.AQUA), false);
            user.sendMessage(Text.literal("Ocean & Corniche: /buildocean <1000x1000|1000x2000|1000x4000|1000x8000> (50 blocks deep!).").formatted(Formatting.BLUE), false);
            user.sendMessage(Text.literal("Drone View: /drone (toggle aerial camera mode) | /buildairport | /builddistrict <commercial|residential>").formatted(Formatting.GREEN), false);
            user.sendMessage(Text.literal("Volume Wand Block Swap: Shift+Right-Click block to copy, select area, choose 'Replace Existing' in Shift+Right-Click menu!").formatted(Formatting.LIGHT_PURPLE), false);
            user.sendMessage(Text.literal("Buildings & Roads: /catalog | /buildmall | /buildtower | /buildcompound <12|16|24> | /hq claim <Name>").formatted(Formatting.YELLOW), false);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
