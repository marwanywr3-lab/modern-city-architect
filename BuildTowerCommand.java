package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.TowerBuilderJob;
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

public class BuildTowerCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildtower")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("tier", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                List.of("mid", "high", "supertall"), builder))
                        .executes(BuildTowerCommand::executeBuild)));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        String tierArg = StringArgumentType.getString(context, "tier").toLowerCase();

        TowerBuilderJob.Tier tier = switch (tierArg) {
            case "mid", "30" -> TowerBuilderJob.Tier.MID;
            case "high", "80" -> TowerBuilderJob.Tier.HIGH;
            case "supertall", "120" -> TowerBuilderJob.Tier.SUPERTALL;
            default -> null;
        };

        if (tier == null) {
            source.sendError(Text.literal("Invalid tier! Use: mid, high, or supertall."));
            return 0;
        }

        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        BlockPos origin = playerPos.add(3, 0, -(tier.footprint / 2));
        UUID ownerId = (source.getEntity() instanceof ServerPlayerEntity player) ? player.getUuid() : null;

        StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, ownerId, origin, tier));
        source.sendFeedback(() -> Text.literal("Queued " + tier.label + " at X:" + origin.getX()
                + " Y:" + origin.getY() + " Z:" + origin.getZ()).formatted(Formatting.AQUA), true);
        return 1;
    }
}
