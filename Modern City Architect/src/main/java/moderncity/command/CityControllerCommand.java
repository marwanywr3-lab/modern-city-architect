package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import moderncity.item.ModItems;
import moderncity.road.RoadNetworkManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class CityControllerCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("citycontroller")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .executes(CityControllerCommand::giveController))
                .then(CommandManager.literal("lights")
                        .then(CommandManager.literal("on").executes(ctx -> setLights(ctx, true)))
                        .then(CommandManager.literal("off").executes(ctx -> setLights(ctx, false))))
                .then(CommandManager.literal("sign")
                        .then(CommandManager.argument("streetName", StringArgumentType.greedyString())
                                .executes(CityControllerCommand::placeStreetSign))));
    }

    private static int giveController(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            player.getInventory().insertStack(new ItemStack(ModItems.CITY_CONTROLLER));
            context.getSource().sendFeedback(() -> Text.literal("Given City Controller! Right-click to open GUI.")
                    .formatted(Formatting.AQUA), false);
            return 1;
        }
        return 0;
    }

    private static int setLights(CommandContext<ServerCommandSource> context, boolean lit) {
        RoadNetworkManager.forceLightingState(context.getSource().getServer(), lit);
        context.getSource().sendFeedback(() -> Text.literal("City streetlights forced " + (lit ? "ON" : "OFF") + ".")
                .formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int placeStreetSign(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        String name = StringArgumentType.getString(context, "streetName");
        BlockPos base = BlockPos.ofFloored(source.getPosition()).east(2);

        world.setBlockState(base, Blocks.IRON_BARS.getDefaultState());
        world.setBlockState(base.up(), Blocks.IRON_BARS.getDefaultState());
        BlockPos signPos = base.up(2);
        world.setBlockState(signPos, Blocks.BIRCH_SIGN.getDefaultState());

        if (world.getBlockEntity(signPos) instanceof SignBlockEntity signEntity) {
            SignText text = new SignText()
                    .withMessage(0, Text.literal("STREET").formatted(Formatting.DARK_BLUE))
                    .withMessage(1, Text.literal(name).formatted(Formatting.BLACK))
                    .withMessage(2, Text.literal("MODERN CITY").formatted(Formatting.GRAY));
            signEntity.setText(text, true);
            signEntity.markDirty();
        }

        source.sendFeedback(() -> Text.literal("Placed street sign: " + name).formatted(Formatting.GREEN), true);
        return 1;
    }
}
