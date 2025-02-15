package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import ninja.trek.pocketportals.PocketPortals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GridBiomeSource extends BiomeSource {
    private record Parameters(long seed) {}
    private final Parameters parameters;

    public static final MapCodec<GridBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").stable().forGetter(source -> source.parameters.seed())
            ).apply(instance, seed -> new GridBiomeSource(new Parameters(seed))));

    private List<RegistryKey<Biome>> biomeKeys;
    private Registry<Biome> biomeRegistry;

    public GridBiomeSource(Parameters parameters) {
        this.parameters = parameters;
        this.biomeKeys = new ArrayList<>();
        // Initialize the biome registry from the dynamic registry
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            setBiomeRegistry(server.getRegistryManager().get(RegistryKeys.BIOME));
        });
    }

    public void setBiomeRegistry(Registry<Biome> registry) {
        this.biomeRegistry = registry;
        populateBiomeList();
    }

    private void populateBiomeList() {
        biomeKeys.clear();
        // Add all biomes from the registry that are valid overworld biomes
        biomeRegistry.getKeys().forEach(key -> {
            if (isValidOverworldBiome(key)) {
                biomeKeys.add(key);
            }
        });
        if (biomeKeys.isEmpty()) {
            PocketPortals.LOGGER.warn("No valid overworld biomes found, falling back to plains");
            biomeKeys.add(RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains")));
        } else {
            PocketPortals.LOGGER.info("Loaded {} overworld biomes for pocket dimensions", biomeKeys.size());
        }
    }

    private boolean isValidOverworldBiome(RegistryKey<Biome> key) {
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
        return CODEC;
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
            return biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME,
                    Identifier.of("minecraft", "plains"))).orElseThrow();
        }

        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);
        // Use seed from parameters
        var random = new java.util.Random(parameters.seed() ^ ((long)gridX << 32 | (long)gridZ));
        RegistryKey<Biome> biomeKey = biomeKeys.get(random.nextInt(biomeKeys.size()));
        return biomeRegistry.getEntry(biomeKey).orElseThrow(() ->
                new RuntimeException("Could not find biome for key: " + biomeKey.getValue()));
    }
}