package moderncity.gui;

import moderncity.builder.FacilityBuilderJob;
import moderncity.builder.IntersectionBuilderJob;
import moderncity.builder.MallBuilderJob;
import moderncity.builder.OverpassBuilderJob;
import moderncity.builder.PlazaBuilderJob;
import moderncity.builder.TowerBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.item.ModItems;
import moderncity.road.RoadClass;
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

public class VisualCatalogScreenHandler extends GenericContainerScreenHandler {

    public VisualCatalogScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, new SimpleInventory(54), 6);
        populateCatalog();
    }

    private void populateCatalog() {
        // Row 1: Mega-Buildings & Skyline Towers
        setEntry(10, Items.LIGHT_BLUE_STAINED_GLASS, "Build Modern Glass Mall (160x70, 3F)", Formatting.AQUA);
        setEntry(11, Items.QUARTZ_BLOCK, "Build Mid-Rise Tower (30b)", Formatting.WHITE);
        setEntry(12, Items.TINTED_GLASS, "Build High-Rise Tower (80b)", Formatting.BLUE);
        setEntry(13, Items.BEACON, "Build Supertall Skyscraper (120b)", Formatting.GOLD);
        setEntry(14, Items.WHITE_TERRACOTTA, "Build L-Shape Plaza (Retail, 4F)", Formatting.YELLOW);
        setEntry(15, Items.SMOOTH_QUARTZ, "Build U-Shape Plaza (Office, 5F)", Formatting.YELLOW);

        // Row 2: Intersections & Overpasses
        setEntry(19, Items.GRAY_CONCRETE, "Build 4-Way Avenue Intersection", Formatting.GREEN);
        setEntry(20, Items.GRASS_BLOCK, "Build Green Roundabout", Formatting.GREEN);
        setEntry(21, Items.STONE_BRICK_WALL, "Build Elevated Overpass + Ramp", Formatting.AQUA);

        // Row 3: Modern Facilities & Civic
        setEntry(28, Items.IRON_BLOCK, "Build Multi-Story Parking", Formatting.GRAY);
        setEntry(29, Items.RAIL, "Build Futuristic Metro Station", Formatting.AQUA);
        setEntry(30, Items.MINECART, "Build Glass Car Showroom", Formatting.BLUE);
        setEntry(31, Items.REDSTONE_BLOCK, "Build Modern Gas Station", Formatting.RED);
        setEntry(32, Items.BIRCH_PLANKS, "Build Contemporary Villa", Formatting.GOLD);
        setEntry(33, Items.SEA_LANTERN, "Build Modern Mosque", Formatting.GREEN);
        setEntry(34, Items.RED_CONCRETE, "Build Modern City Hospital", Formatting.RED);

        // Row 4: Architect Tools
        setEntry(40, Items.BLAZE_ROD, "Get Smart Road Wand", Formatting.GOLD);
        setEntry(41, Items.COMPASS, "Get City Controller", Formatting.AQUA);
        setEntry(42, Items.DIAMOND_SHOVEL, "Get Mega-Terraformer", Formatting.GREEN);
        setEntry(43, Items.PAPER, "Get Custom Blueprint Saver", Formatting.YELLOW);
    }

    private void setEntry(int slot, Item item, String label, Formatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(label).formatted(color, Formatting.BOLD));
        getInventory().setStack(slot, stack);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player.getWorld().isClient() || !(player instanceof ServerPlayerEntity sp)) return;
        ServerWorld world = sp.getServerWorld();
        BlockPos pos = sp.getBlockPos();

        switch (slotIndex) {
            case 10 -> StreamingBuilderEngine.enqueueJob(new MallBuilderJob(world, sp.getUuid(), pos.add(3, 0, -35), 160, 70, 3));
            case 11 -> StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, sp.getUuid(), pos.add(3, 0, -11), TowerBuilderJob.Tier.MID));
            case 12 -> StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, sp.getUuid(), pos.add(3, 0, -14), TowerBuilderJob.Tier.HIGH));
            case 13 -> StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, sp.getUuid(), pos.add(3, 0, -17), TowerBuilderJob.Tier.SUPERTALL));
            case 14 -> StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(world, sp.getUuid(), pos.add(3, 0, -24), PlazaBuilderJob.Shape.L, PlazaBuilderJob.Layout.RETAIL, 4));
            case 15 -> StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(world, sp.getUuid(), pos.add(3, 0, -24), PlazaBuilderJob.Shape.U, PlazaBuilderJob.Layout.OFFICE, 5));
            case 19 -> StreamingBuilderEngine.enqueueJob(new IntersectionBuilderJob(world, sp.getUuid(), pos, IntersectionBuilderJob.Type.FOUR_WAY, RoadClass.AVENUE));
            case 20 -> StreamingBuilderEngine.enqueueJob(new IntersectionBuilderJob(world, sp.getUuid(), pos, IntersectionBuilderJob.Type.ROUNDABOUT, RoadClass.AVENUE));
            case 21 -> StreamingBuilderEngine.enqueueJob(new OverpassBuilderJob(world, sp.getUuid(), pos));
            case 28 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -13), FacilityBuilderJob.FacilityType.PARKING));
            case 29 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -10), FacilityBuilderJob.FacilityType.METRO));
            case 30 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -11), FacilityBuilderJob.FacilityType.SHOWROOM));
            case 31 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -11), FacilityBuilderJob.FacilityType.GAS_STATION));
            case 32 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -12), FacilityBuilderJob.FacilityType.VILLA));
            case 33 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -15), FacilityBuilderJob.FacilityType.MOSQUE));
            case 34 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), pos.add(3, 0, -17), FacilityBuilderJob.FacilityType.HOSPITAL));
            case 40 -> sp.getInventory().insertStack(new ItemStack(ModItems.ROAD_WAND));
            case 41 -> sp.getInventory().insertStack(new ItemStack(ModItems.CITY_CONTROLLER));
            case 42 -> sp.getInventory().insertStack(new ItemStack(ModItems.MEGA_TERRAFORMER));
            case 43 -> sp.getInventory().insertStack(new ItemStack(ModItems.BLUEPRINT_SAVER));
            default -> { return; }
        }
        sp.closeHandledScreen();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
