package ninja.trek.pocketportals.data;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;

public class DataLoader {
    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(PocketPortals.MOD_ID, "dimension_loader");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        // Check for dimension type file
                        Identifier dimTypeId = Identifier.of(PocketPortals.MOD_ID, "dimension_type/pocket_dimension_type.json");
                        if (manager.getResource(dimTypeId).isPresent()) {
                            PocketPortals.LOGGER.info("Found pocket dimension type configuration");
                        } else {
                            PocketPortals.LOGGER.error("Missing pocket dimension type configuration at {}", dimTypeId);
                        }

                        // Check for dimension file
                        Identifier dimId = Identifier.of(PocketPortals.MOD_ID, "dimension/pocket_dimension.json");
                        if (manager.getResource(dimId).isPresent()) {
                            PocketPortals.LOGGER.info("Found pocket dimension configuration");
                        } else {
                            PocketPortals.LOGGER.error("Missing pocket dimension configuration at {}", dimId);
                        }
                    }
                }
        );
    }
}