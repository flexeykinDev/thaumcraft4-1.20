package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.registry.ModBlocks;

/**
 * Очищающие соли для ванн (ItemBathSalts оригинала): брошенные в воду, через десять секунд растворяются и
 * превращают источник воды в очищающую жидкость. Ими же заправляют магическую купальню.
 */
public class BathSaltsItem extends Item {
    public BathSaltsItem() {
        super(new Item.Properties());
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return 200;
    }

    /** Вызывается, когда брошенные соли исчезают от времени. */
    public static void dissolve(ItemEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()) {
            level.setBlockAndUpdate(pos, ModBlocks.PURIFYING_FLUID.get().defaultBlockState());
        }
    }
}
