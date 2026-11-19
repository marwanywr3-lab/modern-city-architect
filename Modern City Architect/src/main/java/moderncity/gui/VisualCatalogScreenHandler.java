package moderncity.gui;

import moderncity.builder.ApartmentTowerJob;
import moderncity.builder.CompoundBuilderJob;
import moderncity.builder.FacilityBuilderJob;
import moderncity.builder.CentralParkBuilderJob;
import moderncity.builder.FinancialCenterBuilderJob;
import moderncity.builder.StadiumBuilderJob;
import moderncity.builder.HotelBuilderJob;
import moderncity.builder.LuxuryCompoundBuilderJob;
import moderncity.util.BuildingPreviewManager;
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
import net.minecraft.util.math.Direction;

public class VisualCatalogScreenHandler extends GenericContainerScreenHandler {

    public VisualCatalogScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, new SimpleInventory(54), 6);
        populateCatalog();
    }

    private void populateCatalog() {
        // Row 1: Malls & 5 Tower Styles
        setEntry(10, Items.LIGHT_BLUE_STAINED_GLASS, "Modern Glass Mall (140x64, 3F)", Formatting.AQUA);
        setEntry(11, Items.QUARTZ_BLOCK, "Tower: Twisted Helix (85b)", Formatting.WHITE);
        setEntry(12, Items.TINTED_GLASS, "Tower: Crystal Diagrid (135b)", Formatting.BLUE);
        setEntry(13, Items.BEACON, "Tower: Arch Portal (120b)", Formatting.GOLD);
        setEntry(14, Items.AZALEA_LEAVES, "Tower: Spiral Sky-Garden (95b)", Formatting.GREEN);
        setEntry(15, Items.DIAMOND_BLOCK, "Tower: Diamond Sail Spire (160b)", Formatting.GOLD);
        setEntry(16, Items.WHITE_TERRACOTTA, "Plaza L-Shape (Cafe + Offices)", Formatting.YELLOW);

        // Row 2: Compounds, Apartments & Intersections
        setEntry(19, Items.OAK_DOOR, "Compound: 12 Modern Villas", Formatting.GREEN);
        setEntry(20, Items.BIRCH_DOOR, "Compound: 16 Modern Villas", Formatting.GREEN);
        setEntry(21, Items.DARK_OAK_DOOR, "Compound: 24 Villas", Formatting.GOLD);
        setEntry(18, Items.EMERALD_BLOCK, "Compound: 12 Luxury 4BR Villas", Formatting.GREEN);
        setEntry(22, Items.BRICKS, "Apartment Tower (36x24, 10F)", Formatting.AQUA);
        setEntry(23, Items.GRAY_CONCRETE, "4-Way Medium Intersection", Formatting.WHITE);
        setEntry(24, Items.GRASS_BLOCK, "Green Roundabout", Formatting.GREEN);
        setEntry(25, Items.STONE_BRICK_WALL, "Elevated Overpass + Ramp", Formatting.AQUA);

        // Row 3: Facilities
        setEntry(28, Items.IRON_BLOCK, "Multi-Story Parking", Formatting.GRAY);
        setEntry(29, Items.RAIL, "Futuristic Metro Station", Formatting.AQUA);
        setEntry(30, Items.MINECART, "Glass Car Showroom", Formatting.BLUE);
        setEntry(31, Items.REDSTONE_BLOCK, "Modern Gas Station", Formatting.RED);
        setEntry(32, Items.BIRCH_PLANKS, "Contemporary Villa", Formatting.GOLD);
        setEntry(33, Items.SEA_LANTERN, "Modern Mosque", Formatting.GREEN);
        setEntry(34, Items.RED_CONCRETE, "Modern City Hospital", Formatting.RED);

        // Row 4: All Architect Tools
        setEntry(38, Items.BLAZE_ROD, "Get Smart Road Wand", Formatting.GOLD);
        setEntry(39, Items.WOODEN_AXE, "Get Architect Volume Wand", Formatting.LIGHT_PURPLE);
        setEntry(40, Items.NETHER_STAR, "Get Corporate HQ Architect", Formatting.AQUA);
        setEntry(41, Items.COMPASS, "Get City Controller", Formatting.AQUA);
        setEntry(42, Items.DIAMOND_SHOVEL, "Get Mega-Terraformer", Formatting.GREEN);
        setEntry(43, Items.PAPER, "Get Custom Blueprint Saver", Formatting.YELLOW);
        setEntry(44, Items.MAGENTA_BED, "Build Grand Hotel (80x50x46)", Formatting.LIGHT_PURPLE);
        setEntry(46, Items.LIME_CONCRETE, "Build International Stadium (110x140)", Formatting.GREEN);
        setEntry(47, Items.AZALEA, "Build Grand Central Park (120x120)", Formatting.GREEN);
        setEntry(48, Items.GOLD_BLOCK, "Build Financial CENTER (3000x3000)", Formatting.GOLD);
        setEntry(49, Items.BARRIER, "GLOBAL UNDO (Last Build/Road)", Formatting.RED);
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
        Direction facing = sp.getHorizontalFacing();
        BlockPos anchor = sp.getBlockPos().offset(facing, 2);

        switch (slotIndex) {
            case 10 -> BuildingPreviewManager.setPreview(sp, "Modern Glass Mall", 64, 140, 26, (w, id, a, f) -> new MallBuilderJob(w, id, a, f, 140, 64, 3));
            case 11 -> BuildingPreviewManager.setPreview(sp, "Twisted Helix Tower (85b)", 34, 34, 85, (w, id, a, f) -> new TowerBuilderJob(w, id, a, f, TowerBuilderJob.Style.TWISTED, 85));
            case 12 -> StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, sp.getUuid(), anchor, facing, TowerBuilderJob.Style.CRYSTAL_DIAGRID, 135));
            case 13 -> BuildingPreviewManager.setPreview(sp, "Arch Portal Tower (120b)", 34, 34, 120, (w, id, a, f) -> new TowerBuilderJob(w, id, a, f, TowerBuilderJob.Style.ARCH_PORTAL, 120));
            case 14 -> StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, sp.getUuid(), anchor, facing, TowerBuilderJob.Style.SPIRAL_GARDEN, 95));
            case 15 -> StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, sp.getUuid(), anchor, facing, TowerBuilderJob.Style.SAIL_SPIRE, 160));
            case 16 -> StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(world, sp.getUuid(), anchor, facing, PlazaBuilderJob.Shape.L, PlazaBuilderJob.Layout.OFFICE, 4));
            case 18 -> BuildingPreviewManager.setPreview(sp, "Luxury 4BR Villa Compound", 104, 136, 12, LuxuryCompoundBuilderJob::new);
            case 19 -> BuildingPreviewManager.setPreview(sp, "Compound (12 Villas)", 68, 88, 12, (w, id, a, f) -> new CompoundBuilderJob(w, id, a, f, CompoundBuilderJob.CompoundSize.SMALL_12));
            case 20 -> StreamingBuilderEngine.enqueueJob(new CompoundBuilderJob(world, sp.getUuid(), anchor, facing, CompoundBuilderJob.CompoundSize.MEDIUM_16));
            case 21 -> StreamingBuilderEngine.enqueueJob(new CompoundBuilderJob(world, sp.getUuid(), anchor, facing, CompoundBuilderJob.CompoundSize.LARGE_24));
            case 22 -> StreamingBuilderEngine.enqueueJob(new ApartmentTowerJob(world, sp.getUuid(), anchor, facing, 36, 24, 10));
            case 23 -> StreamingBuilderEngine.enqueueJob(new IntersectionBuilderJob(world, sp.getUuid(), sp.getBlockPos(), IntersectionBuilderJob.Type.FOUR_WAY, RoadClass.MEDIUM_7));
            case 24 -> StreamingBuilderEngine.enqueueJob(new IntersectionBuilderJob(world, sp.getUuid(), sp.getBlockPos(), IntersectionBuilderJob.Type.ROUNDABOUT, RoadClass.MEDIUM_7));
            case 25 -> StreamingBuilderEngine.enqueueJob(new OverpassBuilderJob(world, sp.getUuid(), sp.getBlockPos()));
            case 28 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.PARKING));
            case 29 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.METRO));
            case 30 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.SHOWROOM));
            case 31 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.GAS_STATION));
            case 32 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.VILLA));
            case 33 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.MOSQUE));
            case 34 -> StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(world, sp.getUuid(), anchor, FacilityBuilderJob.FacilityType.HOSPITAL));
            case 38 -> sp.getInventory().insertStack(new ItemStack(ModItems.ROAD_WAND));
            case 39 -> sp.getInventory().insertStack(new ItemStack(ModItems.VOLUME_WAND));
            case 40 -> sp.getInventory().insertStack(new ItemStack(ModItems.CORPORATE_HQ_TOOL));
            case 41 -> sp.getInventory().insertStack(new ItemStack(ModItems.CITY_CONTROLLER));
            case 42 -> sp.getInventory().insertStack(new ItemStack(ModItems.MEGA_TERRAFORMER));
            case 43 -> sp.getInventory().insertStack(new ItemStack(ModItems.BLUEPRINT_SAVER));
            case 44 -> BuildingPreviewManager.setPreview(sp, "Grand Hotel (80x50x46)", 50, 80, 46, (w, id, a, f) -> new HotelBuilderJob(w, id, a, f, 80, 50, 46));
            case 46 -> BuildingPreviewManager.setPreview(sp, "International Stadium", 110, 140, 30, StadiumBuilderJob::new);
            case 47 -> BuildingPreviewManager.setPreview(sp, "Grand Central Park", 120, 120, 6, CentralParkBuilderJob::new);
            case 48 -> StreamingBuilderEngine.enqueueJob(new FinancialCenterBuilderJob(world, sp.getUuid(), anchor, facing));
            case 49 -> StreamingBuilderEngine.triggerGlobalUndo(sp.getUuid());
            default -> { return; }
        }
        sp.closeHandledScreen();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
