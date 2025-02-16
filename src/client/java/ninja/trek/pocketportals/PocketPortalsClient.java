package ninja.trek.pocketportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import ninja.trek.pocketportals.network.SpawnRulesPacket;

public class PocketPortalsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry<RegistryByteBuf> playS2CRegistry = PayloadTypeRegistry.playS2C();
		playS2CRegistry.register(SpawnRulesPacket.ID, SpawnRulesPacket.CODEC);

		// Register client-side receiver
		ClientPlayNetworking.registerGlobalReceiver(SpawnRulesPacket.ID, (packet, context) -> {
			// Schedule handling on the main client thread
			context.client().execute(() -> {
				SpawnRulesData.setSpawnRules(packet.dimensionIndex(), packet.rules());
			});
		});
	}
}