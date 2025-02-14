package ninja.trek.pocketportals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ninja.trek.pocketportals.dimension.PocketDimensionsRegistry;
import ninja.trek.pocketportals.item.ModItems;
import static ninja.trek.pocketportals.data.PocketPortalDataTypes.DIMENSION_INDEX;

/**
 * Manages creation of the block entity and uses Fabric Data Components
 * to store the dimension index in the item. No direct NBT usage.
 */
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

    /**
     * Called after the block is placed in the world.
     * We read the dimension index from the item’s DataComponent,
     * or assign a new one if it's missing.
     */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);

        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PocketPortalBlockEntity portalBE) {
                // 1) Read dimension index from the placed item
                Integer storedIndex = stack.get(DIMENSION_INDEX);

                // 2) If none present, pick a new unused one
                if (storedIndex == null) {
                    storedIndex = PocketDimensionsRegistry.findUnusedSlot();
                }

                // 3) Assign it to the block entity
                portalBE.setDimensionIndex(storedIndex);
                portalBE.markDirty();

                // 4) (Optional) Create "frame" blocks above
                BlockPos portalPos1 = pos.up();
                BlockPos portalPos2 = portalPos1.up();
                world.setBlockState(portalPos1, ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
                world.setBlockState(portalPos2, ModBlocks.POCKET_PORTAL_FRAME.getDefaultState());
            }
        }
    }

    /**
     * Called when the block is broken. We create a new item that has the
     * dimension index from the block entity stored in its DataComponent,
     * then drop it in the world.
     *
     * @return
     */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PocketPortalBlockEntity portalBE) {
                Integer dimIndex = portalBE.getDimensionIndex();

                // Create a new item with that dimension index
                ItemStack newStack = new ItemStack(ModItems.POCKET_PORTAL);

                if (dimIndex != null) {
                    // Store the dimension index using data components
                    newStack.set(DIMENSION_INDEX, dimIndex);
                }

                // Drop it
                Block.dropStack(world, pos, newStack);
            }
        }

        // Remove frames above, then proceed with normal break logic
        return super.onBreak(world, pos, state, player);
//        return state;
    }

    /**
     * Pass collision handling to the block entity, which handles dimension teleports.
     */
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PocketPortalBlockEntity portalBE) {
                portalBE.handleEntityCollision(entity);
            }
        }
    }

    /**
     * If the block is replaced with a different block, remove the frame blocks above it.
     */
    @Override
    public void onStateReplaced(BlockState state, World world,
                                BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // Remove frames
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
            world.setBlockState(pos.up(2), Blocks.AIR.getDefaultState());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
