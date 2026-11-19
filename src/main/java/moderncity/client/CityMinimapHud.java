package moderncity.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class CityMinimapHud implements HudRenderCallback {

    public static final int MAP_SIZE = 104;
    public static final int MARGIN = 8;
    private static final int SCALE = 2; // 2x2 px per block -> covers ~52x52 blocks

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        if (client.currentScreen instanceof FullCityMapScreen) return;

        int screenW = client.getWindow().getScaledWidth();
        int x0 = screenW - MAP_SIZE - MARGIN;
        int y0 = MARGIN;

        // Outer frame
        context.fill(x0 - 2, y0 - 2, x0 + MAP_SIZE + 2, y0 + MAP_SIZE + 2, 0xCC111827);

        World world = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        int halfGrid = MAP_SIZE / (SCALE * 2);
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int gx = -halfGrid; gx < halfGrid; gx++) {
            for (int gz = -halfGrid; gz < halfGrid; gz++) {
                int wx = playerPos.getX() + gx;
                int wz = playerPos.getZ() + gz;
                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                m.set(wx, topY, wz);
                MapColor mc = world.getBlockState(m).getMapColor(world, m);
                int argb = (mc != null && mc.color != 0) ? (0xFF000000 | mc.color) : 0xFF1F2937;

                int px = x0 + (gx + halfGrid) * SCALE;
                int py = y0 + (gz + halfGrid) * SCALE;
                context.fill(px, py, px + SCALE, py + SCALE, argb);
            }
        }

        // Player center marker
        int cx = x0 + MAP_SIZE / 2;
        int cy = y0 + MAP_SIZE / 2;
        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFF3333);

        // Label below minimap
        context.drawCenteredTextWithShadow(client.textRenderer, "[M] Full Map", x0 + MAP_SIZE / 2, y0 + MAP_SIZE + 4, 0xFF38BDF8);
    }
}
