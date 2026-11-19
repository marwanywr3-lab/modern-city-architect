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
                                List.of("twisted", "crystal", "arch", "garden", "sail", "mid", "high", "supertall"), b))
                        .executes(ctx -> executeBuild(ctx, -1))
                        .then(CommandManager.argument("height", IntegerArgumentType.integer(20, 480))
                                .executes(ctx -> executeBuild(ctx, IntegerArgumentType.getInteger(ctx, "height"))))));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context, int explicitHeight) {
        ServerCommandSource source = context.getSource();
        String arg = StringArgumentType.getString(context, "style").toLowerCase();

        int height = explicitHeight > 0 ? explicitHeight : switch (arg) {
            case "mid" -> 35;
            case "high" -> 85;
            case "supertall" -> 135;
            default -> 100;
        };

        TowerBuilderJob.Style style = switch (arg) {
            case "crystal", "supertall" -> TowerBuilderJob.Style.CRYSTAL_DIAGRID;
            case "arch" -> TowerBuilderJob.Style.ARCH_PORTAL;
            case "garden", "mid" -> TowerBuilderJob.Style.SPIRAL_GARDEN;
            case "sail" -> TowerBuilderJob.Style.SAIL_SPIRE;
            default -> TowerBuilderJob.Style.TWISTED;
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
        source.sendFeedback(() -> Text.literal("Queued Sculptural " + job.getName() + "!")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
