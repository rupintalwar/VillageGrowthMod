package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.blocks.ModBlocks;
import com.CSC584.villagegrowth.helpers.BuildQueue;
import com.CSC584.villagegrowth.helpers.StructureStore;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.BeeEntity;
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
    private static final int MAX_ATTEMPTS = 100;
    private static final long DELAY = 5;
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

            if((structureStore.queue != null && structureStore.queue.getBlock() != null) ||
                    !structureStore.scaffoldStack.isEmpty()) {
                return true;
            } else {
                //nothing left to build or clean up, forget the memory
                villagerEntity.getBrain().forget(ModVillagers.STRUCTURE_BUILD_INFO);

            }
        }
        return false;
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(optional.isPresent() && optional.get().queue != null && optional.get().queue.getBlock() != null) {
            StructureStore structureStore = optional.get();
            setWalkTarget(villagerEntity, StructureTemplate.transform(
                    structureStore.placementData,
                    structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset));
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

            if (l > this.nextResponseTime) {

                boolean cleanupMode = false;

                //Timeout the build after too long. Villager may get stuck otherwise. Start cleanup
                if (structureStore.queue == null ||
                        structureStore.queue.getBlock() == null ||
                        structureStore.getAttempts() > MAX_ATTEMPTS) {
                    if(structureStore.queue != null) {
                        structureStore.setAttempts(0);
                    }
                    structureStore.queue = null;
                    cleanupMode = true;
                }
                structureStore.incrementAttempt();

                if(cleanupMode) {
                    if(!structureStore.scaffoldStack.isEmpty()) {
                        BlockPos pos = structureStore.scaffoldStack.peek();
                        while (!serverWorld.getBlockState(pos).isOf(ModBlocks.MARKED_SCAFFOLD)) {
                            structureStore.scaffoldStack.pop();
                            if (structureStore.scaffoldStack.isEmpty()) {
                                //nothing left to clean, exit
                                return;
                            }
                            pos = structureStore.scaffoldStack.peek();
                        }

                        if (structureStore.getAttempts() > MAX_ATTEMPTS) {
                            //villager can't clean up, let blocks stay
                            while (!structureStore.scaffoldStack.isEmpty()) {
                                pos = structureStore.scaffoldStack.pop();
                                if (serverWorld.getBlockState(pos).isOf(ModBlocks.MARKED_SCAFFOLD)) {
                                    //nothing left to clean, exit
                                    serverWorld.setBlockState(pos, Blocks.SCAFFOLDING.getDefaultState());
                                }
                            }
                            return;
                        }

                        setWalkTarget(villagerEntity, pos);

                        if (pos.isWithinDistance(villagerEntity.getPos(), BUILD_RANGE)) {
                            serverWorld.removeBlock(pos, false);
                            structureStore.scaffoldStack.pop();
                        }
                    }
                } else {
                    //Position of target block
                    BlockPos pos = StructureTemplate.transform(
                            structureStore.placementData,
                            structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset);

//                    VillageGrowthMod.LOGGER.info(
//                            "Target Pos: " + pos.toString().substring(8) +
//                                    " Attempt: " + structureStore.getAttempts()
//                    );


                    //Check whether to attempt placing
                    if (pos.isWithinDistance(villagerEntity.getPos(), BUILD_RANGE)) {
                        BlockState blockState = serverWorld.getBlockState(pos);

                        BuildQueue.PriorityBlock target = structureStore.queue.removeBlock();
                        structureStore.setAttempts(0);

                        if ((blockState.isReplaceable() || blockState.isOf(ModBlocks.MARKED_SCAFFOLD)) &&
                                (hasSolidNeighbor(serverWorld, pos) || target.getQueueCount() > structureStore.template.getSize().getY())) {
                            //place the block
                            serverWorld.setBlockState(pos, target.getBlock().state);
                            serverWorld.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(villagerEntity, target.getBlock().state));

                        } else if (!blockState.isReplaceable() && target.getQueueCount() > structureStore.template.getSize().getY()) {
                            //attempted plenty of times, ignore the block
                            VillageGrowthMod.LOGGER.debug("Ignored " + target.getBlock().state.getBlock().getName());
                        } else {
                            //try again later
                            structureStore.queue.requeueBlock(target);
                        }

                        if (structureStore.queue.getBlock() != null) {
                            setWalkTarget(villagerEntity, StructureTemplate.transform(
                                    structureStore.placementData,
                                    structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset));
                        }
                    }

                    //attempt to build a path
                    if(structureStore.getAttempts() > MAX_ATTEMPTS / 2) {
                        BlockPos pos2 = createTempPath(serverWorld, pos, villagerEntity);

                        if(pos != pos2) {
                            //there exists a block that can be placed
                            if (pos2.isWithinDistance(villagerEntity.getPos(), BUILD_RANGE)) {
                                //place the block and add it to the stack
                                BlockState target =  ModBlocks.MARKED_SCAFFOLD.getDefaultState();
                                serverWorld.setBlockState(pos2,target);
                                serverWorld.emitGameEvent(GameEvent.BLOCK_PLACE, pos2, GameEvent.Emitter.of(villagerEntity, target));
                                structureStore.scaffoldStack.push(pos2);
                            }
                        }
                    }
                }
                this.nextResponseTime = l + DELAY;
            }
        }
    }
    private void setWalkTarget(VillagerEntity villagerEntity, BlockPos pos) {
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


    private BlockPos createTempPath(ServerWorld world, BlockPos pos, VillagerEntity villagerEntity) {
        PathAwareEntity flyingVillager = new BeeEntity(EntityType.BEE, world);
        flyingVillager.setPosition(villagerEntity.getPos());
        Path path =  flyingVillager.getNavigation().findPathTo(pos, 1);
        BlockPos prev = pos;
        while(path != null && !path.isFinished()) {
            BlockPos pos2 = path.getCurrentNode().getBlockPos();
            if(prev == pos2.down()) {
                break;
            }
            if(world.getBlockState(pos2.down()).isAir()) {
                flyingVillager.discard();
                return pos2;
            }
            prev = pos2;
            path.next();
        }
        flyingVillager.discard();
        return pos;
    }
}
