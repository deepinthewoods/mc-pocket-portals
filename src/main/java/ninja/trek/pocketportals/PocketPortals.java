package ninja.trek.pocketportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.block.PocketPortalBlockEntity;
import ninja.trek.pocketportals.data.DataLoader;
import ninja.trek.pocketportals.dimension.ModDimensions;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.item.ModItems;
import ninja.trek.pocketportals.network.RequestSpawnRulesPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PocketPortals implements ModInitializer {
	public static final String MOD_ID = "pocketportals";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static MinecraftServer server;
	PayloadTypeRegistry<RegistryByteBuf> playB2SRegistry = PayloadTypeRegistry.playC2S();


	public static MinecraftServer getServer() {
		return server;
	}

	public static void setServer(MinecraftServer server) {
		PocketPortals.server = server;
	}

	@Override
	public void onInitialize() {
		// Register server lifecycle events
		ServerLifecycleEvents.SERVER_STARTING.register((MinecraftServer server) -> {
			PocketPortals.setServer(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
			PocketPortals.setServer(null);
		});


		// Register blocks and items
		if (System.getProperty("fabric.datagen") == null) {
			LOGGER.info("Registering blocks...");
			ModBlocks.registerBlocks();
		}
		LOGGER.info("Registering items...");
		ModItems.registerItems();

		// Register dimensions and related systems
		LOGGER.info("Registering dimensions...");
		ModDimensions.register();
		PocketDimensionsRegistry.init();

		playB2SRegistry.register(RequestSpawnRulesPacket.ID, RequestSpawnRulesPacket.CODEC);
		// Register network packet receiver for RequestSpawnRulesPacket
		LOGGER.info("Registering network packets...");
		ServerPlayNetworking.registerGlobalReceiver(
				RequestSpawnRulesPacket.ID,
				new ServerPlayNetworking.PlayPayloadHandler<RequestSpawnRulesPacket>() {
					@Override
					public void receive(RequestSpawnRulesPacket request, ServerPlayNetworking.Context context) {
						// Retrieve the BlockPos that was sent from the client
						BlockPos pos = request.pos();
						// Schedule our logic on the server thread

						context.server().execute(() -> {
							ServerPlayerEntity player = context.player();
							World world = player.getWorld();
							BlockPos basePos = findPortalBase(world, pos);
							if (basePos != null) {
								BlockEntity be = world.getBlockEntity(basePos);
								if (be instanceof PocketPortalBlockEntity portalBE) {
									portalBE.syncSpawnRules(player);
								}
							}
						});
					}
				}
		);

		// Register data loaders
		LOGGER.info("Registering data loaders...");
		DataLoader.register();

		LOGGER.info("Pocket Portals mod initialized!");
		// Removed previous chunk load event that sent SpawnRulesPacket.
	}

	/**
	 * Searches downward from the given position for a portal base block.
	 * Returns the position of the first found portal base (either pocket or return portal),
	 * or null if none is found.
	 */
	private static BlockPos findPortalBase(World world, BlockPos startPos) {
		BlockPos.Mutable mutablePos = startPos.mutableCopy();
		for (int y = 0; y > -3; y--) {
			mutablePos.setY(startPos.getY() + y);
			Block block = world.getBlockState(mutablePos).getBlock();
			if (block == ModBlocks.POCKET_PORTAL || block == ModBlocks.RETURN_POCKET_PORTAL) {
				return mutablePos.toImmutable();
			}
		}
		return null;
	}
}
