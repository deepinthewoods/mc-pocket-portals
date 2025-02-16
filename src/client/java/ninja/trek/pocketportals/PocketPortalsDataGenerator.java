package ninja.trek.pocketportals;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.block.Block;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;

import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.dimension.PocketDimensionJsonProvider;

import java.util.concurrent.CompletableFuture;

public class PocketPortalsDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {



        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) ->
                new PocketDimensionJsonProvider(output));

        // Add Language Provider
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) ->
                new FabricLanguageProvider(output, registriesFuture) {

                    @Override
                    public void generateTranslations(RegistryWrapper.WrapperLookup registries, TranslationBuilder translationBuilder) {
                        addIfPresent(translationBuilder, "pocket_portal", "Pocket Portal");
                        addIfPresent(translationBuilder, "return_pocket_portal", "Return Portal");
                        addIfPresent(translationBuilder, "pocket_portal_frame", "Portal Frame");
                    }

                    private void addIfPresent(TranslationBuilder translationBuilder, String blockId, String translation) {
                        Identifier id = Identifier.of(PocketPortals.MOD_ID, blockId);
                        if (Registries.BLOCK.containsId(id)) {  // Ensure block exists before adding translation
                            translationBuilder.add(Registries.BLOCK.get(id), translation);
                        } else {
                            PocketPortals.LOGGER.warn("Skipping translation for missing block: {}", blockId);
                        }
                    }


                });

        // Add Loot Table Provider with proper null checks
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) ->
                new FabricBlockLootTableProvider(output, registriesFuture) {
                    @Override
                    public void generate() {
                        if (ModBlocks.POCKET_PORTAL != null &&
                                Registries.BLOCK.getId(ModBlocks.POCKET_PORTAL).getNamespace().equals(PocketPortals.MOD_ID)) {
                            addDrop(ModBlocks.POCKET_PORTAL);
                        }

                        if (ModBlocks.RETURN_POCKET_PORTAL != null &&
                                Registries.BLOCK.getId(ModBlocks.RETURN_POCKET_PORTAL).getNamespace().equals(PocketPortals.MOD_ID)) {
                            addDrop(ModBlocks.RETURN_POCKET_PORTAL);
                        }

                        if (ModBlocks.POCKET_PORTAL_FRAME != null &&
                                Registries.BLOCK.getId(ModBlocks.POCKET_PORTAL_FRAME).getNamespace().equals(PocketPortals.MOD_ID)) {
                            addDrop(ModBlocks.POCKET_PORTAL_FRAME);
                        }
                    }
                });

        // Add Model Provider
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) ->
                new FabricModelProvider(output) {
                    @Override
                    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
                        if (ModBlocks.POCKET_PORTAL != null) {
                            blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.POCKET_PORTAL);
                        }
                        if (ModBlocks.RETURN_POCKET_PORTAL != null) {
                            blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RETURN_POCKET_PORTAL);
                        }
                        if (ModBlocks.POCKET_PORTAL_FRAME != null) {
                            blockStateModelGenerator.registerSimpleState(ModBlocks.POCKET_PORTAL_FRAME);
                        }
                    }

                    @Override
                    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
                        // Add item models if needed
                    }
                });
    }
}