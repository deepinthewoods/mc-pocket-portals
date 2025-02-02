package ninja.trek.pocketportals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

public class PocketPortalBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final MapCodec<PocketPortalBlock> CODEC = createCodec(PocketPortalBlock::new);

    public PocketPortalBlock(Settings settings) {
        super(Settings.copy(Blocks.STONE)
                .strength(3.0f)
                .requiresTool()
                .nonOpaque());
        setDefaultState(getStateManager().getDefaultState());
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PocketPortalBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!world.isClient) {
            // Create our custom portal blocks above instead of nether portal
            BlockPos portalPos1 = pos.up();
            BlockPos portalPos2 = portalPos1.up();

            world.setBlockState(portalPos1, ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
            world.setBlockState(portalPos2, ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // Remove portal blocks when base block is broken
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
            world.setBlockState(pos.up(2), Blocks.AIR.getDefaultState());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PocketPortalBlockEntity portalBlockEntity) {
                portalBlockEntity.handleEntityCollision(entity);
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }
}