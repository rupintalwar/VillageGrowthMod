package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.buildqueue.BuildQueue;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class FindBuildSiteTask extends MultiTickTask<VillagerEntity> {
    private static final int MAX_RUN_TIME = 800;
    private static final int RADIUS = 100;
    @Nullable
    private BlockPos currentTarget;
    private long nextResponseTime;
    private int ticksRan;

    private String[] structures = {
            "armorer_1.nbt", "butcher_shop_1.nbt", "cartographer_house_1.nbt", "farm_1.nbt", "farm_2.nbt",
            "fletcher_house_1.nbt", "large_farm_1.nbt", "library_1.nbt", "mason_1.nbt", "medium_house_1.nbt",
            "medium_house_2.nbt", "shepherd_house_1.nbt", "small_house_1.nbt", "small_house_2.nbt", "small_house_3.nbt",
            "small_house_4.nbt", "small_house_5.nbt", "small_house_6.nbt", "small_house_7.nbt", "small_house_8.nbt",
            "tannery_1.nbt", "temple_1.nbt", "temple_2.nbt", "tool_smith_1.nbt", "weaponsmith_1.nbt"
    };

    public FindBuildSiteTask() {
        super(
                ImmutableMap.of(ModVillagers.BUILD_SITE, MemoryModuleState.VALUE_ABSENT),
                MAX_RUN_TIME);
    }

    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        return !entity.getBrain().hasMemoryModule(ModVillagers.BUILD_SITE);
    }

    protected void run(ServerWorld world, VillagerEntity entity, long time) {
        VillageGrowthMod.LOGGER.info("Find Build Site:run!");
        Vec3d curPos = entity.getPos();
        BlockPos emptySpot = findEmptySpace(world, curPos);
        if (null != emptySpot) {
            entity.getBrain().remember(ModVillagers.BUILD_SITE, GlobalPos.create(world.getRegistryKey(), emptySpot));
            String villageType = entity.getVillagerData().getType().toString();
            String selectedStruct = this.structures[new Random().nextInt(this.structures.length)];
            entity.getBrain().remember(ModVillagers.BUILD_QUEUE, new BuildQueue(villageType + selectedStruct));
        }
    }

    public static BlockPos findEmptySpace(ServerWorld world, Vec3d center) {
        final int SEARCH_RADIUS = 100;
        final int EMPTY_SPACE_SIZE = 15;

        for (int x = (int) (center.getX() - SEARCH_RADIUS); x <= center.getX() + SEARCH_RADIUS; x++) {
            for (int y = (int) (center.getY() - SEARCH_RADIUS); y <= center.getY() + SEARCH_RADIUS; y++) {
                for (int z = (int) (center.getZ() - EMPTY_SPACE_SIZE); z <= center.getZ() + EMPTY_SPACE_SIZE; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.isAir(pos)) {
                        boolean enoughSpace = true;
                        for (int dx = -EMPTY_SPACE_SIZE; dx <= EMPTY_SPACE_SIZE; dx++) {
                            for (int dy = 0; dy <= EMPTY_SPACE_SIZE * 2; dy++) {
                                for (int dz = -EMPTY_SPACE_SIZE; dz <= EMPTY_SPACE_SIZE; dz++) {
                                    BlockPos checkPos = pos.add(dx, dy, dz);
                                    if (!world.isAir(checkPos)) {
                                        enoughSpace = false;
                                        break;
                                    }
                                }
                                if (!enoughSpace) {
                                    break;
                                }
                            }
                            if (!enoughSpace) {
                                break;
                            }
                        }
                        if (enoughSpace) {
                            return pos;
                        }
                    }
                }
            }
        }
        // If no empty space was found, return null
        return null;
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        villagerEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
        this.ticksRan = 0;
        this.nextResponseTime = l + 40L;
    }
}
