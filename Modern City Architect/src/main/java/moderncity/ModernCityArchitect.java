package moderncity;

import moderncity.command.BlueprintCommand;
import moderncity.command.BuildCompoundCommand;
import moderncity.command.BuildDistrictCommand;
import moderncity.command.BuildOceanCommand;
import moderncity.command.BuildFinancialCenterCommand;
import moderncity.command.BuildHotelCommand;
import moderncity.command.DroneCommand;
import moderncity.command.PreviewCommand;
import moderncity.command.UndoCommand;
import moderncity.command.BuildFacilityCommand;
import moderncity.command.BuildIntersectionCommand;
import moderncity.command.BuildMallCommand;
import moderncity.command.BuildOverpassCommand;
import moderncity.command.BuildPlazaCommand;
import moderncity.command.BuildTowerCommand;
import moderncity.command.CatalogCommand;
import moderncity.command.CityControllerCommand;
import moderncity.command.CompanyHQCommand;
import moderncity.command.RoadWandCommand;
import moderncity.command.TerraformCommand;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.item.ModItems;
import moderncity.item.VolumeWandItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModernCityArchitect implements ModInitializer {
    public static final String MOD_ID = "modern-city-architect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Modern City Architect Mod v2.0...");

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
            CompanyHQCommand.register(dispatcher);
            BuildCompoundCommand.register(dispatcher);
            BuildOceanCommand.register(dispatcher);
            DroneCommand.register(dispatcher);
            BuildHotelCommand.register(dispatcher);
            BuildFinancialCenterCommand.register(dispatcher);
            UndoCommand.register(dispatcher);
            PreviewCommand.register(dispatcher);
            BuildDistrictCommand.register(dispatcher);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity sp
                    && sp.getMainHandStack().isOf(ModItems.VOLUME_WAND)) {
                VolumeWandItem.unlockToDrag(sp);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (!player.getInventory().contains(new ItemStack(ModItems.ARCHITECT_GUIDE))) {
                player.getInventory().insertStack(new ItemStack(ModItems.VISUAL_CATALOG));
                player.getInventory().insertStack(new ItemStack(ModItems.ROAD_WAND));
                player.getInventory().insertStack(new ItemStack(ModItems.VOLUME_WAND));
                player.getInventory().insertStack(new ItemStack(ModItems.CORPORATE_HQ_TOOL));
                player.getInventory().insertStack(new ItemStack(ModItems.CITY_CONTROLLER));
                player.getInventory().insertStack(new ItemStack(ModItems.MEGA_TERRAFORMER));
                player.getInventory().insertStack(new ItemStack(ModItems.BLUEPRINT_SAVER));
                player.getInventory().insertStack(new ItemStack(ModItems.ARCHITECT_GUIDE));

                player.sendMessage(Text.literal("Modern City Architect v2.0 ready! Check your hotbar & English Guide.")
                        .formatted(Formatting.AQUA), false);
            }
        });
    }
}
