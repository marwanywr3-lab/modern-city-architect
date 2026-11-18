package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.PlazaBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class BuildPlazaCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildplaza")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("shape", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("l", "u"), b))
                        .then(CommandManager.argument("layout", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("retail", "office", "open"), b))
                                .then(CommandManager.argument("floors", IntegerArgumentType.integer(2, 8))
                                        .executes(BuildPlazaCommand::executeBuild)))));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        String shapeArg = StringArgumentType.getString(context, "shape").toUpperCase();
        String layoutArg = StringArgumentType.getString(context, "layout").toUpperCase();
        int floors = IntegerArgumentType.getInteger(context, "floors");

        PlazaBuilderJob.Shape shape = shapeArg.equals("U") ? PlazaBuilderJob.Shape.U : PlazaBuilderJob.Shape.L;
        PlazaBuilderJob.Layout layout = switch (layoutArg) {
            case "OFFICE" -> PlazaBuilderJob.Layout.OFFICE;
            case "OPEN" -> PlazaBuilderJob.Layout.OPEN;
            default -> PlazaBuilderJob.Layout.RETAIL;
        };

        BlockPos origin = BlockPos.ofFloored(source.getPosition()).add(3, 0, -24);
        UUID ownerId = (source.getEntity() instanceof ServerPlayerEntity player) ? player.getUuid() : null;

        StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(world, ownerId, origin, shape, layout, floors));
        source.sendFeedback(() -> Text.literal("Queued " + shape.name() + "-Shape Plaza (" + layout.name() + ", "
                + floors + " floors).").formatted(Formatting.AQUA), true);
        return 1;
    }
}
