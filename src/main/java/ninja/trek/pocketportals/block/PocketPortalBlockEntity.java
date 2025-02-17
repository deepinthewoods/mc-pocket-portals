package ninja.trek.pocketportals.block;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.dimension.GridSpawnRules;
import ninja.trek.pocketportals.dimension.ModDimensions;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.network.SpawnRulesPacket;


import java.util.*;

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

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound tag = super.toInitialChunkDataNbt(registries);
        if (dimensionIndex != null) {
            tag.putInt(DIMENSION_INDEX_KEY, dimensionIndex);
        }
        return tag;
    }




    private BlockPos findSafePortalLocation(ServerWorld world, ModDimensions.WorldPosition basePos) {
        BlockPos.Mutable checkPos = new BlockPos.Mutable(
                basePos.x(),
                basePos.y(),
                basePos.z()
        );

        // Start at y=64 and search up/down
        int startY = basePos.y() + 10; // Start 10 blocks above base height

        // Search upward first (more likely to find air)
        for (int y = 0; y < 20; y++) {
            checkPos.setY(startY + y);
            if (isSafePlaceForPortal(world, checkPos)) {
                return checkPos.toImmutable();
            }
        }

        // Then search downward
        for (int y = -1; y > -10; y--) {
            checkPos.setY(startY + y);
            if (isSafePlaceForPortal(world, checkPos)) {
                return checkPos.toImmutable();
            }
        }

        // If we can't find a good spot, create one
        checkPos.setY(startY);
        prepareReturnPortalArea(world, checkPos);
        return checkPos.toImmutable();
    }

    private boolean isSafePlaceForPortal(ServerWorld world, BlockPos pos) {
        // Need three blocks of vertical clearance for portal + frames
        for (int y = 0; y < 3; y++) {
            if (!world.getBlockState(pos.up(y)).isAir()) {
                return false;
            }
        }

        // Need solid ground below
        return world.getBlockState(pos.down()).isSolid();
    }

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

        // Set portal cooldown
        entity.setPortalCooldown(20);

        // Get the target dimension
        ServerWorld targetWorld = serverWorld.getServer().getWorld(PocketDimensionsRegistry.getDimensionKey());
        if (targetWorld == null) {
            PocketPortals.LOGGER.error("Pocket dimension is not loaded!");
            return;
        }

        // Calculate grid position from index
        ModDimensions.GridPosition gridPos = ModDimensions.indexToGridPosition(dimensionIndex);
        ModDimensions.WorldPosition worldPos = ModDimensions.gridToWorldPosition(gridPos);

        // Find a safe location for the return portal
        BlockPos returnPortalPos = findSafePortalLocation(targetWorld, worldPos);

        // Create the return portal
        targetWorld.setBlockState(returnPortalPos,
                ModBlocks.RETURN_POCKET_PORTAL.getDefaultState());
        targetWorld.setBlockState(returnPortalPos.up(),
                ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
        targetWorld.setBlockState(returnPortalPos.up(2),
                ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());

        // Set up the return portal block entity
        BlockEntity be = targetWorld.getBlockEntity(returnPortalPos);
        if (be instanceof ReturnPocketPortalBlockEntity returnPortalBE) {
            returnPortalBE.setReturnPosition(pos.toImmutable(), world.getRegistryKey());
            returnPortalBE.markDirty();
        }

        // Teleport the player into the return portal
        teleportEntity(entity, targetWorld,
                returnPortalPos.getX() + 0.5,
                returnPortalPos.getY() + 0.5, // Center of the portal block
                returnPortalPos.getZ() + 0.5);
    }

    private void prepareReturnPortalArea(ServerWorld world, BlockPos portalPos) {
        // Clear space for portal and frames
        for (int y = 0; y < 3; y++) {
            BlockPos clearPos = portalPos.up(y);
            world.setBlockState(clearPos, Blocks.AIR.getDefaultState());
        }

        // Create 5x5 platform underneath
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos platformPos = portalPos.down().add(x, 0, z);
                world.setBlockState(platformPos, Blocks.STONE.getDefaultState());
            }
        }
    }


    private void teleportEntity(Entity entity, ServerWorld targetWorld, double x, double y, double z) {
        // Ensure the chunk is loaded before teleporting
        BlockPos.Mutable checkPos = new BlockPos.Mutable((int)x, (int)y, (int)z);
        targetWorld.getChunk(checkPos); // Force chunk load

        // Get the actual height from the chunk generator
        int surfaceHeight = targetWorld.getChunkManager()
                .getChunkGenerator()
                .getHeight(checkPos.getX(), checkPos.getZ(),
                        Heightmap.Type.MOTION_BLOCKING,
                        targetWorld,
                        targetWorld.getChunkManager().getNoiseConfig());

        // Set our check position to the surface height
        checkPos.set((int)x, surfaceHeight, (int)z);

        // Ensure chunk is loaded and blocks are initialized
        targetWorld.getChunk(checkPos);

        // Move up until we have 2 blocks of air clearance
        int attempts = 0;
        int maxAttempts = 10; // Prevent infinite loops

        while (attempts < maxAttempts &&
                checkPos.getY() < targetWorld.getTopYInclusive() - 2 &&
                (!isValidTeleportLocation(targetWorld, checkPos))) {
            checkPos.move(0, 1, 0);
            attempts++;
        }

        // If we couldn't find a safe spot, create one
        if (!isValidTeleportLocation(targetWorld, checkPos)) {
            createSafeTeleportLocation(targetWorld, checkPos);
        }

        // Teleport to the safe position we found
        entity.teleport(
                targetWorld,
                checkPos.getX() + 0.5,
                checkPos.getY(),
                checkPos.getZ() + 0.5,
                EnumSet.noneOf(PositionFlag.class),
                entity.getYaw(),
                entity.getPitch(),
                true
        );
    }

    private boolean isValidTeleportLocation(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isAir() &&
                world.getBlockState(pos.up()).isAir() &&
                !world.getBlockState(pos.down()).isAir();
    }

    private void createSafeTeleportLocation(ServerWorld world, BlockPos pos) {
        // Create a safe platform
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());

        // Add some surrounding blocks for safety
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    BlockPos platformPos = pos.down().add(dx, 0, dz);
                    if (world.getBlockState(platformPos).isAir()) {
                        world.setBlockState(platformPos, Blocks.STONE.getDefaultState());
                    }
                }
            }
        }
    }



    public void syncSpawnRules(PlayerEntity player) {
        if (dimensionIndex == null || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Get spawn rules for this dimension index
        GridSpawnRules rules = PocketDimensionsRegistry.getSpawnRules(world.getServer(), dimensionIndex);
        if (rules == null) {
            PocketPortals.LOGGER.warn("Null spawnrules in portal block entity for index: {}", dimensionIndex);
            return;
        }

        // Create map of spawn rules
        Map<EntityType<?>, Boolean> spawnRules = new HashMap<>();
        // Add all managed mobs to the map
        for (EntityType<?> entityType : PocketDimensionsRegistry.MANAGED_MOBS) {
            spawnRules.put(entityType, rules.canSpawn(entityType));
        }

        // Create and send packet
        SpawnRulesPacket packet = new SpawnRulesPacket(dimensionIndex, spawnRules);
        ServerPlayNetworking.send(serverPlayer, packet);
//        PocketPortals.LOGGER.info("Sent spawnrules for dimension {} to player {}",
//                dimensionIndex, player.getName().getString());
    }

}
