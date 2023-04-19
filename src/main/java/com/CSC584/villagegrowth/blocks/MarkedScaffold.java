package com.CSC584.villagegrowth.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;

public class MarkedScaffold extends Block {
    public static final BooleanProperty MARKED = BooleanProperty.of("marked");

    public static final MarkedScaffold MARKED_SCAFFOLD = new MarkedScaffold(FabricBlockSettings.copyOf(Blocks.GLASS));
    public MarkedScaffold(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(MARKED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MARKED);
    }
}
