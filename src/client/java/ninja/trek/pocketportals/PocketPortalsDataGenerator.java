package ninja.trek.pocketportals;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.dimension.PocketDimensionJsonProvider;
import ninja.trek.pocketportals.item.ModItems;

import java.util.concurrent.CompletableFuture;
import net.minecraft.registry.RegistryWrapper;

public class PocketPortalsDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // Add Dimension Data Provider
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) ->
                new PocketDimensionJsonProvider(output));

        // Add Model Provider
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) ->
                new FabricModelProvider(output) {
                    @Override
                    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
                        // Create texture identifiers
                        Identifier portalTexture = Identifier.of(PocketPortals.MOD_ID, "block/pocket_portal");
                        Identifier returnPortalTexture = Identifier.of(PocketPortals.MOD_ID, "block/return_pocket_portal");
                        Identifier frameTexture = Identifier.of(PocketPortals.MOD_ID, "block/pocket_portal_frame");

                        // Create texture mappings
                        TextureMap portalTextures = new TextureMap()
                                .put(TextureKey.ALL, portalTexture);
                        TextureMap returnPortalTextures = new TextureMap()
                                .put(TextureKey.ALL, returnPortalTexture);
                        TextureMap frameTextures = new TextureMap()
                                .put(TextureKey.ALL, frameTexture);

                        // Generate base models
                        Identifier portalModel = Models.CUBE_ALL.upload(
                                ModBlocks.POCKET_PORTAL,
                                portalTextures,
                                blockStateModelGenerator.modelCollector
                        );
                        Identifier returnPortalModel = Models.CUBE_ALL.upload(
                                ModBlocks.RETURN_POCKET_PORTAL,
                                returnPortalTextures,
                                blockStateModelGenerator.modelCollector
                        );
                        Identifier frameModel = Models.CUBE_ALL.upload(
                                ModBlocks.POCKET_PORTAL_FRAME,
                                frameTextures,
                                blockStateModelGenerator.modelCollector
                        );

                        // Generate blockstates with variants
                        // Pocket Portal (with facing variants)
                        blockStateModelGenerator.blockStateCollector.accept(
                                VariantsBlockStateSupplier.create(ModBlocks.POCKET_PORTAL)
                                        .coordinate(BlockStateVariantMap.create(Properties.HORIZONTAL_FACING)
                                                .register(net.minecraft.util.math.Direction.NORTH, BlockStateVariant.create().put(VariantSettings.MODEL, portalModel))
                                                .register(net.minecraft.util.math.Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.MODEL, portalModel).put(VariantSettings.Y, VariantSettings.Rotation.R180))
                                                .register(net.minecraft.util.math.Direction.WEST, BlockStateVariant.create().put(VariantSettings.MODEL, portalModel).put(VariantSettings.Y, VariantSettings.Rotation.R270))
                                                .register(net.minecraft.util.math.Direction.EAST, BlockStateVariant.create().put(VariantSettings.MODEL, portalModel).put(VariantSettings.Y, VariantSettings.Rotation.R90)))
                        );

                        // Return Portal (simple block)
                        blockStateModelGenerator.blockStateCollector.accept(
                                BlockStateModelGenerator.createSingletonBlockState(
                                        ModBlocks.RETURN_POCKET_PORTAL,
                                        returnPortalModel
                                )
                        );

                        // Portal Frame (simple block)
                        blockStateModelGenerator.blockStateCollector.accept(
                                BlockStateModelGenerator.createSingletonBlockState(
                                        ModBlocks.POCKET_PORTAL_FRAME,
                                        frameModel
                                )
                        );
                    }

                    @Override
                    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
                        // Generate item models that reference block models
                        itemModelGenerator.register(ModItems.POCKET_PORTAL_ITEM, Models.GENERATED);
                    }
                });
    }
}