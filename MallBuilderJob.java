package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class MallBuilderJob implements StreamingBuilderEngine.BuildJob {

    private static final int FLOOR_HEIGHT = 6;
    private static final int SHOP_DEPTH = 14;
    private static final int SHOP_WIDTH = 16;

    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos origin;
    private final int length;
    private final int width;
    private final int floors;

    private final int totalHeight;
    private final int atriumHalfWidth;
    private final long totalBlocks;

    private int curX = 0;
    private int curY = 0;
    private int curZ = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public MallBuilderJob(ServerWorld world, UUID ownerId, BlockPos origin, int length, int width, int floors) {
        this.world = world;
        this.ownerId = ownerId;
        this.origin = origin;
        this.length = length;
        this.width = width;
        this.floors = floors;

        int maxConcourse = Math.max(8, (width - (SHOP_DEPTH * 2) - 4) / 2);
        this.atriumHalfWidth = Math.min(maxConcourse, Math.max(6, width / 6));

        int roofArchHeight = Math.max(4, atriumHalfWidth / 2 + 2);
        this.totalHeight = (floors * FLOOR_HEIGHT) + roofArchHeight + 1;
        this.totalBlocks = (long) length * width * totalHeight;
    }

    @Override
    public String getName() {
        return String.format("Modern Glass Mall (%dx%d, %d floors)", length, width, floors);
    }

    @Override
    public ServerWorld getWorld() {
        return world;
    }

    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public long getTotalBlocks() {
        return totalBlocks;
    }

    @Override
    public long getPlacedBlocks() {
        return placedBlocks;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public int processBatch(int maxBlocks, long deadlineNanos) {
        int processed = 0;

        while (processed < maxBlocks && !finished) {
            if ((processed & 255) == 0 && System.nanoTime() >= deadlineNanos) {
                break;
            }

            BlockState state = computeBlockAt(curX, curY, curZ);
            if (state != null) {
                mutablePos.set(origin.getX() + curX, origin.getY() + curY, origin.getZ() + curZ);
                StreamingBuilderEngine.placeBlockFast(world, mutablePos, state);
            }

            processed++;
            placedBlocks++;
            advanceCursor();
        }

        return processed;
    }

    private void advanceCursor() {
        curZ++;
        if (curZ >= width) {
            curZ = 0;
            curY++;
            if (curY >= totalHeight) {
                curY = 0;
                curX++;
                if (curX >= length) {
                    finished = true;
                }
            }
        }
    }

    private BlockState computeBlockAt(int x, int y, int z) {
        int centerZ = width / 2;
        int distFromCenterZ = Math.abs(z - centerZ);

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
            int midX = length / 2;
            plazaLocalX = Math.abs(x - midX);
            inPlazaX = plazaLocalX <= 7;
        }

        int mallMainTopY = floors * FLOOR_HEIGHT;

        if (y > mallMainTopY) {
            if (distFromCenterZ <= atriumHalfWidth + 1) {
                int archHeight = Math.max(4, atriumHalfWidth / 2 + 2);
                double norm = (double) distFromCenterZ / (atriumHalfWidth + 1);
                int targetRoofY = mallMainTopY + (int) Math.round(archHeight * Math.sqrt(Math.max(0.0, 1.0 - norm * norm)));

                if (y == targetRoofY) {
                    if (x % 10 == 0 || x == 0 || x == length - 1) {
                        return Blocks.WHITE_CONCRETE.getDefaultState();
                    }
                    if (distFromCenterZ <= 1 && x % 8 == 4) {
                        return Blocks.SEA_LANTERN.getDefaultState();
                    }
                    return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
                } else if (y < targetRoofY && (x == 0 || x == length - 1)) {
                    return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
                }
            }
            return Blocks.AIR.getDefaultState();
        }

        int floorIndex = Math.min(floors - 1, y / FLOOR_HEIGHT);
        int localY = y % FLOOR_HEIGHT;
        boolean isTopSlab = (y == mallMainTopY);

        if (isTopSlab) {
            if (distFromCenterZ <= atriumHalfWidth) {
                return Blocks.AIR.getDefaultState();
            }
            if (x == 0 || x == length - 1 || z == 0 || z == width - 1) {
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            return (x % 8 == 0 && z % 8 == 0)
                    ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState()
                    : Blocks.SMOOTH_QUARTZ.getDefaultState();
        }

        boolean isOuterX = (x == 0 || x == length - 1);
        boolean isOuterZ = (z == 0 || z == width - 1);

        if (isOuterX || isOuterZ) {
            if (isOuterX && distFromCenterZ <= atriumHalfWidth) {
                if (floorIndex == 0 && localY >= 1 && localY <= 4 && distFromCenterZ <= 4) {
                    return Blocks.AIR.getDefaultState();
                }
                if (localY == 0) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                if (localY == 5) return Blocks.WHITE_CONCRETE.getDefaultState();
                return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }

            if (localY == 0 || localY == 5) {
                return (floorIndex % 2 == 0)
                        ? Blocks.WHITE_CONCRETE.getDefaultState()
                        : Blocks.WHITE_TERRACOTTA.getDefaultState();
            }
            if ((x % 12 == 0) || (z % 12 == 0)) {
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            return Blocks.TINTED_GLASS.getDefaultState();
        }

        if (localY == 0) {
            if (floorIndex == 0) {
                if (inPlazaX && distFromCenterZ <= 5) {
                    int r = Math.max(plazaLocalX, distFromCenterZ);
                    if (r == 5) return Blocks.SMOOTH_QUARTZ.getDefaultState();
                    if (r <= 4) return Blocks.SEA_LANTERN.getDefaultState();
                }
                if (distFromCenterZ <= atriumHalfWidth) {
                    return (x % 6 == 0 && distFromCenterZ % 4 == 0)
                            ? Blocks.SEA_LANTERN.getDefaultState()
                            : Blocks.SMOOTH_QUARTZ.getDefaultState();
                }
                return Blocks.BIRCH_PLANKS.getDefaultState();
            } else {
                if (distFromCenterZ <= atriumHalfWidth - 3) {
                    return Blocks.AIR.getDefaultState();
                }
                if (distFromCenterZ <= atriumHalfWidth) {
                    return (x % 8 == 0 && distFromCenterZ == atriumHalfWidth - 1)
                            ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState()
                            : Blocks.SMOOTH_QUARTZ.getDefaultState();
                }
                return (x % 6 == 3 && z % 6 == 3)
                        ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState()
                        : Blocks.WHITE_CONCRETE.getDefaultState();
            }
        }

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

        if (floorIndex > 0 && localY == 1 && distFromCenterZ == atriumHalfWidth - 3) {
            return Blocks.GRAY_STAINED_GLASS_PANE.getDefaultState();
        }

        if (floorIndex < floors - 1 && distFromCenterZ == atriumHalfWidth - 1) {
            int stairCycle = x % 80;
            if (stairCycle >= 10 && stairCycle <= 15) {
                int stepOffset = stairCycle - 10;
                if (localY == stepOffset + 1) {
                    return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState();
                }
                if (localY > stepOffset + 1) {
                    return Blocks.AIR.getDefaultState();
                }
            }
        }

        if (distFromCenterZ <= atriumHalfWidth) {
            return Blocks.AIR.getDefaultState();
        }

        int shopZoneStart = atriumHalfWidth + 1;
        int distIntoWing = distFromCenterZ - shopZoneStart;

        if (distIntoWing == 0) {
            int shopLocalX = x % SHOP_WIDTH;
            if (shopLocalX >= (SHOP_WIDTH / 2 - 2) && shopLocalX <= (SHOP_WIDTH / 2 + 1) && localY <= 3) {
                return Blocks.AIR.getDefaultState();
            }
            if (localY == 4 || localY == 5) {
                return Blocks.WHITE_TERRACOTTA.getDefaultState();
            }
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }

        if (x % SHOP_WIDTH == 0) {
            return Blocks.WHITE_CONCRETE.getDefaultState();
        }

        if (distIntoWing == SHOP_DEPTH) {
            return Blocks.WHITE_TERRACOTTA.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }
}
