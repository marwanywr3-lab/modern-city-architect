package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.item.BillboardItem;
import moderncity.item.RowClonerItem;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CityPropsCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("billboard")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(CityPropsCommand::setBillboard)));

        dispatcher.register(CommandManager.literal("rowclone")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("blueprintName", StringArgumentType.word())
                        .executes(CityPropsCommand::setClone)));

        dispatcher.register(CommandManager.literal("clearweather")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(CityPropsCommand::clearWeather));
    }

    private static int setBillboard(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            String text = StringArgumentType.getString(context, "text");
            BillboardItem.setText(player.getUuid(), text);
            player.sendMessage(Text.literal("Billboard text set to: " + text).formatted(Formatting.GOLD), false);
            return 1;
        }
        return 0;
    }

    private static int setClone(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            String name = StringArgumentType.getString(context, "blueprintName");
            RowClonerItem.setTargetBlueprint(player.getUuid(), name);
            player.sendMessage(Text.literal("Row Cloner linked to blueprint: " + name).formatted(Formatting.GREEN), false);
            return 1;
        }
        return 0;
    }

    private static int clearWeather(CommandContext<ServerCommandSource> context) {
        context.getSource().getWorld().setWeather(60000, 0, false, false);
        context.getSource().sendFeedback(() -> Text.literal("Sky cleared for city building!").formatted(Formatting.AQUA), true);
        return 1;
    }
}
