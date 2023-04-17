package com.CSC584.villagegrowth.helpers;

import com.CSC584.villagegrowth.mixin.StructureTemplateInterfaceMixin;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StructureStore {
    ServerWorld world;
    public StructureTemplate template;
    public List<StructureTemplate.StructureBlockInfo> blockInfoList;
    public BuildQueue queue;

    public StructurePlacementData placementData;

    public StructureStore(ServerWorld world, Identifier struct_id, boolean randomPlacement) {
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();

        Optional<StructureTemplate> template = structureTemplateManager.getTemplate(struct_id);

        if(template.isPresent()) {
            this.placementData = new StructurePlacementData();
            this.template = template.get();
            this.blockInfoList = extractBlockList();
            this.queue = new BuildQueue(this.blockInfoList);
            this.world = world;
            if(randomPlacement) {
                this.randomPlacement();
            }
        }
    }

    public StructureStore(ServerWorld world, Identifier struct_id) {
        this(world, struct_id, false);
    }

    private List<StructureTemplate.StructureBlockInfo> extractBlockList() {
        List<StructureTemplate.StructureBlockInfo> output = new ArrayList<>();
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((StructureTemplateInterfaceMixin) template).getBlockInfoLists();

        for (StructureTemplate.PalettedBlockInfoList list: blockInfoLists) {
            output.addAll(list.getAll());
        }

        return output;
    }

    public void randomPlacement() {
        BlockMirror[] mirrorTypes = BlockMirror.values();
        this.placementData.setMirror(Util.getRandom(mirrorTypes, this.world.getRandom()));
        this.placementData.setRotation(BlockRotation.random(this.world.getRandom()));
    }
}
