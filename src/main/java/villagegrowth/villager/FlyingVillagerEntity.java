package villagegrowth.villager;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;

public class FlyingVillagerEntity extends VillagerEntity {

    public FlyingVillagerEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(1.125f);
        this.navigation = createNavigation(world);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        MobNavigation mobNavigation = new MobNavigation(this, world) {
            @Override
            protected PathNodeNavigator createPathNodeNavigator(int range) {
                this.nodeMaker = new FlyingVillagerNodeMaker();
                this.nodeMaker.setCanEnterOpenDoors(true);
                this.nodeMaker.setCanSwim(true);
                this.nodeMaker.setCanOpenDoors(true);
                return new PathNodeNavigator(this.nodeMaker, range);
            }

            @Override
            protected boolean isAtValidPosition() {
                return true;
            }

        };

        mobNavigation.setCanPathThroughDoors(true);
        mobNavigation.setCanSwim(true);
        mobNavigation.setCanEnterOpenDoors(true);

        return mobNavigation;
    }
}
