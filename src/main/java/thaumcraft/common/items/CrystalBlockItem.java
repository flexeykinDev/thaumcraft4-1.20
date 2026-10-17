package thaumcraft.common.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Кристаллическая гроздь (и друза странных кристаллов) в виде предмета: рисуется теми же кристаллами, что и блок. */
public class CrystalBlockItem extends BlockItem {
    public CrystalBlockItem(Block block) {
        super(block, new Item.Properties());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return getBlock() instanceof thaumcraft.common.blocks.StrangeCrystalsBlock
                        ? thaumcraft.client.render.StrangeCrystalsRenderer.ITEM : thaumcraft.client.render.CrystalRenderer.ITEM;
            }
        });
    }
}
