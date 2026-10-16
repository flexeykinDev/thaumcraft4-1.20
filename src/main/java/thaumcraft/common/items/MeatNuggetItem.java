package thaumcraft.common.items;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** ItemNuggetEdible оригинала: мясной кусочек — одна единица сытости, съедается вдвое быстрее обычной еды. */
public class MeatNuggetItem extends Item {
    public MeatNuggetItem() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(0.3f).build()));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 10;
    }
}
