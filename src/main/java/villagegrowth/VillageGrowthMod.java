package villagegrowth;

import villagegrowth.blocks.ModBlocks;
import villagegrowth.villager.ModVillagers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageGrowthMod implements ModInitializer {
	public static final String MOD_ID = "villagegrowth";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Mod: " + MOD_ID);
		ModVillagers.registerVillagers();
		ModBlocks.registerBlocks();
	}
}
