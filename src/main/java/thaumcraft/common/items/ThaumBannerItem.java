package thaumcraft.common.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.common.blocks.ThaumBannerBlock;

import java.util.function.Consumer;

/** Знамя как предмет: название по цвету полотнища, рисуется тем же знаменем, что и блок. */
public class ThaumBannerItem extends BlockItem {
    public ThaumBannerItem(Block block) {
        super(block, new Item.Properties().stacksTo(16));
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        int color = ThaumBannerBlock.getColor(stack);
        return color >= 0 ? getDescriptionId() + "." + color : getDescriptionId();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return thaumcraft.client.render.ThaumBannerRenderer.ITEM;
            }
        });
    }
}
