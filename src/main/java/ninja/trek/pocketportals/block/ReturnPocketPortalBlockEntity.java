package ninja.trek.pocketportals.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;
import java.util.EnumSet;

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
            PocketPortals.LOGGER.info("Loaded return portal data: pos={}, dim={}",
                    returnPosition, returnDimension.getValue());
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
            PocketPortals.LOGGER.info("Saved return portal data: pos={}, dim={}",
                    returnPosition, returnDimension.getValue());
        }
    }

    private BlockPos findSafeReturnLocation(ServerWorld targetWorld, BlockPos originalPos) {
        // First check if the portal and frames are still there
        if (targetWorld.getBlockState(originalPos).getBlock() instanceof PocketPortalBlock) {
            // Portal block exists, use original position
            return originalPos;
        }

        // Portal is gone, need to find safe spot
        BlockPos.Mutable checkPos = new BlockPos.Mutable(
                originalPos.getX(),
                originalPos.getY(),
                originalPos.getZ()
        );

        // First try to find ground below
        for (int y = originalPos.getY(); y >= targetWorld.getBottomY(); y--) {
            checkPos.setY(y);
            if (targetWorld.getBlockState(checkPos).isSolid()) {
                // Found solid ground, return position above it
                return checkPos.up().toImmutable();
            }
        }

        // Search up and down from original Y
        int originalY = originalPos.getY();
        int searchRadius = 10;

        // Search downward first (preferred)
        for (int y = originalY - 1; y >= Math.max(targetWorld.getBottomY(), originalY - searchRadius); y--) {
            checkPos.setY(y);
            if (isSafeSpot(targetWorld, checkPos)) {
                return checkPos.toImmutable();
            }
        }

        // Then search upward
        for (int y = originalY + 1; y <= Math.min(targetWorld.getTopY() - 2, originalY + searchRadius); y++) {
            checkPos.setY(y);
            if (isSafeSpot(targetWorld, checkPos)) {
                return checkPos.toImmutable();
            }
        }

        // If still no safe spot, find highest solid block
        for (int y = Math.min(originalY + 5, targetWorld.getTopY()); y >= targetWorld.getBottomY(); y--) {
            checkPos.setY(y);
            if (targetWorld.getBlockState(checkPos).isSolid()) {
                return checkPos.up().toImmutable();
            }
        }

        // Last resort - return to y=64
        return new BlockPos(originalPos.getX(), 64, originalPos.getZ());
    }

    private boolean isSafeSpot(ServerWorld world, BlockPos pos) {
        // Check if we have solid ground below
        if (!world.getBlockState(pos.down()).isSolid()) {
            return false;
        }

        // Check if we have 2 air blocks for the player
        if (!world.getBlockState(pos).isAir() ||
                !world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        return true;
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

        // Find a safe return location
        BlockPos safePos = findSafeReturnLocation(targetWorld, returnPosition);
        PocketPortals.LOGGER.info("Found safe return location: {} (original was {})",
                safePos, returnPosition);

        // Only apply cooldown if teleport is successful
        if (entity.hasPortalCooldown()) {
            PocketPortals.LOGGER.debug("Entity {} has portal cooldown", entity.getUuidAsString());
            return;
        }

        // Reduced cooldown from 1000 to 100 ticks (5 seconds)
        entity.setPortalCooldown(100);

        // Perform the teleport
        boolean success = entity.teleport(
                targetWorld,
                safePos.getX() + 0.5,
                safePos.getY(),
                safePos.getZ() + 0.5,
                EnumSet.noneOf(PositionFlag.class),
                entity.getYaw(),
                entity.getPitch()
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