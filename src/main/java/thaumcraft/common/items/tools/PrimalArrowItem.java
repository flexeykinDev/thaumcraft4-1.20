package thaumcraft.common.items.tools;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import thaumcraft.common.entity.PrimalArrow;

/**
 * Первозданная стрела как предмет (ItemPrimalArrow оригинала). Это обычная стрела для любого лука, арбалета и
 * раздатчика; «Бесконечность» сберегает её лишь в трети выстрелов, как в оригинале.
 */
public class PrimalArrowItem extends ArrowItem {
    private final int type;

    public PrimalArrowItem(int type) {
        super(new Item.Properties());
        this.type = type;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new PrimalArrow(level, shooter, type);
    }

    @Override
    public boolean isInfinite(ItemStack stack, ItemStack bow, Player player) {
        return EnchantmentHelper.getTagEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0 && player.getRandom().nextFloat() < 0.33f;
    }
}
