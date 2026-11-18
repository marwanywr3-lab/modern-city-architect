package moderncity.gui;

import moderncity.builder.CorporateHQBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CorporateHQScreenHandler extends GenericContainerScreenHandler {

    public CorporateHQScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, new SimpleInventory(36), 4);
        populate();
    }

    private void populate() {
        // HQ Tower Style A (3 heights up to 200b)
        setBtn(10, Items.QUARTZ_BLOCK, "HQ Tower A - 60 Blocks", Formatting.AQUA);
        setBtn(11, Items.LIGHT_BLUE_STAINED_GLASS, "HQ Tower A - 120 Blocks", Formatting.AQUA);
        setBtn(12, Items.BEACON, "HQ Tower A - 200 Blocks (Max)", Formatting.GOLD);

        // HQ Tower Style B (3 heights up to 200b)
        setBtn(14, Items.WHITE_CONCRETE, "HQ Tower B - 70 Blocks", Formatting.GREEN);
        setBtn(15, Items.TINTED_GLASS, "HQ Tower B - 140 Blocks", Formatting.GREEN);
        setBtn(16, Items.DIAMOND_BLOCK, "HQ Tower B - 200 Blocks (Max)", Formatting.GOLD);

        // 2 Low-Rise Office Designs (2 heights each)
        setBtn(20, Items.WHITE_TERRACOTTA, "Low Office A (2 Floors - 12b)", Formatting.YELLOW);
        setBtn(21, Items.BIRCH_PLANKS, "Low Office A (3 Floors - 17b)", Formatting.YELLOW);
        setBtn(23, Items.SMOOTH_QUARTZ, "Low Office B (4 Floors - 22b)", Formatting.WHITE);
        setBtn(24, Items.POLISHED_DEEPSLATE, "Low Office B (5 Floors - 27b)", Formatting.WHITE);

        // Claim Reception Desk inside any office/tower
        setBtn(31, Items.OAK_SIGN, "Place Company Reception + 2 Chairs Here", Formatting.LIGHT_PURPLE);
    }

    private void setBtn(int slot, Item item, String title, Formatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(title).formatted(color, Formatting.BOLD));
        getInventory().setStack(slot, stack);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player.getWorld().isClient() || !(player instanceof ServerPlayerEntity sp)) return;
        ServerWorld world = sp.getServerWorld();
        Direction facing = sp.getHorizontalFacing();
        BlockPos anchor = sp.getBlockPos().offset(facing, 2);

        switch (slotIndex) {
            case 10 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.HQ_TOWER_TWIN_FIN, 60));
            case 11 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.HQ_TOWER_TWIN_FIN, 120));
            case 12 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.HQ_TOWER_TWIN_FIN, 200));
            case 14 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.HQ_TOWER_ATRIUM, 70));
            case 15 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.HQ_TOWER_ATRIUM, 140));
            case 16 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.HQ_TOWER_ATRIUM, 200));
            case 20 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.LOW_OFFICE_COURTYARD, 12));
            case 21 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.LOW_OFFICE_COURTYARD, 17));
            case 23 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.LOW_OFFICE_PAVILION, 22));
            case 24 -> StreamingBuilderEngine.enqueueJob(new CorporateHQBuilderJob(world, sp.getUuid(), anchor, facing, CorporateHQBuilderJob.HQType.LOW_OFFICE_PAVILION, 27));
            case 31 -> {
                CorporateHQBuilderJob.placeCompanyReception(world, anchor, facing, sp.getName().getString() + " Corp");
                sp.sendMessage(Text.literal("Placed Company Reception! Use /hq claim <CompanyName> for custom name.")
                        .formatted(Formatting.LIGHT_PURPLE), false);
            }
            default -> { return; }
        }
        sp.closeHandledScreen();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
