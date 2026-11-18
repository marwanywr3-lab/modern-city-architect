package moderncity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import moderncity.gui.VisualCatalogScreenHandler;
import moderncity.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CatalogCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("catalog")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(CatalogCommand::openCatalog)
                .then(CommandManager.literal("give").executes(CatalogCommand::giveCatalog)));
    }

    private static int openCatalog(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new VisualCatalogScreenHandler(syncId, inv),
                    Text.literal("Modern City Architect - Visual Catalog")
            ));
            return 1;
        }
        return 0;
    }

    private static int giveCatalog(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            player.getInventory().insertStack(new ItemStack(ModItems.VISUAL_CATALOG));
            return 1;
        }
        return 0;
    }
}
