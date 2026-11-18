package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.TowerBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.UUID;

public class BuildTowerCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildtower")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("style", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(
                                List.of("classic", "setback", "curved", "cantilever", "twin", "mid", "high", "supertall"), b))
                        .executes(ctx -> executeBuild(ctx, -1))
                        .then(CommandManager.argument("height", IntegerArgumentType.integer(20, 480))
                                .executes(ctx -> executeBuild(ctx, IntegerArgumentType.getInteger(ctx, "height"))))));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context, int explicitHeight) {
        ServerCommandSource source = context.getSource();
        String arg = StringArgumentType.getString(context, "style").toLowerCase();

        int height = explicitHeight > 0 ? explicitHeight : switch (arg) {
            case "mid" -> 30;
            case "high" -> 80;
            case "supertall" -> 120;
            default -> 90;
        };

        TowerBuilderJob.Style style = switch (arg) {
            case "setback", "high" -> TowerBuilderJob.Style.SETBACK;
            case "curved", "supertall" -> TowerBuilderJob.Style.CURVED;
            case "cantilever" -> TowerBuilderJob.Style.CANTILEVER;
            case "twin" -> TowerBuilderJob.Style.TWIN;
            default -> TowerBuilderJob.Style.CLASSIC;
        };

        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);

        TowerBuilderJob job = new TowerBuilderJob(source.getWorld(), ownerId, anchor, facing, style, height);
        StreamingBuilderEngine.enqueueJob(job);
        source.sendFeedback(() -> Text.literal("Queued " + job.getName() + " from Front-Left Anchor.")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
