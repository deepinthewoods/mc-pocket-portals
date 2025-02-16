package ninja.trek.pocketportals.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.PortalOverlayText;
import ninja.trek.pocketportals.SpawnRulesData;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.block.PocketPortalBlockEntity;
import ninja.trek.pocketportals.network.RequestSpawnRulesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.entity.BlockEntity;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    private long lastSpawnRulesRequestTime = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            PortalOverlayText.clearText();
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        ClientWorld world = (ClientWorld) (Object) this;
        BlockPos portalBasePos = null;

        // Check if looking at frame or base
        if (world.getBlockState(blockHit.getBlockPos()).isOf(ModBlocks.POCKET_PORTAL_FRAME) ||
                world.getBlockState(blockHit.getBlockPos()).isOf(ModBlocks.POCKET_PORTAL)) {
            // Search for portal base
            for (int y = 0; y > -3; y--) {
                BlockPos checkPos = blockHit.getBlockPos().add(0, y, 0);
                if (world.getBlockState(checkPos).isOf(ModBlocks.POCKET_PORTAL)) {
                    portalBasePos = checkPos;
                    break;
                }
            }
        }

        if (portalBasePos != null) {
//            PocketPortals.LOGGER.info("found portal");
            // Request spawn rules if needed
            if (System.currentTimeMillis() - lastSpawnRulesRequestTime > 1250) {
                ClientPlayNetworking.getSender().sendPacket(new RequestSpawnRulesPacket(portalBasePos));
                lastSpawnRulesRequestTime = System.currentTimeMillis();
            }

            // Get block entity data for destination info
            BlockEntity be = world.getBlockEntity(portalBasePos);
            if (be instanceof PocketPortalBlockEntity portalBE) {
                Integer dimensionIndex = portalBE.getDimensionIndex();
//                PocketPortals.LOGGER.info("found portal be {}", dimensionIndex);
                if (dimensionIndex != null) {
                    // Get spawn rules for this dimension
                    Map<?, ?> rules = SpawnRulesData.getSpawnRules(dimensionIndex);

                    StringBuilder overlayText = new StringBuilder();
                    overlayText.append("§6✧ Pocket Dimension §7#").append(dimensionIndex).append("\n");

                    if (!rules.isEmpty()) {
//                        PocketPortals.LOGGER.info("rules not empty");
                        long disabledCount = rules.values().stream()
                                .filter(allowed -> !(Boolean) allowed)
                                .count();

                        if (disabledCount == rules.size()) {
                            overlayText.append("§a⚠ Peaceful");
                        } else if (disabledCount > 0) {
                            overlayText.append("§e⚠ Limited Spawns:\n")
                                    .append(rules.entrySet().stream()
                                            .filter(e -> !(Boolean) e.getValue())
                                            .map(e -> "§7" + formatEntityName(e.getKey().toString()))
                                            .collect(Collectors.joining("\n§8, ")));
                        } else {
                            overlayText.append("§c✓ Hostile");
                        }
                    }

                    PortalOverlayText.setText(overlayText.toString());
                }
            }
        } else {
            PortalOverlayText.clearText();
        }
    }

    private String formatEntityName(String entityId) {
        int index = entityId.lastIndexOf(":");
        if (index == -1) {
            index = entityId.lastIndexOf(".");
        }
        String name = entityId.substring(index + 1);
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

}