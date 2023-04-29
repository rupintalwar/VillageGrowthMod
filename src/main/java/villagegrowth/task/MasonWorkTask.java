package villagegrowth.task;

import villagegrowth.VillageGrowthMod;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.VillagerWorkTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public class MasonWorkTask extends VillagerWorkTask {

    @Override
    protected void performAdditionalWork(ServerWorld world, VillagerEntity entity) {
        Optional<GlobalPos> optional = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
        if (optional.isEmpty()) {
            return;
        }
        GlobalPos globalPos = optional.get();
        BlockState blockState = world.getBlockState(globalPos.getPos());
        if (blockState.isOf(Blocks.STONECUTTER)) {
            //this.placeBlockOnTop(world, entity, globalPos);
        }
    }

    private void placeBlockOnTop(ServerWorld world, VillagerEntity entity, GlobalPos globalPos) {
        //Testing function to verify that villagers do work
        Block block2 = world.getBlockState(globalPos.getPos().up()).getBlock();

        VillageGrowthMod.LOGGER.info("Checking to place block: " + globalPos.getPos().up().toString());
        if(block2 instanceof AirBlock)  {
            BlockState blockState2 = Blocks.DIRT.getDefaultState();
            world.setBlockState(globalPos.getPos().up(), blockState2);
            world.emitGameEvent(GameEvent.BLOCK_PLACE, globalPos.getPos().up(), GameEvent.Emitter.of(entity, blockState2));
            VillageGrowthMod.LOGGER.info("Villager: " + entity.toString() + " Placed block: " + globalPos.getPos().up().toString());
        }

    }
}
