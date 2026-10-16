package thaumcraft.common.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.research.Warp;

/**
 * Мозг зомби (ItemZombieBrain оригинала): выпадает из злых зомби. Съедобен, но голод от него почти обеспечен,
 * а главное — каждый съеденный мозг добавляет искажения: обычно временного, изредка «липкого».
 */
public class ZombieBrainItem extends Item {
    public ZombieBrainItem() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.2f).meat()
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8f).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            if (level.random.nextFloat() < 0.1f) {
                Warp.addSticky(player, 1);
            } else {
                Warp.add(player, 1 + level.random.nextInt(3), true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
