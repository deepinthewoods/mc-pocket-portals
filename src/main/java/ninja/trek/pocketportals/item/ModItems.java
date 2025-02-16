package ninja.trek.pocketportals.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;

public class ModItems {
    // Define the registry key
    private static final RegistryKey<Item> POCKET_PORTAL_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(PocketPortals.MOD_ID, "pocket_portal")
    );

    // Register a custom PocketPortalItem so we can store dimension index in its NBT
    public static final PocketPortalItem POCKET_PORTAL_ITEM;

    static {
        // Create settings with registry key
        Item.Settings settings = new Item.Settings()
                .maxCount(1) // non-stackable
                .useBlockPrefixedTranslationKey() // Use block.namespace.path format for translation
                .registryKey(POCKET_PORTAL_KEY);

        POCKET_PORTAL_ITEM = new PocketPortalItem(settings);
    }

    public static void registerItems() {
        // Register the item using the same key used in settings
        Registry.register(Registries.ITEM, POCKET_PORTAL_KEY, POCKET_PORTAL_ITEM);
    }
}