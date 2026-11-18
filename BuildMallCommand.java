package moderncity.command;

import moderncity.builder.MallBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class BuildMallCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildmall")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("cancel")
                        .executes(BuildMallCommand::executeCancel))
                .then(CommandManager.argument("length", IntegerArgumentType.integer(40, 1000))
                        .then(CommandManager.argument("width", IntegerArgumentType.integer(30, 300))
                                .then(CommandManager.argument("floors", IntegerArgumentType.integer(1, 10))
                                        .executes(BuildMallCommand::executeBuild)))));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        int length = IntegerArgumentType.getInteger(context, "length");
        int width = IntegerArgumentType.getInteger(context, "width");
        int floors = IntegerArgumentType.getInteger(context, "floors");

        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        BlockPos origin = playerPos.add(2, 0, -(width / 2));

        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
        }

        MallBuilderJob job = new MallBuilderJob(world, ownerId, origin, length, width, floors);
        StreamingBuilderEngine.enqueueJob(job);

        source.sendFeedback(() -> Text.literal(String.format(
                "Queued Modern Glass Mall (%dx%d, %d floors) at X:%d Y:%d Z:%d. Building smoothly tick-by-tick...",
                length, width, floors, origin.getX(), origin.getY(), origin.getZ()))
                .formatted(Formatting.AQUA), true);

        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> context) {
        int cancelled = StreamingBuilderEngine.cancelAllJobs();
        context.getSource().sendFeedback(() -> Text.literal(
                "Cancelled " + cancelled + " active/queued build job(s).")
                .formatted(Formatting.YELLOW), true);
        return 1;
    }
}
