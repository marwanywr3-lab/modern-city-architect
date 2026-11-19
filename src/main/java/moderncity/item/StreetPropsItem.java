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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StreetPropsItem extends Item {

    private enum PropType { BUS_STOP, BENCH, PALM_PLANTER }
    private static final Map<UUID, PropType> MODE = new HashMap<>();

    public StreetPropsItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        PropType current = MODE.getOrDefault(player.getUuid(), PropType.BUS_STOP);
        if (player.isSneaking()) {
            PropType[] vals = PropType.values();
            current = vals[(current.ordinal() + 1) % vals.length];
            MODE.put(player.getUuid(), current);
            player.sendMessage(Text.literal("Street Prop: " + current.name()).formatted(Formatting.AQUA), true);
            return ActionResult.SUCCESS;
        }

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos base = context.getBlockPos().up();
        Direction facing = player.getHorizontalFacing();

        switch (current) {
            case BUS_STOP -> {
                for (int r = 0; r <= 4; r++) {
                    world.setBlockState(PlacementUtil.toWorld(base, facing, r, 0, 2), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
                    world.setBlockState(PlacementUtil.toWorld(base, facing, r, 1, 2), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
                    world.setBlockState(PlacementUtil.toWorld(base, facing, r, 2, 2), Blocks.WHITE_CONCRETE.getDefaultState());
                    world.setBlockState(PlacementUtil.toWorld(base, facing, r, 2, 1), Blocks.WHITE_CONCRETE.getDefaultState());
                }
                world.setBlockState(PlacementUtil.toWorld(base, facing, 2, 2, 1), Blocks.SEA_LANTERN.getDefaultState());
            }
            case BENCH -> {
                for (int r = 0; r <= 2; r++) {
                    world.setBlockState(PlacementUtil.toWorld(base, facing, r, 0, 0), Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
                }
            }
            case PALM_PLANTER -> {
                world.setBlockState(base, Blocks.GRASS_BLOCK.getDefaultState());
                for (int h = 1; h <= 3; h++) world.setBlockState(base.up(h), Blocks.BIRCH_LOG.getDefaultState());
                world.setBlockState(base.up(4), Blocks.AZALEA_LEAVES.getDefaultState());
                world.setBlockState(base.up(4).north(), Blocks.AZALEA_LEAVES.getDefaultState());
                world.setBlockState(base.up(4).south(), Blocks.AZALEA_LEAVES.getDefaultState());
                world.setBlockState(base.up(4).east(), Blocks.AZALEA_LEAVES.getDefaultState());
                world.setBlockState(base.up(4).west(), Blocks.AZALEA_LEAVES.getDefaultState());
            }
        }
        return ActionResult.SUCCESS;
    }
}
