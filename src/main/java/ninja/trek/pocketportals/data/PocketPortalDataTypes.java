package ninja.trek.pocketportals.data;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

/**
 * Defines a custom ComponentType<Integer> for storing a dimension index
 * in an item or block entity, using Minecraft 1.21's new item component system.
 */
public final class PocketPortalDataTypes {
    /**
     * Our custom component type that stores an Integer (dimension index).
     *
     * - Codec: Handles saving/loading from NBT and JSON.
     * - PacketCodec: Handles network sync and serialization.
     */

    public static final ComponentType<Integer> DIMENSION_INDEX = new ComponentType.Builder<Integer>()
            .codec(Codec.INT) // Set up codec for serialization
            .packetCodec(new PacketCodec<RegistryByteBuf, Integer>() {
                @Override
                public Integer decode(RegistryByteBuf buf) {
                    return buf.readVarInt(); // Read the dimension index from the network buffer
                }

                @Override
                public void encode(RegistryByteBuf buf, Integer value) {
                    buf.writeVarInt(value); // Write the dimension index to the network buffer
                }
            })
            .build(); // Build the ComponentType

    // Prevent instantiation
    private PocketPortalDataTypes() {}
}
