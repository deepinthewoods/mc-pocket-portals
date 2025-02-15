package ninja.trek.pocketportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import ninja.trek.pocketportals.network.SpawnRulesPacket;

/**
 * Client-side initializer that registers the spawn rules packet receiver.
 */
public class PocketPortalsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register the receiver.
		// Note: Fabric expects a PlayPayloadHandler<T> whose receive() method has the signature:
		//     void receive(MinecraftClient client, T packet, PacketSender responseSender)
		// Fabric will automatically call the PacketByteBuf constructor of SpawnRulesPacket.
		ClientPlayNetworking.registerGlobalReceiver(
				SpawnRulesPacket.ID,
				new ClientPlayNetworking.PlayPayloadHandler<SpawnRulesPacket>() {
					@Override
					public void receive(SpawnRulesPacket packet, ClientPlayNetworking.Context context) {
						context.client().execute(() ->
								SpawnRulesData.setSpawnRules(packet.dimensionIndex(), packet.rules())
						);
					}


				}
		);
	}
}
