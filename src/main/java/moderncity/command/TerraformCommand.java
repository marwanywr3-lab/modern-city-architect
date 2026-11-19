package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.TerraformerJob;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class TerraformCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("terraform")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .executes(TerraformCommand::giveTool))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(10, 150))
                        .executes(TerraformCommand::executeRadius)));
    }

    private static int giveTool(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            player.getInventory().insertStack(new ItemStack(ModItems.MEGA_TERRAFORMER));
            context.getSource().sendFeedback(() -> Text.literal("Given Mega-Terraformer!").formatted(Formatting.AQUA), false);
            return 1;
        }
        return 0;
    }

    private static int executeRadius(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        int r = IntegerArgumentType.getInteger(context, "radius");
        BlockPos center = BlockPos.ofFloored(source.getPosition()).down();
        UUID ownerId = (source.getEntity() instanceof ServerPlayerEntity player) ? player.getUuid() : null;

        StreamingBuilderEngine.enqueueJob(new TerraformerJob(
                source.getWorld(), ownerId, center.add(-r, 0, -r), center.add(r, 0, r), center.getY()));
        source.sendFeedback(() -> Text.literal("Queued Mega-Terraformer (" + (r * 2) + "x" + (r * 2) + ").")
                .formatted(Formatting.GREEN), true);
        return 1;
    }
}
