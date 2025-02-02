package ninja.trek.pocketportals;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.data.DataLoader;
import ninja.trek.pocketportals.dimension.ModDimensions;
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

		// Then register blocks and items that might depend on dimensions
		ModBlocks.registerBlocks();
		ModItems.registerItems();

		// Add initialization verification
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("Verifying EndNew mod initialization...");

			// Verify dimension registration
			if (server.getWorldRegistryKeys().contains(ModDimensions.POCKET_DIMENSION_KEY)) {
				LOGGER.info("Pocket dimension registration verified");
			} else {
				LOGGER.warn("Pocket dimension might need manual world creation - this is normal on first load");
			}
		});

		LOGGER.info("EndNew mod initialized!");
	}
}