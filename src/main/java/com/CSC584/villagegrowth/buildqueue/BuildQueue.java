package com.CSC584.villagegrowth.buildqueue;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.block.StructureBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class BuildQueue {
    String struct_name;
    boolean finished;
    PriorityQueue<PriorityBlock> pending;
    List<StructureBlockBlockEntity> allBlocks;

    public BuildQueue(String struct_name) {
        this.struct_name = struct_name;
        this.finished = false;
        this.pending = new PriorityQueue<>(10, new PriorityBlockComparator());
        this.allBlocks = extractBlockList(struct_name);
        for (StructureBlockBlockEntity block : this.allBlocks) {
            this.pending.add(new PriorityBlock(block, initialPriority(block)));
        }
    }
    public StructureBlockBlockEntity getBlock() {
        if (this.finished) {
            return null;
        }
        return this.pending.poll().block;
    }

    public void requeueBlock(StructureBlockBlockEntity block, int newPriority) {
        this.pending.add(new PriorityBlock(block, newPriority));
    }

    public int initialPriority(StructureBlockBlockEntity block) {
        return block.getPos().getY();
    }
    public static StructureBlockBlockEntity getBlockEntityFromNBTTag(NbtCompound blockTag) {
        int blockId = blockTag.getInt("state");
        BlockState blockState = Block.getStateFromRawId(blockId);
        if (blockState.getProperties().contains(Properties.HORIZONTAL_FACING)) {
            blockState = HorizontalFacingBlock.getStateFromRawId(blockId);
        }
        NbtList posList = blockTag.getList("pos", NbtElement.INT_TYPE);
        BlockPos pos = new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2));

        return new StructureBlockBlockEntity(pos, blockState);
    }
    private List<StructureBlockBlockEntity> extractBlockList(String struct_name) {
        List<StructureBlockBlockEntity> output = new ArrayList<>();
        try {
            File file = new File(struct_name);
            FileInputStream stream = new FileInputStream(file);

            NbtCompound structureTag = NbtIo.readCompressed(stream);
            NbtList blocksTag = structureTag.getList("blocks", 10);

            for (int i = 0; i < blocksTag.size(); i++) {
                NbtCompound blockTag = blocksTag.getCompound(i);
                output.add(getBlockEntityFromNBTTag(blockTag));
            }

            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public class PriorityBlock {
        StructureBlockBlockEntity block;
        int priority;

        public PriorityBlock(StructureBlockBlockEntity b, int p) {
            this.block = b;
            this.priority = p;
        }

        public void setPriority(int p) {
            this.priority = p;
        }
    }

    public class PriorityBlockComparator implements Comparator<PriorityBlock> {

        @Override
        public int compare(PriorityBlock o1, PriorityBlock o2) {
            return Integer.compare(o1.priority, o2.priority);
        }
    }
}
