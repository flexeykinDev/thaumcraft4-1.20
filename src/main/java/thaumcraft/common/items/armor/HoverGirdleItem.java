package thaumcraft.common.items.armor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.RunicArmor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Пояс парения — перенос ItemGirdleHover оригинала: смягчает падения, а с летательной упряжью делает парение
 * быстрее и тратит её эссенцию на пятую часть быстрее.
 */
public class HoverGirdleItem extends Item implements ICurioItem, RunicArmor {
    public HoverGirdleItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        var wearer = context.entity();
        if (wearer.fallDistance > 0f) {
            wearer.fallDistance = Math.max(0f, wearer.fallDistance - 0.33f);
        }
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }
}
