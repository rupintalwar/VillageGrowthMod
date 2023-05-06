package villagegrowth.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;
import villagegrowth.VillageGrowthMod;
import villagegrowth.blocks.ModBlocks;
import villagegrowth.helpers.BuildQueue;
import villagegrowth.helpers.StructureStore;
import villagegrowth.mixin.EntityNavigationInterfaceMixin;
import villagegrowth.villager.FlyingVillagerEntity;
import villagegrowth.villager.ModVillagers;

import java.util.Optional;

public class ConstructBuildingTask extends MultiTickTask<VillagerEntity> {

    public static final int BUILD_RANGE = 7;
    private static final int MAX_ATTEMPTS = 300;
    private static final long DELAY = 5;

    private long nextResponseTime;

    private Path storedPath;

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
        if (!villagerEntity.getVillagerData().getProfession().equals(VillagerProfession.MASON)) {
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
                if(ModVillagers.MARK_NEXT_BLOCK) {
                    structureStore.marker.discard();
                    structureStore.pathmarker.forEach(Entity::discard);
                }
                villagerEntity.getBrain().forget(ModVillagers.STRUCTURE_BUILD_INFO);

            }
        }
        return false;
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Optional<StructureStore> optional = villagerEntity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if (optional.isPresent() && optional.get().queue != null && optional.get().queue.getBlock() != null) {
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
                        this.storedPath = null;
                    }

                    structureStore.queue = null;
                    cleanupMode = true;
                }

                if(isAttemptingToBuild(villagerEntity, structureStore)) {
                    structureStore.incrementAttempt();
                }
                if(ModVillagers.MARK_NEXT_BLOCK) {
                    structureStore.marker.setCustomName(Text.of(villagerEntity.getCustomName().getString() + ": " + (MAX_ATTEMPTS - structureStore.getAttempts())));
                }

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
                            //villager can't clean up, replace blocks
                            while (!structureStore.scaffoldStack.isEmpty()) {
                                pos = structureStore.scaffoldStack.pop();
                                if (serverWorld.getBlockState(pos).isOf(ModBlocks.MARKED_SCAFFOLD)) {
                                    //replace with specified type
                                    serverWorld.setBlockState(pos, ModBlocks.replaceType);
                                }
                            }
                            return;
                        }

                        setWalkTarget(villagerEntity, pos);

                        if (pos.isWithinDistance(villagerEntity.getPos(), BUILD_RANGE)) {
                            serverWorld.removeBlock(pos, false);
                            structureStore.scaffoldStack.pop();
                            structureStore.setAttempts(0);
                        }
                    }
                } else {
                    //Position of target block
                    BlockPos pos = StructureTemplate.transform(
                            structureStore.placementData,
                            structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset);

                    if(ModVillagers.MARK_NEXT_BLOCK) {
                        structureStore.marker.setPosition(pos.toCenterPos());
                    }
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
                            this.storedPath = null;
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

                        if(ModVillagers.MARK_NEXT_BLOCK) {
                            for(int i = 0; i < this.storedPath.getLength(); i++) {
                                structureStore.pathmarker.get(i).setPosition(this.storedPath.getNode(i).getBlockPos().toCenterPos());
                            }
                        }

                        /*
                        if(//pos2.getY() > villagerEntity.getBlockPos().getY() ||
                                !serverWorld.getBlockState(pos2.up()).isAir() ||
                                !serverWorld.getBlockState(pos2.up().up()).isAir() ||
                                (!serverWorld.getBlockState(villagerEntity.getBlockPos().up().up()).isAir()
                                        && pos2.getY() ==  villagerEntity.getPos().getY())) {
                            //don't block yourself with scaffold
                            pos2 = pos;
                        }*/
                        if(!pos.equals(pos2)) {
                            //there exists a block that can be placed
                            if (pos2.isWithinDistance(villagerEntity.getPos(), BUILD_RANGE) && serverWorld.getBlockState(pos2).isReplaceable()) {
                                //place the block and add it to the stack
                                BlockState target =  ModBlocks.MARKED_SCAFFOLD.getDefaultState();
                                serverWorld.setBlockState(pos2,target);
                                serverWorld.emitGameEvent(GameEvent.BLOCK_PLACE, pos2, GameEvent.Emitter.of(villagerEntity, target));
                                structureStore.scaffoldStack.push(pos2);
                            }
                            setWalkTarget(villagerEntity, this.storedPath.getCurrentNodePos());
                        }
                    }
                }
                this.nextResponseTime = l + DELAY;
            }
        }
    }

    private boolean isAttemptingToBuild(VillagerEntity villagerEntity, StructureStore structureStore) {
        Optional<WalkTarget> optional = villagerEntity.getBrain().getOptionalMemory(MemoryModuleType.WALK_TARGET);
        if(optional.isPresent()) {
            BlockPos villagerTarget = optional.get().getLookTarget().getBlockPos();
            BlockPos p = structureStore.queue != null &&
                    structureStore.queue.getBlock() != null ?
                    StructureTemplate.transform(
                            structureStore.placementData,
                            structureStore.queue.getBlock().getBlock().pos).add(structureStore.offset)
                    : !structureStore.scaffoldStack.isEmpty() ? structureStore.scaffoldStack.peek() : null;

            return p != null && p.equals(villagerTarget);
        } else {
            return true;
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
        PathAwareEntity flyingVillager = new FlyingVillagerEntity(EntityType.VILLAGER, world);
        flyingVillager.setPosition(villagerEntity.getPos());

        Path path = storedPath;
        if(shouldRecalculatePath(world)) {
            path = ((EntityNavigationInterfaceMixin) flyingVillager.getNavigation())
                    .invokeFindPathTo(ImmutableSet.of(pos),
                            BUILD_RANGE,
                            false,
                            1);
            this.storedPath = path;
        }

        Path path2 = villagerEntity.getNavigation().getCurrentPath();
        if(path2 == null || !path2.getTarget().equals(pos)) {
            path2 = ((EntityNavigationInterfaceMixin) villagerEntity.getNavigation())
                    .invokeFindPathTo(ImmutableSet.of(pos),
                            BUILD_RANGE,
                            false,
                            1);
        }

        BlockPos prev = villagerEntity.getBlockPos();
        while((path2 == null || !path2.reachesTarget()) && path != null && !path.isFinished()) {
            BlockPos pos2 = path.getCurrentNode().getBlockPos();
            if(prev.equals(pos2.down())) {
                flyingVillager.discard();
                return prev;
            }
            if(world.getBlockState(pos2.down()).isReplaceable()) {
                flyingVillager.discard();
                return pos2.down();
            }
            prev = pos2;
            path.next();
        }
        flyingVillager.discard();
        return pos;
    }

    private boolean shouldRecalculatePath(ServerWorld world) {
        if(this.storedPath == null || this.storedPath.isFinished() || (!this.storedPath.reachesTarget() &&
                this.storedPath.getCurrentNode().equals(this.storedPath.getEnd()))) {
            return true;
        }

        for(int i = this.storedPath.getCurrentNodeIndex();
            i < this.storedPath.getLength(); i++) {

            BlockPos pos = this.storedPath.getNode(i).getBlockPos();

            if(!world.getBlockState(pos).canPathfindThrough(world, pos, NavigationType.LAND) ||
                    !world.getBlockState(pos.up()).canPathfindThrough(world, pos.up(), NavigationType.LAND)) {
                return true;
            }

        }

        return false;
    }
}
