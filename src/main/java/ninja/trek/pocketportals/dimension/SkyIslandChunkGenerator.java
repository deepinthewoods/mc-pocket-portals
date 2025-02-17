package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkyIslandChunkGenerator extends ChunkGenerator {
    private static final int ISLAND_RADIUS = 15;
    private static final int BASE_HEIGHT = 64;

    public static final MapCodec<SkyIslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
            ).apply(instance, SkyIslandChunkGenerator::new));

    private final RegistryEntry<ChunkGeneratorSettings> settings;

    public SkyIslandChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk) {

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
                    int height = (int)(Math.sqrt(ISLAND_RADIUS * ISLAND_RADIUS - distanceSquared));

                    // Generate the island core
                    for (int y = BASE_HEIGHT - height; y <= BASE_HEIGHT + height; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z),
                                Blocks.STONE.getDefaultState(),
                                false);
                    }

                    // Add dirt and grass on top
                    int topY = BASE_HEIGHT + height;
                    chunk.setBlockState(new BlockPos(x, topY, z),
                            Blocks.DIRT.getDefaultState(),
                            false);
                    chunk.setBlockState(new BlockPos(x, topY + 1, z),
                            Blocks.GRASS_BLOCK.getDefaultState(),
                            false);
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

                // Only generate within island radius (slightly smaller than terrain radius)
                if (distanceSquared <= (ISLAND_RADIUS - 2) * (ISLAND_RADIUS - 2)) {
                    int y = region.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                    pos.set(worldX, y, worldZ);

                    // Get biome and features for this position
                    RegistryEntry<Biome> biome = region.getBiome(pos);
                    var features = biome.value().getGenerationSettings().getFeatures();

                    // Try to generate features at this position
                    for (int step = 0; step < features.size(); step++) {
                        RegistryEntryList<PlacedFeature> stepFeatures = features.get(step);
                        for (RegistryEntry<PlacedFeature> feature : stepFeatures) {
                            // Adjust probability based on feature type
                            float chance = step == 0 ? 0.05f : 0.1f; // Lower chance for trees
                            if (random.nextFloat() < chance) {
                                try {
                                    feature.value().generateUnregistered(
                                            region,
                                            this,
                                            random,
                                            pos
                                    );
                                } catch (Exception e) {
                                    PocketPortals.LOGGER.error(
                                            "Error placing feature at {}: {}",
                                            pos,
                                            e.getMessage()
                                    );
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
            int height = (int)(Math.sqrt(ISLAND_RADIUS * ISLAND_RADIUS - distanceSquared));
            return BASE_HEIGHT + height + 1; // +1 for grass block
        }
        return world.getBottomY();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[world.getHeight()];

        // Fill with air by default
        for (int y = 0; y < world.getHeight(); y++) {
            states[y] = Blocks.AIR.getDefaultState();
        }

        // Calculate if this column is part of an island
        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        double dx = x - centerX;
        double dz = z - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        if (distanceSquared <= ISLAND_RADIUS * ISLAND_RADIUS) {
            int height = (int)(Math.sqrt(ISLAND_RADIUS * ISLAND_RADIUS - distanceSquared));

            // Fill in the island structure
            for (int y = BASE_HEIGHT - height; y <= BASE_HEIGHT + height; y++) {
                int idx = y - world.getBottomY();
                if (idx >= 0 && idx < states.length) {
                    if (y == BASE_HEIGHT + height) {
                        states[idx] = Blocks.GRASS_BLOCK.getDefaultState();
                    } else if (y >= BASE_HEIGHT + height - 3) {
                        states[idx] = Blocks.DIRT.getDefaultState();
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
    }
}