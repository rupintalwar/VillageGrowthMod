package com.CSC584.villagegrowth.helpers;

import com.CSC584.villagegrowth.mixin.StructureTemplateInterfaceMixin;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.*;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
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
            this.world = world;
            this.blockInfoList = extractBlockList();
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
            list.getAll().stream()
                    .map(this::preprocessBlock)
                    .filter(block -> !block.state.isAir())
                    .forEach(output::add);
        }

        return output;
    }

    public StructureTemplate.StructureBlockInfo preprocessBlock(StructureTemplate.StructureBlockInfo block) {
        StructureTemplate.StructureBlockInfo returnedBlock = block;
        BlockState output = block.state;
        if (block.state.isOf(Blocks.JIGSAW)) {
            String string = block.nbt.getString("final_state");
            try {
                BlockArgumentParser.BlockResult blockResult = BlockArgumentParser.block(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), string, true);
                output = blockResult.blockState();
            } catch (CommandSyntaxException commandSyntaxException) {
                throw new RuntimeException(commandSyntaxException);
            }
            if (output.isOf(Blocks.STRUCTURE_VOID)) {
                // Return air to be filtered afterward
                output = Blocks.AIR.getDefaultState();
            }
        }
        output = output.rotate(this.placementData.getRotation());
        output = output.mirror(this.placementData.getMirror());
        returnedBlock = new StructureTemplate.StructureBlockInfo(block.pos,
                output, block.nbt);
        return returnedBlock;
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
                while(this.world.getBlockState(
                        StructureTemplate.transform(this.placementData, underBlock).add(this.offset))
                        .getMaterial().isReplaceable() && underBlock.getY() >= world.getBottomY()) {
                    foundationBlocks.add(new StructureTemplate.StructureBlockInfo(underBlock, adaptedState, null) );
                    underBlock = underBlock.down();
                }
            }
        }
        this.blockInfoList.addAll(foundationBlocks);
        this.queue = new BuildQueue(this.blockInfoList, this.placementData.getBoundingBox());
    }
}
