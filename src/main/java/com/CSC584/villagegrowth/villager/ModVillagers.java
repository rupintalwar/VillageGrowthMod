package com.CSC584.villagegrowth.villager;

import com.CSC584.villagegrowth.VillageGrowthMod;
import com.CSC584.villagegrowth.helpers.StructureStore;
import com.CSC584.villagegrowth.mixin.VillagerEntityAccessor;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ModVillagers {
    public static final MemoryModuleType<StructureStore> STRUCTURE_BUILD_INFO =
        Registry.register(
                Registries.MEMORY_MODULE_TYPE,
                new Identifier(VillageGrowthMod.MOD_ID, "structure_build_info"),
                new MemoryModuleType<>(Optional.empty())
        );

    public static ImmutableList<MemoryModuleType<?>> MEMORY_MODULES= new ImmutableList.Builder<MemoryModuleType<?>>()
            .addAll(VillagerEntityAccessor.getMemoryModules())
            .add(STRUCTURE_BUILD_INFO)
            .build();

    public static ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS = new ImmutableList.Builder<SensorType<? extends Sensor<? super VillagerEntity>>>()
            .addAll(VillagerEntityAccessor.getSensors())
            .build();

    public static void registerVillagers() {
        VillageGrowthMod.LOGGER.debug("Registering villagers for " + VillageGrowthMod.MOD_ID);
    }
}
