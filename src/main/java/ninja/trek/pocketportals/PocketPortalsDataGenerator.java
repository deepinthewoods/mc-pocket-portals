package ninja.trek.pocketportals;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.*;
import net.minecraft.data.client.*;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.dimension.PocketDimensionJsonProvider;
import ninja.trek.pocketportals.item.ModItems;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PocketPortalsDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // Models and Blockstates
        pack.addProvider((output, registriesLookup) -> new FabricModelProvider(output) {
            @Override
            public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
                // Register block models
                blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.POCKET_PORTAL);
                blockStateModelGenerator.registerSimpleState(ModBlocks.POCKET_PORTAL_FRAME);
            }

            @Override
            public void generateItemModels(ItemModelGenerator itemModelGenerator) {
                // Generate item model for the portal item using the block texture
                TextureMap portalTexture = new TextureMap()
                        .put(TextureKey.LAYER0, Identifier.of(PocketPortals.MOD_ID, "block/pocket_portal"));
                Model portalModel = new Model(Optional.empty(), Optional.empty(), TextureKey.LAYER0);
                itemModelGenerator.register(ModItems.POCKET_PORTAL, portalModel);
            }
        });

        // Rest of providers remain unchanged
        pack.addProvider((output, registriesLookup) -> new FabricRecipeProvider(output, registriesLookup) {
            @Override
            public void generate(RecipeExporter exporter) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.POCKET_PORTAL)
                        .pattern("OEO")
                        .pattern("EDE")
                        .pattern("OEO")
                        .input('O', Items.OBSIDIAN)
                        .input('E', Items.ENDER_PEARL)
                        .input('D', Items.DIAMOND)
                        .criterion(hasItem(Items.ENDER_PEARL), conditionsFromItem(Items.ENDER_PEARL))
                        .offerTo(exporter, Identifier.of(PocketPortals.MOD_ID, "pocket_portal"));
            }
        });

        pack.addProvider((output, registriesLookup) -> new FabricBlockLootTableProvider(output, registriesLookup) {
            @Override
            public void generate() {
                addDrop(ModBlocks.POCKET_PORTAL);
                addDrop(ModBlocks.POCKET_PORTAL_FRAME, drops(ModBlocks.POCKET_PORTAL_FRAME));
            }
        });

        pack.addProvider((output, registriesLookup) -> new FabricLanguageProvider(output, registriesLookup) {
            @Override
            public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, TranslationBuilder translationBuilder) {
                translationBuilder.add(ModBlocks.POCKET_PORTAL, "Pocket Portal");
                translationBuilder.add(ModBlocks.POCKET_PORTAL_FRAME, "Pocket Portal Frame");
                translationBuilder.add("itemGroup.pocket-portals.main", "Pocket Portals");
            }
        });

        pack.addProvider(PocketDimensionJsonProvider::new);
    }
}