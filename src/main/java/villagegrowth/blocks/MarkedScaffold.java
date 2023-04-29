package villagegrowth.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class MarkedScaffold extends Block implements PolymerBlock {

    public static final MarkedScaffold MARKED_SCAFFOLD = new MarkedScaffold(FabricBlockSettings.copyOf(Blocks.GLASS)
            .dropsLike(Blocks.SCAFFOLDING).sounds(BlockSoundGroup.SCAFFOLDING).breakInstantly());

    public MarkedScaffold(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState());
    }


    @Override
    public Block getPolymerBlock(BlockState state) {
        if(state.isOf(ModBlocks.MARKED_SCAFFOLD)) {
            return ModBlocks.MARKED_SCAFFOLD;
        } else {
            return Blocks.AIR;
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        if(state.equals(ModBlocks.MARKED_SCAFFOLD.getDefaultState())) {
            return Blocks.SCAFFOLDING.getDefaultState().with(Properties.BOTTOM, true);
        } else {
            return Blocks.AIR.getDefaultState();
        }
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
}
