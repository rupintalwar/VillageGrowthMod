package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.helpers.BuildQueue;
import com.CSC584.villagegrowth.helpers.StructureStore;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.JigsawBlock;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public class ConstructBuildingTask extends MultiTickTask<VillagerEntity> {

    private static final int BUILD_RANGE = 7;
    private static final int MAX_RUN_TICKS = 10000;
    private long nextResponseTime;

    public ConstructBuildingTask() {
        super(ImmutableMap.of(
                ModVillagers.STRUCTURE_BUILD_INFO, MemoryModuleState.VALUE_PRESENT
        ));

    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
        //VillageGrowthMod.LOGGER.info("Checking Construct");

        if (!serverWorld.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return false;
        }
        if (villagerEntity.getVillagerData().getProfession() != VillagerProfession.MASON) {
            return false;
        }


        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(optional.isPresent()) {
            StructureStore structureStore = optional.get();

            if(structureStore.queue != null && structureStore.queue.getBlock() != null) {
                return true;
            } else {
                //nothing left to build, forget the memory
                villagerEntity.getBrain().forget(ModVillagers.STRUCTURE_BUILD_INFO);
            }
        }
        return false;
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(optional.isPresent()) {
            StructureStore structureStore = optional.get();
            setWalkTarget(villagerEntity, structureStore);
        }
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
        return this.shouldRun(world, entity);
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(optional.isPresent()) {
            StructureStore structureStore = optional.get();
            //Position of target block
            BlockPos pos = StructureTemplate.transform(
                    structureStore.placementData,
                    structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset);
            VillageGrowthMod.LOGGER.info("Target Pos: " + pos);

            //Check whether to attempt placing
            if (l > this.nextResponseTime && pos.isWithinDistance(villagerEntity.getPos(), BUILD_RANGE)) {
                BlockState blockState = serverWorld.getBlockState(pos);
                BuildQueue.PriorityBlock target = structureStore.queue.removeBlock();
                if (    blockState.isReplaceable() &&
                        (hasSolidNeighbor(serverWorld, pos) || target.getQueueCount() > structureStore.template.getSize().getY())) {
                    //place the block
                    serverWorld.setBlockState(pos, target.getBlock().state);
                    serverWorld.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(villagerEntity, target.getBlock().state));
                    VillageGrowthMod.LOGGER.debug("Target Pos: " + pos);
                    VillageGrowthMod.LOGGER.debug("Placed " + target.getBlock().state.getBlock().getName());
                } else if (!blockState.isReplaceable() && target.getQueueCount() > structureStore.template.getSize().getY()) {
                    //attempted plenty of times, ignore the block
                    VillageGrowthMod.LOGGER.debug("Target Pos: " + pos);
                    VillageGrowthMod.LOGGER.debug("Ignored " + target.getBlock().state.getBlock().getName());
                } else {
                    //try again later
                    structureStore.queue.requeueBlock(target);
                }

                if (structureStore.queue.getBlock() != null) {
                    this.nextResponseTime = l + 2L;
                    setWalkTarget(villagerEntity, structureStore);
                }
            }
        }
    }
    private void setWalkTarget(VillagerEntity villagerEntity, StructureStore structureStore) {
        BlockPos pos = StructureTemplate.transform(
                structureStore.placementData,
                structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset);
        VillageGrowthMod.LOGGER.info("Walk target: " + pos.toString());
        villagerEntity.getBrain().remember(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosLookTarget(pos), 0.5f, 3));

        villagerEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET,
                new BlockPosLookTarget(pos));
    }

    private boolean hasSolidNeighbor(ServerWorld world, BlockPos pos) {
        return  world.getBlockState(pos.up()).getMaterial().isSolid() ||
                world.getBlockState(pos.down()).getMaterial().isSolid() ||
                world.getBlockState(pos.north()).getMaterial().isSolid() ||
                world.getBlockState(pos.south()).getMaterial().isSolid() ||
                world.getBlockState(pos.east()).getMaterial().isSolid() ||
                world.getBlockState(pos.west()).getMaterial().isSolid();
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        villagerEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
    }

}
