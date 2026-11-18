package moderncity.item;

import moderncity.ModernCityArchitect;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ROAD_WAND = Registry.register(
            Registries.ITEM,
            Identifier.of(ModernCityArchitect.MOD_ID, "road_wand"),
            new RoadWandItem(new Item.Settings().maxCount(1))
    );

    public static final Item CITY_CONTROLLER = Registry.register(
            Registries.ITEM,
            Identifier.of(ModernCityArchitect.MOD_ID, "city_controller"),
            new CityControllerItem(new Item.Settings().maxCount(1))
    );

    public static void register() {
        ModernCityArchitect.LOGGER.info("Registered Modern City Architect items.");
    }
}
