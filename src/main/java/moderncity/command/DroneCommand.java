package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class DroneCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("drone")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(DroneCommand::toggleDrone));
    }

    private static int toggleDrone(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                player.changeGameMode(GameMode.CREATIVE);
                player.getAbilities().setFlySpeed(0.05f);
                player.sendAbilitiesUpdate();
                player.sendMessage(Text.literal("Drone Mode OFF (Returned to Creative Builder).").formatted(Formatting.YELLOW), true);
            } else {
                player.changeGameMode(GameMode.SPECTATOR);
                player.getAbilities().setFlySpeed(0.15f);
                player.sendAbilitiesUpdate();
                player.sendMessage(Text.literal("Drone Camera Mode ON! High-speed aerial view active.").formatted(Formatting.AQUA), true);
            }
            return 1;
        }
        return 0;
    }
}
