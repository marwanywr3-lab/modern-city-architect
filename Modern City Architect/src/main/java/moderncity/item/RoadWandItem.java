package moderncity.item;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.gui.RoadWandScreenHandler;
import moderncity.road.RoadBuilderJob;
import moderncity.road.RoadClass;
import moderncity.road.RoadNetworkManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoadWandItem extends Item {

    public static class WandState {
        public BlockPos firstPoint = null;
        public RoadClass roadClass = RoadClass.MEDIUM_7;
        public RoadNetworkManager.DirectionMode directionMode = RoadNetworkManager.DirectionMode.TWO_WAY;
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && user.isSneaking()) {
            user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new RoadWandScreenHandler(syncId, inv),
                    Text.literal("Smart Road Wand - Settings")
            ));
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        if (player.isSneaking()) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new RoadWandScreenHandler(syncId, inv),
                    Text.literal("Smart Road Wand - Settings")
            ));
            return ActionResult.SUCCESS;
        }

        WandState state = getState(player.getUuid());
        BlockPos clicked = RoadNetworkManager.autoSnap(context.getBlockPos());

        if (state.firstPoint == null) {
            state.firstPoint = clicked;
            player.sendMessage(Text.literal("Road Start set! Walk/fly to End Point and Right-Click (Preview active).")
                    .formatted(Formatting.AQUA), true);
        } else {
            BlockPos start = state.firstPoint;
            state.firstPoint = null;
            StreamingBuilderEngine.enqueueJob(new RoadBuilderJob(
                    (ServerWorld) context.getWorld(),
                    player.getUuid(),
                    start,
                    clicked,
                    state.roadClass,
                    state.directionMode,
                    state.curveType
            ));
        }
        return ActionResult.SUCCESS;
    }
}
