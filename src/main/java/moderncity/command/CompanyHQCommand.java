package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.CorporateHQBuilderJob;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CompanyHQCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("hq")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("claim")
                        .then(CommandManager.argument("companyName", StringArgumentType.greedyString())
                                .executes(CompanyHQCommand::claimHQ))));
    }

    private static int claimHQ(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "companyName");
        Direction facing = Direction.NORTH;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        CorporateHQBuilderJob.placeCompanyReception(source.getWorld(), anchor, facing, name);
        source.sendFeedback(() -> Text.literal("Claimed office space for '" + name + "' with reception desk & 2 chairs!")
                .formatted(Formatting.GREEN), true);
        return 1;
    }
}
