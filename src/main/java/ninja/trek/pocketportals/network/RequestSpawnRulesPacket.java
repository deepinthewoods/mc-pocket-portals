package ninja.trek.pocketportals.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import ninja.trek.pocketportals.PocketPortals;

/**
 * Packet sent from the client to request spawn rules for a portal.
 * Carries the BlockPos of the portal base as three ints.
 */
public record RequestSpawnRulesPacket(BlockPos pos) implements CustomPayload {

    public static final Id<RequestSpawnRulesPacket> ID =
            new Id<>(Identifier.of(PocketPortals.MOD_ID, "request_spawn_rules"));

    public static final PacketCodec<PacketByteBuf, RequestSpawnRulesPacket> CODEC =
            PacketCodec.of(RequestSpawnRulesPacket::write, RequestSpawnRulesPacket::new);

    // Constructor for decoding from the buffer
    public RequestSpawnRulesPacket(PacketByteBuf buf) {
        this(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }

    // Write the BlockPos as three ints into the buffer
    public void write(PacketByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    @Override
    public Id<?> getId() {
        return ID;
    }
}
