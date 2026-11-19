package moderncity.block;

import moderncity.ModernCityArchitect;
import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModBlocks {

    public static final Block ELEVATOR_BLOCK = Registry.register(
            Registries.BLOCK,
            Identifier.of(ModernCityArchitect.MOD_ID, "elevator_block"),
            new Block(AbstractBlock.Settings.create()
                    .mapColor(MapColor.OFF_WHITE)
                    .strength(2.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> 10))
    );

    public static final Item ELEVATOR_BLOCK_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(ModernCityArchitect.MOD_ID, "elevator_block"),
            new BlockItem(ELEVATOR_BLOCK, new Item.Settings())
    );

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    public static void register() {
        ModernCityArchitect.LOGGER.info("Registered Modern City Architect custom blocks (Elevator).");
    }

    public static void tickElevators(MinecraftServer server) {
        long now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            long last = COOLDOWNS.getOrDefault(player.getUuid(), 0L);
            if (now - last < 12) continue;

            ServerWorld world = player.getServerWorld();
            BlockPos pos = player.getBlockPos();

            // Check Sneak (Down) while standing on Elevator
            if (player.isSneaking() && world.getBlockState(pos.down()).isOf(ELEVATOR_BLOCK)) {
                for (int y = pos.getY() - 2; y >= world.getBottomY() + 1; y--) {
                    BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
                    if (world.getBlockState(check).isOf(ELEVATOR_BLOCK)) {
                        player.requestTeleport(pos.getX() + 0.5, y + 1.0, pos.getZ() + 0.5);
                        world.playSound(null, check, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.8f, 0.7f);
                        player.sendMessage(Text.literal("Elevator: Down to Y=" + (y + 1)).formatted(Formatting.AQUA), true);
                        COOLDOWNS.put(player.getUuid(), now);
                        break;
                    }
                }
            }
            // Check Jump (Up) when jumping off Elevator
            else if (!player.isOnGround() && player.getVelocity().y > 0.15
                    && (world.getBlockState(pos.down()).isOf(ELEVATOR_BLOCK) || world.getBlockState(pos.down(2)).isOf(ELEVATOR_BLOCK))) {
                int startY = world.getBlockState(pos.down()).isOf(ELEVATOR_BLOCK) ? pos.getY() : pos.getY() - 1;
                for (int y = startY + 1; y < world.getTopY() - 2; y++) {
                    BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
                    if (world.getBlockState(check).isOf(ELEVATOR_BLOCK)) {
                        player.requestTeleport(pos.getX() + 0.5, y + 1.0, pos.getZ() + 0.5);
                        world.playSound(null, check, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.8f, 1.3f);
                        player.sendMessage(Text.literal("Elevator: Up to Y=" + (y + 1)).formatted(Formatting.GREEN), true);
                        COOLDOWNS.put(player.getUuid(), now);
                        break;
                    }
                }
            }
        }
    }
}
