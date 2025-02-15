package ninja.trek.pocketportals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;

import static ninja.trek.pocketportals.data.PocketPortalDataTypes.DIMENSION_INDEX;

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
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PocketPortalBlockEntity(pos, state);
    }

    // In PocketPortalBlock.java, modify the onPlaced method:
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PocketPortalBlockEntity portalBE) {
                // Read dimension index from the item's components
                Integer storedIndex = stack.get(DIMENSION_INDEX);
                // If none present, get a new unused one
                if (storedIndex == null) {
                    storedIndex = PocketDimensionsRegistry.findUnusedSlot(((ServerWorld)world).getServer());
                }
                // Set it in the block entity
                portalBE.setDimensionIndex(storedIndex);
                portalBE.markDirty();
                // Create frame blocks above
                BlockPos portalPos1 = pos.up();
                BlockPos portalPos2 = portalPos1.up();
                world.setBlockState(portalPos1, ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
                world.setBlockState(portalPos2, ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
            }
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            // Only allow teleportation from overworld
            if (!world.getRegistryKey().equals(World.OVERWORLD)) {
                return;
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PocketPortalBlockEntity portalBE) {
                portalBE.handleEntityCollision(entity);
            }
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PocketPortalBlockEntity portalBE) {
                Integer dimIndex = portalBE.getDimensionIndex();

                // Create a new item with the dimension index
                ItemStack newStack = new ItemStack(ModBlocks.POCKET_PORTAL);
                if (dimIndex != null) {
                    // Store using item components
                    newStack.set(DIMENSION_INDEX, dimIndex);
                }

                // Drop the item
                Block.dropStack(world, pos, newStack);
            }
        }

        // Remove frames above
        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.up(2), Blocks.AIR.getDefaultState());

        return super.onBreak(world, pos, state, player);
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