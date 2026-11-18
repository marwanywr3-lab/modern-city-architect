package moderncity.gui;

import moderncity.item.RoadWandItem;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
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

public class RoadWandScreenHandler extends GenericContainerScreenHandler {

    public RoadWandScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, new SimpleInventory(36), 4);
        populateMenu();
    }

    private void populateMenu() {
        // Row 1: Road Widths
        setBtn(10, Items.SMOOTH_QUARTZ_SLAB, "Size: Small (5 blocks)", Formatting.WHITE);
        setBtn(11, Items.SMOOTH_QUARTZ, "Size: Medium (7 blocks)", Formatting.AQUA);
        setBtn(12, Items.WHITE_CONCRETE, "Size: Large (12 blocks)", Formatting.GREEN);
        setBtn(13, Items.GRAY_CONCRETE, "Highway: 8 blocks", Formatting.GOLD);
        setBtn(14, Items.BLACK_CONCRETE, "Highway: 16 blocks", Formatting.GOLD);

        // Row 2: Direction Mode
        setBtn(19, Items.ARROW, "Mode: One-Way (Lamps on Quartz Sidewalk)", Formatting.AQUA);
        setBtn(20, Items.GRASS_BLOCK, "Mode: Two-Way (Grass + Quartz Median Lamps)", Formatting.GREEN);

        // Row 3: Curve Mode & Undo
        setBtn(28, Items.STICK, "Curve: Straight", Formatting.WHITE);
        setBtn(29, Items.RAIL, "Curve: 90-Degree Corner", Formatting.YELLOW);
        setBtn(30, Items.POWERED_RAIL, "Curve: S-Curve", Formatting.YELLOW);
        setBtn(31, Items.DETECTOR_RAIL, "Curve: U-Turn", Formatting.YELLOW);
        setBtn(34, Items.BARRIER, "Undo Last Road", Formatting.RED);
    }

    private void setBtn(int slot, Item item, String title, Formatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(title).formatted(color, Formatting.BOLD));
        getInventory().setStack(slot, stack);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player.getWorld().isClient() || !(player instanceof ServerPlayerEntity sp)) return;
        RoadWandItem.WandState state = RoadWandItem.getState(sp.getUuid());

        switch (slotIndex) {
            case 10 -> state.roadClass = RoadClass.SMALL_5;
            case 11 -> state.roadClass = RoadClass.MEDIUM_7;
            case 12 -> state.roadClass = RoadClass.LARGE_12;
            case 13 -> state.roadClass = RoadClass.HIGHWAY_8;
            case 14 -> state.roadClass = RoadClass.HIGHWAY_16;
            case 19 -> state.directionMode = RoadNetworkManager.DirectionMode.ONE_WAY;
            case 20 -> state.directionMode = RoadNetworkManager.DirectionMode.TWO_WAY;
            case 28 -> state.curveType = RoadNetworkManager.CurveType.STRAIGHT;
            case 29 -> state.curveType = RoadNetworkManager.CurveType.CORNER90;
            case 30 -> state.curveType = RoadNetworkManager.CurveType.SCURVE;
            case 31 -> state.curveType = RoadNetworkManager.CurveType.UTURN;
            case 34 -> {
                boolean ok = RoadNetworkManager.undoLastRoad(sp.getServerWorld());
                sp.sendMessage(Text.literal(ok ? "Undid last road." : "No road to undo.")
                        .formatted(ok ? Formatting.YELLOW : Formatting.RED), false);
                sp.closeHandledScreen();
                return;
            }
            default -> { return; }
        }

        sp.sendMessage(Text.literal("Road Wand: " + state.roadClass.displayName
                + " | " + state.directionMode.label + " | " + state.curveType.name()).formatted(Formatting.GREEN), false);
        sp.closeHandledScreen();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
