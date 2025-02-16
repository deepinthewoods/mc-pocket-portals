package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import ninja.trek.pocketportals.PocketPortals;

public class ModDimensions {
    public static final int GRID_SPACING = 100_000; // Blocks between each portal location
    public static final int GRID_SIZE = 600; // 600x600 grid

    public static void register() {
        // Register chunk generator - this works fine
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(PocketPortals.MOD_ID, "sky_island"),
                SkyIslandChunkGenerator.CODEC
        );

        // Get the proper registry type for biome source
        Registry<MapCodec<? extends BiomeSource>> biomeSourceRegistry =
                (Registry<MapCodec<? extends BiomeSource>>)(Registry<?>) Registries.BIOME_SOURCE;

        // Convert GridBiomeSource.CODEC to MapCodec
        MapCodec<GridBiomeSource> biomeSourceMapCodec = GridBiomeSource.CODEC;

        Registry.register(
                biomeSourceRegistry,
                Identifier.of(PocketPortals.MOD_ID, "grid_biome_source"),
                biomeSourceMapCodec
        );
    }

    /**
     * Convert a dimension index (0-359999) to grid coordinates
     */
    public static GridPosition indexToGridPosition(int index) {
        if (index < 0 || index >= GRID_SIZE * GRID_SIZE) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        int x = index % GRID_SIZE;
        int z = index / GRID_SIZE;
        return new GridPosition(x, z);
    }

    /**
     * Convert grid coordinates to world coordinates
     */
    public static WorldPosition gridToWorldPosition(GridPosition grid) {
        // Convert grid coordinates to world coordinates, centered in each cell
        // Add 50,000 block offset to keep away from world border
        int worldX = (grid.x() - (GRID_SIZE / 2)) * GRID_SPACING + 50_000;
        int worldZ = (grid.z() - (GRID_SIZE / 2)) * GRID_SPACING + 50_000;
        return new WorldPosition(worldX, 64, worldZ); // Y=64 is base height
    }

    public record GridPosition(int x, int z) {}
    public record WorldPosition(int x, int y, int z) {}
}