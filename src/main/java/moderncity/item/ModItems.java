package moderncity.item;

import moderncity.ModernCityArchitect;
import moderncity.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ROAD_WAND = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "road_wand"), new RoadWandItem(new Item.Settings().maxCount(1)));
    public static final Item VOLUME_WAND = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "volume_wand"), new VolumeWandItem(new Item.Settings().maxCount(1)));
    public static final Item CORPORATE_HQ_TOOL = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "corporate_hq_tool"), new CorporateHQItem(new Item.Settings().maxCount(1)));
    public static final Item CITY_CONTROLLER = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "city_controller"), new CityControllerItem(new Item.Settings().maxCount(1)));
    public static final Item MEGA_TERRAFORMER = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "mega_terraformer"), new TerraformerItem(new Item.Settings().maxCount(1)));
    public static final Item BLUEPRINT_SAVER = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "blueprint_saver"), new BlueprintSaverItem(new Item.Settings().maxCount(1)));
    public static final Item VISUAL_CATALOG = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "visual_catalog"), new VisualCatalogItem(new Item.Settings().maxCount(1)));
    public static final Item ARCHITECT_GUIDE = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "architect_guide"), new ArchitectGuideItem(new Item.Settings().maxCount(1)));

    public static final Item STREET_PROPS_TOOL = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "street_props_tool"), new StreetPropsItem(new Item.Settings().maxCount(1)));
    public static final Item BILLBOARD_TOOL = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "billboard_tool"), new BillboardItem(new Item.Settings().maxCount(1)));
    public static final Item ROW_CLONER_TOOL = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "row_cloner_tool"), new RowClonerItem(new Item.Settings().maxCount(1)));
    public static final Item ROOFTOP_LOUNGE_TOOL = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "rooftop_lounge_tool"), new RooftopLoungeItem(new Item.Settings().maxCount(1)));
    public static final Item BRIDGE_RIVER_TOOL = Registry.register(Registries.ITEM, Identifier.of(ModernCityArchitect.MOD_ID, "bridge_river_tool"), new BridgeRiverItem(new Item.Settings().maxCount(1)));

    public static final ItemGroup MODERN_CITY_GROUP = Registry.register(
            Registries.ITEM_GROUP, Identifier.of(ModernCityArchitect.MOD_ID, "modern_city_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(CITY_CONTROLLER))
                    .displayName(Text.translatable("itemGroup.modern-city-architect.main"))
                    .entries((context, entries) -> {
                        entries.add(ARCHITECT_GUIDE);
                        entries.add(VISUAL_CATALOG);
                        entries.add(ROAD_WAND);
                        entries.add(VOLUME_WAND);
                        entries.add(CORPORATE_HQ_TOOL);
                        entries.add(CITY_CONTROLLER);
                        entries.add(MEGA_TERRAFORMER);
                        entries.add(BLUEPRINT_SAVER);
                        entries.add(ModBlocks.ELEVATOR_BLOCK_ITEM);
                        entries.add(STREET_PROPS_TOOL);
                        entries.add(BILLBOARD_TOOL);
                        entries.add(ROW_CLONER_TOOL);
                        entries.add(ROOFTOP_LOUNGE_TOOL);
                        entries.add(BRIDGE_RIVER_TOOL);
                    })
                    .build()
    );

    public static void register() {
        ModernCityArchitect.LOGGER.info("Registered Modern City Architect v3.1 items.");
    }
}
