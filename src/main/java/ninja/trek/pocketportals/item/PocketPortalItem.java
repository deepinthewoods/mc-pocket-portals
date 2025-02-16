package ninja.trek.pocketportals.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import ninja.trek.pocketportals.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ninja.trek.pocketportals.data.PocketPortalDataTypes.DIMENSION_INDEX;

/**
 * Custom PocketPortalItem that reads the dimension index from
 * Fabric Data Components for its tooltip (instead of raw NBT).
 */
public class PocketPortalItem extends BlockItem {

    public PocketPortalItem(Settings settings) {
        super(ModBlocks.POCKET_PORTAL, validateSettings(settings));
    }

    private static Settings validateSettings(Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }
        return settings;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        // 1) Let the parent BlockItem do any default work
        super.appendTooltip(stack, context, tooltip, type);

        // 2) Fetch our dimension index from Fabric Data Components
        Integer index = stack.get(DIMENSION_INDEX); // If none set, returns null

        // 3) Show a line of text based on that index
        if (index != null) {
            tooltip.add(Text.literal("Bound to dimension index: " + index)
                    .formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.literal("Unlinked").formatted(Formatting.DARK_GRAY));
        }
    }


}
