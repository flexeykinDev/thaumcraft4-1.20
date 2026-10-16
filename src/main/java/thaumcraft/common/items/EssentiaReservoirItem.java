package thaumcraft.common.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Резервуар эссенции как предмет: стеклянный куб с каркасом, как блок. */
public class EssentiaReservoirItem extends BlockItem {
    public EssentiaReservoirItem(Block block) {
        super(block, new Item.Properties());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return thaumcraft.client.render.EssentiaReservoirRenderer.ITEM;
            }
        });
    }
}
