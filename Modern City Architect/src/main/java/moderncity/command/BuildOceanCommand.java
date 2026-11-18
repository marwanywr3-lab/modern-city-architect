package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.OceanBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class BuildOceanCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildocean")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("1000x1000").executes(ctx -> startOcean(ctx, 1000, 1000)))
                .then(CommandManager.literal("1000x2000").executes(ctx -> startOcean(ctx, 1000, 2000)))
                .then(CommandManager.literal("1000x4000").executes(ctx -> startOcean(ctx, 1000, 4000)))
                .then(CommandManager.literal("1000x8000").executes(ctx -> startOcean(ctx, 1000, 8000)))
                .then(CommandManager.argument("width", IntegerArgumentType.integer(40, 2000))
                        .then(CommandManager.argument("length", IntegerArgumentType.integer(40, 8000))
                                .executes(ctx -> startOcean(ctx,
                                        IntegerArgumentType.getInteger(ctx, "width"),
                                        IntegerArgumentType.getInteger(ctx, "length"))))));
    }

    private static int startOcean(CommandContext<ServerCommandSource> context, int width, int length) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2).down();
        StreamingBuilderEngine.enqueueJob(new OceanBuilderJob(source.getWorld(), ownerId, anchor, facing, width, length));
        source.sendFeedback(() -> Text.literal("Queued Ocean & Corniche (" + width + "x" + length + ", Depth 50).")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
