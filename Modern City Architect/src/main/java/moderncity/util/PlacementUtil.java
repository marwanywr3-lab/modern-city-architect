package moderncity.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PlacementUtil {

    /**
     * Converts local building coordinates (right, up, forward) into world BlockPos
     * where (0, 0, 0) is the Front-Left bottom corner facing the player.
     */
    public static BlockPos toWorld(BlockPos frontLeftAnchor, Direction facing, int right, int up, int forward) {
        Direction rightDir = facing.rotateYClockwise();
        int dx = rightDir.getOffsetX() * right + facing.getOffsetX() * forward;
        int dz = rightDir.getOffsetZ() * right + facing.getOffsetZ() * forward;
        return frontLeftAnchor.add(dx, up, dz);
    }

    public static Direction rotateDir(Direction baseFacing, Direction localDir) {
        if (localDir == Direction.UP || localDir == Direction.DOWN) return localDir;
        // local NORTH = forward, local SOUTH = back, local EAST = right, local WEST = left
        return switch (localDir) {
            case NORTH -> baseFacing;
            case SOUTH -> baseFacing.getOpposite();
            case EAST -> baseFacing.rotateYClockwise();
            case WEST -> baseFacing.rotateYCounterclockwise();
            default -> baseFacing;
        };
    }
}
