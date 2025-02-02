package ninja.trek.pocketportals.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PocketDimensionChunkGenerator extends ChunkGenerator {
    private record GeneratorSettings(boolean generating, int islandSize, int baseHeight) {
        public static final MapCodec<GeneratorSettings> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("generating").forGetter(GeneratorSettings::generating),
                        Codec.INT.fieldOf("island_size").forGetter(GeneratorSettings::islandSize),
                        Codec.INT.fieldOf("base_height").forGetter(GeneratorSettings::baseHeight)
                ).apply(instance, GeneratorSettings::new)
        );
    }

    private final GeneratorSettings settings;
    private final long seed;

    public static final MapCodec<PocketDimensionChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed),
                    GeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.settings)
            ).apply(instance, PocketDimensionChunkGenerator::new)
    );

    public PocketDimensionChunkGenerator(BiomeSource biomeSource, long seed, GeneratorSettings settings) {
        super(biomeSource);
        this.seed = seed;
        this.settings = settings;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {

    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        if (!settings.generating()) return;

        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;

                double distance = Math.sqrt(worldX * worldX + worldZ * worldZ);
                if (distance <= settings.islandSize()) {
                    int height = settings.baseHeight() + (int)(5 * Math.cos(distance / 10));

                    for (int y = settings.baseHeight() - 5; y < height; y++) {
                        mutable.set(worldX, y, worldZ);

                        if (y == height - 1) {
                            region.setBlockState(mutable, Blocks.GRASS_BLOCK.getDefaultState(), 3);
                        } else {
                            region.setBlockState(mutable, Blocks.DIRT.getDefaultState(), 3);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getWorldHeight() {
        return 0;
    }


    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        double distance = Math.sqrt(x * x + z * z);
        if (distance <= settings.islandSize()) {
            return settings.baseHeight() + (int)(5 * Math.cos(distance / 10));
        }
        return world.getBottomY();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[world.getHeight()];
        int baseY = world.getBottomY();

        double distance = Math.sqrt(x * x + z * z);
        if (distance <= settings.islandSize()) {
            int height = settings.baseHeight() + (int)(5 * Math.cos(distance / 10));

            for (int y = 0; y < world.getHeight(); y++) {
                int worldY = baseY + y;
                if (worldY < height - 1) {
                    states[y] = Blocks.DIRT.getDefaultState();
                } else if (worldY == height - 1) {
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

        return new VerticalBlockSample(baseY, states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Pocket Dimension Generator");
        text.add(String.format("Generating: %s", settings.generating()));
        text.add(String.format("Island Size: %d", settings.islandSize()));
        text.add(String.format("Base Height: %d", settings.baseHeight()));
    }
}