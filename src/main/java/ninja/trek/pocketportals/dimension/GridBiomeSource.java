package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import ninja.trek.pocketportals.PocketPortals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GridBiomeSource extends BiomeSource {
    private static final Codec<GridBiomeSource> GRID_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(source -> source.seed)
            ).apply(instance, GridBiomeSource::new));

    private final long seed;
    private List<RegistryKey<Biome>> biomeKeys;
    private Registry<Biome> biomeRegistry;

    public GridBiomeSource(long seed) {
        this.seed = seed;
        this.biomeKeys = new ArrayList<>();
    }

    public void setBiomeRegistry(Registry<Biome> registry) {
        this.biomeRegistry = registry;
        populateBiomeList();
    }

    private void populateBiomeList() {
        biomeKeys.clear();

        // Add all biomes from the registry that are valid overworld biomes
        biomeRegistry.getKeys().forEach(key -> {
            RegistryEntry<Biome> biomeEntry = biomeRegistry.getEntry(key).orElse(null);
            if (biomeEntry != null && isValidOverworldBiome(key)) {
                biomeKeys.add(key);
            }
        });

        if (biomeKeys.isEmpty()) {
            PocketPortals.LOGGER.warn("No valid overworld biomes found, falling back to plains");
            biomeKeys.add(BiomeKeys.PLAINS);
        } else {
            PocketPortals.LOGGER.info("Loaded {} overworld biomes for pocket dimensions", biomeKeys.size());
        }
    }

    private boolean isValidOverworldBiome(RegistryKey<Biome> key) {
        // Skip technical biomes and non-overworld biomes
        String path = key.getValue().getPath();
        return !path.contains("end") &&
                !path.contains("nether") &&
                !path.contains("basalt") &&
                !path.contains("void") &&
                !path.contains("small") &&
                !path.startsWith("debug_") &&
                !path.equals("custom");
    }

    @Override
    public MapCodec<? extends BiomeSource> getCodec() {
        return (MapCodec<? extends BiomeSource>) GRID_CODEC;
    }

    @Override
    public Stream<RegistryEntry<Biome>> biomeStream() {
        if (biomeRegistry == null) {
            return Stream.empty();
        }
        return biomeKeys.stream()
                .map(key -> biomeRegistry.getEntry(key))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        if (biomeRegistry == null || biomeKeys.isEmpty()) {
            // Default to plains if registry not set or no biomes loaded
            return biomeRegistry.getEntry(BiomeKeys.PLAINS).orElseThrow();
        }

        // Convert to grid coordinates
        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);

        // Create a random number generator using the grid position and seed
        var random = new java.util.Random(seed ^ ((long)gridX << 32 | (long)gridZ));

        // Pick a consistent biome key for this grid cell
        RegistryKey<Biome> biomeKey = biomeKeys.get(random.nextInt(biomeKeys.size()));

        // Get the actual biome entry from the registry
        return biomeRegistry.getEntry(biomeKey).orElseThrow(() ->
                new RuntimeException("Could not find biome for key: " + biomeKey.getValue()));
    }
}