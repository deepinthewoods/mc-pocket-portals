package ninja.trek.pocketportals.dimension;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;

public class PocketDimensionsRegistry extends PersistentState {
    private static final String NEXT_INDEX_KEY = "NextPortalIndex";
    private static int nextAvailableIndex = 0;

    private static final Type<PocketDimensionsRegistry> TYPE = new Type<>(
            PocketDimensionsRegistry::new,
            (nbt, lookup) -> createFromNbt(nbt, lookup),
            null // No datafixer needed
    );

    private static final RegistryKey<World> DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(PocketPortals.MOD_ID, "pocket_dimension")
    );

    public PocketDimensionsRegistry() {
        super();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt(NEXT_INDEX_KEY, nextAvailableIndex);
        return nbt;
    }

    public static PocketDimensionsRegistry createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PocketDimensionsRegistry state = new PocketDimensionsRegistry();
        nextAvailableIndex = nbt.getInt(NEXT_INDEX_KEY);
        return state;
    }



    private static PocketDimensionsRegistry getState(MinecraftServer server) {
        if (server.getOverworld() == null) {
            PocketPortals.LOGGER.warn("Tried to access persistent state before overworld was loaded!");
            return null;
        }
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(TYPE, PocketPortals.MOD_ID + "_registry");
    }

    public static void init() {
        // Register server STARTED event instead of STARTING
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            getState(server); // This will create or load the state
        });
    }

    public static RegistryKey<World> getDimensionKey() {
        return DIMENSION_KEY;
    }

    public static synchronized int findUnusedSlot(MinecraftServer server) {
        PocketDimensionsRegistry state = getState(server);
        if (state == null) {
            // If state isn't available yet, just return nextAvailableIndex
            // It will be saved once the state is properly loaded
            int index = nextAvailableIndex;
            nextAvailableIndex = (nextAvailableIndex + 1) % (ModDimensions.GRID_SIZE * ModDimensions.GRID_SIZE);
            return index;
        }

        // Get next available index, starting from 0
        int nextIndex = nextAvailableIndex;
        // Increment for next time, wrapping around if we hit the limit
        nextAvailableIndex = (nextAvailableIndex + 1) % (ModDimensions.GRID_SIZE * ModDimensions.GRID_SIZE);
        // Save the state
        state.markDirty();
        return nextIndex;
    }
}