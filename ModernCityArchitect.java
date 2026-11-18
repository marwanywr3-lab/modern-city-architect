package moderncity;

import moderncity.command.BuildMallCommand;
import moderncity.command.BuildPlazaCommand;
import moderncity.command.BuildTowerCommand;
import moderncity.command.RoadWandCommand;
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
        LOGGER.info("Initializing Modern City Architect Mod...");

        ModItems.register();
        StreamingBuilderEngine.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BuildMallCommand.register(dispatcher);
            BuildTowerCommand.register(dispatcher);
            BuildPlazaCommand.register(dispatcher);
            RoadWandCommand.register(dispatcher);
        });

        LOGGER.info("Modern City Architect Mod initialized successfully!");
    }
}
