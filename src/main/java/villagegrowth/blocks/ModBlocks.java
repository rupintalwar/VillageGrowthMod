package villagegrowth.blocks;

import eu.pb4.polymer.core.api.block.SimplePolymerBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import villagegrowth.VillageGrowthMod;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block MARKED_SCAFFOLD =
            Registry.register(
                    Registries.BLOCK,
                    new Identifier(VillageGrowthMod.MOD_ID, "marked_scaffold"),
                    MarkedScaffold.MARKED_SCAFFOLD
            );
    public static final Item MARKED_SCAFFOLD_ITEM =
            Registry.register(
                    Registries.ITEM,
                    new Identifier(VillageGrowthMod.MOD_ID, "marked_scaffold"),
                    new BlockItem(MarkedScaffold.MARKED_SCAFFOLD, new FabricItemSettings())
            );
    public static void registerBlocks() {
        VillageGrowthMod.LOGGER.debug("Registering blocks for " + VillageGrowthMod.MOD_ID);

    }
}
