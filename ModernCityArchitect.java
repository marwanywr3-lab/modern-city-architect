package moderncity;

import moderncity.command.BlueprintCommand;
import moderncity.command.BuildFacilityCommand;
import moderncity.command.BuildIntersectionCommand;
import moderncity.command.BuildMallCommand;
import moderncity.command.BuildOverpassCommand;
import moderncity.command.BuildPlazaCommand;
import moderncity.command.BuildTowerCommand;
import moderncity.command.CatalogCommand;
import moderncity.command.CityControllerCommand;
import moderncity.command.RoadWandCommand;
import moderncity.command.TerraformCommand;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModernCityArchitect implements ModInitializer {
    public static final String MOD_ID = "modern-city-architect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Modern City Architect Mod v1.0.0...");

        ModItems.register();
        StreamingBuilderEngine.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BuildMallCommand.register(dispatcher);
            BuildTowerCommand.register(dispatcher);
            BuildPlazaCommand.register(dispatcher);
            RoadWandCommand.register(dispatcher);
            BuildIntersectionCommand.register(dispatcher);
            BuildOverpassCommand.register(dispatcher);
            CityControllerCommand.register(dispatcher);
            BuildFacilityCommand.register(dispatcher);
            TerraformCommand.register(dispatcher);
            BlueprintCommand.register(dispatcher);
            CatalogCommand.register(dispatcher);
        });

        LOGGER.info("Modern City Architect Mod v1.0.0 initialized successfully!");
    }
}
