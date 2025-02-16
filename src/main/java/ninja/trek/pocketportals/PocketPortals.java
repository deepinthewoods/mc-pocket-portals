package ninja.trek.pocketportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.data.DataLoader;
import ninja.trek.pocketportals.dimension.ModDimensions;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.item.ModItems;
import ninja.trek.pocketportals.network.SpawnRulesPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PocketPortals implements ModInitializer {
	public static final String MOD_ID = "pocketportals";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static MinecraftServer server;

	public static MinecraftServer getServer() {
		return server;
	}

	public static void setServer(MinecraftServer server) {
		PocketPortals.server = server;
	}

	@Override
	public void onInitialize() {
		// Add server lifecycle event
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			PocketPortals.setServer(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PocketPortals.setServer(null);
		});
		// First register blocks to ensure IDs are set
		if (System.getProperty("fabric.datagen") == null) {
			LOGGER.info("Registering blocks...");
			ModBlocks.registerBlocks();
		}

		// Then register items that might depend on blocks
		LOGGER.info("Registering items...");
		ModItems.registerItems();
		// Then register dimensions and related systems
		LOGGER.info("Registering dimensions...");
		ModDimensions.register();
		PocketDimensionsRegistry.init();
		// Register network packets
		LOGGER.info("Registering network packets...");

		// Register server-side receiver
//		ServerPlayNetworking.registerGlobalReceiver(SpawnRulesPacket.ID, (packet, context) -> {
//			// Handle the packet on the server side if needed
//			LOGGER.debug("Received spawn rules packet for dimension {}", packet.dimensionIndex());
//		});
		// Finally register data loaders
		LOGGER.info("Registering data loaders...");
		DataLoader.register();
		LOGGER.info("Pocket Portals mod initialized!");
	}
}