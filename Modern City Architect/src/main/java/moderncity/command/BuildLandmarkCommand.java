package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.CentralParkBuilderJob;
import moderncity.builder.StadiumBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class BuildLandmarkCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildstadium")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(BuildLandmarkCommand::buildStadium));

        dispatcher.register(CommandManager.literal("buildpark")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(BuildLandmarkCommand::buildPark));
    }

    private static int buildStadium(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new StadiumBuilderJob(source.getWorld(), ownerId, anchor, facing));
        source.sendFeedback(() -> Text.literal("Queued Modern International Stadium (110x140)!").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int buildPark(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new CentralParkBuilderJob(source.getWorld(), ownerId, anchor, facing));
        source.sendFeedback(() -> Text.literal("Queued Grand Central Park (120x120)!").formatted(Formatting.GREEN), true);
        return 1;
    }
}
