package ninja.trek.pocketportals.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;

public class ModItems {
    // Register a custom PocketPortalItem so we can store dimension index in its NBT
    public static final PocketPortalItem POCKET_PORTAL = new PocketPortalItem(
            new Item.Settings().maxCount(1) // non-stackable
    );

    public static void registerItems() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(PocketPortals.MOD_ID, "pocket_portal"),
                POCKET_PORTAL
        );
    }
}
