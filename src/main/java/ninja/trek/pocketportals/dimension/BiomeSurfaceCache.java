package ninja.trek.pocketportals.dimension;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import ninja.trek.pocketportals.PocketPortals;

import java.util.HashMap;
import java.util.Map;

public class BiomeSurfaceCache {
    private static final Map<Identifier, BlockState> BIOME_TOP_BLOCKS = new HashMap<>();
    private static final Map<Identifier, BlockState> BIOME_UNDER_BLOCKS = new HashMap<>();

    // Default blocks for unknown biomes
    private static final BlockState DEFAULT_TOP_BLOCK = Blocks.GRASS_BLOCK.getDefaultState();
    private static final BlockState DEFAULT_UNDER_BLOCK = Blocks.DIRT.getDefaultState();

    /**
     * Gets the appropriate top block for a biome based on its characteristics.
     * Uses biome temperature and downfall to determine appropriate surface blocks.
     */
    public static BlockState getTopBlock(Identifier biomeId) {
        return BIOME_TOP_BLOCKS.computeIfAbsent(biomeId, id -> {
            String path = id.getPath();

            // Desert-like biomes
            if (path.contains("desert") || path.contains("badlands") || path.contains("mesa")) {
                return Blocks.SAND.getDefaultState();
            }

            // Snowy biomes
            if (path.contains("snowy") || path.contains("frozen") || path.contains("ice")) {
                return Blocks.SNOW_BLOCK.getDefaultState();
            }

            // Beach biomes
            if (path.contains("beach")) {
                return Blocks.SAND.getDefaultState();
            }

            // Mushroom biomes
            if (path.contains("mushroom")) {
                return Blocks.MYCELIUM.getDefaultState();
            }

            // Stone-based biomes
            if (path.contains("stone") || path.contains("mountain") || path.contains("peak")) {
                return Blocks.STONE.getDefaultState();
            }

            // Swamp biomes
            if (path.contains("swamp") || path.contains("marsh")) {
                return Blocks.GRASS_BLOCK.getDefaultState();
            }

            // Savanna biomes
            if (path.contains("savanna")) {
                return Blocks.GRASS_BLOCK.getDefaultState();
            }

            // Default to grass for all other biomes
            return DEFAULT_TOP_BLOCK;
        });
    }

    /**
     * Gets the appropriate under block for a biome.
     * This is usually dirt, but can be different for special biomes.
     */
    public static BlockState getUnderBlock(Identifier biomeId) {
        return BIOME_UNDER_BLOCKS.computeIfAbsent(biomeId, id -> {
            String path = id.getPath();

            // Desert/Mesa biomes
            if (path.contains("desert") || path.contains("badlands") || path.contains("mesa")) {
                return Blocks.SANDSTONE.getDefaultState();
            }

            // Beach biomes
            if (path.contains("beach")) {
                return Blocks.SANDSTONE.getDefaultState();
            }

            // Stone-based biomes
            if (path.contains("stone") || path.contains("mountain") || path.contains("peak")) {
                return Blocks.STONE.getDefaultState();
            }

            // Default to dirt for all other biomes
            return DEFAULT_UNDER_BLOCK;
        });
    }

    /**
     * Clears the cache. Useful for reload events or testing.
     */
    public static void clearCache() {
        BIOME_TOP_BLOCKS.clear();
        BIOME_UNDER_BLOCKS.clear();
    }

    /**
     * Gets the number of cached biome surfaces. Useful for debugging.
     */
    public static int getCacheSize() {
        return BIOME_TOP_BLOCKS.size();
    }

    // Prevent instantiation
    private BiomeSurfaceCache() {}
}