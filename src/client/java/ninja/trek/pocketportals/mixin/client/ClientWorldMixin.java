package ninja.trek.pocketportals.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import ninja.trek.pocketportals.SpawnRulesData;
import ninja.trek.pocketportals.block.ModBlocks;
import ninja.trek.pocketportals.block.PocketPortalBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.stream.Collectors;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        ClientWorld world = (ClientWorld) (Object) this;

        if (world.getBlockState(blockHit.getBlockPos()).isOf(ModBlocks.POCKET_PORTAL_FRAME)) {
            // Look for portal base block
            for (int y = 0; y > -3; y--) {
                var basePos = blockHit.getBlockPos().add(0, y, 0);
                if (world.getBlockState(basePos).isOf(ModBlocks.POCKET_PORTAL)) {
                    var blockEntity = world.getBlockEntity(basePos);
                    if (blockEntity instanceof PocketPortalBlockEntity portalBE) {
                        Integer dimensionIndex = portalBE.getDimensionIndex();
                        if (dimensionIndex != null) {
                            Map<EntityType<?>, Boolean> rules = SpawnRulesData.getSpawnRules(dimensionIndex);
                            if (!rules.isEmpty()) {
                                // Count disabled spawns
                                long disabledCount = rules.values().stream().filter(allowed -> !allowed).count();

                                String overlay;
                                if (disabledCount == rules.size()) {
                                    overlay = "§7All mob spawns disabled";
                                } else if (disabledCount > 0) {
                                    overlay = "§7Disabled spawns: " +
                                            rules.entrySet().stream()
                                                    .filter(e -> !e.getValue())
                                                    .map(e -> e.getKey().getName().getString())
                                                    .collect(Collectors.joining(", "));
                                } else {
                                    overlay = "§7All mob spawns enabled";
                                }

                                client.player.sendMessage(Text.literal(overlay), true);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}