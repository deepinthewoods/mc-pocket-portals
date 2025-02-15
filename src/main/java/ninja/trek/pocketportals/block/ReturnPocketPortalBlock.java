package ninja.trek.pocketportals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.block.ReturnPocketPortalBlockEntity;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;

public class ReturnPocketPortalBlock extends BlockWithEntity {
    public static final MapCodec<ReturnPocketPortalBlock> CODEC = createCodec(ReturnPocketPortalBlock::new);

    public ReturnPocketPortalBlock(Settings settings) {
        super(Settings.copy(Blocks.STONE)
                .strength(3.0f)
                .requiresTool()
                .nonOpaque());
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ReturnPocketPortalBlockEntity(pos, state);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            // Only handle collision if we're in the pocket dimension
            if (!world.getRegistryKey().equals(PocketDimensionsRegistry.getDimensionKey())) {
                return;
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ReturnPocketPortalBlockEntity returnPortalBE) {
                // Add debug logging
                PocketPortals.LOGGER.info("Entity {} collided with return portal at {}",
                        entity.getUuidAsString(), pos);

                returnPortalBE.handleEntityCollision(entity);
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // Remove frame blocks
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
            world.setBlockState(pos.up(2), Blocks.AIR.getDefaultState());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}