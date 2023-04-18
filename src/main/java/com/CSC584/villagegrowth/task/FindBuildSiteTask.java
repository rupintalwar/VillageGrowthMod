package com.CSC584.villagegrowth.task;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.helpers.StructureStore;
import com.CSC584.villagegrowth.villager.ModVillagers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

import java.util.*;

public class FindBuildSiteTask extends MultiTickTask<VillagerEntity> {
    private static final int SEARCH_RADIUS = 100;
    private static final int EMPTY_SPACE_SIZE = 15;

    private final Map<String, ArrayList<Identifier>> houseStructureMap = new HashMap<>();
    private final Set<Identifier> houseStructureSet = new HashSet<>();

    private boolean foundSpot;

    public FindBuildSiteTask() {
        super(ImmutableMap.of());
    }

    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        VillageGrowthMod.LOGGER.info("Checking Find");
        updateHouseStructures(world);
        return !entity.getBrain().hasMemoryModule(ModVillagers.STRUCTURE_BUILD_INFO) && !houseStructureMap.isEmpty();
    }

    protected void run(ServerWorld world, VillagerEntity entity, long time) {
        VillageGrowthMod.LOGGER.info("Find Build Site:run123!");

        String villageType = entity.getVillagerData().getType().toString();
        ArrayList<Identifier> houseStructureList = houseStructureMap.get(villageType);
        Identifier selectedStruct = houseStructureList.get(new Random().nextInt(houseStructureList.size()));

        StructureStore structureStore = new StructureStore(world, selectedStruct, villageType, false);

        entity.getBrain().remember(ModVillagers.STRUCTURE_BUILD_INFO, structureStore);
        this.foundSpot = false;
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
        VillageGrowthMod.LOGGER.info("Find Build Site:should keep running?" + entity.getBrain().hasMemoryModule(ModVillagers.STRUCTURE_BUILD_INFO));
        return entity.getBrain().hasMemoryModule(ModVillagers.STRUCTURE_BUILD_INFO) && !this.foundSpot;
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
        //Try to find an empty spot for the structure
        VillageGrowthMod.LOGGER.info("Find Build Site:keep running!");

        //Get the center of all villagers within search radius
        Box box = entity.getBoundingBox().expand(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
        Vec3d groupCenter = entity.getPos();
        List<VillagerEntity> list = world.getNonSpectatingEntities(VillagerEntity.class, box);
        list.forEach(villager -> groupCenter.add(villager.getPos()));
        groupCenter.multiply((double) 1 / (list.size() + 1));

        //find the furthest villager from the center to define the projection length
        double projectDist = entity.getPos().squaredDistanceTo(groupCenter);
        for(VillagerEntity villager : list) {
            double computedDist = villager.getPos().squaredDistanceTo(groupCenter);
            if(computedDist > projectDist) {
                projectDist = computedDist;
            }
        }
        projectDist = Math.sqrt(projectDist);

        //project out from the center to a random direction
        Vec3d projectedCenter = groupCenter.addRandom(world.getRandom(), (float) projectDist*2);
        BlockPos structureCorner = BlockPos.ofFloored(projectedCenter);

        //retrieve structure info
        Optional<StructureStore> optional = entity.getBrain().getOptionalMemory(ModVillagers.STRUCTURE_BUILD_INFO);
        if(optional.isPresent()) {
            StructureStore structureStore = optional.get();

            //Find empty space in the column within reason
            int y = structureCorner.getY();
            int maxY = Math.min(world.getTopY() - structureStore.template.getSize().getY(), y + (int) projectDist);
            while(y <= maxY && !this.foundSpot) {
                checkSpot(world, structureStore, structureCorner, y);
                y++;
            }

            if(y == structureCorner.getY() + 1) {
                //We found our first probe is valid. This may be floating in the air though.
                //Go down and find the lowest valid point.
                int minY = Math.max(world.getBottomY(), y - (int) projectDist);
                while(y > minY && this.foundSpot) {
                    checkSpot(world, structureStore, structureCorner, y);
                    y--;
                }
                structureStore.offset = new BlockPos(
                        structureCorner.getX(),
                        y + (this.foundSpot ? 0 : 1),
                        structureCorner.getZ());

                this.foundSpot = true;
            }

            //add filler blocks as a foundation
            if(this.foundSpot) {
                structureStore.createFoundation();
            }
        }
    }

    private void checkSpot(ServerWorld world, StructureStore structureStore, BlockPos structureCorner, int y) {
        structureStore.offset = new BlockPos(structureCorner.getX(), y, structureCorner.getZ());
        BlockBox structureBox = structureStore.template.calculateBoundingBox(
                structureStore.placementData, structureStore.offset);
        structureStore.placementData.setBoundingBox(structureBox);

        this.foundSpot = world.isSpaceEmpty(Box.from(structureBox));
    }

    private void updateHouseStructures(ServerWorld world) {
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();
        structureTemplateManager.streamTemplates()
                .filter(k -> k.getPath().contains("houses"))
                .forEach(this::addToMap);
    }

    private void addToMap(Identifier id) {
        //Adds the id to the set. If it isn't already present, add to the map as well
        boolean added = houseStructureSet.add(id);
        if(added) {
            //Get village type. Ex: desert, plains, savanna
            String type = id.getPath().split("/")[1];
            ArrayList<Identifier> list = houseStructureMap.getOrDefault(type, new ArrayList<>());
            list.add(id);
            houseStructureMap.put(type, list);
        }
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
