package moderncity.item;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.road.RoadBuilderJob;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoadWandItem extends Item {

    public static class WandState {
        public BlockPos firstPoint = null;
        public RoadClass roadClass = RoadClass.AVENUE;
        public RoadNetworkManager.CurveType curveType = RoadNetworkManager.CurveType.STRAIGHT;
    }

    private static final Map<UUID, WandState> STATES = new HashMap<>();

    public RoadWandItem(Settings settings) {
        super(settings);
    }

    public static WandState getState(UUID playerId) {
        return STATES.computeIfAbsent(playerId, id -> new WandState());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;

        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        WandState state = getState(player.getUuid());

        // Sneak + Right-Click cycles road class
        if (player.isSneaking()) {
            state.roadClass = state.roadClass.next();
            player.sendMessage(Text.literal("Road Class: " + state.roadClass.displayName)
                    .formatted(Formatting.GOLD), true);
            return ActionResult.SUCCESS;
        }

        BlockPos clicked = RoadNetworkManager.autoSnap(context.getBlockPos());
        if (state.firstPoint == null) {
            state.firstPoint = clicked;
            player.sendMessage(Text.literal("Start Point set (Auto-Snap): X:" + clicked.getX()
                    + " Y:" + clicked.getY() + " Z:" + clicked.getZ()).formatted(Formatting.AQUA), true);
        } else {
            BlockPos start = state.firstPoint;
            state.firstPoint = null;
            StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                    (ServerWorld) context.getWorld(),
                    player.getUuid(),
                    start,
                    clicked,
                    state.roadClass,
                    state.curveType
            ));
            player.sendMessage(Text.literal("Building " + state.roadClass.displayName + " ("
                    + state.curveType.name() + ")...").formatted(Formatting.GREEN), true);
        }
        return ActionResult.SUCCESS;
    }
}
