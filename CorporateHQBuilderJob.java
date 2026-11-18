package moderncity.builder;

import moderncity.engine.StreamingBuilderEngine;
import moderncity.util.PlacementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class CorporateHQBuilderJob implements StreamingBuilderEngine.BuildJob {

    public enum HQType {
        HQ_TOWER_TWIN_FIN(32, 32, 60, "HQ Tower Style A (Twin-Fin)"),
        HQ_TOWER_ATRIUM(34, 34, 120, "HQ Tower Style B (Executive Atrium)"),
        LOW_OFFICE_COURTYARD(36, 28, 12, "Low-Rise Office A (2F Suites)"),
        LOW_OFFICE_PAVILION(34, 28, 22, "Mid-Rise Office B (4F Suites)");

        public final int width;
        public final int depth;
        public final int defaultHeight;
        public final String label;

        HQType(int width, int depth, int defaultHeight, String label) {
            this.width = width;
            this.depth = depth;
            this.defaultHeight = defaultHeight;
            this.label = label;
        }
    }

    private static final int FLOOR_HEIGHT = 5;
    private final ServerWorld world;
    private final UUID ownerId;
    private final BlockPos anchor;
    private final Direction facing;
    private final HQType type;
    private final int height;
    private final int floors;
    private final long totalBlocks;

    private int curF = 0, curY = 0, curR = 0;
    private long placedBlocks = 0;
    private boolean finished = false;

    public CorporateHQBuilderJob(ServerWorld world, UUID ownerId, BlockPos anchor, Direction facing, HQType type, int requestedHeight) {
        this.world = world;
        this.ownerId = ownerId;
        this.anchor = anchor;
        this.facing = facing;
        this.type = type;
        int cap = (type == HQType.HQ_TOWER_TWIN_FIN || type == HQType.HQ_TOWER_ATRIUM) ? 200 : 30;
        this.height = Math.min(cap, Math.max(11, requestedHeight));
        this.floors = Math.max(2, (this.height - 1) / FLOOR_HEIGHT);
        this.totalBlocks = (long) type.width * type.depth * this.height;
    }

    @Override
    public String getName() { return type.label + " (" + height + "b)"; }

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
            BlockState state = computeBlock(curR, curY, curF);
            if (state != null) {
                BlockPos wp = PlacementUtil.toWorld(anchor, facing, curR, curY, curF);
                StreamingBuilderEngine.placeBlockFast(world, wp, state);
            }
            processed++;
            placedBlocks++;
            curR++;
            if (curR >= type.width) {
                curR = 0;
                curY++;
                if (curY >= height) {
                    curY = 0;
                    curF++;
                    if (curF >= type.depth) finished = true;
                }
            }
        }
        return processed;
    }

    private BlockState computeBlock(int x, int y, int z) {
        int W = type.width;
        int D = type.depth;
        int topY = floors * FLOOR_HEIGHT;
        if (y > topY) {
            if (type == HQType.HQ_TOWER_TWIN_FIN && (x == 0 || x == W - 1) && z >= 4 && z <= D - 5) {
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }
        if (y == topY) return Blocks.WHITE_CONCRETE.getDefaultState();

        int floor = y / FLOOR_HEIGHT;
        int ly = y % FLOOR_HEIGHT;
        boolean outer = (x == 0 || x == W - 1 || z == 0 || z == D - 1);

        if (outer) {
            if (floor == 0 && z == 0 && Math.abs(x - W / 2) <= 3 && ly >= 1 && ly <= 3) return Blocks.AIR.getDefaultState();
            if (ly == 0) return Blocks.POLISHED_DEEPSLATE.getDefaultState();
            if (x % 5 == 0 || z % 5 == 0) return Blocks.WHITE_CONCRETE.getDefaultState();
            return Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        }

        boolean inStair = (x >= 3 && x <= 4) && (z >= 4 && z <= 8);
        if (inStair) {
            int step = z - 4;
            for (int f = 0; f < floors - 1; f++) {
                int stairY = f * FLOOR_HEIGHT + step + 1;
                if (y == stairY) {
                    return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                            .with(StairsBlock.FACING, PlacementUtil.rotateDir(facing, Direction.NORTH));
                }
                if (y > stairY && y <= stairY + 4) return Blocks.AIR.getDefaultState();
            }
        }

        if (ly == 0) {
            return (floor == 0) ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.BIRCH_PLANKS.getDefaultState();
        }

        // Hidden ceiling lights on ly == 4
        if (ly == 4 && (x % 6 == 0 && z % 6 == 0)) {
            return Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
        }

        boolean isLowOffice = (type == HQType.LOW_OFFICE_COURTYARD || type == HQType.LOW_OFFICE_PAVILION);
        if (isLowOffice) {
            // Varied Company Suites: Left/Center = Medium suites (10w), Right = Large Anchor Suite (14w)
            int hallwayZ = D / 2;
            if (Math.abs(z - hallwayZ) == 2) {
                if (ly <= 3 && (x == 9 || x == 19 || x == 27)) return Blocks.AIR.getDefaultState();
                return Blocks.WHITE_CONCRETE.getDefaultState();
            }
            if (Math.abs(z - hallwayZ) > 2 && (x == 12 || x == 22)) {
                return (ly == 4) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
        } else {
            // HQ Tower: 1 Company per floor -> Open Employee Workspace + Back Executive Offices
            if (floor > 0 && (z == D - 10 || (z > D - 10 && x % 10 == 0))) {
                if (ly <= 3 && (x % 10 == 5 || z == D - 5)) return Blocks.AIR.getDefaultState();
                return (ly == 4) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
            }
        }

        return Blocks.AIR.getDefaultState();
    }

    public static void placeCompanyReception(ServerWorld world, BlockPos anchor, Direction facing, String companyName) {
        for (int r = 0; r <= 2; r++) {
            world.setBlockState(PlacementUtil.toWorld(anchor, facing, r, 0, 2), Blocks.SMOOTH_QUARTZ.getDefaultState());
        }
        for (int r = -1; r <= 3; r++) {
            for (int y = 0; y <= 2; y++) {
                world.setBlockState(PlacementUtil.toWorld(anchor, facing, r, y, 4), Blocks.WHITE_CONCRETE.getDefaultState());
            }
        }
        BlockPos signPos = PlacementUtil.toWorld(anchor, facing, 1, 1, 3);
        world.setBlockState(signPos, Blocks.BIRCH_WALL_SIGN.getDefaultState()
                .with(net.minecraft.block.WallSignBlock.FACING, facing.getOpposite()));
        if (world.getBlockEntity(signPos) instanceof SignBlockEntity signEntity) {
            SignText text = new SignText()
                    .withMessage(0, Text.literal("WELCOME TO").formatted(Formatting.DARK_GRAY))
                    .withMessage(1, Text.literal(companyName).formatted(Formatting.DARK_BLUE))
                    .withMessage(2, Text.literal("CORPORATE HQ").formatted(Formatting.GRAY));
            signEntity.setText(text, true);
            signEntity.markDirty();
        }
        BlockState chairState = Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, facing);
        world.setBlockState(PlacementUtil.toWorld(anchor, facing, 0, 0, 0), chairState);
        world.setBlockState(PlacementUtil.toWorld(anchor, facing, 2, 0, 0), chairState);
    }
}
