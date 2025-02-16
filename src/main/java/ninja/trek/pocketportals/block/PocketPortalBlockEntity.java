package ninja.trek.pocketportals.block;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.SpawnRulesData;
import ninja.trek.pocketportals.dimension.GridSpawnRules;
import ninja.trek.pocketportals.dimension.ModDimensions;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.network.SpawnRulesPacket;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class PocketPortalBlockEntity extends BlockEntity {
    private static final String DIMENSION_INDEX_KEY = "PocketDimensionIndex";
    private Integer dimensionIndex = null; // store an index from 0..255

    public PocketPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POCKET_PORTAL_BLOCK_ENTITY, pos, state);
    }

    public Integer getDimensionIndex() {
        return dimensionIndex;
    }

    public void setDimensionIndex(Integer dimensionIndex) {
        this.dimensionIndex = dimensionIndex;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.contains(DIMENSION_INDEX_KEY)) {
            dimensionIndex = nbt.getInt(DIMENSION_INDEX_KEY);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (dimensionIndex != null) {
            nbt.putInt(DIMENSION_INDEX_KEY, dimensionIndex);
        }
    }


    // In PocketPortalBlockEntity.java, modify handleEntityCollision:

    public void handleEntityCollision(Entity entity) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!entity.canUsePortals(false)) return;
        if (dimensionIndex == null) {
            PocketPortals.LOGGER.warn("Portal block has no dimension index assigned!");
            return;
        }
        if (entity.hasPortalCooldown()) {
            return;
        }
        // Set portal cooldown (20 ticks = 1 second)
        entity.setPortalCooldown(20);

        // Get the target dimension
        ServerWorld targetWorld = serverWorld.getServer().getWorld(PocketDimensionsRegistry.getDimensionKey());
        if (targetWorld == null) {
            PocketPortals.LOGGER.error("Pocket dimension is not loaded!");
            return;
        }

        // Are we already in the pocket dimension?
        if (serverWorld.getRegistryKey().equals(PocketDimensionsRegistry.getDimensionKey())) {
            // Then go back to Overworld
            ServerWorld overworld = serverWorld.getServer().getOverworld();
            teleportEntity(entity, overworld, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        } else {
            // Calculate grid position from index
            ModDimensions.GridPosition gridPos = ModDimensions.indexToGridPosition(dimensionIndex);
            ModDimensions.WorldPosition worldPos = ModDimensions.gridToWorldPosition(gridPos);

            // Teleport to the specific grid location in the pocket dimension
            teleportEntity(entity, targetWorld,
                    worldPos.x() + 0.5, worldPos.y() + 1, worldPos.z() + 0.5);

            // Build return portal at the destination
            BlockPos base = new BlockPos(worldPos.x() + 2, worldPos.y(), worldPos.z());

            // Create RETURN portal block
            targetWorld.setBlockState(base, ModBlocks.RETURN_POCKET_PORTAL.getDefaultState());
            targetWorld.setBlockState(base.up(), ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
            targetWorld.setBlockState(base.up(2), ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());

            // Set up the return portal block entity
            BlockEntity be = targetWorld.getBlockEntity(base);
            if (be instanceof ReturnPocketPortalBlockEntity returnPortalBE) {
                // Store the entry Y position instead of the block's Y position
                BlockPos returnPos = new BlockPos(pos.getX(), entity.getBlockY(), pos.getZ());
                returnPortalBE.setReturnPosition(returnPos, world.getRegistryKey());
                returnPortalBE.markDirty();
            }
        }
    }

    private void teleportEntity(Entity entity, ServerWorld targetWorld, double x, double y, double z) {
        BlockPos.Mutable checkPos = new BlockPos.Mutable((int)x, (int)y, (int)z);

        // Get the actual height from the chunk generator
        int surfaceHeight = targetWorld.getChunkManager()
                .getChunkGenerator()
                .getHeight((int)x, (int)z,
                        Heightmap.Type.MOTION_BLOCKING,
                        targetWorld,
                        targetWorld.getChunkManager().getNoiseConfig());

        // Set our check position to the surface height
        checkPos.set((int)x, surfaceHeight, (int)z);

        // Move up until we have 2 blocks of air clearance
        while (checkPos.getY() < targetWorld.getTopYInclusive() &&
                (!targetWorld.getBlockState(checkPos).isAir() ||
                        !targetWorld.getBlockState(checkPos.up()).isAir())) {
            checkPos.move(0, 1, 0);
        }

        // Teleport to the safe position we found
        entity.teleport(
                targetWorld,
                x, checkPos.getY(), z,
                EnumSet.noneOf(PositionFlag.class),
                entity.getYaw(),
                entity.getPitch(),
                true
        );
    }

    public void syncSpawnRules(PlayerEntity player) {
        if (dimensionIndex == null || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Get spawn rules for this dimension index
        GridSpawnRules rules = PocketDimensionsRegistry.getSpawnRules(world.getServer(), dimensionIndex);
        if (rules == null) return;

        // Create map of spawn rules
        Map<EntityType<?>, Boolean> spawnRules = new HashMap<>();
        // Add all managed mobs to the map
        for (EntityType<?> entityType : PocketDimensionsRegistry.MANAGED_MOBS) {
            spawnRules.put(entityType, rules.canSpawn(entityType));
        }

        // Create and send packet
        SpawnRulesPacket packet = new SpawnRulesPacket(dimensionIndex, spawnRules);
        ServerPlayNetworking.send(serverPlayer, packet);
    }



}
