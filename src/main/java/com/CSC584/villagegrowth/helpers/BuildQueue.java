package com.CSC584.villagegrowth.helpers;

import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockBox;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class BuildQueue {
    boolean finished;
    PriorityQueue<PriorityBlock> pending;
    List<StructureBlockInfo> allBlocks;
    BlockBox bounds;

    public BuildQueue(List<StructureBlockInfo> allBlocks, BlockBox bounds) {
        this.finished = false;
        this.pending = new PriorityQueue<>(10, new PriorityBlockComparator());
        this.allBlocks = allBlocks;
        this.bounds = bounds;
        for (StructureBlockInfo block : this.allBlocks) {
            queueNewBlock(block);
        }
    }
    public PriorityBlock getBlock() {
        if (this.pending.isEmpty()) {
            this.finished = true;
        }
        return this.pending.peek();
    }

    public PriorityBlock removeBlock() {
        if (this.pending.isEmpty()) {
            this.finished = true;
        }
        return this.pending.poll();
    }

    public void requeueBlock(PriorityBlock priorityBlock) {
        priorityBlock.priority += 2 * (bounds.getBlockCountX() * bounds.getBlockCountZ());
        priorityBlock.queueCount++;
        this.pending.add(priorityBlock);
    }

    public void queueNewBlock(StructureBlockInfo block) {
        this.pending.add(new PriorityBlock(block, initialPriority(block)));
    }

    public int initialPriority(StructureBlockInfo block) {
        return  block.pos.getY() * ((bounds.getBlockCountX() -1) * bounds.getBlockCountZ()) +
                block.pos.getX() * bounds.getBlockCountZ() +
                block.pos.getZ();
    }

    public static class PriorityBlock {
        StructureBlockInfo block;
        int priority;
        int queueCount;

        public StructureBlockInfo getBlock() {
            return block;
        }

        public int getPriority() {
            return priority;
        }

        public int getQueueCount() {
            return queueCount;
        }

        public PriorityBlock(StructureBlockInfo b, int p) {
            this.block = b;
            this.priority = p;
            this.queueCount = 1;
        }
    }

    public static class PriorityBlockComparator implements Comparator<PriorityBlock> {

        @Override
        public int compare(PriorityBlock o1, PriorityBlock o2) {
            return Integer.compare(o1.getPriority(), o2.getPriority());
        }
    }

}
