package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.noise.NoiseConfig;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.block.ModBlocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkyIslandChunkGenerator extends ChunkGenerator {
    private static final int ISLAND_RADIUS = 32; // Increased from 15
    private static final int BASE_HEIGHT = 64;
    private static final int ISLAND_HEIGHT = 12; // Reduced for flatter islands
    private static final double NOISE_SCALE = 0.1; // Controls the "bumpiness" of the terrain
    private static final double NOISE_AMPLITUDE = 4; // How much the noise affects height

    private PerlinNoiseSampler noiseSampler;

    public static final MapCodec<SkyIslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
            ).apply(instance, SkyIslandChunkGenerator::new));

    private final RegistryEntry<ChunkGeneratorSettings> settings;

    public SkyIslandChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        // Initialize noise sampler with a random seed
        this.noiseSampler = new PerlinNoiseSampler(Random.create(1234));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk) {
        // No carving needed
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;

        // Calculate grid position
        int gridX = Math.floorDiv(chunkX, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(chunkZ, ModDimensions.GRID_SPACING);

        // Calculate center of current grid cell
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        // For each block in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;

                // Calculate distance from center
                double dx = worldX - centerX;
                double dz = worldZ - centerZ;
                double distanceSquared = dx * dx + dz * dz;

                // If within radius, generate island
                if (distanceSquared <= ISLAND_RADIUS * ISLAND_RADIUS) {
                    // Calculate base height using smooth falloff
                    double normalizedDistance = Math.sqrt(distanceSquared) / ISLAND_RADIUS;
                    double falloff = 1 - Math.pow(normalizedDistance, 2);

                    // Add noise variation
                    double noise = noiseSampler.sample(
                            worldX * NOISE_SCALE,
                            0,
                            worldZ * NOISE_SCALE
                    ) * NOISE_AMPLITUDE;

                    // Calculate final height including noise
                    int height = (int) (ISLAND_HEIGHT * falloff + noise);

                    // Generate the island with more natural layers
                    for (int y = BASE_HEIGHT - height; y <= BASE_HEIGHT + height; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (y == BASE_HEIGHT + height) {
                            chunk.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), false);
                        } else if (y >= BASE_HEIGHT + height - 3) {
                            chunk.setBlockState(pos, Blocks.DIRT.getDefaultState(), false);
                        } else if (y >= BASE_HEIGHT + height - 5) {
                            chunk.setBlockState(pos,
                                    Random.create().nextBoolean() ?
                                            Blocks.DIRT.getDefaultState() :
                                            Blocks.STONE.getDefaultState(),
                                    false);
                        } else {
                            chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                        }
                    }


                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // Surface building handled in populateNoise
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        int chunkX = region.getCenterPos().x;
        int chunkZ = region.getCenterPos().z;
        Random random = Random.create(region.getSeed() + chunkX * 341873128712L + chunkZ * 132897987541L);

        // Calculate island center for this chunk's grid position
        int gridX = Math.floorDiv(chunkX * 16, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(chunkZ * 16, ModDimensions.GRID_SPACING);
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        // Get chunk bounds
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                double dx = worldX - centerX;
                double dz = worldZ - centerZ;
                double distanceSquared = dx * dx + dz * dz;

                // Only generate features within island radius (slightly smaller than terrain radius)
                if (distanceSquared <= (ISLAND_RADIUS - 4) * (ISLAND_RADIUS - 4)) {
                    int y = region.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                    pos.set(worldX, y, worldZ);

                    // Get biome and features for this position
                    RegistryEntry<Biome> biome = region.getBiome(pos);
                    var features = biome.value().getGenerationSettings().getFeatures();

                    // Generate features with adjusted probabilities
                    for (int step = 0; step < features.size(); step++) {
                        RegistryEntryList<PlacedFeature> stepFeatures = features.get(step);
                        for (RegistryEntry<PlacedFeature> feature : stepFeatures) {
                            // Adjust probability based on feature type and distance from center
                            float baseChance = step == 0 ? 0.2f : 0.2f;
                            float distanceMultiplier = (float) (1 - Math.sqrt(distanceSquared) / ISLAND_RADIUS);
                            if (random.nextFloat() < baseChance * distanceMultiplier) {
                                try {
                                    feature.value().generateUnregistered(region, this, random, pos);
                                } catch (Exception e) {
                                    PocketPortals.LOGGER.error(
                                            "Error placing feature at {}: {}",
                                            pos, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world,
                         NoiseConfig noiseConfig) {
        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        double dx = x - centerX;
        double dz = z - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        if (distanceSquared <= ISLAND_RADIUS * ISLAND_RADIUS) {
            double normalizedDistance = Math.sqrt(distanceSquared) / ISLAND_RADIUS;
            double falloff = 1 - Math.pow(normalizedDistance, 2);

            double noise = noiseSampler.sample(x * NOISE_SCALE, 0, z * NOISE_SCALE) * NOISE_AMPLITUDE;
            int height = (int) (ISLAND_HEIGHT * falloff + noise);

            return BASE_HEIGHT + height + 1;
        }
        return world.getBottomY();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[world.getHeight()];
        for (int y = 0; y < world.getHeight(); y++) {
            states[y] = Blocks.AIR.getDefaultState();
        }

        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        double dx = x - centerX;
        double dz = z - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        if (distanceSquared <= ISLAND_RADIUS * ISLAND_RADIUS) {
            double normalizedDistance = Math.sqrt(distanceSquared) / ISLAND_RADIUS;
            double falloff = 1 - Math.pow(normalizedDistance, 2);

            double noise = noiseSampler.sample(x * NOISE_SCALE, 0, z * NOISE_SCALE) * NOISE_AMPLITUDE;
            int height = (int) (ISLAND_HEIGHT * falloff + noise);

            for (int y = BASE_HEIGHT - height; y <= BASE_HEIGHT + height; y++) {
                int idx = y - world.getBottomY();
                if (idx >= 0 && idx < states.length) {
                    if (y == BASE_HEIGHT + height) {
                        states[idx] = Blocks.GRASS_BLOCK.getDefaultState();
                    } else if (y >= BASE_HEIGHT + height - 3) {
                        states[idx] = Blocks.DIRT.getDefaultState();
                    } else if (y >= BASE_HEIGHT + height - 5) {
                        states[idx] = Random.create().nextBoolean() ?
                                Blocks.DIRT.getDefaultState() :
                                Blocks.STONE.getDefaultState();
                    } else {
                        states[idx] = Blocks.STONE.getDefaultState();
                    }
                }
            }
        }

        return new VerticalBlockSample(world.getBottomY(), states);
    }

    @Override
    public int getWorldHeight() {
        return this.settings.value().generationShapeConfig().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinimumY() {
        return this.settings.value().generationShapeConfig().minimumY();
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        // Add debug information if needed
    }
}