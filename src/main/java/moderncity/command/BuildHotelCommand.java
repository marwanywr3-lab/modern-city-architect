package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.HotelBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class BuildHotelCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildhotel")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("small").executes(ctx -> buildPreset(ctx, HotelBuilderJob.HotelTier.SMALL)))
                .then(CommandManager.literal("medium").executes(ctx -> buildPreset(ctx, HotelBuilderJob.HotelTier.MEDIUM)))
                .then(CommandManager.literal("tall").executes(ctx -> buildPreset(ctx, HotelBuilderJob.HotelTier.TALL)))
                .then(CommandManager.literal("mega").executes(ctx -> buildPreset(ctx, HotelBuilderJob.HotelTier.MEGA)))
                .then(CommandManager.argument("length", IntegerArgumentType.integer(30, 1000))
                        .then(CommandManager.argument("width", IntegerArgumentType.integer(24, 300))
                                .then(CommandManager.argument("height", IntegerArgumentType.integer(16, 200))
                                        .executes(ctx -> buildCustom(ctx,
                                                IntegerArgumentType.getInteger(ctx, "length"),
                                                IntegerArgumentType.getInteger(ctx, "width"),
                                                IntegerArgumentType.getInteger(ctx, "height")))))));
    }

    private static int buildPreset(CommandContext<ServerCommandSource> context, HotelBuilderJob.HotelTier tier) {
        return buildCustom(context, tier.length, tier.width, tier.height);
    }

    private static int buildCustom(CommandContext<ServerCommandSource> context, int len, int wid, int hgt) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new HotelBuilderJob(source.getWorld(), ownerId, anchor, facing, len, wid, hgt));
        source.sendFeedback(() -> Text.literal("Queued Modern Hotel (" + len + "x" + wid + "x" + hgt + ").")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
