package thaumictinkerer.common.enchantment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registry.ModEnchantments;
import thaumcraft.common.research.PlayerKnowledge;
import thaumictinkerer.ThaumicTinkerer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Что умеет осмотический зачарователь (EnchantmentManager оригинала): какие чары, сколько вис стоит первый уровень
 * и какое исследование нужно. Цена уровня L — цена первого, умноженная на L·(1 + 0,2·L).
 */
public final class EnchanterData {
    public record Entry(Supplier<Enchantment> enchantment, ResourceLocation icon, AspectList base, String research) {
        public AspectList cost(int level) {
            AspectList out = new AspectList();
            double factor = level * (1 + level * 0.2);
            for (Aspect aspect : base.getAspects()) {
                out.add(aspect, (int) (base.getAmount(aspect) * factor));
            }
            return out;
        }
    }

    private static List<Entry> entries;

    private static AspectList a(Object... pairs) {
        AspectList list = new AspectList();
        for (int i = 0; i < pairs.length; i += 2) {
            list.add((Aspect) pairs[i], (Integer) pairs[i + 1]);
        }
        return list;
    }

    public static List<Entry> all() {
        if (entries == null) {
            List<Entry> list = new ArrayList<>();
            Aspect air = Aspect.AIR, earth = Aspect.EARTH, fire = Aspect.FIRE, water = Aspect.WATER, order = Aspect.ORDER,
                    entropy = Aspect.ENTROPY;
            add(list, () -> Enchantments.ALL_DAMAGE_PROTECTION, "protection", a(earth, 10, entropy, 7), "");
            add(list, () -> Enchantments.FIRE_PROTECTION, "fire_protection", a(fire, 10, entropy, 3, water, 4), "");
            add(list, () -> Enchantments.FALL_PROTECTION, "feather_falling", a(air, 16, order, 5), "");
            add(list, () -> Enchantments.BLAST_PROTECTION, "blast_protection", a(earth, 5, fire, 5, entropy, 8), "");
            add(list, () -> Enchantments.PROJECTILE_PROTECTION, "projectile_protection", a(air, 10, entropy, 7), "");
            add(list, () -> Enchantments.RESPIRATION, "respiration", a(water, 10, air, 8, order, 5), "");
            add(list, () -> Enchantments.AQUA_AFFINITY, "aqua_affinity", a(water, 25, order, 20, earth, 5), "");
            add(list, () -> Enchantments.THORNS, "thorns", a(earth, 10, entropy, 12), "");
            add(list, () -> Enchantments.SHARPNESS, "sharpness", a(order, 10), "");
            add(list, () -> Enchantments.SMITE, "smite", a(order, 5, air, 5), "");
            add(list, () -> Enchantments.BANE_OF_ARTHROPODS, "bane_of_arthropods", a(order, 5, fire, 5), "");
            add(list, () -> Enchantments.KNOCKBACK, "knockback", a(entropy, 5, air, 10), "");
            add(list, () -> Enchantments.FIRE_ASPECT, "fire_aspect", a(fire, 15, earth, 4), "");
            add(list, () -> Enchantments.MOB_LOOTING, "looting", a(air, 10, fire, 10, water, 10, earth, 10, order, 15, entropy, 15), "");
            add(list, () -> Enchantments.BLOCK_EFFICIENCY, "efficiency", a(entropy, 12, earth, 4), "");
            add(list, () -> Enchantments.SILK_TOUCH, "silk_touch", a(order, 50, earth, 10, entropy, 10), "");
            add(list, () -> Enchantments.UNBREAKING, "unbreaking", a(order, 15, water, 8, earth, 8), "");
            add(list, () -> Enchantments.BLOCK_FORTUNE, "fortune", a(air, 10, fire, 10, water, 10, earth, 10, order, 15, entropy, 15), "");
            add(list, () -> Enchantments.POWER_ARROWS, "power", a(earth, 5, order, 10), "");
            add(list, () -> Enchantments.PUNCH_ARROWS, "punch", a(air, 4, earth, 10, entropy, 5), "");
            add(list, () -> Enchantments.FLAMING_ARROWS, "flame", a(entropy, 5, fire, 20, earth, 5), "");
            add(list, () -> Enchantments.INFINITY_ARROWS, "infinity", a(entropy, 40, order, 40, earth, 10), "");
            add(list, () -> Enchantments.FISHING_SPEED, "lure", a(water, 20, Aspect.BEAST, 20), "");
            add(list, () -> Enchantments.FISHING_LUCK, "luck_of_the_sea", a(entropy, 20, water, 20), "");
            add(list, ModEnchantments.HASTE, "haste", a(air, 10, entropy, 5, earth, 5), "");
            add(list, ModEnchantments.REPAIR, "repair", a(water, 20, fire, 20, earth, 20, air, 20, order, 20, entropy, 5), "");
            add(list, TTEnchantments.ASCENT_BOOST, "ascent_boost", a(entropy, 8, air, 10), "TTENCH_ASCENT_BOOST");
            add(list, TTEnchantments.SLOW_FALL, "slow_fall", a(order, 8, air, 10), "TTENCH_SLOW_FALL");
            add(list, TTEnchantments.AUTO_SMELT, "auto_smelt", a(entropy, 20, fire, 30), "TTENCH_AUTO_SMELT");
            add(list, TTEnchantments.DESINTEGRATE, "desintegrate", a(entropy, 25, air, 10, earth, 10), "TTENCH_DESINTEGRATE");
            add(list, TTEnchantments.QUICK_DRAW, "quick_draw", a(order, 10, air, 10, water, 5), "TTENCH_QUICK_DRAW");
            add(list, TTEnchantments.VAMPIRISM, "vampirism", a(entropy, 8, fire, 10, water, 10), "TTENCH_VAMPIRISM");
            // в оригинале остальные боевые и горные чары открываются уже с «Вампиризмом»
            add(list, TTEnchantments.FOCUSED_STRIKES, "focused_strikes", a(order, 12, air, 10, water, 10), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.DISPERSED_STRIKES, "dispersed_strikes", a(entropy, 12, fire, 10, earth, 10), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.VALIANCE, "valiance", a(order, 12, fire, 10, earth, 10), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.FINAL_STRIKE, "final_strike", a(entropy, 16, fire, 16), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.TUNNEL, "tunnel", a(earth, 16, order, 16), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.SHATTER, "shatter", a(earth, 16, entropy, 16), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.SHOCKWAVE, "shockwave", a(earth, 16, air, 16), "TTENCH_VAMPIRISM");
            add(list, TTEnchantments.POUNCE, "pounce", a(earth, 16, air, 16), "TTENCH_VAMPIRISM");
            entries = List.copyOf(list);
        }
        return entries;
    }

    private static void add(List<Entry> list, Supplier<Enchantment> enchantment, String icon, AspectList base, String research) {
        list.add(new Entry(enchantment, ThaumicTinkerer.id("textures/enchants/" + icon + ".png"), base, research));
    }

    public static Entry get(int index) {
        return all().get(index);
    }

    public static int indexOf(Enchantment enchantment) {
        for (int i = 0; i < all().size(); i++) {
            if (all().get(i).enchantment().get() == enchantment) {
                return i;
            }
        }
        return -1;
    }

    /** canApply оригинала: чары подходят вещи и уживаются с уже выбранными. */
    public static boolean canApply(ItemStack stack, Enchantment enchantment, List<Integer> chosen) {
        if (stack.isEmpty() || stack.isEnchanted() || stack.getEnchantmentValue() == 0 || !enchantment.canEnchant(stack)) {
            return false;
        }
        for (int index : chosen) {
            Enchantment other = get(index).enchantment().get();
            if (other == enchantment || !other.isCompatibleWith(enchantment)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isUnlocked(PlayerKnowledge knowledge, Entry entry) {
        return entry.research().isEmpty() || knowledge.isResearchComplete(entry.research());
    }

    private EnchanterData() {
    }
}
