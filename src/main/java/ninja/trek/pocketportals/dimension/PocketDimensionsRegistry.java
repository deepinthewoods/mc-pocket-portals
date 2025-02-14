package ninja.trek.pocketportals.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import ninja.trek.pocketportals.PocketPortals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class PocketDimensionsRegistry {
    // We now only need one dimension
    private static final RegistryKey<World> DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(PocketPortals.MOD_ID, "pocket_dimension")
    );

    public static void init() {
        // No initialization needed anymore
    }

    public static RegistryKey<World> getDimensionKey() {
        return DIMENSION_KEY;
    }

    public static int findUnusedSlot() {
        // This could be enhanced to track used positions if needed
        // For now, just return the next available index
        return 0; // Implement your slot allocation strategy
    }
}
