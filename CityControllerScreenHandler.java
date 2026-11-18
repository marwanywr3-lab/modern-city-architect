package moderncity.gui;

import moderncity.road.RoadNetworkManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CityControllerScreenHandler extends GenericContainerScreenHandler {

    public CityControllerScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, new SimpleInventory(27), 3);
        populateButtons();
    }

    private void populateButtons() {
        setButton(10, Items.DIAMOND_PICKAXE, "Upgrade Last Road Class", Formatting.AQUA);
        setButton(12, Items.BARRIER, "Undo Last Road", Formatting.RED);
        setButton(14, Items.ANVIL, "Repair Last Road", Formatting.GREEN);
        setButton(16, Items.REDSTONE_LAMP, "Toggle Auto Night Lighting", Formatting.GOLD);
    }

    private void setButton(int slot, net.minecraft.item.Item item, String title, Formatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(title).formatted(color, Formatting.BOLD));
        getInventory().setStack(slot, stack);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        ServerWorld world = serverPlayer.getServerWorld();

        switch (slotIndex) {
            case 10 -> {
                boolean ok = RoadNetworkManager.upgradeLastRoad(world, serverPlayer.getUuid());
                serverPlayer.sendMessage(Text.literal(ok ? "Upgrading last road..." : "No upgradable road found.")
                        .formatted(ok ? Formatting.AQUA : Formatting.YELLOW), false);
                serverPlayer.closeHandledScreen();
            }
            case 12 -> {
                boolean ok = RoadNetworkManager.undoLastRoad(world);
                serverPlayer.sendMessage(Text.literal(ok ? "Undid last road segment." : "No road to undo.")
                        .formatted(ok ? Formatting.YELLOW : Formatting.RED), false);
                serverPlayer.closeHandledScreen();
            }
            case 14 -> {
                boolean ok = RoadNetworkManager.repairLastRoad(world, serverPlayer.getUuid());
                serverPlayer.sendMessage(Text.literal(ok ? "Repairing last road..." : "No road to repair.")
                        .formatted(ok ? Formatting.GREEN : Formatting.YELLOW), false);
                serverPlayer.closeHandledScreen();
            }
            case 16 -> {
                boolean enabled = RoadNetworkManager.toggleAutoNightLighting(serverPlayer.getServer());
                serverPlayer.sendMessage(Text.literal("Auto Night Lighting: " + (enabled ? "ON" : "OFF"))
                        .formatted(enabled ? Formatting.GREEN : Formatting.GRAY), false);
                serverPlayer.closeHandledScreen();
            }
            default -> {
                // Prevent taking decorative GUI items
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
