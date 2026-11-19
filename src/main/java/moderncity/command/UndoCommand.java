package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class UndoCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("undo")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(UndoCommand::executeUndo));
    }

    private static int executeUndo(CommandContext<ServerCommandSource> context) {
        UUID ownerId = (context.getSource().getEntity() instanceof ServerPlayerEntity p) ? p.getUuid() : null;
        boolean ok = StreamingBuilderEngine.triggerGlobalUndo(ownerId);
        context.getSource().sendFeedback(() -> Text.literal(
                ok ? "Global Undo started! Restoring previous blocks..." : "Nothing to undo.")
                .formatted(ok ? Formatting.YELLOW : Formatting.RED), true);
        return ok ? 1 : 0;
    }
}
