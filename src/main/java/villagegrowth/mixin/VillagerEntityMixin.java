package villagegrowth.mixin;

import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static villagegrowth.villager.ModVillagers.MEMORY_MODULES;
import static villagegrowth.villager.ModVillagers.SENSORS;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {
    @Inject(method = "createBrainProfile", at = @At("HEAD"), cancellable = true)
    protected void replaceCreateBrainProfile(CallbackInfoReturnable<Brain.Profile<VillagerEntity>> info) {
        info.setReturnValue(Brain.createProfile(MEMORY_MODULES, SENSORS));
        info.cancel();
    }

}
