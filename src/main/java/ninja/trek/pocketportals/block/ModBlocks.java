package ninja.trek.pocketportals.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ninja.trek.pocketportals.PocketPortals;
import ninja.trek.pocketportals.block.*;

public class ModBlocks {
    public static final PocketPortalBlock POCKET_PORTAL = new PocketPortalBlock(
            FabricBlockSettings.create()
                    .strength(3.0f)
                    .requiresTool()
                    .nonOpaque()
    );

    public static final ReturnPocketPortalBlock RETURN_POCKET_PORTAL = new ReturnPocketPortalBlock(
            FabricBlockSettings.create()
                    .strength(3.0f)
                    .requiresTool()
                    .nonOpaque()
    );

    public static final PocketPortalFrame POCKET_PORTAL_FRAME = new PocketPortalFrame(
            FabricBlockSettings.create()
                    .luminance(state -> 11)
                    .noCollision()
                    .strength(-1.0F)
    );

    public static final BlockEntityType<PocketPortalBlockEntity> POCKET_PORTAL_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    PocketPortalBlockEntity::new,
                    POCKET_PORTAL
            ).build();

    public static final BlockEntityType<ReturnPocketPortalBlockEntity> RETURN_PORTAL_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    ReturnPocketPortalBlockEntity::new,
                    RETURN_POCKET_PORTAL
            ).build();

    public static void registerBlocks() {
        Registry.register(
                Registries.BLOCK,
                Identifier.of(PocketPortals.MOD_ID, "pocket_portal"),
                POCKET_PORTAL
        );

        Registry.register(
                Registries.BLOCK,
                Identifier.of(PocketPortals.MOD_ID, "return_pocket_portal"),
                RETURN_POCKET_PORTAL
        );

        Registry.register(
                Registries.BLOCK,
                Identifier.of(PocketPortals.MOD_ID, "pocket_portal_frame"),
                POCKET_PORTAL_FRAME
        );

        Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(PocketPortals.MOD_ID, "pocket_portal_entity"),
                POCKET_PORTAL_BLOCK_ENTITY
        );

        Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(PocketPortals.MOD_ID, "return_portal_entity"),
                RETURN_PORTAL_BLOCK_ENTITY
        );
    }
}