package ninja.trek.pocketportals.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;

public class ModBlocks {
    // --- Block Identifiers and Registry Keys ---
    public static final Identifier POCKET_PORTAL_ID = Identifier.of(PocketPortals.MOD_ID, "pocket_portal");
    public static final RegistryKey<Block> POCKET_PORTAL_KEY = RegistryKey.of(RegistryKeys.BLOCK, POCKET_PORTAL_ID);

    public static final Identifier RETURN_POCKET_PORTAL_ID = Identifier.of(PocketPortals.MOD_ID, "return_pocket_portal");
    public static final RegistryKey<Block> RETURN_POCKET_PORTAL_KEY = RegistryKey.of(RegistryKeys.BLOCK, RETURN_POCKET_PORTAL_ID);

    public static final Identifier POCKET_PORTAL_FRAME_ID = Identifier.of(PocketPortals.MOD_ID, "pocket_portal_frame");
    public static final RegistryKey<Block> POCKET_PORTAL_FRAME_KEY = RegistryKey.of(RegistryKeys.BLOCK, POCKET_PORTAL_FRAME_ID);

    // --- Block Entity Identifiers ---
    public static final Identifier POCKET_PORTAL_ENTITY_ID = Identifier.of(PocketPortals.MOD_ID, "pocket_portal_entity");
    public static final Identifier RETURN_PORTAL_ENTITY_ID = Identifier.of(PocketPortals.MOD_ID, "return_portal_entity");

    // --- Blocks ---
    public static final PocketPortalBlock POCKET_PORTAL = new PocketPortalBlock(
            AbstractBlock.Settings.create()

                    .strength(3.0f)
                    .requiresTool()
                    .nonOpaque()
                    .registryKey(POCKET_PORTAL_KEY)
    );

    public static final ReturnPocketPortalBlock RETURN_POCKET_PORTAL = new ReturnPocketPortalBlock(
            AbstractBlock.Settings.create()

                    .strength(3.0f)
                    .requiresTool()
                    .nonOpaque()
                    .registryKey(RETURN_POCKET_PORTAL_KEY)
    );

    public static final PocketPortalFrame POCKET_PORTAL_FRAME = new PocketPortalFrame(
            AbstractBlock.Settings.create()

                    .luminance(state -> 11)
                    .noCollision()
                    .strength(-1.0F)
                    .registryKey(POCKET_PORTAL_FRAME_KEY)
    );

    // --- Block Entities ---
    public static final BlockEntityType<PocketPortalBlockEntity> POCKET_PORTAL_BLOCK_ENTITY =
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                    PocketPortalBlockEntity::new,
                    POCKET_PORTAL
            ).build();

    public static final BlockEntityType<ReturnPocketPortalBlockEntity> RETURN_PORTAL_BLOCK_ENTITY =
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                    ReturnPocketPortalBlockEntity::new,
                    RETURN_POCKET_PORTAL
            ).build();

    // --- Registration ---
    public static void registerBlocks() {
        // Register blocks using their registry keys
        Registry.register(Registries.BLOCK, POCKET_PORTAL_KEY.getValue(), POCKET_PORTAL);
        Registry.register(Registries.BLOCK, RETURN_POCKET_PORTAL_KEY.getValue(), RETURN_POCKET_PORTAL);
        Registry.register(Registries.BLOCK, POCKET_PORTAL_FRAME_KEY.getValue(), POCKET_PORTAL_FRAME);

        // Register block entities using explicit identifiers
        Registry.register(Registries.BLOCK_ENTITY_TYPE, POCKET_PORTAL_ENTITY_ID, POCKET_PORTAL_BLOCK_ENTITY);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, RETURN_PORTAL_ENTITY_ID, RETURN_PORTAL_BLOCK_ENTITY);
    }
}
