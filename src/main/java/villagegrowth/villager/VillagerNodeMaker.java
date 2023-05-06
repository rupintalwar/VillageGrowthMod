package villagegrowth.villager;

import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import villagegrowth.blocks.ModBlocks;

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

        BlockPos pos = node.getBlockPos();

        double d = this.getFeetY(pos);

        BlockPos pos1 = pos.add(0,0,1);
        PathNode pathNode1 = this.getPathNode(node.x, node.y, node.z + 1, j, d, Direction.SOUTH, pathNodeType2);
        i = checkAddSuccessor(node, successors, i, pos1, pathNode1);

        BlockPos pos2 = pos.add(-1, 0, 0);
        PathNode pathNode2 = this.getPathNode(node.x - 1, node.y, node.z, j, d, Direction.WEST, pathNodeType2);
        i = checkAddSuccessor(node, successors, i, pos2, pathNode2);

        BlockPos pos3 = pos.add(1,0,0);
        PathNode pathNode3 = this.getPathNode(node.x + 1, node.y, node.z, j, d, Direction.EAST, pathNodeType2);
        i = checkAddSuccessor(node, successors, i, pos3, pathNode3);


        BlockPos pos4 = pos.add(0, 0, -1);
        PathNode pathNode4 = this.getPathNode(node.x, node.y, node.z - 1, j, d, Direction.NORTH, pathNodeType2);
        i = checkAddSuccessor(node, successors, i, pos4, pathNode4);


        BlockPos pos5 = pos.add(-1,0,-1);
        PathNode pathNode5 = this.getPathNode(node.x - 1, node.y, node.z - 1, j, d, Direction.NORTH, pathNodeType2);
        i = checkAddSuccessorDiag(node, successors, i, pos5, pathNode5, pathNode2, pathNode4);

        BlockPos pos6 = pos.add(1,0,-1);
        PathNode pathNode6 = this.getPathNode(node.x + 1, node.y, node.z - 1, j, d, Direction.NORTH, pathNodeType2);
        i = checkAddSuccessorDiag(node, successors, i, pos6, pathNode6, pathNode3, pathNode4);

        BlockPos pos7 = pos.add(-1,0,1);
        PathNode pathNode7 = this.getPathNode(node.x - 1, node.y, node.z + 1, j, d, Direction.SOUTH, pathNodeType2);
        i = checkAddSuccessorDiag(node, successors, i, pos7, pathNode7, pathNode2, pathNode1);


        BlockPos pos8 = pos.add(1,0,1);
        PathNode pathNode8 = this.getPathNode(node.x + 1, node.y, node.z + 1, j, d, Direction.SOUTH, pathNodeType2);
        i = checkAddSuccessorDiag(node, successors, i, pos8, pathNode8, pathNode3, pathNode1);


        BlockPos pos9 = pos.add(0,1,0);
        PathNode pathNode9 = this.getPathNode(node.x, node.y+1, node.z, 1, this.getFeetY(new BlockPos(node.x, node.y+1, node.z)), Direction.UP, pathNodeType2);
        i = checkAddSuccessor(node, successors, i, pos9, pathNode9, true);


        return i;
    }

    private int checkAddSuccessor(PathNode node, PathNode[] successors, int i, BlockPos pos, PathNode pathNode) {
        return checkAddSuccessor(node, successors, i, pos, pathNode, false);
    }

    public int checkAddSuccessor(PathNode node, PathNode[] successors, int i, BlockPos pos, PathNode pathNode, boolean ensureScaffold) {
        boolean isScaffold = isScaffold(pos);

        //Replace type for walking through
        if(pathNode == null && isScaffold) {
            pathNode = this.getNode(pos);
            pathNode.type = PathNodeType.WALKABLE;
            pathNode.penalty -= PathNodeType.BLOCKED.getDefaultPenalty();
        }

        if (this.isValidAdjacentSuccessor(pathNode, node) && (!ensureScaffold || isScaffold)) {
            successors[i++] = pathNode;
        }

        //revert back for solid block
        if(isScaffold) {
            pathNode.type = PathNodeType.BLOCKED;
            pathNode.penalty += PathNodeType.BLOCKED.getDefaultPenalty();
        }
        return i;
    }

    public int checkAddSuccessorDiag(PathNode node, PathNode[] successors, int i, BlockPos pos, PathNode pathNode, PathNode pathNode2, PathNode pathNode3) {
        boolean isScaffold = isScaffold(pos);

        //Replace type for walking through
        if(pathNode == null && isScaffold) {
            pathNode = this.getNode(pos);
            pathNode.type = PathNodeType.WALKABLE;
        }

        if (this.isValidDiagonalSuccessor(node, pathNode2, pathNode3, pathNode)) {
            successors[i++] = pathNode;
        }

        //revert back for solid block
        if(isScaffold) {
            pathNode.type = PathNodeType.BLOCKED;
        }
        return i;
    }

    public boolean isScaffold(BlockPos pos) {
        return ModBlocks.scaffoldSet.contains(this.cachedWorld.getBlockState(pos).getBlock());
    }
}
