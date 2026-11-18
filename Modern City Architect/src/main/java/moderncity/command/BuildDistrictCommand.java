package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.builder.AirportBuilderJob;
import moderncity.builder.CompoundBuilderJob;
import moderncity.builder.PlazaBuilderJob;
import moderncity.builder.TowerBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.map.WaypointManager;
import moderncity.road.RoadBuilderJob;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class BuildDistrictCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("builddistrict")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("commercial").executes(ctx -> buildDistrict(ctx, true)))
                .then(CommandManager.literal("residential").executes(ctx -> buildDistrict(ctx, false))));

        dispatcher.register(CommandManager.literal("buildairport")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(BuildDistrictCommand::buildAirport));
    }

    private static int buildDistrict(CommandContext<ServerCommandSource> context, boolean commercial) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos base = BlockPos.ofFloored(source.getPosition()).offset(facing, 4);

        // Main Boulevard Road
        StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                world, ownerId, base, base.offset(facing.rotateYClockwise(), 90),
                RoadClass.LARGE_12, RoadNetworkManager.DirectionMode.TWO_WAY, RoadNetworkManager.CurveType.STRAIGHT));

        if (commercial) {
            WaypointManager.addWaypoint("Commercial District", base, 0x38BDF8);
            StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, ownerId, base.offset(facing, 14), facing, TowerBuilderJob.Style.SETBACK, 85));
            StreamingBuilderEngine.enqueueJob(new TowerBuilderJob(world, ownerId, base.offset(facing, 14).offset(facing.rotateYClockwise(), 40), facing, TowerBuilderJob.Style.CURVED, 120));
            StreamingBuilderEngine.enqueueJob(new PlazaBuilderJob(world, ownerId, base.offset(facing, 60), facing, PlazaBuilderJob.Shape.U, PlazaBuilderJob.Layout.OFFICE, 4));
            source.sendFeedback(() -> Text.literal("Queued Auto Commercial District (Boulevard + 2 Towers + U-Plaza)!").formatted(Formatting.AQUA), true);
        } else {
            WaypointManager.addWaypoint("Residential District", base, 0x22C55E);
            StreamingBuilderEngine.enqueueJob(new CompoundBuilderJob(world, ownerId, base.offset(facing, 14), facing, CompoundBuilderJob.CompoundSize.SMALL_12));
            source.sendFeedback(() -> Text.literal("Queued Auto Residential District (Boulevard + 12-Villa Compound)!").formatted(Formatting.GREEN), true);
        }
        return 1;
    }

    private static int buildAirport(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Direction facing = Direction.NORTH;
        UUID ownerId = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ownerId = player.getUuid();
            facing = player.getHorizontalFacing();
        }
        BlockPos anchor = BlockPos.ofFloored(source.getPosition()).offset(facing, 2);
        StreamingBuilderEngine.enqueueJob(new AirportBuilderJob(source.getWorld(), ownerId, anchor, facing));
        source.sendFeedback(() -> Text.literal("Queued Modern City Airport (80x140)!").formatted(Formatting.GOLD), true);
        return 1;
    }
}
