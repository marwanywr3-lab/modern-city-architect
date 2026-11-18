package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.FinancialCenterBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class BuildFinancialCenterCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildfinancialcenter")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(BuildFinancialCenterCommand::executeBuild));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new FinancialCenterBuilderJob(source.getWorld(), ownerId, anchor, facing));
        source.sendFeedback(() -> Text.literal("Queued Financial CENTER (3000x3000 Masterplan: Skyline Towers, Cafes, Lakes, Fountains & Gardens)!")
                .formatted(Formatting.GOLD, Formatting.BOLD), true);
        return 1;
    }
}
