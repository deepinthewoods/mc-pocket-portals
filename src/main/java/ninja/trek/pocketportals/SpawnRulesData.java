package ninja.trek.pocketportals;

import net.minecraft.entity.EntityType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnRulesData {
    // Use ConcurrentHashMap for thread safety when accessing from different threads
    private static final Map<Integer, Map<EntityType<?>, Boolean>> dimensionSpawnRules =
            new ConcurrentHashMap<>();

    public static void setSpawnRules(int dimensionIndex, Map<EntityType<?>, Boolean> rules) {
        dimensionSpawnRules.put(dimensionIndex, new HashMap<>(rules));
//        PocketPortals.LOGGER.info("set spawn rules");
    }

    public static Map<EntityType<?>, Boolean> getSpawnRules(int dimensionIndex) {
        return dimensionSpawnRules.getOrDefault(dimensionIndex, new HashMap<>());
    }

    public static void clearAllRules() {
        dimensionSpawnRules.clear();
    }

    // Private constructor to prevent instantiation
    private SpawnRulesData() {}
}