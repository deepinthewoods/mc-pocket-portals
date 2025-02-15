package ninja.trek.pocketportals.network;

import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import ninja.trek.pocketportals.PocketPortals;

import java.util.HashMap;
import java.util.Map;

/**
 * A record representing spawn rules for a given dimension.
 */
public record SpawnRulesPacket(int dimensionIndex, Map<EntityType<?>, Boolean> rules)
        implements CustomPayload {

    public static final CustomPayload.Id<SpawnRulesPacket> ID =
            new CustomPayload.Id<>(Identifier.of(PocketPortals.MOD_ID, "spawn_rules_sync"));

    /**
     * Constructor for automatic decoding.
     * Fabric will call this to create a new instance from the received PacketByteBuf.
     */
    public SpawnRulesPacket(PacketByteBuf buf) {
        this(
                buf.readVarInt(),
                readRules(buf)
        );
    }

    private static Map<EntityType<?>, Boolean> readRules(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Map<EntityType<?>, Boolean> rules = new HashMap<>();
        for (int i = 0; i < size; i++) {
            EntityType<?> entityType = Registries.ENTITY_TYPE.get(buf.readIdentifier());
            boolean allowed = buf.readBoolean();
            rules.put(entityType, allowed);
        }
        return rules;
    }

    /**
     * Writes this packet's data to the given buffer.
     */
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(dimensionIndex());
        buf.writeVarInt(rules().size());
        rules().forEach((entityType, allowed) -> {
            buf.writeIdentifier(Registries.ENTITY_TYPE.getId(entityType));
            buf.writeBoolean(allowed);
        });
    }

    @Override
    public CustomPayload.Id<?> getId() {
        return ID;
    }

    // Optionally, if you want to keep a PacketCodec for sending:
    /*
    public static final PacketCodec<PacketByteBuf, SpawnRulesPacket> CODEC =
        PacketCodec.of(
            (PacketByteBuf buf, SpawnRulesPacket packet) -> {
                packet.write(buf);
            },
            (PacketByteBuf buf) -> {
                int dimensionIndex = buf.readVarInt();
                int size = buf.readVarInt();
                Map<EntityType<?>, Boolean> rules = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    EntityType<?> entityType = Registries.ENTITY_TYPE.get(buf.readIdentifier());
                    boolean allowed = buf.readBoolean();
                    rules.put(entityType, allowed);
                }
                return new SpawnRulesPacket(dimensionIndex, rules);
            }
        );
    */
}
