package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SkyIslandChunkGenerator extends ChunkGenerator {
    public static final MapCodec<SkyIslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
            ).apply(instance, SkyIslandChunkGenerator::new));

    private static final int BASE_HEIGHT = 64;
    private static final int ISLAND_RADIUS = 50;
    private static final double NOISE_SCALE = 0.05;

    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final NoiseChunkGenerator vanillaGenerator;

    public SkyIslandChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.vanillaGenerator = new NoiseChunkGenerator(biomeSource, settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }



    private boolean isInIslandRange(int worldX, int worldZ) {
        // Convert to grid coordinates
        int gridX = Math.floorDiv(worldX, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(worldZ, ModDimensions.GRID_SPACING);

        // Get center of current grid cell
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        // Calculate distance from center
        double dx = worldX - centerX;
        double dz = worldZ - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        return distanceSquared <= ISLAND_RADIUS * ISLAND_RADIUS;
    }

    private double getIslandNoise(int x, int z) {
        // Convert to grid coordinates
        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);

        // Get center of current grid cell
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        // Calculate normalized distance from center (0.0 to 1.0)
        double dx = x - centerX;
        double dz = z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double normalizedDist = Math.min(1.0, distance / ISLAND_RADIUS);

        // Create smooth falloff from center
        double falloff = 1.0 - (normalizedDist * normalizedDist); // Quadratic falloff

        // Add some variation using sine waves
        double variation = Math.sin(x * NOISE_SCALE) * Math.cos(z * NOISE_SCALE) * 0.2;

        return Math.max(0.0, falloff + variation);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        // First, let vanilla handle base generation
        return vanillaGenerator.populateNoise(blender, noiseConfig, structureAccessor, chunk).thenApply(populatedChunk -> {
            // Then modify it to create our island shape
            int chunkX = chunk.getPos().x * 16;
            int chunkZ = chunk.getPos().z * 16;

            BlockPos.Mutable mutable = new BlockPos.Mutable();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunkX + x;
                    int worldZ = chunkZ + z;

                    if (!isInIslandRange(worldX, worldZ)) {
                        // Outside island range - clear all blocks
                        for (int y = chunk.getBottomY(); y < chunk.getTopYInclusive(); y++) {
                            mutable.set(x, y, z);
                            chunk.setBlockState(mutable, Blocks.AIR.getDefaultState(), false);
                        }
                    } else {
                        // Inside island range - apply noise modification
                        double islandFactor = getIslandNoise(worldX, worldZ);
                        int currentHeight = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x, z);
                        int targetHeight = (int)(BASE_HEIGHT + (islandFactor * 30)); // 30 blocks of max variation

                        // Adjust terrain height
                        if (currentHeight > targetHeight) {
                            // Remove blocks above target height
                            for (int y = targetHeight + 1; y <= currentHeight; y++) {
                                mutable.set(x, y, z);
                                chunk.setBlockState(mutable, Blocks.AIR.getDefaultState(), false);
                            }
                        }
                    }
                }
            }

            return populatedChunk;
        });
    }


    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk) {
// Delegate to vanilla for carving
        vanillaGenerator.carve(chunkRegion, seed, noiseConfig, biomeAccess, structureAccessor, chunk);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Delegate to vanilla for surface building
        vanillaGenerator.buildSurface(region, structures, noiseConfig, chunk);
    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (!isInIslandRange(x, z)) {
            return world.getBottomY();
        }
        return vanillaGenerator.getHeight(x, z, heightmap, world, noiseConfig);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return vanillaGenerator.getColumnSample(x, z, world, noiseConfig);
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

    }


    @Override
    public int getWorldHeight() {
        return vanillaGenerator.getWorldHeight();
    }



    @Override
    public int getSeaLevel() {
        return -64; // No sea in sky islands
    }

    @Override
    public int getMinimumY() {
        return vanillaGenerator.getMinimumY();
    }
}