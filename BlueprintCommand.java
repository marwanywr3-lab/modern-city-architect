package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.blueprint.BlueprintManager;
import moderncity.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class BlueprintCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("blueprint")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give").executes(BlueprintCommand::giveTool))
                .then(CommandManager.literal("save")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(BlueprintCommand::saveBlueprint)))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(BlueprintCommand::pasteBlueprint))));
    }

    private static int giveTool(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            player.getInventory().insertStack(new ItemStack(ModItems.BLUEPRINT_SAVER));
            context.getSource().sendFeedback(() -> Text.literal("Given Custom Blueprint Saver!").formatted(Formatting.AQUA), false);
            return 1;
        }
        return 0;
    }

    private static int saveBlueprint(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            String name = StringArgumentType.getString(context, "name");
            int count = BlueprintManager.saveSelection(player.getUuid(), context.getSource().getWorld(), name);
            if (count < 0) {
                context.getSource().sendError(Text.literal("Select Corner 1 (Right-Click) and Corner 2 (Sneak+Right-Click) first!"));
                return 0;
            }
            context.getSource().sendFeedback(() -> Text.literal("Saved blueprint '" + name + "' (" + count + " blocks).")
                    .formatted(Formatting.GREEN), true);
            return 1;
        }
        return 0;
    }

    private static int pasteBlueprint(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            String name = StringArgumentType.getString(context, "name");
            BlockPos origin = BlockPos.ofFloored(context.getSource().getPosition()).add(2, 0, 0);
            boolean ok = BlueprintManager.pasteBlueprint(player.getUuid(), context.getSource().getWorld(), origin, name);
            if (!ok) {
                context.getSource().sendError(Text.literal("Blueprint '" + name + "' not found!"));
                return 0;
            }
            context.getSource().sendFeedback(() -> Text.literal("Pasting blueprint '" + name + "'...")
                    .formatted(Formatting.AQUA), true);
            return 1;
        }
        return 0;
    }
}
