package ninja.trek.pocketportals;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.data.DataLoader;
import ninja.trek.pocketportals.dimension.ModDimensions;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PocketPortals implements ModInitializer {
	public static final String MOD_ID = "pocket-portals";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register data loader first
		DataLoader.register();
		// Register dimension system first
		ModDimensions.register();
		PocketDimensionsRegistry.init();


		// Then register blocks and items that might depend on dimensions
		ModBlocks.registerBlocks();
		ModItems.registerItems();




		LOGGER.info("EndNew mod initialized!");
	}
}