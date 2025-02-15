package ninja.trek.pocketportals.dimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

public class PocketDimensionsRegistry extends PersistentState {
    private static final String NEXT_INDEX_KEY = "NextPortalIndex";
    private static final String SPAWN_RULES_KEY = "SpawnRules";
    private static int nextAvailableIndex = 0;
    private final Map<Integer, GridSpawnRules> spawnRules = new HashMap<>();

    // List of common hostile mobs that we'll manage spawn rules for
    public static final List<EntityType<?>> MANAGED_MOBS = Arrays.asList(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.PHANTOM,
            EntityType.SLIME
    );

    public static GridSpawnRules getSpawnRules(MinecraftServer server, int index) {
        PocketDimensionsRegistry state = getState(server);
        if (state == null) return null;

        // Get or create rules for this index
        GridSpawnRules rules = state.spawnRules.get(index);
        if (rules == null) {
            rules = new GridSpawnRules(index);
            setRandomSpawnRules(rules);
            state.spawnRules.put(index, rules);
            state.markDirty();
        }
        return rules;
    }

    private static final Type<PocketDimensionsRegistry> TYPE = new Type<>(
            PocketDimensionsRegistry::new,
            (nbt, lookup) -> createFromNbt(nbt, lookup),
            null
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
        NbtList rulesNbt = new NbtList();
        spawnRules.values().forEach(rules -> {
            rulesNbt.add(rules.writeNbt());
        });
        nbt.put(SPAWN_RULES_KEY, rulesNbt);
        return nbt;
    }

    public static PocketDimensionsRegistry createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PocketDimensionsRegistry state = new PocketDimensionsRegistry();
        nextAvailableIndex = nbt.getInt(NEXT_INDEX_KEY);
        if (nbt.contains(SPAWN_RULES_KEY)) {
            NbtList rulesNbt = nbt.getList(SPAWN_RULES_KEY, 10);
            rulesNbt.forEach(element -> {
                GridSpawnRules rules = GridSpawnRules.fromNbt((NbtCompound) element);
                state.spawnRules.put(rules.getGridIndex(), rules);
            });
        }
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

    private static void setRandomSpawnRules(GridSpawnRules rules) {
        Random random = new Random();
        int randomType = random.nextInt(4); // 0-3

        if (randomType == 0) {
            // 25% chance: Fully peaceful
            PocketPortals.LOGGER.info("Creating peaceful dimension space");
            MANAGED_MOBS.forEach(mob -> rules.setSpawnRule(mob, false));
        }
        else if (randomType == 1) {
            // 25% chance: Fully hostile
            PocketPortals.LOGGER.info("Creating hostile dimension space");
            MANAGED_MOBS.forEach(mob -> rules.setSpawnRule(mob, true));
        }
        else {
            // 50% chance: Random for each mob
            PocketPortals.LOGGER.info("Creating mixed dimension space");
            MANAGED_MOBS.forEach(mob -> {
                boolean allowed = random.nextBoolean();
                rules.setSpawnRule(mob, allowed);
                PocketPortals.LOGGER.debug("Set spawn rule for {}: {}", mob, allowed);
            });
        }
    }

    public static synchronized int findUnusedSlot(MinecraftServer server) {
        PocketDimensionsRegistry state = getState(server);
        if (state == null) {
            int index = nextAvailableIndex;
            nextAvailableIndex = (nextAvailableIndex + 1) % (ModDimensions.GRID_SIZE * ModDimensions.GRID_SIZE);
            return index;
        }

        int nextIndex = nextAvailableIndex;
        nextAvailableIndex = (nextAvailableIndex + 1) % (ModDimensions.GRID_SIZE * ModDimensions.GRID_SIZE);

        // Create new GridSpawnRules with random configuration
        GridSpawnRules rules = new GridSpawnRules(nextIndex);
        setRandomSpawnRules(rules);

        state.spawnRules.put(nextIndex, rules);
        state.markDirty();

        return nextIndex;
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            getState(server);
        });
    }

    public static RegistryKey<World> getDimensionKey() {
        return DIMENSION_KEY;
    }

    public static boolean canMobSpawn(MinecraftServer server, BlockPos pos, EntityType<?> entityType) {
        PocketDimensionsRegistry state = getState(server);
        if (state == null) return true;

        int gridX = Math.floorDiv(pos.getX(), ModDimensions.GRID_SPACING);
        int gridZ = Math.floorDiv(pos.getZ(), ModDimensions.GRID_SPACING);
        int index = gridX + (gridZ * ModDimensions.GRID_SIZE);

        GridSpawnRules rules = state.spawnRules.get(index);
        if (rules == null) {
            rules = new GridSpawnRules(index);
            setRandomSpawnRules(rules);
            state.spawnRules.put(index, rules);
            state.markDirty();
        }
        return rules.canSpawn(entityType);
    }

    public static void setMobSpawnRule(MinecraftServer server, int gridIndex,
                                       EntityType<?> entityType, boolean allowed) {
        PocketDimensionsRegistry state = getState(server);
        if (state == null) return;
        GridSpawnRules rules = state.spawnRules.computeIfAbsent(gridIndex,
                GridSpawnRules::new);
        rules.setSpawnRule(entityType, allowed);
        state.markDirty();
    }
}