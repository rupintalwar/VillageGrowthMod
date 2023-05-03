package villagegrowth.villager;

import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class VillagerNodeMaker extends LandPathNodeMaker {

    public VillagerNodeMaker() {
        super();
        this.setCanEnterOpenDoors(true);
        this.setCanOpenDoors(true);
    }

    public int getSuccessors(PathNode[] successors, PathNode node) {
        int i = 0;
        int j = 0;
        PathNodeType pathNodeType = this.getNodeType(this.entity, node.x, node.y + 1, node.z);
        PathNodeType pathNodeType2 = this.getNodeType(this.entity, node.x, node.y, node.z);
        if (this.entity.getPathfindingPenalty(pathNodeType) >= 0.0F && pathNodeType2 != PathNodeType.STICKY_HONEY) {
            j = MathHelper.floor(Math.max(1.0F, this.entity.getStepHeight()));
        }

        double d = this.getFeetY(new BlockPos(node.x, node.y, node.z));
        PathNode pathNode = this.getPathNode(node.x, node.y, node.z + 1, j, d, Direction.SOUTH, pathNodeType2);
        if (this.isValidAdjacentSuccessor(pathNode, node)) {
            successors[i++] = pathNode;
        }

        PathNode pathNode2 = this.getPathNode(node.x - 1, node.y, node.z, j, d, Direction.WEST, pathNodeType2);
        if (this.isValidAdjacentSuccessor(pathNode2, node)) {
            successors[i++] = pathNode2;
        }

        PathNode pathNode3 = this.getPathNode(node.x + 1, node.y, node.z, j, d, Direction.EAST, pathNodeType2);
        if (this.isValidAdjacentSuccessor(pathNode3, node)) {
            successors[i++] = pathNode3;
        }

        PathNode pathNode4 = this.getPathNode(node.x, node.y, node.z - 1, j, d, Direction.NORTH, pathNodeType2);
        if (this.isValidAdjacentSuccessor(pathNode4, node)) {
            successors[i++] = pathNode4;
        }

        PathNode pathNode5 = this.getPathNode(node.x - 1, node.y, node.z - 1, j, d, Direction.NORTH, pathNodeType2);
        if (this.isValidDiagonalSuccessor(node, pathNode2, pathNode4, pathNode5)) {
            successors[i++] = pathNode5;
        }

        PathNode pathNode6 = this.getPathNode(node.x + 1, node.y, node.z - 1, j, d, Direction.NORTH, pathNodeType2);
        if (this.isValidDiagonalSuccessor(node, pathNode3, pathNode4, pathNode6)) {
            successors[i++] = pathNode6;
        }

        PathNode pathNode7 = this.getPathNode(node.x - 1, node.y, node.z + 1, j, d, Direction.SOUTH, pathNodeType2);
        if (this.isValidDiagonalSuccessor(node, pathNode2, pathNode, pathNode7)) {
            successors[i++] = pathNode7;
        }

        PathNode pathNode8 = this.getPathNode(node.x + 1, node.y, node.z + 1, j, d, Direction.SOUTH, pathNodeType2);
        if (this.isValidDiagonalSuccessor(node, pathNode3, pathNode, pathNode8)) {
            successors[i++] = pathNode8;
        }

        PathNode pathNode9 = this.getPathNode(node.x, node.y+1, node.z, 1, this.getFeetY(new BlockPos(node.x, node.y+1, node.z)), Direction.UP, pathNodeType2);
        if (this.isValidAdjacentSuccessor(pathNode9, node) && pathNodeType2.equals(PathNodeType.TRAPDOOR)) {
            successors[i++] = pathNode9;
        }

        return i;
    }
}
