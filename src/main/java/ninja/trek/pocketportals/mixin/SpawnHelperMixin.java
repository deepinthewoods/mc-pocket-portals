package ninja.trek.pocketportals.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {
    @Inject(
            method = "canSpawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/world/biome/SpawnSettings$SpawnEntry;Lnet/minecraft/util/math/BlockPos$Mutable;D)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onCanSpawn(
            ServerWorld world,
            SpawnGroup group,
            StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator,
            SpawnSettings.SpawnEntry spawnEntry,
            BlockPos.Mutable pos,
            double squaredDistance,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Only proceed if vanilla spawn checks passed
        if (!cir.getReturnValue()) {
            return;
        }

        // Only apply our rules in the pocket dimension
        if (!world.getRegistryKey().equals(PocketDimensionsRegistry.getDimensionKey())) {
            return;
        }

        // Get the entity type from the spawn entry
        EntityType<?> entityType = spawnEntry.type;

        // Check if spawning is allowed in this grid space
        if (!PocketDimensionsRegistry.canMobSpawn(world.getServer(), pos, entityType)) {
            cir.setReturnValue(false);
        }
    }
}