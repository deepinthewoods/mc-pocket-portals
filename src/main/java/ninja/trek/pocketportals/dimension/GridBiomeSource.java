package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import ninja.trek.pocketportals.PocketPortals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class GridBiomeSource extends BiomeSource {
    private final long seed;
    private Registry<Biome> biomeRegistry;
    private final List<RegistryEntry<Biome>> biomes;
    private RegistryEntry<Biome> fallbackBiome;

    public static final MapCodec<GridBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").stable().forGetter(source -> source.seed)
            ).apply(instance, GridBiomeSource::new));

    public GridBiomeSource(long seed) {
        this.seed = seed;
        this.biomes = new CopyOnWriteArrayList<>(); // Thread-safe list
    }

    @Override
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        initializeBiomesIfNeeded();
        if (biomes.isEmpty()) {
            return getFallbackBiome();
        }

        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);

        // Create deterministic random using grid coordinates and seed
        var random = new java.util.Random(seed ^ ((long)gridX << 32 | (long)gridZ));

        // Safely get a random biome
        int index = random.nextInt(biomes.size());
        return biomes.get(Math.min(index, biomes.size() - 1));
    }

    private RegistryEntry<Biome> getFallbackBiome() {
        if (fallbackBiome == null && biomeRegistry != null) {
            // Try to get plains biome
            var plainsId = BiomeKeys.PLAINS.getValue();
            if (biomeRegistry.containsId(plainsId)) {
                var plainsBiome = biomeRegistry.get(plainsId);
                if (plainsBiome != null) {
                    fallbackBiome = biomeRegistry.getEntry(plainsBiome);
                }
            }

            // If plains isn't available, use the first biome in the registry
            if (fallbackBiome == null) {
                var firstEntry = biomeRegistry.getEntrySet().stream().findFirst();
                if (firstEntry.isPresent()) {
                    fallbackBiome = biomeRegistry.getEntry(firstEntry.get().getValue());
                } else {
                    throw new IllegalStateException("No biomes available in registry");
                }
            }
        }

        if (fallbackBiome == null) {
            throw new IllegalStateException("Could not initialize fallback biome");
        }
        return fallbackBiome;
    }

    @Override
    public Stream<RegistryEntry<Biome>> biomeStream() {
        initializeBiomesIfNeeded();
        return biomes.stream();
    }

    private synchronized void initializeBiomesIfNeeded() {
        if (biomeRegistry == null || biomes.isEmpty()) {
            var server = PocketPortals.getServer();
            if (server == null) {
                PocketPortals.LOGGER.warn("Server not yet available for biome initialization");
                return;
            }

            var overworld = server.getOverworld();
            if (overworld == null) {
                PocketPortals.LOGGER.warn("Overworld not yet available for biome initialization");
                return;
            }

            biomeRegistry = overworld.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
            if (biomeRegistry != null) {
                populateBiomes();
            }
        }
    }

    private synchronized void populateBiomes() {
        if (biomeRegistry == null) return;

        biomes.clear();
        List<RegistryEntry<Biome>> tempBiomes = new ArrayList<>();

        // Add all valid overworld biomes to temporary list first
        biomeRegistry.getEntrySet().forEach(entry -> {
            if (isValidOverworldBiome(entry.getKey().getValue())) {
                tempBiomes.add(biomeRegistry.getEntry(entry.getValue()));
            }
        });

        // If no valid biomes found, add plains as fallback
        if (tempBiomes.isEmpty()) {
            PocketPortals.LOGGER.warn("No valid overworld biomes found, adding plains as fallback");
            var plainsId = BiomeKeys.PLAINS.getValue();
            if (biomeRegistry.containsId(plainsId)) {
                var plainsBiome = biomeRegistry.get(plainsId);
                if (plainsBiome != null) {
                    tempBiomes.add(biomeRegistry.getEntry(plainsBiome));
                }
            }
        }

        // Now safely add all biomes to our thread-safe list
        biomes.addAll(tempBiomes);

        PocketPortals.LOGGER.info("Initialized GridBiomeSource with {} biomes", biomes.size());
    }

    private boolean isValidOverworldBiome(Identifier id) {
        String path = id.getPath();
        return !path.contains("end") &&
                !path.contains("nether") &&
                !path.contains("basalt") &&
                !path.contains("void") &&
                !path.contains("small") &&
                !path.startsWith("debug_") &&
                !path.equals("custom");
    }
}