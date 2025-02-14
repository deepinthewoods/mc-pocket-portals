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
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkyIslandChunkGenerator extends ChunkGenerator {
    // Constants for island generation
    private static final int BASE_HEIGHT = 64;
    private static final int ISLAND_RADIUS = 50;
    private static final int ISLAND_HEIGHT = 30;
    private static final double NOISE_SCALE = 0.05;

    public static final MapCodec<SkyIslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed)
            ).apply(instance, SkyIslandChunkGenerator::new));

    private final long seed;

    public SkyIslandChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.seed = seed;
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

        // Calculate distance from center of nearest island
        double dx = worldX - centerX;
        double dz = worldZ - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        return distanceSquared <= ISLAND_RADIUS * ISLAND_RADIUS;
    }

    private double getNoise(int x, int z) {
        double nx = x * NOISE_SCALE;
        double nz = z * NOISE_SCALE;
        return Math.sin(nx) * Math.cos(nz) * 5.0;
    }

    private int getIslandHeight(int x, int z) {
        if (!isInIslandRange(x, z)) {
            return -1; // Not on an island
        }

        // Get grid coordinates
        int gridX = Math.floorDiv(x, ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(z, ModDimensions.GRID_SPACING);

        // Get center of current grid cell
        int centerX = (gridX * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);
        int centerZ = (gridZ * ModDimensions.GRID_SPACING) + (ModDimensions.GRID_SPACING / 2);

        // Calculate distance from center and normalize
        double dx = x - centerX;
        double dz = z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double normalizedDist = distance / ISLAND_RADIUS;

        // Calculate height with smooth falloff and noise
        double falloff = 1.0 - (normalizedDist * normalizedDist); // Quadratic falloff
        double noiseHeight = getNoise(x, z);

        return BASE_HEIGHT + (int)(ISLAND_HEIGHT * falloff) + (int)noiseHeight;
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;
                int height = getIslandHeight(worldX, worldZ);

                if (height > 0) {
                    // Get biome for this position
                    RegistryEntry<Biome> biome = region.getBiome(mutable.set(worldX, height, worldZ));
                    BlockState topBlock = BiomeSurfaceCache.getTopBlock(biome.getKey().get().getValue());
                    BlockState underBlock = BiomeSurfaceCache.getUnderBlock(biome.getKey().get().getValue());

                    // Build the column
                    for (int y = height - 5; y <= height; y++) {
                        mutable.set(worldX, y, worldZ);
                        if (y == height) {
                            region.setBlockState(mutable, topBlock, 3);
                        } else {
                            region.setBlockState(mutable, underBlock, 3);
                        }
                    }
                }
            }
        }
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        int height = getIslandHeight(x, z);
        return height > 0 ? height : world.getBottomY();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int height = getIslandHeight(x, z);
        BlockState[] states = new BlockState[world.getHeight()];

        if (height > 0) {
            for (int y = 0; y < world.getHeight(); y++) {
                if (y < height - 5) {
                    states[y] = Blocks.AIR.getDefaultState();
                } else if (y < height) {
                    states[y] = Blocks.DIRT.getDefaultState();
                } else if (y == height) {
                    states[y] = Blocks.GRASS_BLOCK.getDefaultState();
                } else {
                    states[y] = Blocks.AIR.getDefaultState();
                }
            }
        } else {
            for (int y = 0; y < world.getHeight(); y++) {
                states[y] = Blocks.AIR.getDefaultState();
            }
        }

        return new VerticalBlockSample(world.getBottomY(), states);
    }

    // Required method implementations
    @Override
    public void carve(ChunkRegion region, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // No carving in sky islands
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Optional: Add entity spawning logic here
    }

    @Override
    public int getWorldHeight() {
        return 384; // Matches vanilla world height
    }

    @Override
    public int getSeaLevel() {
        return -64; // No sea in sky islands
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Sky Island Generator");
        text.add("Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        text.add("Island Height: " + getIslandHeight(pos.getX(), pos.getZ()));
    }
}