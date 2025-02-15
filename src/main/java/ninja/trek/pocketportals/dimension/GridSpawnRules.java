package ninja.trek.pocketportals.dimension;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class GridSpawnRules {
    private final int gridIndex;
    private final Map<EntityType<?>, Boolean> spawnRules;

    public GridSpawnRules(int gridIndex) {
        this.gridIndex = gridIndex;
        this.spawnRules = new HashMap<>();

        // Default to allowing all standard hostile mobs
        setDefaultRules();
    }

    private void setDefaultRules() {
        // Allow common hostile mobs by default
        spawnRules.put(EntityType.ZOMBIE, true);
        spawnRules.put(EntityType.SKELETON, true);
        spawnRules.put(EntityType.SPIDER, true);
        spawnRules.put(EntityType.CREEPER, true);
        spawnRules.put(EntityType.ENDERMAN, true);
        spawnRules.put(EntityType.WITCH, true);
        spawnRules.put(EntityType.PHANTOM, true);
        spawnRules.put(EntityType.SLIME, true);
    }

    public boolean canSpawn(EntityType<?> entityType) {
        return spawnRules.getOrDefault(entityType, true);
    }

    public void setSpawnRule(EntityType<?> entityType, boolean allowed) {
        spawnRules.put(entityType, allowed);
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("GridIndex", gridIndex);

        NbtList rulesList = new NbtList();
        spawnRules.forEach((entityType, allowed) -> {
            NbtCompound ruleNbt = new NbtCompound();
            ruleNbt.putString("EntityType", Registries.ENTITY_TYPE.getId(entityType).toString());
            ruleNbt.putBoolean("Allowed", allowed);
            rulesList.add(ruleNbt);
        });
        nbt.put("SpawnRules", rulesList);

        return nbt;
    }

    public static GridSpawnRules fromNbt(NbtCompound nbt) {
        int gridIndex = nbt.getInt("GridIndex");
        GridSpawnRules rules = new GridSpawnRules(gridIndex);

        NbtList rulesList = nbt.getList("SpawnRules", 10); // 10 is the NBT type for compound tags
        rulesList.forEach(element -> {
            NbtCompound ruleNbt = (NbtCompound) element;
            EntityType<?> entityType = Registries.ENTITY_TYPE.get(
                    Identifier.tryParse(ruleNbt.getString("EntityType")));
            boolean allowed = ruleNbt.getBoolean("Allowed");
            rules.setSpawnRule(entityType, allowed);
        });

        return rules;
    }

    public int getGridIndex() {
        return gridIndex;
    }
}