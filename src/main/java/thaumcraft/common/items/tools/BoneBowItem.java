package thaumcraft.common.items.tools;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Костяной лук — перенос ItemBowBone оригинала. Натягивается вдвое быстрее обычного (полная сила за полсекунды),
 * сам спускает тетиву, если держать дольше 18 тактов, пускает стрелы на четверть быстрее обычного лука (в оригинале —
 * сила 2.5 против 2) и бьёт ими на полединицы сильнее; «критических» выстрелов у него, как в оригинале, нет.
 */
public class BoneBowItem extends BowItem {
    public BoneBowItem() {
        super(new Item.Properties().durability(512));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (getUseDuration(stack) - remaining > 18) {
            user.releaseUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) {
            return;
        }
        boolean infinite = player.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0;
        ItemStack ammo = player.getProjectile(bow);
        // натяг считается вдвое быстрее, чем у обычного лука
        int charge = ForgeEventFactory.onArrowLoose(bow, level, player, (getUseDuration(bow) - timeLeft) * 2, !ammo.isEmpty() || infinite);
        if (charge < 0 || ammo.isEmpty() && !infinite) {
            return;
        }
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.ARROW);
        }
        float power = getPowerForTime(charge);
        if (power < 0.1f) {
            return;
        }
        boolean free = player.getAbilities().instabuild || ammo.getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(ammo, bow, player);
        if (!level.isClientSide) {
            ArrowItem arrowItem = (ArrowItem) (ammo.getItem() instanceof ArrowItem ? ammo.getItem() : Items.ARROW);
            AbstractArrow arrow = customArrow(arrowItem.createArrow(level, ammo, player));
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, power * 3.75f, 1f);
            int strength = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
            if (strength > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + strength * 0.5 + 0.5);
            }
            int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
            if (punch > 0) {
                arrow.setKnockback(punch);
            }
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) {
                arrow.setSecondsOnFire(100);
            }
            bow.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(player.getUsedItemHand()));
            if (free || player.getAbilities().instabuild && (ammo.is(Items.SPECTRAL_ARROW) || ammo.is(Items.TIPPED_ARROW))) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(arrow);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f,
                1f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
        if (!free && !player.getAbilities().instabuild) {
            ammo.shrink(1);
            if (ammo.isEmpty()) {
                player.getInventory().removeItem(ammo);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() + 0.5);
        return arrow;
    }

    @Override
    public int getEnchantmentValue() {
        return 3;
    }
}
