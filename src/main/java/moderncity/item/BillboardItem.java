package moderncity.item;

import moderncity.util.PlacementUtil;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
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

public class BillboardItem extends Item {

    private static final Map<UUID, String> TEXTS = new HashMap<>();

    public BillboardItem(Settings settings) {
        super(settings);
    }

    public static void setText(UUID playerId, String text) {
        TEXTS.put(playerId, text);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos base = context.getBlockPos().up();
        Direction facing = player.getHorizontalFacing();
        String msg = TEXTS.getOrDefault(player.getUuid(), "MODERN CITY");

        // Twin pillars + giant glowing LED Billboard screen (11x5)
        for (int y = 0; y <= 5; y++) {
            world.setBlockState(PlacementUtil.toWorld(base, facing, 2, y, 0), Blocks.POLISHED_DEEPSLATE.getDefaultState());
            world.setBlockState(PlacementUtil.toWorld(base, facing, 8, y, 0), Blocks.POLISHED_DEEPSLATE.getDefaultState());
        }
        for (int r = 0; r <= 10; r++) {
            for (int y = 6; y <= 10; y++) {
                boolean border = (r == 0 || r == 10 || y == 6 || y == 10);
                world.setBlockState(PlacementUtil.toWorld(base, facing, r, y, 0),
                        border ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.SEA_LANTERN.getDefaultState());
            }
        }
        BlockPos signPos = PlacementUtil.toWorld(base, facing, 5, 8, -1);
        world.setBlockState(signPos, Blocks.BIRCH_WALL_SIGN.getDefaultState()
                .with(net.minecraft.block.WallSignBlock.FACING, facing.getOpposite()));
        if (world.getBlockEntity(signPos) instanceof SignBlockEntity se) {
            se.setText(new SignText()
                    .withMessage(1, Text.literal(msg).formatted(Formatting.DARK_BLUE))
                    .withMessage(2, Text.literal("ADVERTISEMENT").formatted(Formatting.GRAY)), true);
            se.markDirty();
        }
        player.sendMessage(Text.literal("Placed LED Billboard! Use /billboard <text> to customize.").formatted(Formatting.GOLD), true);
        return ActionResult.SUCCESS;
    }
}
