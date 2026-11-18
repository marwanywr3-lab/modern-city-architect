package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.FacilityBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class BuildFacilityCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildfacility")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(
                                List.of("parking", "metro", "showroom", "gas", "villa", "mosque", "hospital"), b))
                        .executes(BuildFacilityCommand::executeBuild)));
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String arg = StringArgumentType.getString(context, "type").toLowerCase();

        FacilityBuilderJob.FacilityType type = switch (arg) {
            case "parking" -> FacilityBuilderJob.FacilityType.PARKING;
            case "metro" -> FacilityBuilderJob.FacilityType.METRO;
            case "showroom" -> FacilityBuilderJob.FacilityType.SHOWROOM;
            case "gas" -> FacilityBuilderJob.FacilityType.GAS_STATION;
            case "villa" -> FacilityBuilderJob.FacilityType.VILLA;
            case "mosque" -> FacilityBuilderJob.FacilityType.MOSQUE;
            case "hospital" -> FacilityBuilderJob.FacilityType.HOSPITAL;
            default -> null;
        };

        if (type == null) {
            source.sendError(Text.literal("Unknown facility! Use: parking, metro, showroom, gas, villa, mosque, hospital."));
            return 0;
        }

        BlockPos origin = BlockPos.ofFloored(source.getPosition()).add(3, 0, -(type.width / 2));
        UUID ownerId = (source.getEntity() instanceof ServerPlayerEntity player) ? player.getUuid() : null;

        StreamingBuilderEngine.enqueueJob(new FacilityBuilderJob(source.getWorld(), ownerId, origin, type));
        source.sendFeedback(() -> Text.literal("Queued " + type.label + ".").formatted(Formatting.AQUA), true);
        return 1;
    }
}
