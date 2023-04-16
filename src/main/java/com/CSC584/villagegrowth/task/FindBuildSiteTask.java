package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class FindBuildSiteTask extends MultiTickTask<VillagerEntity> {
    private static final int MAX_RUN_TIME = 800;
    @Nullable
    private BlockPos currentTarget;
    private long nextResponseTime;
    private int ticksRan;


    public FindBuildSiteTask() {
        super(
                ImmutableMap.of(ModVillagers.BUILD_SITE, MemoryModuleState.VALUE_ABSENT),
                MAX_RUN_TIME);
    }

    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        /*
        return world.getPointOfInterestStorage()
                .getNearestPosition(
                        ModVillagers.BUILDER.getCompletionCondition(),
                        entity.getBlockPos(),
                        48,
                        PointOfInterestStorage.OccupationStatus.ANY
                ).isPresent();

         */
        return true;
    }

    protected void run(ServerWorld world, VillagerEntity entity, long time) {
        VillageGrowthMod.LOGGER.info("Find Build Site:run!");
        /*
        world.getPointOfInterestStorage()
                .getPositions(
                        ModVillagers.BUILDER.getCompletionCondition(),
                        (blockPos) -> {
                            Path path = entity
                                    .getNavigation()
                                    .findPathTo(blockPos, ModVillagers.BUILDER.getSearchDistance());
                            return (path != null && path.reachesTarget());
                        },
                        entity.getBlockPos(),
                        48,
                        PointOfInterestStorage.OccupationStatus.ANY
                )
                .findAny()
                .ifPresent(blockPos -> {
                    GlobalPos globalPos = GlobalPos.create(world.getRegistryKey(), blockPos);
                    entity.getBrain().remember(ModVillagers.BUILD_SITE, globalPos);
                });

         */
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        villagerEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
        this.ticksRan = 0;
        this.nextResponseTime = l + 40L;
    }
}
