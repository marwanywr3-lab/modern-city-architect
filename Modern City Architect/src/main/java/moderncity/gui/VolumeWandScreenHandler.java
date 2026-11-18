package moderncity.gui;

import moderncity.builder.VolumeBuilderJob;
import moderncity.item.VolumeWandItem;
import net.minecraft.block.Blocks;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class VolumeWandScreenHandler extends GenericContainerScreenHandler {

    public VolumeWandScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, new SimpleInventory(36), 4);
        populate();
    }

    private void populate() {
        // Fill Modes
        setBtn(10, Items.IRON_BLOCK, "Mode: SOLID (Filled)", Formatting.GREEN);
        setBtn(11, Items.GLASS, "Mode: HOLLOW (Shell)", Formatting.AQUA);
        setBtn(12, Items.BRICK_WALL, "Mode: WALLS ONLY", Formatting.GOLD);
        setBtn(13, Items.SMOOTH_QUARTZ_SLAB, "Mode: FLOOR ONLY", Formatting.WHITE);
        setBtn(14, Items.NETHER_STAR, "Mode: REPLACE EXISTING WITH COPIED BLOCK", Formatting.LIGHT_PURPLE);

        // Quick Block Presets
        setBtn(19, Items.WHITE_CONCRETE, "Block: White Concrete", Formatting.WHITE);
        setBtn(20, Items.SMOOTH_QUARTZ, "Block: Smooth Quartz", Formatting.WHITE);
        setBtn(21, Items.LIGHT_BLUE_STAINED_GLASS, "Block: Light Blue Glass", Formatting.AQUA);
        setBtn(22, Items.TINTED_GLASS, "Block: Tinted Glass", Formatting.BLUE);
        setBtn(23, Items.WHITE_TERRACOTTA, "Block: Warm Terracotta", Formatting.YELLOW);
        setBtn(24, Items.BIRCH_PLANKS, "Block: Light Birch Wood", Formatting.YELLOW);

        // Actions
        setBtn(30, Items.BARRIER, "Undo Last Volume Build", Formatting.RED);
        setBtn(32, Items.REDSTONE_TORCH, "Cancel Current Selection", Formatting.GRAY);
    }

    private void setBtn(int slot, Item item, String title, Formatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(title).formatted(color, Formatting.BOLD));
        getInventory().setStack(slot, stack);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player.getWorld().isClient() || !(player instanceof ServerPlayerEntity sp)) return;
        VolumeWandItem.VolumeState state = VolumeWandItem.getState(sp.getUuid());

        switch (slotIndex) {
            case 10 -> state.fillMode = VolumeBuilderJob.FillMode.SOLID;
            case 11 -> state.fillMode = VolumeBuilderJob.FillMode.HOLLOW;
            case 12 -> state.fillMode = VolumeBuilderJob.FillMode.WALLS;
            case 13 -> state.fillMode = VolumeBuilderJob.FillMode.FLOOR;
            case 14 -> state.fillMode = VolumeBuilderJob.FillMode.REPLACE_ALL;
            case 19 -> state.selectedBlock = Blocks.WHITE_CONCRETE.getDefaultState();
            case 20 -> state.selectedBlock = Blocks.SMOOTH_QUARTZ.getDefaultState();
            case 21 -> state.selectedBlock = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            case 22 -> state.selectedBlock = Blocks.TINTED_GLASS.getDefaultState();
            case 23 -> state.selectedBlock = Blocks.WHITE_TERRACOTTA.getDefaultState();
            case 24 -> state.selectedBlock = Blocks.BIRCH_PLANKS.getDefaultState();
            case 30 -> { VolumeWandItem.undoLast(sp); sp.closeHandledScreen(); return; }
            case 32 -> {
                state.stage = VolumeWandItem.Stage.IDLE;
                state.corner1 = null;
                state.corner2 = null;
                sp.sendMessage(Text.literal("Selection cleared.").formatted(Formatting.GRAY), true);
                sp.closeHandledScreen();
                return;
            }
            default -> { return; }
        }

        sp.sendMessage(Text.literal("Volume Wand: " + state.fillMode.label
                + " | Block: " + state.selectedBlock.getBlock().getName().getString()).formatted(Formatting.GREEN), false);
        sp.closeHandledScreen();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
