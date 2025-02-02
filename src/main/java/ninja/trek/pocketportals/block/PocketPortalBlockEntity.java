package ninja.trek.pocketportals.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.dimension.ModDimensions;

import java.util.EnumSet;

public class PocketPortalBlockEntity extends BlockEntity {
    private Long seed;
    private static final String SEED_KEY = "PocketSeed";

    public PocketPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POCKET_PORTAL_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains(SEED_KEY)) {
            seed = nbt.getLong(SEED_KEY);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (seed != null) {
            nbt.putLong(SEED_KEY, seed);
        }
    }

    public void handleEntityCollision(Entity entity) {
        if (!entity.hasVehicle() && !entity.hasPassengers() && entity.canUsePortals(false)) {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            // Generate seed on first use if needed
            if (seed == null) {
                seed = Random.create().nextLong();
                markDirty();
            }

            // The dimension key should match our JSON configuration
            RegistryKey<World> pocketDimensionKey = RegistryKey.of(
                    RegistryKeys.WORLD,
                    Identifier.of(PocketPortals.MOD_ID, "pocket_dimension")
            );

            if (world.getRegistryKey() == pocketDimensionKey) {
                // If in pocket dimension, return to overworld at the portal location
                ServerWorld targetWorld = serverWorld.getServer().getOverworld();
                entity.teleport(
                        targetWorld,
                        pos.getX() + 0.5,
                        pos.getY() + 1,
                        pos.getZ() + 0.5,
                        EnumSet.noneOf(PositionFlag.class),
                        entity.getYaw(),
                        entity.getPitch()
                );
            } else {
                // Going to pocket dimension
                ServerWorld pocketWorld = ModDimensions.getOrCreatePocketDimension(
                        serverWorld.getServer(),
                        pocketDimensionKey,
                        seed
                );

                if (pocketWorld != null) {
                    entity.teleport(
                            pocketWorld,
                            0.5,
                            66,
                            0.5,
                            EnumSet.noneOf(PositionFlag.class),
                            entity.getYaw(),
                            entity.getPitch()
                    );
                } else {
                    PocketPortals.LOGGER.error("Failed to access pocket dimension!");
                }
            }
        }
    }

    public Long getSeed() {
        return seed;
    }
}