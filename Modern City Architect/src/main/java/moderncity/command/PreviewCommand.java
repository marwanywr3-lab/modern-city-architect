package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.util.BuildingPreviewManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PreviewCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("confirmbuild")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(PreviewCommand::confirm));

        dispatcher.register(CommandManager.literal("cancelpreview")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(PreviewCommand::cancel));
    }

    private static int confirm(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            boolean ok = BuildingPreviewManager.confirmBuild(player);
            if (!ok) {
                context.getSource().sendError(Text.literal("No active building preview!"));
                return 0;
            }
            return 1;
        }
        return 0;
    }

    private static int cancel(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            boolean ok = BuildingPreviewManager.cancelPreview(player.getUuid());
            context.getSource().sendFeedback(() -> Text.literal(ok ? "Preview cancelled." : "No active preview.")
                    .formatted(Formatting.YELLOW), false);
            return 1;
        }
        return 0;
    }
}
