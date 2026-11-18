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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModernCityArchitect implements ModInitializer {
    public static final String MOD_ID = "modern-city-architect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Modern City Architect Mod v1.0.1...");

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

        // Give all Architect tools + English User Guide automatically on join if player doesn't have them
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (!player.getInventory().contains(new ItemStack(ModItems.ARCHITECT_GUIDE))) {
                player.getInventory().insertStack(new ItemStack(ModItems.VISUAL_CATALOG));
                player.getInventory().insertStack(new ItemStack(ModItems.ROAD_WAND));
                player.getInventory().insertStack(new ItemStack(ModItems.CITY_CONTROLLER));
                player.getInventory().insertStack(new ItemStack(ModItems.MEGA_TERRAFORMER));
                player.getInventory().insertStack(new ItemStack(ModItems.BLUEPRINT_SAVER));
                player.getInventory().insertStack(new ItemStack(ModItems.ARCHITECT_GUIDE));

                player.sendMessage(Text.literal("Welcome to Modern City Architect! Right-click the Visual City Catalog or Architect User Guide in your hotbar.")
                        .formatted(Formatting.AQUA), false);
            }
        });

        LOGGER.info("Modern City Architect Mod v1.0.1 initialized successfully!");
    }
}
