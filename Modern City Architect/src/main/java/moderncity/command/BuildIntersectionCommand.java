package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.IntersectionBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.road.RoadClass;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class BuildIntersectionCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildintersection")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("4way", "t", "y", "roundabout"), b))
                        .then(CommandManager.argument("class", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("5", "7", "12", "16"), b))
                                .executes(BuildIntersectionCommand::executeBuild))));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String typeArg = StringArgumentType.getString(context, "type").toLowerCase();
        String classArg = StringArgumentType.getString(context, "class").toLowerCase();

        IntersectionBuilderJob.Type type = switch (typeArg) {
            case "t" -> IntersectionBuilderJob.Type.T_JUNCTION;
            case "y" -> IntersectionBuilderJob.Type.Y_JUNCTION;
            case "roundabout" -> IntersectionBuilderJob.Type.ROUNDABOUT;
            default -> IntersectionBuilderJob.Type.FOUR_WAY;
        };

        RoadClass roadClass = switch (classArg) {
            case "5", "small" -> RoadClass.SMALL_5;
            case "12", "large" -> RoadClass.LARGE_12;
            case "16", "highway" -> RoadClass.HIGHWAY_16;
            default -> RoadClass.MEDIUM_7;
        };

        BlockPos center = BlockPos.ofFloored(source.getPosition());
        UUID ownerId = (source.getEntity() instanceof ServerPlayerEntity player) ? player.getUuid() : null;

        StreamingBuilderEngine.enqueueJob(new IntersectionBuilderJob(source.getWorld(), ownerId, center, type, roadClass));
        source.sendFeedback(() -> Text.literal("Queued " + type.name() + " Intersection (" + roadClass.displayName + ").")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
