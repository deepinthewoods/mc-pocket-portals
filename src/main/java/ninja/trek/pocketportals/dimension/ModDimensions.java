package ninja.trek.pocketportals.dimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import ninja.trek.pocketportals.PocketPortals;

import java.util.HashMap;
import java.util.Map;

public class ModDimensions {
    public static final RegistryKey<World> POCKET_DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(PocketPortals.MOD_ID, "pocket_dimension")
    );

    public static final RegistryKey<DimensionType> POCKET_DIMENSION_TYPE_KEY = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            Identifier.of(PocketPortals.MOD_ID, "pocket_dimension_type")
    );

    // Cache for dimension seeds
    private static final Map<Long, ServerWorld> DIMENSION_CACHE = new HashMap<>();

    public static void register() {
        // Register chunk generator
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(PocketPortals.MOD_ID, "pocket_dimension"),
                PocketDimensionChunkGenerator.CODEC
        );

        // Server starting event - initialize dimension systems
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PocketPortals.LOGGER.info("Initializing pocket dimension system");
            initializeDimensionSystem(server);
        });

        // Server started event - verify dimensions
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            verifyDimensionAccess(server);
        });

        // Server stopping event - cleanup
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PocketPortals.LOGGER.info("Cleaning up pocket dimension system");
            DIMENSION_CACHE.clear();
        });

        // Server tick event - handle dimension maintenance
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            maintainDimensions(server);
        });
    }

    private static void initializeDimensionSystem(MinecraftServer server) {
        try {
            // Ensure registry entries are available

            if (!server.getWorldRegistryKeys().contains(POCKET_DIMENSION_KEY)) {
                PocketPortals.LOGGER.warn("Pocket dimension registry entry not found - this is expected on first load");
            }
        } catch (Exception e) {
            PocketPortals.LOGGER.error("Failed to initialize dimension system", e);
        }
    }

    private static void verifyDimensionAccess(MinecraftServer server) {
        try {
            // Check if we can access the dimension type
            if (server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).containsId(POCKET_DIMENSION_TYPE_KEY.getValue())) {
                PocketPortals.LOGGER.info("Pocket dimension type verified");
            } else {
                PocketPortals.LOGGER.warn("Pocket dimension type not found - check JSON configs");
            }

            // Try to access the dimension
            ServerWorld pocketWorld = server.getWorld(POCKET_DIMENSION_KEY);
            if (pocketWorld != null) {
                PocketPortals.LOGGER.info("Successfully verified pocket dimension access");
            } else {
                PocketPortals.LOGGER.warn("Pocket dimension not immediately available - this may be normal");
            }
        } catch (Exception e) {
            PocketPortals.LOGGER.error("Error during dimension verification", e);
        }
    }

    private static void maintainDimensions(MinecraftServer server) {
        // Clean up any stale dimension references
        DIMENSION_CACHE.entrySet().removeIf(entry ->
                !server.getWorldRegistryKeys().contains(POCKET_DIMENSION_KEY));
    }

    public static ServerWorld getOrCreatePocketDimension(MinecraftServer server, RegistryKey<World> key, long seed) {
        try {
            // First check the cache
            ServerWorld cachedWorld = DIMENSION_CACHE.get(seed);
            if (cachedWorld != null && cachedWorld.isClient()) {
                return cachedWorld;
            }

            // Then check if the dimension exists on the server
            ServerWorld existingWorld = server.getWorld(key);
            if (existingWorld != null) {
                DIMENSION_CACHE.put(seed, existingWorld);
                PocketPortals.LOGGER.info("Successfully accessed pocket dimension with seed: {}", seed);
                return existingWorld;
            }

            // Log detailed debug info
            PocketPortals.LOGGER.debug("Attempting to access dimension - Key: {}", key.getValue());
            PocketPortals.LOGGER.debug("Available dimensions: {}", server.getWorldRegistryKeys());
            PocketPortals.LOGGER.debug("Current seed: {}", seed);

            // Try one more time after logging
            ServerWorld world = server.getWorld(key);
            if (world != null) {
                DIMENSION_CACHE.put(seed, world);
            }
            return world;

        } catch (Exception e) {
            PocketPortals.LOGGER.error("Failed to access pocket dimension", e);
            e.printStackTrace();
        }
        return null;
    }
}