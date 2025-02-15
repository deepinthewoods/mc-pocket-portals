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


public class PocketPortalsDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // Models and Blockstates
        pack.addProvider((output, registriesLookup) -> new FabricModelProvider(output) {
            @Override
            public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
                // Create unique textures for each portal type
                Identifier portalTexture = Identifier.of(PocketPortals.MOD_ID, "block/pocket_portal");
                Identifier returnPortalTexture = Identifier.of(PocketPortals.MOD_ID, "block/return_portal");
                Identifier frameTexture = Identifier.of(PocketPortals.MOD_ID, "block/portal_frame");

                // Register block models using correct methods for 1.21
                blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.POCKET_PORTAL);
                blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RETURN_POCKET_PORTAL);
                blockStateModelGenerator.registerSimpleState(ModBlocks.POCKET_PORTAL_FRAME);
            }

            @Override
            public void generateItemModels(ItemModelGenerator itemModelGenerator) {
                // Generate item model for the portal item
                itemModelGenerator.register(ModItems.POCKET_PORTAL, Models.GENERATED);
            }
        });

        // Recipes
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
                        .criterion(FabricRecipeProvider.hasItem(Items.ENDER_PEARL),
                                FabricRecipeProvider.conditionsFromItem(Items.ENDER_PEARL))
                        .offerTo(exporter, Identifier.of(PocketPortals.MOD_ID, "pocket_portal"));
            }
        });

        // Loot Tables
        pack.addProvider((output, registriesLookup) -> new FabricBlockLootTableProvider(output, registriesLookup) {
            @Override
            public void generate() {
                // Use proper loot table generation methods for blocks
                this.addDrop(ModBlocks.POCKET_PORTAL, this.drops(ModBlocks.POCKET_PORTAL));
                this.addDrop(ModBlocks.RETURN_POCKET_PORTAL, this.drops(ModBlocks.RETURN_POCKET_PORTAL));
                this.addDrop(ModBlocks.POCKET_PORTAL_FRAME, this.drops(ModBlocks.POCKET_PORTAL_FRAME));
            }
        });

        // Language Provider
        pack.addProvider((output, registriesLookup) -> new FabricLanguageProvider(output, registriesLookup) {
            @Override
            public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder translationBuilder) {
                // Register translations using proper method
                translationBuilder.add(ModBlocks.POCKET_PORTAL, "Pocket Portal");
                translationBuilder.add(ModBlocks.RETURN_POCKET_PORTAL, "Return Portal");
                translationBuilder.add(ModBlocks.POCKET_PORTAL_FRAME, "Portal Frame");
                translationBuilder.add("itemGroup." + PocketPortals.MOD_ID + ".main", "Pocket Portals");
            }
        });

        // Dimension configurations
        pack.addProvider(PocketDimensionJsonProvider::new);
    }
}