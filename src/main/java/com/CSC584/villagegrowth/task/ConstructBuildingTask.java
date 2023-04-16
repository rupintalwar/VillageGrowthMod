package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.buildqueue.BuildQueue;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public class ConstructBuildingTask extends MultiTickTask<VillagerEntity> {

    private static final int BUILD_RANGE = 20;

    private StructureBlockBlockEntity currentTarget;
    private BuildQueue queue;
    private long nextResponseTime;
    private int ticksRan;

    public ConstructBuildingTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryModuleState.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT,
                ModVillagers.BUILD_SITE, MemoryModuleState.VALUE_PRESENT,
                ModVillagers.BUILD_QUEUE, MemoryModuleState.VALUE_PRESENT
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


        Optional<GlobalPos> optionalBuildSite = villagerEntity.getBrain().getOptionalMemory(ModVillagers.BUILD_SITE);
        Optional<BuildQueue> optionalBuildQueue = villagerEntity.getBrain().getOptionalMemory(ModVillagers.BUILD_QUEUE);

        if(optionalBuildSite.isPresent() && optionalBuildQueue.isPresent()) {
            this.queue = optionalBuildQueue.get();
            //return optionalBuildSite.get().getPos().isWithinDistance(villagerEntity.getPos(), BUILD_RANGE);
            return true;
        }
        return false;
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        this.currentTarget = this.queue.getBlock();
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        if(this.currentTarget != null) {

            BlockPos pos = this.currentTarget.getPos();
            VillageGrowthMod.LOGGER.info("Build Site: " + villagerEntity.getBrain().getOptionalMemory(ModVillagers.BUILD_SITE).get());
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
                    this.queue.requeueBlock(this.currentTarget, pos.getY()*2);
                    this.currentTarget = this.queue.getBlock();
                    if (this.currentTarget != null) {
                        this.nextResponseTime = l + 20L;
                        BlockPos pos2 = this.currentTarget.getPos();
                        villagerEntity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosLookTarget(pos2), 0.5f, 1));
                        villagerEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(pos2));
                    } else {
                        villagerEntity.getBrain().forget(ModVillagers.BUILD_SITE);
                        villagerEntity.getBrain().forget(ModVillagers.BUILD_QUEUE);
                    }
                }
            }
        }
        this.ticksRan++;
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        villagerEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
    }

}
