package com.CSC584.villagegrowth.buildqueue;

import com.CSC584.villagegrowth.mixin.StructureTemplateInterfaceMixin;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;

import java.util.*;

public class BuildQueue {
    ServerWorld world;
    Identifier struct_id;
    boolean finished;
    PriorityQueue<PriorityBlock> pending;
    List<StructureBlockInfo> allBlocks;

    public BuildQueue(Identifier struct_id, ServerWorld world) {
        this.world = world;
        this.struct_id = struct_id;
        this.finished = false;
        this.pending = new PriorityQueue<>(10, new PriorityBlockComparator());
        this.allBlocks = extractBlockList(struct_id);
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

    /*
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

     */
    private List<StructureBlockInfo> extractBlockList(Identifier struct_id) {
        List<StructureBlockInfo> output = new ArrayList<>();
        StructureTemplateManager structureTemplateManager = this.world.getStructureTemplateManager();

        Optional<StructureTemplate> template = structureTemplateManager.getTemplate(struct_id);

        if(template.isPresent()){
            List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((StructureTemplateInterfaceMixin) template.get()).getBlockInfoLists();

            for (StructureTemplate.PalettedBlockInfoList list: blockInfoLists) {
                output.addAll(list.getAll());
            }
        }
        return output;
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
