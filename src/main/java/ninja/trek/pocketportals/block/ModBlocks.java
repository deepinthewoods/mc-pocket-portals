package ninja.trek.pocketportals.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public class ModBlocks {
    // --- Blocks ---
    public static Block POCKET_PORTAL;
    public static Block RETURN_POCKET_PORTAL;
    public static Block POCKET_PORTAL_FRAME;
    // --- Block Entities ---
    public static net.minecraft.block.entity.BlockEntityType<PocketPortalBlockEntity> POCKET_PORTAL_BLOCK_ENTITY;
    public static net.minecraft.block.entity.BlockEntityType<ReturnPocketPortalBlockEntity> RETURN_PORTAL_BLOCK_ENTITY;

    public static void registerBlocks() {
        POCKET_PORTAL = registerBlock("pocket_portal",
                AbstractBlock.Settings.create()
                        .strength(3.0f)
                        .requiresTool()
                        .nonOpaque());
        RETURN_POCKET_PORTAL = registerBlock("return_pocket_portal",
                AbstractBlock.Settings.create()
                        .strength(3.0f)
                        .requiresTool()
                        .nonOpaque());
        POCKET_PORTAL_FRAME = registerBlock("pocket_portal_frame",
                AbstractBlock.Settings.create()
                        .luminance(state -> 11)
                        .noCollision()
                        .strength(-1.0F));

        // Register block entities
        POCKET_PORTAL_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(PocketPortals.MOD_ID, "pocket_portal_entity"),
                FabricBlockEntityTypeBuilder.create(PocketPortalBlockEntity::new, POCKET_PORTAL).build()
        );
        RETURN_PORTAL_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(PocketPortals.MOD_ID, "return_portal_entity"),
                FabricBlockEntityTypeBuilder.create(ReturnPocketPortalBlockEntity::new, RETURN_POCKET_PORTAL).build()
        );
    }

    private static Block registerBlock(String name, AbstractBlock.Settings settings) {
        Identifier id = Identifier.of(PocketPortals.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);
        // Always attach the registry key to the settings!
        PocketPortals.LOGGER.info("Registering block {} with registry key {}", name, key);
        AbstractBlock.Settings settingsWithKey = settings.registryKey(key);
        Block block = createBlock(name, settingsWithKey);
        return Registry.register(Registries.BLOCK, key, block);
    }

    private static Block createBlock(String name, AbstractBlock.Settings settings) {
        return switch (name) {
            case "pocket_portal" -> new PocketPortalBlock(settings);
            case "return_pocket_portal" -> new ReturnPocketPortalBlock(settings);
            case "pocket_portal_frame" -> new PocketPortalFrame(settings);
            default -> throw new IllegalArgumentException("Unknown block: " + name);
        };
    }
}
