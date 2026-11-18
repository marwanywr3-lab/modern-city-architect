package moderncity.util;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BuildingPreviewManager {

    public record BoundingRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean intersects(BoundingRegion o) {
            return minX <= o.maxX && maxX >= o.minX
                    && minY <= o.maxY && maxY >= o.minY
                    && minZ <= o.maxZ && maxZ >= o.minZ;
        }

        public BoundingRegion intersection(BoundingRegion o) {
            return new BoundingRegion(
                    Math.max(minX, o.minX), Math.max(minY, o.minY), Math.max(minZ, o.minZ),
                    Math.min(maxX, o.maxX), Math.min(maxY, o.maxY), Math.min(maxZ, o.maxZ)
            );
        }
    }

    @FunctionalInterface
    public interface JobFactory {
        StreamingBuilderEngine.BuildJob create(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing);
    }

    public record PendingPreview(String label, int width, int depth, int height, JobFactory factory) {}

    private static final List<BoundingRegion> PLACED_REGIONS = new ArrayList<>();
    private static final Map<UUID, PendingPreview> PENDING = new HashMap<>();

    public static synchronized void registerPlacedRegion(BlockPos anchor, Direction facing, int width, int height, int depth) {
        PLACED_REGIONS.add(computeRegion(anchor, facing, width, height, depth));
    }

    public static BoundingRegion computeRegion(BlockPos anchor, Direction facing, int width, int height, int depth) {
        BlockPos p1 = PlacementUtil.toWorld(anchor, facing, 0, 0, 0);
        BlockPos p2 = PlacementUtil.toWorld(anchor, facing, width - 1, height - 1, depth - 1);
        return new BoundingRegion(
                Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()),
                Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ())
        );
    }

    public static void setPreview(ServerPlayerEntity player, String label, int width, int depth, int height, JobFactory factory) {
        PENDING.put(player.getUuid(), new PendingPreview(label, width, depth, height, factory));
        player.sendMessage(Text.literal("Preview Mode: " + label + " (" + width + "x" + depth + "x" + height
                + "). Move to position, then Right-Click Catalog or run /confirmbuild!").formatted(Formatting.AQUA), false);
    }

    public static boolean hasPreview(UUID playerId) {
        return PENDING.containsKey(playerId);
    }

    public static boolean confirmBuild(ServerPlayerEntity player) {
        PendingPreview p = PENDING.remove(player.getUuid());
        if (p == null) return false;
        Direction facing = player.getHorizontalFacing();
        BlockPos anchor = player.getBlockPos().offset(facing, 2);
        registerPlacedRegion(anchor, facing, p.width(), p.height(), p.depth());
        StreamingBuilderEngine.enqueueJob(p.factory().create(player.getServerWorld(), player.getUuid(), anchor, facing));
        return true;
    }

    public static boolean cancelPreview(UUID playerId) {
        return PENDING.remove(playerId) != null;
    }

    public static void tickPreviews(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PendingPreview p = PENDING.get(player.getUuid());
            if (p == null) continue;

            Direction facing = player.getHorizontalFacing();
            BlockPos anchor = player.getBlockPos().offset(facing, 2);
            BoundingRegion candidate = computeRegion(anchor, facing, p.width(), p.height(), p.depth());

            List<BoundingRegion> overlaps = new ArrayList<>();
            synchronized (BuildingPreviewManager.class) {
                for (BoundingRegion existing : PLACED_REGIONS) {
                    if (candidate.intersects(existing)) {
                        overlaps.add(candidate.intersection(existing));
                    }
                }
            }

            ServerWorld world = player.getServerWorld();
            if (overlaps.isEmpty()) {
                drawBox(world, player, candidate, ParticleTypes.HAPPY_VILLAGER);
                player.sendMessage(Text.literal(String.format("Preview: %s (%dx%dx%d) | CLEAR | /confirmbuild",
                        p.label(), p.width(), p.depth(), p.height())).formatted(Formatting.GREEN), true);
            } else {
                drawBox(world, player, candidate, ParticleTypes.END_ROD);
                for (BoundingRegion ov : overlaps) {
                    drawBox(world, player, ov, DustParticleEffect.DEFAULT);
                }
                player.sendMessage(Text.literal(String.format("COLLISION! %s overlaps %d building(s) [RED ZONE] | /confirmbuild or /cancelpreview",
                        p.label(), overlaps.size())).formatted(Formatting.RED, Formatting.BOLD), true);
            }
        }
    }

    private static void drawBox(ServerWorld world, ServerPlayerEntity player, BoundingRegion b, ParticleEffect particle) {
        double x1 = b.minX(), y1 = b.minY() + 0.5, z1 = b.minZ();
        double x2 = b.maxX() + 1, y2 = Math.min(b.maxY() + 1, b.minY() + 30), z2 = b.maxZ() + 1;

        drawEdge(world, player, particle, x1, y1, z1, x2, y1, z1);
        drawEdge(world, player, particle, x1, y1, z2, x2, y1, z2);
        drawEdge(world, player, particle, x1, y1, z1, x1, y1, z2);
        drawEdge(world, player, particle, x2, y1, z1, x2, y1, z2);

        drawEdge(world, player, particle, x1, y2, z1, x2, y2, z1);
        drawEdge(world, player, particle, x1, y2, z2, x2, y2, z2);
        drawEdge(world, player, particle, x1, y2, z1, x1, y2, z2);
        drawEdge(world, player, particle, x2, y2, z1, x2, y2, z2);

        drawEdge(world, player, particle, x1, y1, z1, x1, y2, z1);
        drawEdge(world, player, particle, x2, y1, z1, x2, y2, z1);
        drawEdge(world, player, particle, x1, y1, z2, x1, y2, z2);
        drawEdge(world, player, particle, x2, y1, z2, x2, y2, z2);
    }

    private static void drawEdge(ServerWorld world, ServerPlayerEntity player, ParticleEffect particle,
                                 double x1, double y1, double z1, double x2, double y2, double z2) {
        double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int steps = Math.min(24, Math.max(2, (int) Math.ceil(dist / 3.0)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            world.spawnParticles(player, particle, true,
                    x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t,
                    1, 0, 0, 0, 0);
        }
    }
}
