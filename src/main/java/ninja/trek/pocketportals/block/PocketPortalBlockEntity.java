package ninja.trek.pocketportals.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.dimension.ModDimensions;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;

import java.util.EnumSet;

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


    public void handleEntityCollision(Entity entity) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!entity.canUsePortals(false)) return;

        if (dimensionIndex == null) {
            PocketPortals.LOGGER.warn("Portal block has no dimension index assigned!");
            return;
        }

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
            buildReturnPortal(targetWorld, new BlockPos(worldPos.x(), worldPos.y(), worldPos.z()));
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
        while (checkPos.getY() < targetWorld.getTopY() &&
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
                entity.getPitch()
        );
    }

    /**
     * (Optional) Build a return portal in the remote dimension.
     * That portal block can store the same dimensionIndex or a separate route.
     */
    private void buildReturnPortal(ServerWorld targetWorld, BlockPos nearPos) {
        // Example: build our PocketPortalBlock + frames at nearPos + some offset
        BlockPos base = nearPos.add(2, 0, 0);
        targetWorld.setBlockState(base, ModBlocks.POCKET_PORTAL.getDefaultState());
        // Place frame blocks above
        targetWorld.setBlockState(base.up(), ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
        targetWorld.setBlockState(base.up(2), ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
        // Then set the dimensionIndex in that new block entity so it knows
        // which dimension it references (for return).
        BlockEntity be = targetWorld.getBlockEntity(base);
        if (be instanceof PocketPortalBlockEntity pbe) {
            // Option 1: set the same index so if used in that dimension,
            // you go back to Overworld (the dimension you came from).
            pbe.setDimensionIndex(this.dimensionIndex);
            pbe.markDirty();
        }
    }
}
