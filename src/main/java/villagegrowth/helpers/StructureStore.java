package villagegrowth.helpers;

import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3i;
import villagegrowth.mixin.StructureTemplateInterfaceMixin;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import villagegrowth.task.FindBuildSiteTask;
import villagegrowth.villager.ModVillagers;

import java.util.*;


public class StructureStore {
    private static final Map<String, Block> foundationBlockMap = Map.of(
            "plains", Blocks.DIRT,
            "desert", Blocks.SAND,
            "taiga", Blocks.DIRT,
            "savanna", Blocks.DIRT,
            "snowy", Blocks.DIRT);
    private static final double MAX_LAYERS = 1.5;
    private String structType;
    private ServerWorld world;
    public StructureTemplate template;
    public List<StructureTemplate.StructureBlockInfo> blockInfoList;
    public BuildQueue queue;

    public StructurePlacementData placementData;
    public BlockPos offset;
    public int adjustY = 0;
    public BlockBox actualBoundingBox;

    public Stack<BlockPos> scaffoldStack;

    public ArmorStandEntity marker;

    public List<ArmorStandEntity> pathmarker = new ArrayList<>();

    private int attempts;

    public StructureStore(ServerWorld world, Identifier struct_id, String structType, boolean randomPlacement) {
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();

        Optional<StructureTemplate> template = structureTemplateManager.getTemplate(struct_id);

        if(template.isPresent()) {
            this.scaffoldStack = new Stack<>();
            this.placementData = new StructurePlacementData();
            this.offset = new BlockPos(0, 0, 0);
            this.template = template.get();
            this.world = world;

            if(ModVillagers.MARK_NEXT_BLOCK) {
                marker = EntityType.ARMOR_STAND.create(world);
                marker.setNoGravity(true);
                marker.setGlowing(true);
                world.spawnEntity(marker);

                for(int i = 0; i < 50; i++) {
                    ArmorStandEntity pm = EntityType.ARMOR_STAND.create(world);
                    pm.setNoGravity(true);
                    pm.setGlowing(true);
                    pm.setCustomName(Text.of("Path: " + i));
                    pm.setCustomNameVisible(true);
                    world.spawnEntity(pm);
                    pathmarker.add(pm);
                }
            }

            if(randomPlacement) {
                this.randomPlacement();
            }
            this.structType = structType;
            this.blockInfoList = extractBlockList();
            this.actualBoundingBox = BlockBox.create(new Vec3i(0, 0, 0),
                    template.get().calculateBoundingBox(this.placementData, this.offset).getDimensions().add(0, -adjustY, 0));

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
                    .peek(block -> block.pos.add(0,-this.adjustY,0))
                    .forEach(output::add);
        }


        return output;
    }

    public StructureTemplate.StructureBlockInfo preprocessBlock(StructureTemplate.StructureBlockInfo block) {
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
        if(block.pos.getY() < adjustY) {
            adjustY = block.pos.getY();
        }

        output = output.mirror(this.placementData.getMirror());
        output = output.rotate(this.placementData.getRotation());

        return new StructureTemplate.StructureBlockInfo(block.pos, output, block.nbt);
    }

    public void randomPlacement() {
        BlockMirror[] mirrorTypes = BlockMirror.values();
        this.placementData.setMirror(Util.getRandom(mirrorTypes, this.world.getRandom()));
        this.placementData.setRotation(BlockRotation.random(this.world.getRandom()));
    }

    public boolean createFoundation() {
        List<StructureTemplate.StructureBlockInfo> foundationBlocks = new ArrayList<>();
        BlockState adaptedState = foundationBlockMap.get(this.structType).getDefaultState();

        double foundationLimit = this.template.getSize().getX() * this.template.getSize().getZ() * MAX_LAYERS;
        int dY = 0;
        for(StructureTemplate.StructureBlockInfo block : blockInfoList) {
            if(block.pos.getY() == 0) {
                BlockPos underBlock = block.pos.down();
                while(this.world.getBlockState(
                        StructureTemplate.transform(this.placementData, underBlock).add(this.offset))
                        .getMaterial().isReplaceable() && underBlock.add(this.offset).getY() >= world.getBottomY()) {
                    foundationBlocks.add(new StructureTemplate.StructureBlockInfo(underBlock, adaptedState, null) );

                    dY = Math.min(dY, underBlock.getY());
                    underBlock = underBlock.down();

                    if(foundationBlocks.size() > foundationLimit) {
                        //Placing too many blocks as a foundation, probably a bad spot to build
                        return false;
                    }
                }
                BlockState underblockState = this.world.getBlockState(StructureTemplate.transform(this.placementData, underBlock).add(this.offset));
                if(!FindBuildSiteTask.VALID_BLOCKS.contains(underblockState.getBlock())) {
                    //Only build on valid spots
                    return false;
                }
            }
        }
        
        
        
        this.blockInfoList.addAll(foundationBlocks);
        //update bounding box to include y values
        BlockBox b = this.placementData.getBoundingBox();
        this.placementData.setBoundingBox(new BlockBox(
                b.getMinX(), b.getMinY() + dY, b.getMinZ(),
                b.getMaxX(), b.getMaxY(), b.getMaxZ()));

        this.queue = new BuildQueue(this.blockInfoList, this.placementData.getBoundingBox());

        return true;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
    public void incrementAttempt() {
        this.attempts++;
    }

    public void setOffset(BlockPos blockPos) {
        this.offset = blockPos;
        this.placementData.setBoundingBox(this.actualBoundingBox.offset(this.offset.getX(), this.offset.getY(), this.offset.getZ()));
    }
}
