package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.item.ModItems;
import moderncity.item.RoadWandItem;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class RoadWandCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("roadwand")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .executes(RoadWandCommand::giveWand))
                .then(CommandManager.argument("class", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("highway", "avenue", "local"), b))
                        .then(CommandManager.argument("curve", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("straight", "corner90", "scurve", "uturn"), b))
                                .executes(RoadWandCommand::configureWand))));
    }

    private static int giveWand(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            player.getInventory().insertStack(new ItemStack(ModItems.ROAD_WAND));
            context.getSource().sendFeedback(() -> Text.literal("Given Smart Road Wand! Sneak+Right-Click to cycle class.")
                    .formatted(Formatting.AQUA), false);
            return 1;
        }
        return 0;
    }

    private static int configureWand(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            String cls = StringArgumentType.getString(context, "class").toLowerCase();
            String crv = StringArgumentType.getString(context, "curve").toLowerCase();

            RoadWandItem.WandState state = RoadWandItem.getState(player.getUuid());
            state.roadClass = switch (cls) {
                case "highway" -> RoadClass.HIGHWAY;
                case "local" -> RoadClass.LOCAL;
                default -> RoadClass.AVENUE;
            };
            state.curveType = switch (crv) {
                case "corner90" -> RoadNetworkManager.CurveType.CORNER90;
                case "scurve" -> RoadNetworkManager.CurveType.SCURVE;
                case "uturn" -> RoadNetworkManager.CurveType.UTURN;
                default -> RoadNetworkManager.CurveType.STRAIGHT;
            };

            context.getSource().sendFeedback(() -> Text.literal("Road Wand set to: " + state.roadClass.displayName
                    + " | Curve: " + state.curveType.name()).formatted(Formatting.GREEN), false);
            return 1;
        }
        return 0;
    }
}
