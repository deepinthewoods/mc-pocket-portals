package ninja.trek.pocketportals.dimension;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;

import java.util.HashMap;
import java.util.Map;

public class BiomeSurfaceCache {
    private static final Map<RegistryKey<?>, BlockState> BIOME_TOP_BLOCKS = new HashMap<>();
    private static final Map<RegistryKey<?>, BlockState> BIOME_UNDER_BLOCKS = new HashMap<>();

    static {
        // Plains-like biomes
        BIOME_TOP_BLOCKS.put(BiomeKeys.PLAINS, Blocks.GRASS_BLOCK.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.PLAINS, Blocks.DIRT.getDefaultState());

        // Desert biomes
        BIOME_TOP_BLOCKS.put(BiomeKeys.DESERT, Blocks.SAND.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.DESERT, Blocks.SANDSTONE.getDefaultState());

        // Forest biomes
        BIOME_TOP_BLOCKS.put(BiomeKeys.FOREST, Blocks.GRASS_BLOCK.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.FOREST, Blocks.DIRT.getDefaultState());

        // Snowy biomes
        BIOME_TOP_BLOCKS.put(BiomeKeys.SNOWY_PLAINS, Blocks.SNOW_BLOCK.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.SNOWY_PLAINS, Blocks.DIRT.getDefaultState());

        // Jungle biomes
        BIOME_TOP_BLOCKS.put(BiomeKeys.JUNGLE, Blocks.GRASS_BLOCK.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.JUNGLE, Blocks.DIRT.getDefaultState());

        // Mushroom biomes
        BIOME_TOP_BLOCKS.put(BiomeKeys.MUSHROOM_FIELDS, Blocks.MYCELIUM.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.MUSHROOM_FIELDS, Blocks.DIRT.getDefaultState());

        // Cherry grove
        BIOME_TOP_BLOCKS.put(BiomeKeys.CHERRY_GROVE, Blocks.GRASS_BLOCK.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.CHERRY_GROVE, Blocks.DIRT.getDefaultState());

        // Meadow
        BIOME_TOP_BLOCKS.put(BiomeKeys.MEADOW, Blocks.GRASS_BLOCK.getDefaultState());
        BIOME_UNDER_BLOCKS.put(BiomeKeys.MEADOW, Blocks.DIRT.getDefaultState());
    }

    public static BlockState getTopBlock(Identifier biomeId) {
        for (Map.Entry<RegistryKey<?>, BlockState> entry : BIOME_TOP_BLOCKS.entrySet()) {
            if (entry.getKey().getValue().equals(biomeId)) {
                return entry.getValue();
            }
        }
        return Blocks.GRASS_BLOCK.getDefaultState();
    }

    public static BlockState getUnderBlock(Identifier biomeId) {
        for (Map.Entry<RegistryKey<?>, BlockState> entry : BIOME_UNDER_BLOCKS.entrySet()) {
            if (entry.getKey().getValue().equals(biomeId)) {
                return entry.getValue();
            }
        }
        return Blocks.DIRT.getDefaultState();
    }
}