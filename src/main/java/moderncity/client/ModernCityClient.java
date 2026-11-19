package moderncity.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModernCityClient implements ClientModInitializer {

    private static KeyBinding openMapKey;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new CityMinimapHud());

        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modern-city-architect.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.modern-city-architect"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new FullCityMapScreen());
                }
            }
        });
    }
}
