package ninja.trek.pocketportals.dimension;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;

import java.util.concurrent.CompletableFuture;

public class PocketDimensionJsonProvider implements DataProvider {
    private final FabricDataOutput output;
    private static final long DIMENSION_SEED = 12345678L;

    public PocketDimensionJsonProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        return CompletableFuture.allOf(
                generateDimensionType(writer),
                generateDimension(writer)
        );
    }

    private CompletableFuture<?> generateDimensionType(DataWriter writer) {
        JsonObject typeJson = new JsonObject();
        typeJson.addProperty("ultrawarm", false);
        typeJson.addProperty("natural", true);
        typeJson.addProperty("coordinate_scale", 1.0);
        typeJson.addProperty("has_skylight", true);
        typeJson.addProperty("has_ceiling", false);
        typeJson.addProperty("ambient_light", 0.0);
        typeJson.addProperty("fixed_time", 6000L);
        typeJson.addProperty("piglin_safe", false);
        typeJson.addProperty("bed_works", true);
        typeJson.addProperty("respawn_anchor_works", false);
        typeJson.addProperty("has_raids", false);
        typeJson.addProperty("logical_height", 384);
        typeJson.addProperty("min_y", -64);
        typeJson.addProperty("height", 384);
        typeJson.addProperty("infiniburn", "#minecraft:infiniburn_overworld");
        typeJson.addProperty("effects", "minecraft:overworld");
        typeJson.addProperty("monster_spawn_light_level", 0);
        typeJson.addProperty("monster_spawn_block_light_limit", 0);

        return DataProvider.writeToPath(
                writer,
                typeJson,
                output.getPath()
                        .resolve("data")
                        .resolve(PocketPortals.MOD_ID)
                        .resolve("dimension_type")
                        .resolve("pocket_dimension_type.json")
        );
    }

    private CompletableFuture<?> generateDimension(DataWriter writer) {
        JsonObject dimensionJson = new JsonObject();
        dimensionJson.addProperty("type", PocketPortals.MOD_ID + ":pocket_dimension_type");

        JsonObject generator = new JsonObject();
        generator.addProperty("type", PocketPortals.MOD_ID + ":sky_island");
        generator.addProperty("seed", DIMENSION_SEED);

        // Add generator settings reference
        generator.addProperty("settings", "minecraft:overworld");

        JsonObject biomeSource = new JsonObject();
        biomeSource.addProperty("type", PocketPortals.MOD_ID + ":grid_biome_source");
        biomeSource.addProperty("seed", DIMENSION_SEED);
        generator.add("biome_source", biomeSource);

        dimensionJson.add("generator", generator);

        return DataProvider.writeToPath(
                writer,
                dimensionJson,
                output.getPath()
                        .resolve("data")
                        .resolve(PocketPortals.MOD_ID)
                        .resolve("dimension")
                        .resolve("pocket_dimension.json")
        );
    }

    @Override
    public String getName() {
        return "Pocket Dimension Configurations";
    }
}