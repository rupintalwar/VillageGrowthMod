package com.CSC584.villagegrowth.mixin;

import com.CSC584.villagegrowth.task.FindBuildSiteTask;
import com.CSC584.villagegrowth.task.MasonVillagerTask;
import com.CSC584.villagegrowth.task.TillLandTask;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static com.CSC584.villagegrowth.villager.ModVillagers.BUILD_SITE;

@Mixin(VillagerTaskListProvider.class)
public class VillagerTaskListProviderMixin {
    @Inject(method = "createWorkTasks", at = @At("HEAD"), cancellable = true)
    private static void replaceCreateWorkTasks(VillagerProfession profession, float speed,
                                               CallbackInfoReturnable<List<Pair<Integer, ? extends Task<? super VillagerEntity>>>> info) {
        ArrayList<Pair<Task<? super VillagerEntity>, Integer>> randomTasks = new ArrayList<>();
        ArrayList<Pair<Integer, ? extends Task<? super VillagerEntity>>> tasks = new ArrayList<>();

        boolean overrideTasks = true;

        if(profession == VillagerProfession.FARMER) {
            randomTasks.addAll(ImmutableList.of(
                    Pair.of(new FarmerWorkTask(), 7),
                    Pair.of(new FarmerVillagerTask(), 2),
                    Pair.of(new BoneMealTask(), 4),
                    Pair.of(new TillLandTask(), 3)
            ));

        } else if(profession == VillagerProfession.MASON) {
            randomTasks.addAll(ImmutableList.of(
                    Pair.of(new MasonVillagerTask(), 7),
                    Pair.of(new FindBuildSiteTask(), 4)
            ));

            tasks.addAll(ImmutableList.of(
                    Pair.of(3, VillagerWalkTowardsTask.create(BUILD_SITE, speed, 9, 100, 1200))
            ));

        } else {
            overrideTasks = false;
        }

        if(overrideTasks) {
            randomTasks.addAll(ImmutableList.of(
                    Pair.of(GoToIfNearbyTask.create(MemoryModuleType.JOB_SITE, 0.4f, 4), 2),
                    Pair.of(GoToNearbyPositionTask.create(MemoryModuleType.JOB_SITE, 0.4f, 1, 10), 5),
                    Pair.of(GoToSecondaryPositionTask.create(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5)
            ));

            tasks.addAll(ImmutableList.of(
                    VillagerTaskListProviderInterfaceMixin.invokeCreateBusyFollowTask(),
                    Pair.of(5, new RandomTask<>(ImmutableList.copyOf(randomTasks))),
                    Pair.of(10, new HoldTradeOffersTask(400, 1600)),
                    Pair.of(10, FindInteractionTargetTask.create(EntityType.PLAYER, 4)),
                    Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                    Pair.of(3, new GiveGiftsToHeroTask(100)), Pair.of(99, ScheduleActivityTask.create())
            ));

            info.setReturnValue(ImmutableList.copyOf(tasks));
            info.cancel();
        }
    }

}
