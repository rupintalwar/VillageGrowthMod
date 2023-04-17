package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.helpers.StructureStore;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public class ConstructBuildingTask extends MultiTickTask<VillagerEntity> {

    private static final int BUILD_RANGE = 5;

    private StructureBlockInfo currentTarget;
    private long nextResponseTime;

    public ConstructBuildingTask() {
        super(ImmutableMap.of(
                ModVillagers.STRUCTURE_BUILD_INFO, MemoryModuleState.VALUE_PRESENT
        ));

    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
        VillageGrowthMod.LOGGER.info("Checking Construct");

        if (!serverWorld.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return false;
        }
        if (villagerEntity.getVillagerData().getProfession() != VillagerProfession.MASON) {
            return false;
        }


        Optional<StructureStore> optionalStructureStore = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);

        //return optionalBuildSite.get().getPos().isWithinDistance(villagerEntity.getPos(), BUILD_RANGE);
        return optionalStructureStore.isPresent();
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(optional.isPresent()) {
            StructureStore structureStore = optional.get();
            this.currentTarget = structureStore.queue.getBlock();
        }
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
        return this.shouldRun(world, entity);
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(this.currentTarget != null && optional.isPresent()) {
            StructureStore structureStore = optional.get();
            BlockPos pos = structureStore.placementData.getPosition().add(this.currentTarget.pos);
            VillageGrowthMod.LOGGER.info("Build Site: " + structureStore.placementData);
            VillageGrowthMod.LOGGER.info("Target Pos: " + pos.toString());

            if (!pos.isWithinDistance(villagerEntity.getPos(), 1.0)) {
                return;
            }
            if (l > this.nextResponseTime) {
                BlockState blockState = serverWorld.getBlockState(pos);
                Block block = blockState.getBlock();
                if (block instanceof AirBlock) {
                    serverWorld.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(villagerEntity, blockState));
                } else {
                    structureStore.queue.requeueBlock(this.currentTarget, pos.getY()*2);
                    this.currentTarget = structureStore.queue.getBlock();
                    if (this.currentTarget != null) {
                        this.nextResponseTime = l + 20L;
                        BlockPos pos2 = this.currentTarget.pos;
                        villagerEntity.getBrain().remember(MemoryModuleType.WALK_TARGET,
                                new WalkTarget(new BlockPosLookTarget(pos2), 0.5f, BUILD_RANGE));
                        villagerEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET,
                                new BlockPosLookTarget(pos2));
                    } else {
                        villagerEntity.getBrain().forget(ModVillagers.STRUCTURE_BUILD_INFO);
                    }
                }
            }
        }
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        villagerEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
    }

}
