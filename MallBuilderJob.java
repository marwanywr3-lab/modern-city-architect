package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class MallBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int FLOOR_HEIGHT = 6;
    private static final int SHOP_DEPTH = 14;
    private static final int SHOP_WIDTH = 16;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos frontLeftAnchor;
    private final Direction facing;
    private final int length; // forward (+forward)
    private final int width;  // right (+right)
    private final int floors;

    private final int totalHeight;
    private final int atriumHalfWidth;
    private final long totalBlocks;

    private int curF = 0; // forward (0..length-1)
    private int curY = 0;
    private int curR = 0; // right (0..width-1)
    private long placedBlocks = 0;
    private boolean finished = false;

    public MallBuilderJob(ServerWorld world, UUID ownerId, BlockPos frontLeftAnchor, Direction facing,
                          int length, int width, int floors) {
        this.world = world;
        this.ownerId = ownerId;
        this.frontLeftAnchor = frontLeftAnchor;
        this.facing = facing;
        this.length = length;
        this.width = width;
        this.floors = floors;

        int maxConcourse = Math.max(8, (width - (SHOP_DEPTH * 2) - 4) / 2);
        this.atriumHalfWidth = Math.min(maxConcourse, Math.max(6, width / 6));
        int roofArchHeight = Math.max(5, atriumHalfWidth / 2 + 2);
        this.totalHeight = (floors * FLOOR_HEIGHT) + roofArchHeight + 1;
        this.totalBlocks = (long) length * width * totalHeight;
    }

    public MallBuilderJob(ServerWorld world, UUID ownerId, BlockPos origin, int length, int width, int floors) {
        this(world, ownerId, origin, Direction.EAST, length, width, floors);
    }

    @Override
    public String getName() { return String.format("Modern Glass Mall (%dx%d, %dF)", width, length, floors); }

    @Override
    public ServerWorld getWorld() { return world; }

    @Override
    public UUID getOwnerId() { return ownerId; }

    @Override
    public long getTotalBlocks() { return totalBlocks; }

    @Override
    public long getPlacedBlocks() { return placedBlocks; }

    @Override
    public boolean isFinished() { return finished; }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        int processed = 0;
        while (processed < maxBlocks && !finished) {
            if ((processed & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
            BlockState state = computeBlockAt(curF, curY, curR);
            if (state != null) {
                BlockPos wp = PlacementUtil.toWorld(frontLeftAnchor, facing, curR, curY, curF);
                StreamingBuilderEngine.placeBlockFast(world, wp, state);
            }
            processed++;
            placedBlocks++;
            curR++;
            if (curR >= width) {
                curR = 0;
                curY++;
                if (curY >= totalHeight) {
                    curY = 0;
                    curF++;
                    if (curF >= length) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        int centerZ = width / 2;
        int distFromCenterZ = Math.abs(z - centerZ);
        int mallMainTopY = floors * FLOOR_HEIGHT;

        // 1. Fully Closed Vaulted Glass Roof above concourse + Closed Roof Deck over wings
        if (y > mallMainTopY) {
            if (distFromCenterZ <= atriumHalfWidth + 1) {
                int archHeight = Math.max(5, atriumHalfWidth / 2 + 2);
                double norm = (double) distFromCenterZ / (atriumHalfWidth + 1);
                int targetRoofY = mallMainTopY + (int) Math.round(archHeight * Math.sqrt(Math.max(0.0, 1.0 - norm * norm)));

                if (y == targetRoofY) {
                    if (x % 10 == 0 || x == 0 || x == length - 1) return Blocks.WHITE_CONCRETE.getDefaultState();
                    if (distFromCenterZ <= 1 && x % 8 == 4) return Blocks.SEA_LANTERN.getDefaultState();
                    return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
                } else if (y < targetRoofY && (x == 0 || x == length - 1)) {
                    return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
                }
            }
            return Blocks.AIR.getDefaultState();
        }

        // Closed roof slab at mallMainTopY
        if (y == mallMainTopY) {
            if (distFromCenterZ <= atriumHalfWidth) {
                return Blocks.AIR.getDefaultState(); // Directly covered by vaulted glass arch above
            }
            if (x == 0 || x == length - 1 || z == 0 || z == width - 1) return Blocks.WHITE_CONCRETE.getDefaultState();
            return (x % 6 == 0 && z % 6 == 0)
                    ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState()
                    : Blocks.WHITE_CONCRETE.getDefaultState();
        }

        int floorIndex = Math.min(floors - 1, y / FLOOR_HEIGHT);
        int localY = y % FLOOR_HEIGHT;

        // 2. Exterior Perimeter & Grand Entrances
        boolean isOuterX = (x == 0 || x == length - 1);
        boolean isOuterZ = (z == 0 || z == width - 1);
        if (isOuterX || isOuterZ) {
            if (isOuterX && distFromCenterZ <= atriumHalfWidth) {
                // Grand Entrance Opening (6 blocks wide, 4 blocks tall) + Canopy
                if (floorIndex == 0 && localY >= 1 && localY <= 4 && distFromCenterZ <= 4) {
                    return Blocks.AIR.getDefaultState();
                }
                if (floorIndex == 0 && localY == 5 && distFromCenterZ <= 5) {
                    return Blocks.SEA_LANTERN.getDefaultState();
                }
                if (localY == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                if (localY == 5) return Blocks.WHITE_CONCRETE.getDefaultState();
                return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
            if (localY == 0 || localY == 5) {
                return (floorIndex % 2 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.WHITE_TERRACOTTA.getDefaultState();
            }
            if ((x % 12 == 0) || (z % 12 == 0)) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.TINTED_GLASS.getDefaultState();
        }

        // Plaza fountain spacing every 200 blocks
        int plazaSpacing = 200;
        boolean inPlazaX;
        int plazaLocalX;
        if (length >= 220) {
            int nearestPlaza = ((x + (plazaSpacing / 2)) / plazaSpacing) * plazaSpacing;
            if (nearestPlaza == 0) nearestPlaza = plazaSpacing / 2;
            if (nearestPlaza >= length - 20) nearestPlaza = length - 40;
            plazaLocalX = Math.abs(x - nearestPlaza);
            inPlazaX = plazaLocalX <= 9;
        } else {
            plazaLocalX = Math.abs(x - (length / 2));
            inPlazaX = plazaLocalX <= 7;
        }

        // 3. Floor Slabs & Real Staircase Headroom Cutouts
        boolean inStairFootprint = (distFromCenterZ >= atriumHalfWidth - 2 && distFromCenterZ <= atriumHalfWidth - 1)
                && (x % 80 >= 10 && x % 80 <= 16);

        if (localY == 0) {
            if (floorIndex == 0) {
                if (inPlazaX && distFromCenterZ <= 5) {
                    int r = Math.max(plazaLocalX, distFromCenterZ);
                    if (r == 5) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                    if (r <= 4) return Blocks.SEA_LANTERN.getDefaultState();
                }
                if (distFromCenterZ <= atriumHalfWidth) {
                    return (x % 6 == 0 && distFromCenterZ % 4 == 0)
                            ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
                }
                return Blocks.BIRCH_PLANKS.getDefaultState();
            } else {
                if (inStairFootprint) return Blocks.AIR.getDefaultState(); // Open stairwell headroom!
                if (distFromCenterZ <= atriumHalfWidth - 3) return Blocks.AIR.getDefaultState();
                if (distFromCenterZ <= atriumHalfWidth) {
                    return (x % 8 == 0 && distFromCenterZ == atriumHalfWidth - 1)
                            ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState();
                }
                return (x % 6 == 3 && z % 6 == 3)
                        ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
            }
        }

        // 4. Proper Oriented Staircases connecting floors cleanly
        if (floorIndex < floors - 1 && inStairFootprint) {
            int step = (x % 80) - 10;
            if (step >= 0 && step <= 5 && localY == step + 1) {
                return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                        .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
            }
            return Blocks.AIR.getDefaultState();
        }

        // 5. Fountains
        if (floorIndex == 0 && inPlazaX && distFromCenterZ <= 5) {
            int r = Math.max(plazaLocalX, distFromCenterZ);
            if (localY == 1) {
                if (r == 5) return Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState();
                if (r == 4) return Blocks.AZALEA_LEAVES.getDefaultState();
                if (r == 3) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                if (r == 0) return Blocks.QUARTZ_PILLAR.getDefaultState();
                return Blocks.WATER.getDefaultState();
            }
            if (localY == 2 && r == 0) return Blocks.SEA_LANTERN.getDefaultState();
            if (localY == 3 && r == 0) return Blocks.WATER.getDefaultState();
        }

        // 6. Mezzanine Railings
        if (floorIndex > 0 && localY == 1 && distFromCenterZ == atriumHalfWidth - 3) {
            return Blocks.GRAY_STAINED_GLASS_PANE.getDefaultState();
        }

        if (distFromCenterZ <= atriumHalfWidth) return Blocks.AIR.getDefaultState();

        // 7. Divided Retail Wings
        int distIntoWing = distFromCenterZ - (atriumHalfWidth + 1);
        if (distIntoWing == 0) {
            int shopLocalX = x % SHOP_WIDTH;
            if (shopLocalX >= (SHOP_WIDTH / 2 - 2) && shopLocalX <= (SHOP_WIDTH / 2 + 1) && localY <= 3) {
                return Blocks.AIR.getDefaultState();
            }
            if (localY >= 4) return Blocks.WHITE_TERRACOTTA.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }
        if (x % SHOP_WIDTH == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
        if (distIntoWing == SHOP_DEPTH) return Blocks.WHITE_TERRACOTTA.getDefaultState();

        return Blocks.AIR.getDefaultState();
    }
}
