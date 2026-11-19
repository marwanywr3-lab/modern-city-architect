package moderncity.item;

import moderncity.util.PlacementUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class RooftopLoungeItem extends Item {

    public RooftopLoungeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos base = context.getBlockPos().up();
        Direction facing = player.getHorizontalFacing();

        // Modern 10x10 Rooftop Sky-Cafe Pergola + Planters + Lighting
        for (int r = 0; r <= 9; r++) {
            for (int f = 0; f <= 9; f++) {
                world.setBlockState(PlacementUtil.toWorld(base, facing, r, -1, f), Blocks.BIRCH_PLANKS.getDefaultState());
                if ((r == 0 || r == 9) && (f == 0 || f == 9)) {
                    for (int y = 0; y <= 3; y++) world.setBlockState(PlacementUtil.toWorld(base, facing, r, y, f), Blocks.WHITE_CONCRETE.getDefaultState());
                }
                if (f % 2 == 0) {
                    world.setBlockState(PlacementUtil.toWorld(base, facing, r, 4, f), Blocks.WHITE_CONCRETE.getDefaultState());
                }
            }
        }
        world.setBlockState(PlacementUtil.toWorld(base, facing, 5, 4, 4), Blocks.SEA_LANTERN.getDefaultState());
        player.sendMessage(Text.literal("Placed Rooftop Sky Lounge!").formatted(Formatting.AQUA), true);
        return ActionResult.SUCCESS;
    }
}
