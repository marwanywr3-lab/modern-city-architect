package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.OverpassBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class BuildOverpassCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildoverpass")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(BuildOverpassCommand::executeBuild));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        BlockPos center = BlockPos.ofFloored(source.getPosition());
        UUID ownerId = (source.getEntity() instanceof ServerPlayerEntity player) ? player.getUuid() : null;

        StreamingBuilderEngine.enqueueJob(new OverpassBuilderJob(source.getWorld(), ownerId, center));
        source.sendFeedback(() -> Text.literal("Queued Elevated Overpass & Circular Ramp.")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
