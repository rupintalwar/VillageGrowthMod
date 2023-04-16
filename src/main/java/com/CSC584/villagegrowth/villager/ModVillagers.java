package com.CSC584.villagegrowth.villager;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.buildqueue.BuildQueue;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;

public class ModVillagers {
    public static final Activity BUILD = registerActivity("build");

    public static final MemoryModuleType<GlobalPos> BUILD_SITE = registerMem("build_site", GlobalPos.CODEC);

    public static final PointOfInterestType BUILDER = registerPOI("builder", Blocks.EMERALD_BLOCK);

    public static final MemoryModuleType<BuildQueue> BUILD_QUEUE =
            Registry.register(Registries.MEMORY_MODULE_TYPE, "build_queue", new MemoryModuleType<>(Optional.empty()));
    public static Activity registerActivity(String name) {
        return Registry.register(Registries.ACTIVITY, name, new Activity(name));
    }

    public static MemoryModuleType<GlobalPos> registerMem(String name, Codec<GlobalPos> codec) {
        return Registry.register(Registries.MEMORY_MODULE_TYPE, name, new MemoryModuleType<>(Optional.of(codec)));
    }

    public static MemoryModuleType<GlobalPos> registerType(String name, Codec<GlobalPos> codec) {
        return Registry.register(Registries.MEMORY_MODULE_TYPE, name, new MemoryModuleType<>(Optional.of(codec)));
    }

    public static PointOfInterestType registerPOI(String name, Block block) {
        return PointOfInterestHelper.register(new Identifier(VillageGrowthMod.MOD_ID, name),
                1, 1, ImmutableSet.copyOf(block.getStateManager().getStates()));
    }

    public static void registerVillagers() {
        VillageGrowthMod.LOGGER.debug("Registering villagers for " + VillageGrowthMod.MOD_ID);
    }
}
