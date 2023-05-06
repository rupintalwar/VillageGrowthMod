package villagegrowth.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import villagegrowth.helpers.StructureStore;
import villagegrowth.villager.ModVillagers;

import java.util.*;

public class FindBuildSiteTask extends MultiTickTask<VillagerEntity> {
    public static final HashSet VALID_BLOCKS = new HashSet<>(ImmutableSet.of(
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.DIRT,
            Blocks.GRASS_BLOCK,
            Blocks.GRAVEL,
            Blocks.STONE,
            Blocks.ICE,
            Blocks.END_STONE,
            Blocks.NETHERRACK));
    private static final int SEARCH_RADIUS = 50;
    private static final double PROJECTION_OFFSET = 3;

    private final Map<String, ArrayList<Identifier>> houseStructureMap = new HashMap<>();
    private final Set<Identifier> houseStructureSet = new HashSet<>();

    private final Map<String, String> villagerTypeMap = new HashMap<>(ImmutableMap.of(
            "snow", "snowy"
            ));

    private boolean foundSpot;

    public FindBuildSiteTask() {
        super(ImmutableMap.of());
    }

    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        //VillageGrowthMod.LOGGER.info("Checking Find");
        updateHouseStructures(world);
        return !entity.getBrain().hasMemoryModule(ModVillagers.STRUCTURE_BUILD_INFO) && !houseStructureMap.isEmpty();
    }

    protected void run(ServerWorld world, VillagerEntity entity, long time) {
        //VillageGrowthMod.LOGGER.info("Find Build Site:run!");

        String villageType = entity.getVillagerData().getType().toString();
        villageType = villagerTypeMap.get(villageType);

        //Some villagers don't have matching village type names
        if(houseStructureMap.get(villageType) == null ) {
            villageType = villagerTypeMap.get(villageType); //check map for name match
            if(villageType == null) {
                villageType = "plains"; //default to plains structures if it doesn't exist
            }
        }

        ArrayList<Identifier> houseStructureList = houseStructureMap.get(villageType);
        if(houseStructureList != null) {
            Identifier selectedStruct = houseStructureList.get(new Random().nextInt(houseStructureList.size()));

            StructureStore structureStore = new StructureStore(world, selectedStruct, villageType, true);

            if(ModVillagers.MARK_NEXT_BLOCK) {
                if (!entity.hasCustomName()) {
                    entity.setCustomName(Text.of(Integer.toString(entity.getId())));
                    entity.setCustomNameVisible(true);
                }

                structureStore.marker.setCustomName(entity.getCustomName());
                structureStore.marker.setCustomNameVisible(true);
            }

            entity.getBrain().remember(ModVillagers.STRUCTURE_BUILD_INFO, structureStore);
        }
        this.foundSpot = false;
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
        //VillageGrowthMod.LOGGER.info("Find Build Site:should keep running?" + entity.getBrain().hasMemoryModule(ModVillagers.STRUCTURE_BUILD_INFO));
        return entity.getBrain().hasMemoryModule(ModVillagers.STRUCTURE_BUILD_INFO) && !this.foundSpot;
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
        //Try to find an empty spot for the structure
        //VillageGrowthMod.LOGGER.info("Find Build Site:keep running!");

        //Get the center of all villagers within search radius
        Box box = entity.getBoundingBox().expand(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
        Vec3d groupCenter = entity.getPos();
        List<VillagerEntity> list = world.getNonSpectatingEntities(VillagerEntity.class, box);
        list.forEach(villager -> groupCenter.add(villager.getPos()));
        groupCenter.multiply((double) 1 / (list.size() + 1));

        //find the furthest villager from the center to define the projection length
        double projectDist = entity.getPos().squaredDistanceTo(groupCenter);
//        for(VillagerEntity villager : list) {
//            double computedDist = villager.getPos().squaredDistanceTo(groupCenter);
//            if(computedDist > projectDist) {
//                projectDist = computedDist;
//            }
//        }
//        projectDist = Math.sqrt(projectDist);

        //find the villager position standard deviation
        for(VillagerEntity villager : list) {
            projectDist += villager.getPos().squaredDistanceTo(groupCenter);
        }
        projectDist = 2 * Math.sqrt(projectDist / (list.size() + 1)) + PROJECTION_OFFSET;


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
                checkSpot(world, structureStore, structureCorner, y, list, entity);
                y++;
            }

            if(y == structureCorner.getY() + 1) {
                //We found our first probe is valid. This may be floating in the air though.
                //Go down and find the lowest valid point.
                int minY = Math.max(world.getBottomY(), y - (int) projectDist);
                while(y > minY && this.foundSpot) {
                    checkSpot(world, structureStore, structureCorner, y, list, entity);
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
                //if too many filler blocks are needed, it's a bad spot
                this.foundSpot = structureStore.createFoundation();
            }
        }
    }

    private void checkSpot(ServerWorld world,
                           StructureStore structureStore,
                           BlockPos structureCorner,
                           int y,
                           List<VillagerEntity> villagerNeighbors,
                           VillagerEntity entity) {
        structureStore.setOffset(new BlockPos(structureCorner.getX(), y, structureCorner.getZ()));

        this.foundSpot = world.isSpaceEmpty(Box.from(structureStore.placementData.getBoundingBox()));
        this.foundSpot = !world.containsFluid(Box.from(structureStore.placementData.getBoundingBox())) && this.foundSpot; //stop building in water

        if(!this.foundSpot) {
            return;
        }

        //Check if the spot interferes with another nearby villager
        for (VillagerEntity villager : villagerNeighbors) {
            Optional<StructureStore> optional = villager.getBrain().getOptionalRegisteredMemory(ModVillagers.STRUCTURE_BUILD_INFO);
            if (optional.isPresent()) {
                StructureStore store = optional.get();
                //check if villager has a block queue (is building) and if the box intersects
                if(store.queue != null &&
                        store.queue.getBlock() != null &&
                        store.placementData.getBoundingBox() != null &&
                        store.placementData.getBoundingBox().intersects(structureStore.placementData.getBoundingBox())) {
                    //Don't build where someone else is
                    this.foundSpot = false;
                    return;
                }
            }
        }

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

}
