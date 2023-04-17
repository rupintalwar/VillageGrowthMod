package com.CSC584.villagegrowth.helpers;

import net.minecraft.structure.StructureTemplate.StructureBlockInfo;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class BuildQueue {
    boolean finished;
    PriorityQueue<PriorityBlock> pending;
    List<StructureBlockInfo> allBlocks;

    public BuildQueue(List<StructureBlockInfo> allBlocks) {
        this.finished = false;
        this.pending = new PriorityQueue<>(10, new PriorityBlockComparator());
        this.allBlocks = allBlocks;
        for (StructureBlockInfo block : this.allBlocks) {
            this.pending.add(new PriorityBlock(block, initialPriority(block)));
        }
    }
    public StructureBlockInfo getBlock() {
        if (this.pending.isEmpty()) {
            this.finished = true;
            return null;
        }
        return this.pending.poll().block;
    }

    public void requeueBlock(StructureBlockInfo block, int newPriority) {
        this.pending.add(new PriorityBlock(block, newPriority));
    }

    public int initialPriority(StructureBlockInfo block) {

        return block.pos.getY();
    }

    public static class PriorityBlock {
        StructureBlockInfo block;
        int priority;

        public PriorityBlock(StructureBlockInfo b, int p) {
            this.block = b;
            this.priority = p;
        }
    }

    public static class PriorityBlockComparator implements Comparator<PriorityBlock> {

        @Override
        public int compare(PriorityBlock o1, PriorityBlock o2) {
            return Integer.compare(o1.priority, o2.priority);
        }
    }
}
