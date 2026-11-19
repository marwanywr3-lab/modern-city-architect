package moderncity.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import moderncity.ModernCityArchitect;
import moderncity.engine.StreamingBuilderEngine;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlueprintManager {

    public record RelativeBlock(int dx, int dy, int dz, BlockState state) {}
    private record SavedBlockDto(int dx, int dy, int dz, String blockId) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, BlockPos[]> SELECTIONS = new HashMap<>();
    private static final Map<String, List<RelativeBlock>> BLUEPRINTS = new HashMap<>();

    private static Path getBlueprintDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("moderncity/blueprints");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        return dir;
    }

    public static void setCorner1(UUID playerId, BlockPos pos) {
        BlockPos[] sel = SELECTIONS.computeIfAbsent(playerId, id -> new BlockPos[2]);
        sel[0] = pos.toImmutable();
    }

    public static void setCorner2(UUID playerId, BlockPos pos) {
        BlockPos[] sel = SELECTIONS.computeIfAbsent(playerId, id -> new BlockPos[2]);
        sel[1] = pos.toImmutable();
    }

    public static int saveSelection(UUID playerId, ServerWorld world, String name) {
        BlockPos[] sel = SELECTIONS.get(playerId);
        if (sel == null || sel[0] == null || sel[1] == null) return -1;

        int minX = Math.min(sel[0].getX(), sel[1].getX());
        int maxX = Math.max(sel[0].getX(), sel[1].getX());
        int minY = Math.min(sel[0].getY(), sel[1].getY());
        int maxY = Math.max(sel[0].getY(), sel[1].getY());
        int minZ = Math.min(sel[0].getZ(), sel[1].getZ());
        int maxZ = Math.max(sel[0].getZ(), sel[1].getZ());

        List<RelativeBlock> blocks = new ArrayList<>();
        List<SavedBlockDto> dtos = new ArrayList<>();
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    m.set(x, y, z);
                    BlockState state = world.getBlockState(m);
                    if (!state.isAir()) {
                        int dx = x - minX, dy = y - minY, dz = z - minZ;
                        blocks.add(new RelativeBlock(dx, dy, dz, state));
                        Identifier id = Registries.BLOCK.getId(state.getBlock());
                        dtos.add(new SavedBlockDto(dx, dy, dz, id.toString()));
                    }
                }
            }
        }
        String key = name.toLowerCase();
        BLUEPRINTS.put(key, blocks);

        // Save persistently to disk so blueprints survive across worlds & restarts!
        try {
            Path file = getBlueprintDir().resolve(key + ".json");
            Files.writeString(file, GSON.toJson(dtos));
        } catch (IOException e) {
            ModernCityArchitect.LOGGER.error("Failed to save blueprint file", e);
        }
        return blocks.size();
    }

    public static boolean pasteBlueprint(UUID playerId, ServerWorld world, BlockPos origin, String name) {
        String key = name.toLowerCase();
        List<RelativeBlock> blocks = BLUEPRINTS.get(key);

        if (blocks == null) {
            Path file = getBlueprintDir().resolve(key + ".json");
            if (Files.exists(file)) {
                try {
                    String json = Files.readString(file);
                    Type listType = new TypeToken<List<SavedBlockDto>>() {}.getType();
                    List<SavedBlockDto> dtos = GSON.fromJson(json, listType);
                    blocks = new ArrayList<>();
                    for (SavedBlockDto dto : dtos) {
                        Block b = Registries.BLOCK.get(Identifier.of(dto.blockId()));
                        blocks.add(new RelativeBlock(dto.dx(), dto.dy(), dto.dz(), b.getDefaultState()));
                    }
                    BLUEPRINTS.put(key, blocks);
                } catch (Exception e) {
                    ModernCityArchitect.LOGGER.error("Failed to load blueprint file", e);
                    return false;
                }
            } else {
                return false;
            }
        }

        List<RelativeBlock> finalBlocks = blocks;
        StreamingBuilderEngine.enqueueJob(new StreamingBuilderEngine.BuildJob() {
            private int cursor = 0;

            @Override public String getName() { return "Blueprint: " + name; }
            @Override public ServerWorld getWorld() { return world; }
            @Override public UUID getOwnerId() { return playerId; }
            @Override public long getTotalBlocks() { return finalBlocks.size(); }
            @Override public long getPlacedBlocks() { return cursor; }
            @Override public boolean isFinished() { return cursor >= finalBlocks.size(); }

            @Override
            public int processBatch(int maxBlocks, long deadlineNanos) {
                int count = 0;
                while (count < maxBlocks && cursor < finalBlocks.size()) {
                    if ((count & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
                    RelativeBlock rb = finalBlocks.get(cursor++);
                    StreamingBuilderEngine.placeBlockFast(world, origin.add(rb.dx(), rb.dy(), rb.dz()), rb.state());
                    count++;
                }
                return count;
            }
        });
        return true;
    }
}
