package thaumcraft.common.aspects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.WandItem;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Добавки к аспектам, зависящие от самого предмета: тип инструмента, чары, эффекты зелья.
 * Соответствует getBonusTags и зельям из getObjectTags оригинала. Работает одинаково на сервере и клиенте.
 */
public final class AspectBonus {
    private static final int MAX_ASPECTS = 6;

    private static final Map<Enchantment, Aspect> ENCHANTMENTS = new IdentityHashMap<>();
    private static final Map<MobEffect, Aspect> EFFECTS = new IdentityHashMap<>();

    static {
        ENCHANTMENTS.put(Enchantments.AQUA_AFFINITY, Aspect.WATER);
        ENCHANTMENTS.put(Enchantments.BANE_OF_ARTHROPODS, Aspect.BEAST);
        ENCHANTMENTS.put(Enchantments.BLAST_PROTECTION, Aspect.ARMOR);
        ENCHANTMENTS.put(Enchantments.BLOCK_EFFICIENCY, Aspect.TOOL);
        ENCHANTMENTS.put(Enchantments.FALL_PROTECTION, Aspect.FLIGHT);
        ENCHANTMENTS.put(Enchantments.FIRE_ASPECT, Aspect.FIRE);
        ENCHANTMENTS.put(Enchantments.FIRE_PROTECTION, Aspect.ARMOR);
        ENCHANTMENTS.put(Enchantments.FLAMING_ARROWS, Aspect.FIRE);
        ENCHANTMENTS.put(Enchantments.BLOCK_FORTUNE, Aspect.GREED);
        ENCHANTMENTS.put(Enchantments.INFINITY_ARROWS, Aspect.CRAFT);
        ENCHANTMENTS.put(Enchantments.KNOCKBACK, Aspect.AIR);
        ENCHANTMENTS.put(Enchantments.MOB_LOOTING, Aspect.GREED);
        ENCHANTMENTS.put(Enchantments.POWER_ARROWS, Aspect.WEAPON);
        ENCHANTMENTS.put(Enchantments.PROJECTILE_PROTECTION, Aspect.ARMOR);
        ENCHANTMENTS.put(Enchantments.ALL_DAMAGE_PROTECTION, Aspect.ARMOR);
        ENCHANTMENTS.put(Enchantments.PUNCH_ARROWS, Aspect.AIR);
        ENCHANTMENTS.put(Enchantments.RESPIRATION, Aspect.AIR);
        ENCHANTMENTS.put(Enchantments.SHARPNESS, Aspect.WEAPON);
        ENCHANTMENTS.put(Enchantments.SILK_TOUCH, Aspect.EXCHANGE);
        ENCHANTMENTS.put(Enchantments.THORNS, Aspect.WEAPON);
        ENCHANTMENTS.put(Enchantments.SMITE, Aspect.ENTROPY);
        ENCHANTMENTS.put(Enchantments.UNBREAKING, Aspect.EARTH);
        ENCHANTMENTS.put(Enchantments.FISHING_LUCK, Aspect.GREED);
        ENCHANTMENTS.put(Enchantments.FISHING_SPEED, Aspect.BEAST);

        EFFECTS.put(MobEffects.BLINDNESS, Aspect.DARKNESS);
        EFFECTS.put(MobEffects.CONFUSION, Aspect.ELDRITCH);
        EFFECTS.put(MobEffects.DAMAGE_BOOST, Aspect.WEAPON);
        EFFECTS.put(MobEffects.DIG_SLOWDOWN, Aspect.TRAP);
        EFFECTS.put(MobEffects.DIG_SPEED, Aspect.TOOL);
        EFFECTS.put(MobEffects.HARM, Aspect.DEATH);
        EFFECTS.put(MobEffects.HEAL, Aspect.HEAL);
        EFFECTS.put(MobEffects.HUNGER, Aspect.DEATH);
        EFFECTS.put(MobEffects.INVISIBILITY, Aspect.SENSES);
        EFFECTS.put(MobEffects.JUMP, Aspect.FLIGHT);
        EFFECTS.put(MobEffects.MOVEMENT_SLOWDOWN, Aspect.TRAP);
        EFFECTS.put(MobEffects.MOVEMENT_SPEED, Aspect.MOTION);
        EFFECTS.put(MobEffects.NIGHT_VISION, Aspect.SENSES);
        EFFECTS.put(MobEffects.POISON, Aspect.POISON);
        EFFECTS.put(MobEffects.REGENERATION, Aspect.HEAL);
        EFFECTS.put(MobEffects.DAMAGE_RESISTANCE, Aspect.ARMOR);
        EFFECTS.put(MobEffects.WATER_BREATHING, Aspect.AIR);
        EFFECTS.put(MobEffects.WEAKNESS, Aspect.DEATH);
    }

    /** Итоговые аспекты предмета: базовые (из таблицы и рецептов) плюс добавки, не больше шести. */
    public static AspectList apply(ItemStack stack, AspectList base) {
        AspectList out = base.copy();
        Item item = stack.getItem();

        if (item instanceof PotionItem) {
            out.merge(Aspect.WATER, 1);
            if (item instanceof SplashPotionItem) {
                out.merge(Aspect.ENTROPY, 2);
            }
            for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
                int level = effect.getAmplifier() + 1;
                out.merge(Aspect.MAGIC, level * 2);
                if (effect.getEffect() == MobEffects.FIRE_RESISTANCE) {
                    out.merge(Aspect.ARMOR, level);
                    out.merge(Aspect.FIRE, level * 2);
                } else {
                    Aspect aspect = EFFECTS.get(effect.getEffect());
                    if (aspect != null) {
                        out.merge(aspect, level * 3);
                    }
                }
            }
        }

        if (item instanceof WandItem) {
            int cost = WandItem.getRod(stack).getCraftCost() + WandItem.getCap(stack).getCraftCost();
            out.merge(Aspect.MAGIC, cost / 2);
            out.merge(Aspect.TOOL, cost / 3);
        }

        if (item instanceof ArmorItem armor) {
            out.merge(Aspect.ARMOR, armor.getDefense());
        } else if (item instanceof SwordItem sword) {
            out.merge(Aspect.WEAPON, (int) (sword.getTier().getAttackDamageBonus() + 1f));
        } else if (item instanceof BowItem) {
            out.merge(Aspect.WEAPON, 3).merge(Aspect.FLIGHT, 1);
        } else if (item instanceof PickaxeItem pickaxe) {
            out.merge(Aspect.MINE, pickaxe.getTier().getLevel() + 1);
        } else if (item instanceof ShearsItem || item instanceof HoeItem) {
            int durability = stack.getMaxDamage();
            if (durability <= Tiers.WOOD.getUses()) {
                out.merge(Aspect.HARVEST, 1);
            } else if (durability <= Tiers.STONE.getUses() || durability <= Tiers.GOLD.getUses()) {
                out.merge(Aspect.HARVEST, 2);
            } else if (durability <= Tiers.IRON.getUses()) {
                out.merge(Aspect.HARVEST, 3);
            } else {
                out.merge(Aspect.HARVEST, 4);
            }
        } else if (item instanceof DiggerItem tool) {
            out.merge(Aspect.TOOL, tool.getTier().getLevel() + 1);
        }

        int levels = 0;
        for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            Aspect aspect = ENCHANTMENTS.get(e.getKey());
            // зачарования самого мода: к моменту заполнения таблицы выше они ещё не зарегистрированы
            if (e.getKey() == thaumcraft.common.registry.ModEnchantments.HASTE.get()) {
                aspect = Aspect.MOTION;
            } else if (e.getKey() == thaumcraft.common.registry.ModEnchantments.REPAIR.get()) {
                aspect = Aspect.TOOL;
            }
            if (aspect != null) {
                out.merge(aspect, e.getValue());
            }
            levels += e.getValue();
        }
        if (levels > 0) {
            out.merge(Aspect.MAGIC, levels);
        }

        return cull(out);
    }

    /** Оставляет не больше шести аспектов, отбрасывая самые слабые; сложные аспекты ценятся выше простых. */
    public static AspectList cull(AspectList aspects) {
        AspectList out = aspects.copy();
        while (out.size() > MAX_ASPECTS) {
            Aspect lowest = null;
            float low = Float.MAX_VALUE;
            for (Aspect aspect : out.getAspects()) {
                float weight = out.getAmount(aspect) * weight(aspect);
                if (weight < low) {
                    low = weight;
                    lowest = aspect;
                }
            }
            out.remove(lowest);
        }
        return out;
    }

    private static float weight(Aspect aspect) {
        if (aspect.isPrimal()) {
            return 0.9f;
        }
        return componentWeight(aspect.getFirstComponent()) * componentWeight(aspect.getSecondComponent());
    }

    private static float componentWeight(Aspect component) {
        if (component.isPrimal()) {
            return 1f;
        }
        float weight = 1.1f;
        if (!component.getFirstComponent().isPrimal()) {
            weight *= 1.05f;
        }
        if (!component.getSecondComponent().isPrimal()) {
            weight *= 1.05f;
        }
        return weight;
    }

    private AspectBonus() {
    }
}
