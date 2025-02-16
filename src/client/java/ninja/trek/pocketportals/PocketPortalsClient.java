package ninja.trek.pocketportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.network.RegistryByteBuf;
import ninja.trek.pocketportals.network.SpawnRulesPacket;
import net.minecraft.client.render.GameRenderer;

public class PocketPortalsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry<RegistryByteBuf> playS2CRegistry = PayloadTypeRegistry.playS2C();
		playS2CRegistry.register(SpawnRulesPacket.ID, SpawnRulesPacket.CODEC);

		// Register client-side receiver
		ClientPlayNetworking.registerGlobalReceiver(SpawnRulesPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				SpawnRulesData.setSpawnRules(packet.dimensionIndex(), packet.rules());
//				PocketPortals.LOGGER.info("receive spawn rules {}", packet.rules());
			});
		});

		HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickDeltaManager) -> {
			float tickDelta = tickDeltaManager.getTickDelta(true);
			if (MinecraftClient.getInstance().currentScreen == null) {
//				PocketPortals.LOGGER.info("render overlay");
				PortalOverlayText.render(context);
			}
		});



	}
}