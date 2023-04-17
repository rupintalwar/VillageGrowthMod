package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.buildqueue.BuildQueue;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class FindBuildSiteTask extends MultiTickTask<VillagerEntity> {
    private static final int MAX_RUN_TIME = 800;
    private static final int SEARCH_RADIUS = 100;
    private static final int EMPTY_SPACE_SIZE = 15;

    private HashMap<String, ArrayList<Identifier>> house_structure_map = new HashMap<>();

    public FindBuildSiteTask() {
        super(ImmutableMap.of());
    }

    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        VillageGrowthMod.LOGGER.info("Checking Find");
        VillageGrowthMod.LOGGER.info("Has build Site: " + entity.getBrain().hasMemoryModule(ModVillagers.BUILD_SITE));
        updateHouseStructures(world);
        return !entity.getBrain().hasMemoryModule(ModVillagers.BUILD_SITE) && !house_structure_map.isEmpty();
    }

    protected void run(ServerWorld world, VillagerEntity entity, long time) {
        VillageGrowthMod.LOGGER.info("Find Build Site:run!");
        Vec3d curPos = entity.getPos();
        String villageType = entity.getVillagerData().getType().toString();
        ArrayList<Identifier> house_structure_list = house_structure_map.get(villageType);

        BlockPos emptySpot = findEmptySpace(world, curPos);
        if (emptySpot != null && !house_structure_list.isEmpty()) {
            entity.getBrain().remember(ModVillagers.BUILD_SITE, GlobalPos.create(world.getRegistryKey(), emptySpot));
            Identifier selectedStruct = house_structure_list.get(new Random().nextInt(house_structure_list.size()));
            entity.getBrain().remember(ModVillagers.BUILD_QUEUE, new BuildQueue(selectedStruct, world));
        }
    }

    private void updateHouseStructures(ServerWorld world) {
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();
        structureTemplateManager.streamTemplates()
                .filter(k -> k.getPath().contains("houses"))
                .forEach(k -> addToMap(k));
    }

    private void addToMap(Identifier id) {
        //Get village type. Ex: desert, plains, savanna
        String type = id.getPath().split("/")[1];
        ArrayList<Identifier> list = house_structure_map.getOrDefault(type, new ArrayList<>());
        list.add(id);
        house_structure_map.put(type, list);
    }

    public static BlockPos findEmptySpace(ServerWorld world, Vec3d center) {
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

}
