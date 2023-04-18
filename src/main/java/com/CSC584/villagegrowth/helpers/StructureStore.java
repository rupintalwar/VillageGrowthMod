package com.CSC584.villagegrowth.helpers;

import com.CSC584.villagegrowth.mixin.StructureTemplateInterfaceMixin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class StructureStore {
    private static final Map<String, Block> foundationBlockMap = Map.of(
            "plains", Blocks.DIRT,
            "desert", Blocks.SAND,
            "taiga", Blocks.DIRT,
            "savanna", Blocks.DIRT,
            "snowy", Blocks.DIRT);
    private String structType;
    private ServerWorld world;
    public StructureTemplate template;
    public List<StructureTemplate.StructureBlockInfo> blockInfoList;
    public BuildQueue queue;

    public StructurePlacementData placementData;
    public BlockPos offset;

    public StructureStore(ServerWorld world, Identifier struct_id, String structType, boolean randomPlacement) {
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();

        Optional<StructureTemplate> template = structureTemplateManager.getTemplate(struct_id);

        if(template.isPresent()) {
            this.placementData = new StructurePlacementData();
            this.offset = new BlockPos(0, 0, 0);
            this.template = template.get();
            this.blockInfoList = extractBlockList();
            this.queue = new BuildQueue(this.blockInfoList);
            this.world = world;
            this.structType = structType;
            if(randomPlacement) {
                this.randomPlacement();
            }
        }
    }

    public StructureStore(ServerWorld world, Identifier struct_id, String structType) {
        this(world, struct_id, structType, false);
    }

    private List<StructureTemplate.StructureBlockInfo> extractBlockList() {
        List<StructureTemplate.StructureBlockInfo> output = new ArrayList<>();
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((StructureTemplateInterfaceMixin) template).getBlockInfoLists();

        for (StructureTemplate.PalettedBlockInfoList list: blockInfoLists) {
            list.getAll().stream().filter(block -> !block.state.isAir()).forEach(output::add);
        }

        return output;
    }

    public void randomPlacement() {
        BlockMirror[] mirrorTypes = BlockMirror.values();
        this.placementData.setMirror(Util.getRandom(mirrorTypes, this.world.getRandom()));
        this.placementData.setRotation(BlockRotation.random(this.world.getRandom()));
    }

    public void createFoundation() {
        List<StructureTemplate.StructureBlockInfo> foundationBlocks = new ArrayList<>();
        BlockState adaptedState = foundationBlockMap.get(this.structType).getDefaultState();

        for(StructureTemplate.StructureBlockInfo block : blockInfoList) {
            if(block.pos.getY() == 0) {
                BlockPos underBlock = block.pos.down();
                while(this.world.getBlockState(underBlock.add(this.offset)).getMaterial().isReplaceable()) {
                    foundationBlocks.add(new StructureTemplate.StructureBlockInfo(underBlock, adaptedState, null) );
                    underBlock = underBlock.down();
                }
            }
        }
        this.blockInfoList.addAll(foundationBlocks);
        this.queue = new BuildQueue(this.blockInfoList);
    }
}
