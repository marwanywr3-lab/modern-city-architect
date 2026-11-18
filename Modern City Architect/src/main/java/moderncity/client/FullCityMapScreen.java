package moderncity.client;

import moderncity.map.WaypointManager;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class FullCityMapScreen extends Screen {

    private int centerX;
    private int centerZ;
    private int blocksPerPixel = 2;
    private boolean dragging = false;

    public FullCityMapScreen() {
        super(Text.literal("Modern City Architect - Interactive City Map"));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            this.centerX = client.player.getBlockX();
            this.centerZ = client.player.getBlockZ();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        int mapW = Math.min(360, this.width - 40);
        int mapH = Math.min(240, this.height - 60);
        int x0 = (this.width - mapW) / 2;
        int y0 = (this.height - mapH) / 2;

        context.fill(x0 - 3, y0 - 3, x0 + mapW + 3, y0 + mapH + 3, 0xEE0F172A);

        World world = client.world;
        BlockPos.Mutable m = new BlockPos.Mutable();
        int step = 3; // Fast crisp sampling

        for (int sx = 0; sx < mapW; sx += step) {
            for (int sz = 0; sz < mapH; sz += step) {
                int wx = centerX + (sx - mapW / 2) * blocksPerPixel;
                int wz = centerZ + (sz - mapH / 2) * blocksPerPixel;
                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                m.set(wx, topY, wz);
                MapColor mc = world.getBlockState(m).getMapColor(world, m);
                int argb = (mc != null && mc.color != 0) ? (0xFF000000 | mc.color) : 0xFF1E293B;
                context.fill(x0 + sx, y0 + sz, x0 + sx + step, y0 + sz + step, argb);
            }
        }

        // Draw City Waypoint Pins
        for (WaypointManager.Waypoint wp : WaypointManager.getWaypoints()) {
            int px = x0 + mapW / 2 + (wp.pos().getX() - centerX) / blocksPerPixel;
            int pz = y0 + mapH / 2 + (wp.pos().getZ() - centerZ) / blocksPerPixel;
            if (px >= x0 && px <= x0 + mapW && pz >= y0 && pz <= y0 + mapH) {
                context.fill(px - 3, pz - 3, px + 3, pz + 3, 0xFF000000 | wp.color());
                context.drawCenteredTextWithShadow(this.textRenderer, wp.name(), px, pz - 12, 0xFFFFFFFF);
            }
        }

        // Player position pin
        int ppx = x0 + mapW / 2 + (client.player.getBlockX() - centerX) / blocksPerPixel;
        int ppz = y0 + mapH / 2 + (client.player.getBlockZ() - centerZ) / blocksPerPixel;
        if (ppx >= x0 && ppx <= x0 + mapW && ppz >= y0 && ppz <= y0 + mapH) {
            context.fill(ppx - 3, ppz - 3, ppx + 3, ppz + 3, 0xFFFF3333);
        }

        // Hover coordinates & Teleport hint
        if (mouseX >= x0 && mouseX <= x0 + mapW && mouseY >= y0 && mouseY <= y0 + mapH) {
            int hoverX = centerX + (mouseX - (x0 + mapW / 2)) * blocksPerPixel;
            int hoverZ = centerZ + (mouseY - (y0 + mapH / 2)) * blocksPerPixel;
            context.drawCenteredTextWithShadow(this.textRenderer,
                    String.format("X: %d  Z: %d  |  Right-Click to Teleport!  |  Drag to Pan  |  Scroll to Zoom", hoverX, hoverZ),
                    this.width / 2, y0 + mapH + 8, 0xFF38BDF8);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, y0 - 16, 0xFFFFD700);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        int mapW = Math.min(360, this.width - 40);
        int mapH = Math.min(240, this.height - 60);
        int x0 = (this.width - mapW) / 2;
        int y0 = (this.height - mapH) / 2;

        if (mouseX >= x0 && mouseX <= x0 + mapW && mouseY >= y0 && mouseY <= y0 + mapH) {
            if (button == 1 && client.player != null && client.world != null) {
                int targetX = centerX + ((int) mouseX - (x0 + mapW / 2)) * blocksPerPixel;
                int targetZ = centerZ + ((int) mouseY - (y0 + mapH / 2)) * blocksPerPixel;
                int safeY = Math.max(client.world.getBottomY() + 5,
                        client.world.getTopY(Heightmap.Type.WORLD_SURFACE, targetX, targetZ) + 1);
                client.player.networkHandler.sendChatCommand("tp @s " + targetX + " " + safeY + " " + targetZ);
                this.close();
                return true;
            } else if (button == 0) {
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            centerX -= (int) Math.round(deltaX * blocksPerPixel);
            centerZ -= (int) Math.round(deltaY * blocksPerPixel);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && blocksPerPixel > 1) blocksPerPixel--;
        else if (verticalAmount < 0 && blocksPerPixel < 8) blocksPerPixel++;
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
