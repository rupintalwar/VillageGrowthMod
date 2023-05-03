package villagegrowth.blocks;

import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import villagegrowth.VillageGrowthMod;

import java.util.HashSet;

public class ModBlocks {
    public static final BlockState replaceType = Blocks.AIR.getDefaultState();
    public static final Block MARKED_SCAFFOLD =
            Registry.register(
                    Registries.BLOCK,
                    new Identifier(VillageGrowthMod.MOD_ID, "marked_scaffold"),
                    MarkedScaffold.MARKED_SCAFFOLD
            );

    public static HashSet<Block> scaffoldSet = new HashSet<>(ImmutableSet.of(MARKED_SCAFFOLD, Blocks.SCAFFOLDING));

    public static void registerBlocks() {
        VillageGrowthMod.LOGGER.debug("Registering blocks for " + VillageGrowthMod.MOD_ID);

        LandPathNodeTypesRegistry.register(ModBlocks.MARKED_SCAFFOLD, PathNodeType.BLOCKED, null);
    }
}
