package ninja.trek.pocketportals.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.dimension.GridSpawnRules;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.network.SpawnRulesPacket;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class ReturnPocketPortalBlockEntity extends BlockEntity {
    private static final String RETURN_POS_X = "ReturnPosX";
    private static final String RETURN_POS_Y = "ReturnPosY";
    private static final String RETURN_POS_Z = "ReturnPosZ";
    private static final String RETURN_DIMENSION = "ReturnDimension";

    private BlockPos returnPosition;
    private RegistryKey<World> returnDimension;

    public ReturnPocketPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RETURN_PORTAL_BLOCK_ENTITY, pos, state);
    }

    public void setReturnPosition(BlockPos pos, RegistryKey<World> dimension) {
        this.returnPosition = pos;
        this.returnDimension = dimension;
        markDirty();
        PocketPortals.LOGGER.info("Set return portal destination: pos={}, dim={}",
                pos, dimension.getValue());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.contains(RETURN_POS_X)) {
            int x = nbt.getInt(RETURN_POS_X);
            int y = nbt.getInt(RETURN_POS_Y);
            int z = nbt.getInt(RETURN_POS_Z);
            returnPosition = new BlockPos(x, y, z);
            String dimId = nbt.getString(RETURN_DIMENSION);
            returnDimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimId));
//            PocketPortals.LOGGER.info("Loaded return portal data: pos={}, dim={}",
//                    returnPosition, returnDimension.getValue());
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (returnPosition != null && returnDimension != null) {
            nbt.putInt(RETURN_POS_X, returnPosition.getX());
            nbt.putInt(RETURN_POS_Y, returnPosition.getY());
            nbt.putInt(RETURN_POS_Z, returnPosition.getZ());
            nbt.putString(RETURN_DIMENSION, returnDimension.getValue().toString());
//            PocketPortals.LOGGER.info("Saved return portal data: pos={}, dim={}",
//                    returnPosition, returnDimension.getValue());
        }
    }

    private BlockPos findSafeReturnLocation(ServerWorld targetWorld, BlockPos originalPos) {
        // First check if the original position still has a portal
        if (targetWorld.getBlockState(originalPos).isOf(ModBlocks.POCKET_PORTAL)) {
            // Portal is still there, use it!
            PocketPortals.LOGGER.info("Original portal found at {}, using it", originalPos);
            return originalPos;
        }

        // If portal is gone, start looking for safe spots
        if (isSafeSpot(targetWorld, originalPos)) {
            PocketPortals.LOGGER.info("Original position {} is safe, using it", originalPos);
            return originalPos;
        }

        BlockPos.Mutable checkPos = originalPos.mutableCopy();
        // First search near the original Y level (within ±2 blocks)
        for (int y = -2; y <= 2; y++) {
            checkPos.setY(originalPos.getY() + y);
            if (isSafeSpot(targetWorld, checkPos)) {
                PocketPortals.LOGGER.info("Found safe spot near original Y: {}", checkPos);
                return checkPos.toImmutable();
            }
        }

        // If that fails, search more extensively around the original Y
        int searchRadius = 10;
        // Search downward from original Y
        for (int y = originalPos.getY() - 3; y >= Math.max(targetWorld.getBottomY(), originalPos.getY() - searchRadius); y--) {
            checkPos.setY(y);
            if (isSafeSpot(targetWorld, checkPos)) {
                PocketPortals.LOGGER.info("Found safe spot below: {}", checkPos);
                return checkPos.toImmutable();
            }
        }

        // Then search upward from original Y
        for (int y = originalPos.getY() + 3; y <= Math.min(targetWorld.getTopYInclusive() - 2, originalPos.getY() + searchRadius); y++) {
            checkPos.setY(y);
            if (isSafeSpot(targetWorld, checkPos)) {
                PocketPortals.LOGGER.info("Found safe spot above: {}", checkPos);
                return checkPos.toImmutable();
            }
        }

        // If still no safe spot, find highest solid block below original position
        checkPos = originalPos.mutableCopy();
        for (int y = originalPos.getY(); y >= targetWorld.getBottomY(); y--) {
            checkPos.setY(y);
            if (targetWorld.getBlockState(checkPos).isSolid()) {
                BlockPos safePos = checkPos.up().toImmutable();
                PocketPortals.LOGGER.info("Using highest solid block method, found: {}", safePos);
                return safePos;
            }
        }

        // Absolute last resort - go to original Y but ensure there's air
        BlockPos lastResort = new BlockPos(originalPos.getX(), originalPos.getY(), originalPos.getZ());
        targetWorld.setBlockState(lastResort, net.minecraft.block.Blocks.AIR.getDefaultState());
        targetWorld.setBlockState(lastResort.up(), net.minecraft.block.Blocks.AIR.getDefaultState());
        PocketPortals.LOGGER.warn("Using last resort position: {}", lastResort);
        return lastResort;
    }

    private boolean isSafeSpot(ServerWorld world, BlockPos pos) {
        // Check for portal blocks and frames - we don't want to spawn inside these
        // unless it's our original portal (which is checked separately)
        if (world.getBlockState(pos).isOf(ModBlocks.POCKET_PORTAL_FRAME) ||
                world.getBlockState(pos.up()).isOf(ModBlocks.POCKET_PORTAL_FRAME) ||
                world.getBlockState(pos).isOf(ModBlocks.POCKET_PORTAL) ||
                world.getBlockState(pos.up()).isOf(ModBlocks.POCKET_PORTAL)) {
            return false;
        }

        // Must have two air blocks for player height
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        // Must have solid ground below
        BlockPos groundPos = pos.down();
        return world.getBlockState(groundPos).isSolid() ||
                world.getBlockState(groundPos).blocksMovement();
    }

    public void handleEntityCollision(Entity entity) {
        if (!(world instanceof ServerWorld serverWorld)) {
            PocketPortals.LOGGER.error("Return portal not in server world!");
            return;
        }

        if (!entity.canUsePortals(false)) {
            PocketPortals.LOGGER.debug("Entity cannot use portals: {}", entity);
            return;
        }

        if (returnPosition == null || returnDimension == null) {
            PocketPortals.LOGGER.error("Return portal missing destination data: pos={}, dim={}",
                    returnPosition, returnDimension);
            return;
        }

        // Get the target world
        ServerWorld targetWorld = serverWorld.getServer().getWorld(returnDimension);
        if (targetWorld == null) {
            PocketPortals.LOGGER.error("Target world not found: {}", returnDimension.getValue());
            return;
        }

        // Find a safe return location (prioritizing original portal)
        BlockPos safePos = findSafeReturnLocation(targetWorld, returnPosition);
//        PocketPortals.LOGGER.info("Found safe return location: {} (original was {})",
//                safePos, returnPosition);

        // Only apply cooldown if teleport is successful
        if (entity.hasPortalCooldown()) {
//            PocketPortals.LOGGER.debug("Entity {} has portal cooldown", entity.getUuidAsString());
            return;
        }

        // Calculate target position
        double targetX = safePos.getX() + 0.5;
        double targetY = safePos.getY();
        double targetZ = safePos.getZ() + 0.5;

        // If returning to original portal, adjust Y position to be inside the portal
        if (targetWorld.getBlockState(safePos).isOf(ModBlocks.POCKET_PORTAL)) {
            targetY += 0.5; // Position entity in middle of portal block
        }

        // Reduced cooldown from 1000 to 100 ticks (5 seconds)
        entity.setPortalCooldown(100);

        // Perform the teleport
        boolean success = entity.teleport(
                targetWorld,
                targetX,
                targetY,
                targetZ,
                EnumSet.noneOf(PositionFlag.class),
                entity.getYaw(),
                entity.getPitch(),
                true
        );

        if (success) {
            // Play portal sound
            world.playSound(null, pos,
                    SoundEvents.BLOCK_PORTAL_TRAVEL,
                    SoundCategory.BLOCKS,
                    0.25f,
                    1.0f);
            PocketPortals.LOGGER.info("Successfully teleported entity {} to {}",
                    entity.getUuidAsString(), safePos);
        } else {
            PocketPortals.LOGGER.error("Failed to teleport entity {} to {}",
                    entity.getUuidAsString(), safePos);
        }
    }




}