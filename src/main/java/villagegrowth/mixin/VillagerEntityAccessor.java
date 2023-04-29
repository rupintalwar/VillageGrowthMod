package villagegrowth.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerEntity.class)
public interface VillagerEntityAccessor {
    @Accessor("MEMORY_MODULES")
    static ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        throw new AssertionError();
    }

    @Accessor("SENSORS")
    static ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> getSensors() {
        throw new AssertionError();
    }
}
