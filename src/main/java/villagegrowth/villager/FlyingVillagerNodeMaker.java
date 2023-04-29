package villagegrowth.villager;

import net.minecraft.entity.ai.pathing.BirdPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;
import org.jetbrains.annotations.Nullable;

public class FlyingVillagerNodeMaker extends BirdPathNodeMaker {

    FlyingVillagerNodeMaker() {
        super();
        this.setCanEnterOpenDoors(true);
        this.setCanOpenDoors(true);
    }

    @Override
    public int getSuccessors(PathNode[] successors, PathNode node) {
        int i = 0;
        PathNode pathNode = this.getPassableNode(node.x, node.y, node.z + 1);
        if (this.unvisited(pathNode)) {
            successors[i++] = pathNode;
        }

        PathNode pathNode2 = this.getPassableNode(node.x - 1, node.y, node.z);
        if (this.unvisited(pathNode2)) {
            successors[i++] = pathNode2;
        }

        PathNode pathNode3 = this.getPassableNode(node.x + 1, node.y, node.z);
        if (this.unvisited(pathNode3)) {
            successors[i++] = pathNode3;
        }

        PathNode pathNode4 = this.getPassableNode(node.x, node.y, node.z - 1);
        if (this.unvisited(pathNode4)) {
            successors[i++] = pathNode4;
        }

        PathNode pathNode5 = this.getPassableNode(node.x, node.y + 1, node.z);
//        if (this.unvisited(pathNode5)) {
//            successors[i++] = pathNode5;
//        }

        PathNode pathNode6 = this.getPassableNode(node.x, node.y - 1, node.z);
        if (this.unvisited(pathNode6)) {
            successors[i++] = pathNode6;
        }

        PathNode pathNode7 = this.getPassableNode(node.x, node.y + 1, node.z + 1);
//        if (this.unvisited(pathNode7) && this.isPassable(pathNode) && this.isPassable(pathNode5)) {
//            successors[i++] = pathNode7;
//        }

        PathNode pathNode8 = this.getPassableNode(node.x - 1, node.y + 1, node.z);
//        if (this.unvisited(pathNode8) && this.isPassable(pathNode2) && this.isPassable(pathNode5)) {
//            successors[i++] = pathNode8;
//        }

        PathNode pathNode9 = this.getPassableNode(node.x + 1, node.y + 1, node.z);
//        if (this.unvisited(pathNode9) && this.isPassable(pathNode3) && this.isPassable(pathNode5)) {
//            successors[i++] = pathNode9;
//        }

        PathNode pathNode10 = this.getPassableNode(node.x, node.y + 1, node.z - 1);
//        if (this.unvisited(pathNode10) && this.isPassable(pathNode4) && this.isPassable(pathNode5)) {
//            successors[i++] = pathNode10;
//        }

        PathNode pathNode11 = this.getPassableNode(node.x, node.y - 1, node.z + 1);
        if (this.unvisited(pathNode11) && this.isPassable(pathNode) && this.isPassable(pathNode6)) {
            successors[i++] = pathNode11;
        }

        PathNode pathNode12 = this.getPassableNode(node.x - 1, node.y - 1, node.z);
        if (this.unvisited(pathNode12) && this.isPassable(pathNode2) && this.isPassable(pathNode6)) {
            successors[i++] = pathNode12;
        }

        PathNode pathNode13 = this.getPassableNode(node.x + 1, node.y - 1, node.z);
        if (this.unvisited(pathNode13) && this.isPassable(pathNode3) && this.isPassable(pathNode6)) {
            successors[i++] = pathNode13;
        }

        PathNode pathNode14 = this.getPassableNode(node.x, node.y - 1, node.z - 1);
        if (this.unvisited(pathNode14) && this.isPassable(pathNode4) && this.isPassable(pathNode6)) {
            successors[i++] = pathNode14;
        }

        PathNode pathNode15 = this.getPassableNode(node.x + 1, node.y, node.z - 1);
        if (this.unvisited(pathNode15) && this.isPassable(pathNode4) && this.isPassable(pathNode3)) {
            successors[i++] = pathNode15;
        }

        PathNode pathNode16 = this.getPassableNode(node.x + 1, node.y, node.z + 1);
        if (this.unvisited(pathNode16) && this.isPassable(pathNode) && this.isPassable(pathNode3)) {
            successors[i++] = pathNode16;
        }

        PathNode pathNode17 = this.getPassableNode(node.x - 1, node.y, node.z - 1);
        if (this.unvisited(pathNode17) && this.isPassable(pathNode4) && this.isPassable(pathNode2)) {
            successors[i++] = pathNode17;
        }

        PathNode pathNode18 = this.getPassableNode(node.x - 1, node.y, node.z + 1);
        if (this.unvisited(pathNode18) && this.isPassable(pathNode) && this.isPassable(pathNode2)) {
            successors[i++] = pathNode18;
        }

        PathNode pathNode19 = this.getPassableNode(node.x + 1, node.y + 1, node.z - 1);
//        if (this.unvisited(pathNode19) && this.isPassable(pathNode15) && this.isPassable(pathNode4) && this.isPassable(pathNode3) && this.isPassable(pathNode5) && this.isPassable(pathNode10) && this.isPassable(pathNode9)) {
//            successors[i++] = pathNode19;
//        }

        PathNode pathNode20 = this.getPassableNode(node.x + 1, node.y + 1, node.z + 1);
//        if (this.unvisited(pathNode20) && this.isPassable(pathNode16) && this.isPassable(pathNode) && this.isPassable(pathNode3) && this.isPassable(pathNode5) && this.isPassable(pathNode7) && this.isPassable(pathNode9)) {
//            successors[i++] = pathNode20;
//        }

        PathNode pathNode21 = this.getPassableNode(node.x - 1, node.y + 1, node.z - 1);
//        if (this.unvisited(pathNode21) && this.isPassable(pathNode17) && this.isPassable(pathNode4) && this.isPassable(pathNode2) && this.isPassable(pathNode5) && this.isPassable(pathNode10) && this.isPassable(pathNode8)) {
//            successors[i++] = pathNode21;
//        }

        PathNode pathNode22 = this.getPassableNode(node.x - 1, node.y + 1, node.z + 1);
//        if (this.unvisited(pathNode22) && this.isPassable(pathNode18) && this.isPassable(pathNode) && this.isPassable(pathNode2) && this.isPassable(pathNode5) && this.isPassable(pathNode7) && this.isPassable(pathNode8)) {
//            successors[i++] = pathNode22;
//        }

        PathNode pathNode23 = this.getPassableNode(node.x + 1, node.y - 1, node.z - 1);
        if (this.unvisited(pathNode23) && this.isPassable(pathNode15) && this.isPassable(pathNode4) && this.isPassable(pathNode3) && this.isPassable(pathNode6) && this.isPassable(pathNode14) && this.isPassable(pathNode13)) {
            successors[i++] = pathNode23;
        }

        PathNode pathNode24 = this.getPassableNode(node.x + 1, node.y - 1, node.z + 1);
        if (this.unvisited(pathNode24) && this.isPassable(pathNode16) && this.isPassable(pathNode) && this.isPassable(pathNode3) && this.isPassable(pathNode6) && this.isPassable(pathNode11) && this.isPassable(pathNode13)) {
            successors[i++] = pathNode24;
        }

        PathNode pathNode25 = this.getPassableNode(node.x - 1, node.y - 1, node.z - 1);
        if (this.unvisited(pathNode25) && this.isPassable(pathNode17) && this.isPassable(pathNode4) && this.isPassable(pathNode2) && this.isPassable(pathNode6) && this.isPassable(pathNode14) && this.isPassable(pathNode12)) {
            successors[i++] = pathNode25;
        }

        PathNode pathNode26 = this.getPassableNode(node.x - 1, node.y - 1, node.z + 1);
        if (this.unvisited(pathNode26) && this.isPassable(pathNode18) && this.isPassable(pathNode) && this.isPassable(pathNode2) && this.isPassable(pathNode6) && this.isPassable(pathNode11) && this.isPassable(pathNode12)) {
            successors[i++] = pathNode26;
        }

        return i;
    }

    private boolean isPassable(@Nullable PathNode node) {
        return node != null && node.penalty >= 0.0F;
    }
    private boolean unvisited(@Nullable PathNode node) {
        return node != null && !node.visited;
    }
}
