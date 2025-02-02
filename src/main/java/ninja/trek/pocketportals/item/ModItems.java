package ninja.trek.pocketportals.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.block.ModBlocks;


public class ModItems {
    public static final BlockItem POCKET_PORTAL = new BlockItem(
            ModBlocks.POCKET_PORTAL,
            new Item.Settings()
    );

    public static void registerItems() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(PocketPortals.MOD_ID, "pocket_portal"),
                POCKET_PORTAL
        );
    }
}