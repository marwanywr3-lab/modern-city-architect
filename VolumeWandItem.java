package moderncity.item;

import moderncity.builder.VolumeBuilderJob;
import moderncity.engine.StreamingBuilderEngine;
import moderncity.gui.VolumeWandScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VolumeWandItem extends Item {

    public enum Stage { IDLE, DRAGGING, LOCKED }

    public static class VolumeState {
        public Stage stage = Stage.IDLE;
        public BlockPos corner1 = null;
        public BlockPos corner2 = null;
        public BlockState selectedBlock = Blocks.WHITE_CONCRETE.getDefaultState();
        public VolumeBuilderJob.FillMode fillMode = VolumeBuilderJob.FillMode.HOLLOW;
        public VolumeBuilderJob lastJob = null;
    }

    private static final Map<UUID, VolumeState> STATES = new HashMap<>();

    public VolumeWandItem(Settings settings) {
        super(settings);
    }

    public static VolumeState getState(UUID playerId) {
        return STATES.computeIfAbsent(playerId, id -> new VolumeState());
    }

    public static void unlockToDrag(ServerPlayerEntity player) {
        VolumeState state = getState(player.getUuid());
        if (state.stage == Stage.LOCKED) {
            state.stage = Stage.DRAGGING;
            player.sendMessage(Text.literal("Unlocked end corner! Move/fly to extend, Right-Click to lock.")
                    .formatted(Formatting.YELLOW), true);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return TypedActionResult.success(user.getStackInHand(hand));

        if (user.isSneaking()) {
            user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new VolumeWandScreenHandler(syncId, inv),
                    Text.literal("Architect Volume Wand - Settings")
            ));
            return TypedActionResult.success(user.getStackInHand(hand));
        }

        VolumeState state = getState(user.getUuid());
        if (state.stage == Stage.DRAGGING) {
            HitResult hit = user.raycast(60.0, 0.0f, false);
            state.corner2 = (hit.getType() == HitResult.Type.BLOCK)
                    ? ((BlockHitResult) hit).getBlockPos()
                    : BlockPos.ofFloored(hit.getPos());
            state.stage = Stage.LOCKED;
            user.sendMessage(Text.literal("Box Locked! Right-Click to BUILD, or Left-Click to extend more.")
                    .formatted(Formatting.GOLD), true);
            return TypedActionResult.success(user.getStackInHand(hand));
        } else if (state.stage == Stage.LOCKED) {
            buildAndReset((ServerPlayerEntity) user, state);
            return TypedActionResult.success(user.getStackInHand(hand));
        }

        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return ActionResult.PASS;

        VolumeState state = getState(player.getUuid());
        BlockPos clicked = context.getBlockPos();

        // Sneak + Right-Click on a block copies its BlockState!
        if (player.isSneaking()) {
            state.selectedBlock = context.getWorld().getBlockState(clicked);
            player.sendMessage(Text.literal("Copied Block: " + state.selectedBlock.getBlock().getName().getString())
                    .formatted(Formatting.AQUA), true);
            return ActionResult.SUCCESS;
        }

        switch (state.stage) {
            case IDLE -> {
                state.corner1 = clicked;
                state.corner2 = clicked;
                state.stage = Stage.DRAGGING;
                player.sendMessage(Text.literal("Corner 1 set! Fly/walk to expand size, Right-Click to lock.")
                        .formatted(Formatting.AQUA), true);
            }
            case DRAGGING -> {
                state.corner2 = clicked;
                state.stage = Stage.LOCKED;
                player.sendMessage(Text.literal("Box Locked! Right-Click to BUILD, or Left-Click to extend more.")
                        .formatted(Formatting.GOLD), true);
            }
            case LOCKED -> buildAndReset(player, state);
        }
        return ActionResult.SUCCESS;
    }

    private void buildAndReset(ServerPlayerEntity player, VolumeState state) {
        VolumeBuilderJob job = new VolumeBuilderJob(
                player.getServerWorld(), player.getUuid(),
                state.corner1, state.corner2, state.selectedBlock, state.fillMode
        );
        state.lastJob = job;
        state.stage = Stage.IDLE;
        state.corner1 = null;
        state.corner2 = null;
        StreamingBuilderEngine.enqueueJob(job);
    }

    public static void undoLast(ServerPlayerEntity player) {
        VolumeState state = getState(player.getUuid());
        if (state.lastJob == null || state.lastJob.getUndoHistory().isEmpty()) {
            player.sendMessage(Text.literal("Nothing to undo.").formatted(Formatting.RED), false);
            return;
        }
        List<VolumeBuilderJob.UndoBlock> history = state.lastJob.getUndoHistory();
        ServerWorld world = player.getServerWorld();
        for (int i = history.size() - 1; i >= 0; i--) {
            VolumeBuilderJob.UndoBlock ub = history.get(i);
            StreamingBuilderEngine.placeBlockFast(world, ub.pos(), ub.prevState());
        }
        history.clear();
        player.sendMessage(Text.literal("Undid last Volume Wand build.").formatted(Formatting.YELLOW), false);
    }

    public static void tickWireframePreview(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.getMainHandStack().isOf(ModItems.VOLUME_WAND)) continue;
            VolumeState state = getState(player.getUuid());
            if (state.stage == Stage.IDLE || state.corner1 == null) continue;

            BlockPos c2 = state.corner2;
            if (state.stage == Stage.DRAGGING) {
                HitResult hit = player.raycast(60.0, 0.0f, false);
                c2 = (hit.getType() == HitResult.Type.BLOCK)
                        ? ((BlockHitResult) hit).getBlockPos()
                        : BlockPos.ofFloored(hit.getPos());
            }
            if (c2 == null) continue;

            int minX = Math.min(state.corner1.getX(), c2.getX());
            int maxX = Math.max(state.corner1.getX(), c2.getX()) + 1;
            int minY = Math.min(state.corner1.getY(), c2.getY());
            int maxY = Math.max(state.corner1.getY(), c2.getY()) + 1;
            int minZ = Math.min(state.corner1.getZ(), c2.getZ());
            int maxZ = Math.max(state.corner1.getZ(), c2.getZ()) + 1;

            int len = maxX - minX;
            int hgt = maxY - minY;
            int wid = maxZ - minZ;

            if (state.stage == Stage.DRAGGING) {
                player.sendMessage(Text.literal(String.format("Size: %d x %d x %d (H:%d) | Right-Click to Lock",
                        len, wid, hgt, hgt)).formatted(Formatting.AQUA), true);
            }

            ServerWorld world = player.getServerWorld();
            drawEdge(world, player, minX, minY, minZ, maxX, minY, minZ);
            drawEdge(world, player, minX, minY, maxZ, maxX, minY, maxZ);
            drawEdge(world, player, minX, maxY, minZ, maxX, maxY, minZ);
            drawEdge(world, player, minX, maxY, maxZ, maxX, maxY, maxZ);

            drawEdge(world, player, minX, minY, minZ, minX, minY, maxZ);
            drawEdge(world, player, maxX, minY, minZ, maxX, minY, maxZ);
            drawEdge(world, player, minX, maxY, minZ, minX, maxY, maxZ);
            drawEdge(world, player, maxX, maxY, minZ, maxX, maxY, maxZ);

            drawEdge(world, player, minX, minY, minZ, minX, maxY, minZ);
            drawEdge(world, player, maxX, minY, minZ, maxX, maxY, minZ);
            drawEdge(world, player, minX, minY, maxZ, minX, maxY, maxZ);
            drawEdge(world, player, maxX, minY, maxZ, maxX, maxY, maxZ);
        }
    }

    private static void drawEdge(ServerWorld world, ServerPlayerEntity player,
                                 double x1, double y1, double z1, double x2, double y2, double z2) {
        double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int steps = Math.min(25, Math.max(2, (int) Math.ceil(dist / 1.5)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            world.spawnParticles(player, ParticleTypes.END_ROD, true,
                    x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t,
                    1, 0, 0, 0, 0);
        }
    }
}
