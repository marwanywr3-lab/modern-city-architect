package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.ApartmentTowerJob;
import moderncity.builder.CompoundBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class BuildCompoundCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildcompound")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("12").executes(ctx -> buildCompound(ctx, CompoundBuilderJob.CompoundSize.SMALL_12)))
                .then(CommandManager.literal("16").executes(ctx -> buildCompound(ctx, CompoundBuilderJob.CompoundSize.MEDIUM_16)))
                .then(CommandManager.literal("24").executes(ctx -> buildCompound(ctx, CompoundBuilderJob.CompoundSize.LARGE_24))));

        dispatcher.register(CommandManager.literal("buildapartments")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("width", IntegerArgumentType.integer(24, 80))
                        .then(CommandManager.argument("depth", IntegerArgumentType.integer(20, 60))
                                .then(CommandManager.argument("floors", IntegerArgumentType.integer(2, 40))
                                        .executes(BuildCompoundCommand::buildApartments)))));
    }

    private static int buildCompound(CommandContext<ServerCommandSource> context, CompoundBuilderJob.CompoundSize size) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new CompoundBuilderJob(source.getWorld(), ownerId, anchor, facing, size));
        source.sendFeedback(() -> Text.literal("Queued " + size.label + " from Front-Left Anchor.").formatted(Formatting.AQUA), true);
        return 1;
    }

    private static int buildApartments(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        int w = IntegerArgumentType.getInteger(context, "width");
        int d = IntegerArgumentType.getInteger(context, "depth");
        int fl = IntegerArgumentType.getInteger(context, "floors");
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new ApartmentTowerJob(source.getWorld(), ownerId, anchor, facing, w, d, fl));
        source.sendFeedback(() -> Text.literal("Queued Apartment Tower (" + w + "x" + d + ", " + fl + "F).").formatted(Formatting.AQUA), true);
        return 1;
    }
}
