package thaumcraft.common.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.world.NodeGenerator;

import java.util.function.Consumer;

/**
 * Узел ауры в творческой вкладке (blockAiry мета 0 и ItemNodeRenderer оригинала): поставленный, он становится
 * случайным узлом — как те, что рождаются в мире.
 */
public class AuraNodeItem extends BlockItem {
    public AuraNodeItem(Block block) {
        super(block, new Item.Properties());
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        if (!level.isClientSide) {
            NodeGenerator.fill(level, pos, level.random, false, false);
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return thaumcraft.client.render.NodeJarRenderer.NODE_ITEM;
            }
        });
    }
}
