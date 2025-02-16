package ninja.trek.pocketportals.network;

import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import ninja.trek.pocketportals.PocketPortals;

import java.util.HashMap;
import java.util.Map;

public record SpawnRulesPacket(int dimensionIndex, Map<EntityType<?>, Boolean> rules)
        implements CustomPayload {

    public static final Id<SpawnRulesPacket> ID =
            new Id<>(Identifier.of(PocketPortals.MOD_ID, "spawn_rules_sync"));

    public static final PacketCodec<RegistryByteBuf, SpawnRulesPacket> CODEC =
            PacketCodec.of(SpawnRulesPacket::write, SpawnRulesPacket::new);

    // Constructor for decoding
    public SpawnRulesPacket(RegistryByteBuf buf) {
        this(buf.readVarInt(), readRules(buf));
    }

    // Write method for encoding
    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(dimensionIndex);
        buf.writeVarInt(rules.size());
        rules.forEach((entityType, allowed) -> {
            buf.writeIdentifier(Registries.ENTITY_TYPE.getId(entityType));
            buf.writeBoolean(allowed);
        });
    }

    // Helper method to read rules map
    private static Map<EntityType<?>, Boolean> readRules(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        Map<EntityType<?>, Boolean> rules = new HashMap<>();
        for (int i = 0; i < size; i++) {
            EntityType<?> entityType = Registries.ENTITY_TYPE.get(buf.readIdentifier());
            boolean allowed = buf.readBoolean();
            rules.put(entityType, allowed);
        }
        return rules;
    }

    @Override
    public Id<?> getId() {
        return ID;
    }
}