package villagegrowth.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import villagegrowth.villager.VillagerNodeMaker;

import static villagegrowth.villager.ModVillagers.MEMORY_MODULES;
import static villagegrowth.villager.ModVillagers.SENSORS;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "createBrainProfile", at = @At("HEAD"), cancellable = true)
    protected void replaceCreateBrainProfile(CallbackInfoReturnable<Brain.Profile<VillagerEntity>> info) {
        info.setReturnValue(Brain.createProfile(MEMORY_MODULES, SENSORS));
        info.cancel();
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V")
    public void villagerInit(EntityType<? extends MerchantEntity> entityType, World world, CallbackInfo ci) {
        MobNavigation mobNavigation = new MobNavigation(this, world) {
            @Override
            protected PathNodeNavigator createPathNodeNavigator(int range) {
                this.nodeMaker = new VillagerNodeMaker();
                this.nodeMaker.setCanEnterOpenDoors(true);
                this.nodeMaker.setCanSwim(true);
                this.nodeMaker.setCanOpenDoors(true);
                return new PathNodeNavigator(this.nodeMaker, range);
            }
        };

        mobNavigation.setCanPathThroughDoors(true);
        mobNavigation.setCanSwim(true);
        mobNavigation.setCanEnterOpenDoors(true);

        this.navigation = mobNavigation;
    }
}
