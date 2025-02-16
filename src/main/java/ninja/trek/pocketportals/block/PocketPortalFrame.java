package ninja.trek.pocketportals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class PocketPortalFrame extends Block {
    public static final MapCodec<PocketPortalFrame> CODEC = createCodec(PocketPortalFrame::new);

    public PocketPortalFrame(Settings settings) {
        super(validateSettings(settings));
    }

    private static Settings validateSettings(Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }
        return settings;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity.canUsePortals(false)) {
            // Find the portal base block by searching downward
            BlockPos basePos = findPortalBase(world, pos);
            if (basePos != null) {
                BlockEntity blockEntity = world.getBlockEntity(basePos);

                // Handle both types of portal bases
                if (blockEntity instanceof PocketPortalBlockEntity portalBlockEntity) {
                    portalBlockEntity.handleEntityCollision(entity);
                } else if (blockEntity instanceof ReturnPocketPortalBlockEntity returnPortalBlockEntity) {
                    returnPortalBlockEntity.handleEntityCollision(entity);
                }
            }
        }
    }



    /**
     * Allow sneak-right-click to show dimension info in chat.
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockPos basePos = findPortalBase(world, pos);
            if (basePos != null) {
                BlockEntity be = world.getBlockEntity(basePos);
                if (be instanceof PocketPortalBlockEntity portalBE) {
                    portalBE.syncSpawnRules(player);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    private BlockPos findPortalBase(World world, BlockPos startPos) {
        // Search downward for either type of portal base block
        BlockPos.Mutable currentPos = startPos.mutableCopy();
        for (int y = 0; y > -3; y--) {
            currentPos.move(0, y, 0);
            if (world.getBlockState(currentPos).getBlock() instanceof PocketPortalBlock ||
                    world.getBlockState(currentPos).getBlock() instanceof ReturnPocketPortalBlock) {
                return currentPos.toImmutable();
            }
        }
        return null;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(100) == 0) {
            world.playSound(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.BLOCK_PORTAL_AMBIENT,
                    SoundCategory.BLOCKS,
                    0.5F,
                    random.nextFloat() * 0.4F + 0.8F,
                    false
            );
        }
        for(int i = 0; i < 4; ++i) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double dx = (random.nextDouble() - 0.5D) * 0.5D;
            double dy = (random.nextDouble() - 0.5D) * 0.5D;
            double dz = (random.nextDouble() - 0.5D) * 0.5D;
            world.addParticle(ParticleTypes.PORTAL, x, y, z, dx, dy, dz);
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }
}
