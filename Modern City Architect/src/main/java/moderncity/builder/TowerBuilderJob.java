package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.map.WaypointManager;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class TowerBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum Style {
        TWISTED,          // 90-degree rotating helix tower (Al Majdoul / Cayan style)
        CRYSTAL_DIAGRID,  // Hexagonal faceted diamond-grid tower (KAFD PIF Tower style)
        ARCH_PORTAL,      // Monumental twin-leg sky-arch portal tower
        SPIRAL_GARDEN,    // Elliptical tower with spiraling exterior green sky-gardens
        SAIL_SPIRE        // Tapered diamond-sail landmark with illuminated spire
    }

    public enum Tier {
        MID(35, Style.SPIRAL_GARDEN, "Mid-Rise Garden Tower (35b)"),
        HIGH(85, Style.TWISTED, "High-Rise Twisted Tower (85b)"),
        SUPERTALL(135, Style.CRYSTAL_DIAGRID, "Supertall Crystal Tower (135b)");

        public final int height;
        public final Style defaultStyle;
        public final String label;

        Tier(int height, Style defaultStyle, String label) {
            this.height = height;
            this.defaultStyle = defaultStyle;
            this.label = label;
        }
    }

    private static final int FLOOR_HEIGHT = 5;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final Style style;
    private final int size;
    private final int totalHeight;
    private final int floors;
    private final long totalBlocks;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public TowerBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing, Style style, int requestedHeight) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.style = style;

        int maxAllowed = Math.max(20, world.getTopY() - anchor.getY() - 2);
        this.totalHeight = Math.min(480, Math.min(requestedHeight, maxAllowed));
        this.size = (totalHeight >= 150) ? 40 : (totalHeight >= 80 ? 34 : 28);
        this.floors = Math.max(4, (totalHeight - 8) / FLOOR_HEIGHT);
        this.totalBlocks = (long) size * size * totalHeight;
        WaypointManager.addWaypoint(style.name() + " Tower", anchor, 0x38BDF8);
    }

    public TowerBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Tier tier) {
        this(world, ownerId, anchor, Direction.NORTH, tier.defaultStyle, tier.height);
    }

    @Override public String getName() { return style.name() + " Tower (" + totalHeight + "b)"; }
    @Override public ServerWorld getWorld() { return world; }
    @Override public UUID getOwnerId() { return ownerId; }
    @Override public long getTotalBlocks() { return totalBlocks; }
    @Override public long getPlacedBlocks() { return placedBlocks; }
    @Override public boolean isFinished() { return finished; }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        int processed = 0;
        while (processed < maxBlocks && !finished) {
            if ((processed & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
            BlockState state = computeBlockAt(curR, curY, curF);
            if (state != null) {
                BlockPos wp = PlacementUtil.toWorld(anchor, facing, curR, curY, curF);
                StreamingBuilderEngine.placeBlockFast(world, wp, state);
            }
            processed++;
            placedBlocks++;
            curR++;
            if (curR >= size) {
                curR = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curF++;
                    if (curF >= size) finished = true;
                }
            }
        }
        return processed;
    }

    private boolean isInsideShape(int x, int y, int z, int mainTopY) {
        double c = (size - 1) / 2.0;
        double dx = x - c;
        double dz = z - c;
        double halfR = (size / 2.0) - 2.0;
        double hNorm = Math.min(1.0, Math.max(0.0, (double) y / Math.max(1, mainTopY)));

        return switch (style) {
            case TWISTED -> {
                double angle = hNorm * (Math.PI / 2.0);
                double rx = dx * Math.cos(angle) - dz * Math.sin(angle);
                double rz = dx * Math.sin(angle) + dz * Math.cos(angle);
                double r = halfR * (1.0 - 0.12 * hNorm);
                yield Math.abs(rx) <= r && Math.abs(rz) <= r * 0.78;
            }
            case CRYSTAL_DIAGRID -> {
                // Hexagonal / crystalline prism tapering toward crown
                double r = halfR * (1.0 - 0.25 * Math.pow(hNorm, 1.4));
                yield Math.abs(dx) <= r && (Math.abs(dz) + Math.abs(dx) * 0.55) <= r * 1.15;
            }
            case ARCH_PORTAL -> {
                // Two separate legs rising up, bridging at top 25% into a monumental sky-portal!
                if (hNorm >= 0.74) {
                    yield Math.abs(dx) <= halfR && Math.abs(dz) <= halfR * 0.7;
                } else {
                    boolean leftLeg = (dx <= -3.5 && dx >= -halfR);
                    boolean rightLeg = (dx >= 3.5 && dx <= halfR);
                    yield (leftLeg || rightLeg) && Math.abs(dz) <= halfR * 0.7;
                }
            }
            case SPIRAL_GARDEN -> {
                // Elliptical organic footprint tapering smoothly
                double rx = halfR * (1.0 - 0.2 * hNorm);
                double rz = halfR * 0.82 * (1.0 - 0.2 * hNorm);
                yield (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz) <= 1.0;
            }
            case SAIL_SPIRE -> {
                // Sleek diamond sail tapering to a dramatic spire
                double r = halfR * (1.0 - 0.55 * hNorm);
                yield (Math.abs(dx) * 1.15 + Math.abs(dz) * 0.85) <= r * 1.25;
            }
        };
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        int mainTopY = floors * FLOOR_HEIGHT;
        int center = size / 2;

        // Illuminated sculptural spire/crown above mainTopY
        if (y > mainTopY) {
            int crownY = y - mainTopY;
            if (style == Style.SAIL_SPIRE || style == Style.CRYSTAL_DIAGRID) {
                int spireR = Math.max(0, 3 - crownY / 2);
                if (Math.abs(x - center) <= spireR && Math.abs(z - center) <= spireR && crownY <= 7) {
                    return (crownY >= 6) ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
                }
            } else if (totalHeight >= 100 && crownY == 1 && isInsideShape(x, mainTopY, z, mainTopY)) {
                int dx = Math.abs(x - center);
                int dz = Math.abs(z - center);
                if (dx <= 5 && dz <= 5) {
                    if ((dx == 5 || dz == 5) && (dx + dz) % 2 == 0) return Blocks.SEA_LANTERN.getDefaultState();
                    boolean isH = (dz >= -2 && dz <= 2 && dx == 2) || (dx >= -2 && dx <= 2 && dz == 0);
                    return isH ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
                }
            }
            return Blocks.AIR.getDefaultState();
        }

        boolean inside = isInsideShape(x, y, z, mainTopY);
        if (!inside) return Blocks.AIR.getDefaultState();

        // Check if on exterior boundary of the sculptural shape
        boolean isFacade = !isInsideShape(x - 1, y, z, mainTopY)
                || !isInsideShape(x + 1, y, z, mainTopY)
                || !isInsideShape(x, y, z - 1, mainTopY)
                || !isInsideShape(x, y, z + 1, mainTopY);

        if (y == mainTopY) {
            return isFacade ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        int floorIndex = y / FLOOR_HEIGHT;
        int localY = y % FLOOR_HEIGHT;

        if (isFacade) {
            // Ground floor entrance on front side
            if (floorIndex == 0 && z <= center - 4 && Math.abs(x - center) <= 3 && localY >= 1 && localY <= 3) {
                return Blocks.AIR.getDefaultState();
            }

            // Style-specific iconic facade patterns!
            if (style == Style.CRYSTAL_DIAGRID) {
                // Diagonal diamond diagrid fins like KAFD PIF Tower
                boolean onDiagrid = ((x + y) % 7 == 0) || ((z - y + 700) % 7 == 0);
                if (onDiagrid) return Blocks.WHITE_CONCRETE.getDefaultState();
                return (localY == 0) ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            } else if (style == Style.SPIRAL_GARDEN) {
                // Spiraling exterior sky-garden ribbon wrapping around the tower
                double angle = Math.atan2(z - center, x - center);
                double spiralPhase = ((angle + Math.PI) / (2 * Math.PI) + (y / 28.0)) % 1.0;
                if (spiralPhase < 0.16) {
                    if (localY == 0) return Blocks.GRASS_BLOCK.getDefaultState();
                    if (localY == 1) return Blocks.AZALEA_LEAVES.getDefaultState();
                }
            }

            if (localY == 0) {
                return (floorIndex % 2 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState();
            }
            if ((x + z) % 6 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (floorIndex % 2 == 0) ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.TINTED_GLASS.getDefaultState();
        }

        // Flush Stairwell inside central core (or inside left leg for Arch Portal)
        int stairCenterX = (style == Style.ARCH_PORTAL) ? (center - 6) : (center - 4);
        boolean inStairBox = (x >= stairCenterX && x <= stairCenterX + 1) && (z >= center - 2 && z <= center + 2);
        if (inStairBox) {
            int step = z - (center - 2);
            for (int f = 0; f < floors - 1; f++) {
                int stairY = f * FLOOR_HEIGHT + step + 1;
                if (y == stairY) {
                    return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                            .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
                }
                if (y > stairY && y <= stairY + 4) return Blocks.AIR.getDefaultState();
            }
        }

        if (localY == 0) {
            if (Math.abs(x - center) <= 1 && Math.abs(z - center) <= 1 && floorIndex > 0 && style != Style.ARCH_PORTAL) {
                return Blocks.AIR.getDefaultState();
            }
            return (floorIndex == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        if (localY == 4 && x % 6 == 0 && z % 6 == 0) {
            return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
        }

        // 1 Company per Floor: Reception + Open Employee Bullpen + Back Private Offices
        if (floorIndex > 0 && style != Style.ARCH_PORTAL) {
            if (z == center - 5 && Math.abs(x - center) <= 1 && localY == 1) {
                return Blocks.SMOOTH_QUARTZ.getDefaultState();
            }
            if (z == center + 4 || (z > center + 4 && (x == center - 4 || x == center + 4))) {
                if (localY <= 3 && (x == center - 2 || x == center + 2 || z == center + 7)) {
                    return Blocks.AIR.getDefaultState();
                }
                return (localY == 4) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }
}
