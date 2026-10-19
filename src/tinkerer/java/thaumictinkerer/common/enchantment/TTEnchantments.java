package thaumictinkerer.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;

import java.util.function.Predicate;

/**
 * Чары Thaumic Tinkerer (ModEnchantments оригинала). Ни на столе зачарований, ни в книгах, ни у жителей их нет —
 * их накладывает только осмотический зачарователь.
 */
public final class TTEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ThaumicTinkerer.MODID);

    private static final EquipmentSlot[] HAND = {EquipmentSlot.MAINHAND};
    private static final EquipmentSlot[] LEGS = {EquipmentSlot.LEGS};
    private static final EquipmentSlot[] FEET = {EquipmentSlot.FEET};

    public static final RegistryObject<Enchantment> ASCENT_BOOST = register("ascent_boost", 4, EnchantmentCategory.ARMOR_LEGS, LEGS, e -> true);
    public static final RegistryObject<Enchantment> SLOW_FALL = register("slow_fall", 3, EnchantmentCategory.ARMOR_FEET, FEET,
            e -> e != Enchantments.FALL_PROTECTION);
    // мгновенная рубка дерева и мгновенная добыча мягких блоков: в паре можно только с прочностью и починкой
    public static final RegistryObject<Enchantment> AUTO_SMELT = register("auto_smelt", 1, EnchantmentCategory.DIGGER, HAND,
            e -> e == Enchantments.UNBREAKING || e == thaumcraft.common.registry.ModEnchantments.REPAIR.get());
    public static final RegistryObject<Enchantment> DESINTEGRATE = register("desintegrate", 1, EnchantmentCategory.DIGGER, HAND,
            e -> e == Enchantments.UNBREAKING || e == thaumcraft.common.registry.ModEnchantments.REPAIR.get());
    public static final RegistryObject<Enchantment> QUICK_DRAW = register("quick_draw", 2, EnchantmentCategory.BOW, HAND, e -> true);
    public static final RegistryObject<Enchantment> VAMPIRISM = register("vampirism", 2, EnchantmentCategory.WEAPON, HAND,
            e -> e != Enchantments.FIRE_ASPECT && e != Enchantments.KNOCKBACK);
    public static final RegistryObject<Enchantment> DISPERSED_STRIKES = register("dispersed_strikes", 5, EnchantmentCategory.WEAPON, HAND, e -> true);
    public static final RegistryObject<Enchantment> FINAL_STRIKE = register("final_strike", 5, EnchantmentCategory.WEAPON, HAND, e -> true);
    public static final RegistryObject<Enchantment> FOCUSED_STRIKES = register("focused_strikes", 5, EnchantmentCategory.WEAPON, HAND, e -> true);
    public static final RegistryObject<Enchantment> POUNCE = register("pounce", 5, EnchantmentCategory.ARMOR_LEGS, LEGS, e -> true);
    public static final RegistryObject<Enchantment> SHATTER = register("shatter", 5, EnchantmentCategory.DIGGER, HAND, e -> true);
    public static final RegistryObject<Enchantment> SHOCKWAVE = register("shockwave", 5, EnchantmentCategory.ARMOR_FEET, FEET,
            e -> e != Enchantments.FALL_PROTECTION && e != SLOW_FALL.get());
    public static final RegistryObject<Enchantment> TUNNEL = register("tunnel", 5, EnchantmentCategory.DIGGER, HAND, e -> true);
    public static final RegistryObject<Enchantment> VALIANCE = register("valiance", 5, EnchantmentCategory.WEAPON, HAND, e -> true);

    private static RegistryObject<Enchantment> register(String name, int maxLevel, EnchantmentCategory category, EquipmentSlot[] slots,
                                                        Predicate<Enchantment> compatible) {
        return ENCHANTMENTS.register(name, () -> new Osmotic(maxLevel, category, slots, compatible));
    }

    /** EnchantmentMod оригинала. */
    public static class Osmotic extends Enchantment {
        private final int maxLevel;
        private final Predicate<Enchantment> compatible;

        Osmotic(int maxLevel, EnchantmentCategory category, EquipmentSlot[] slots, Predicate<Enchantment> compatible) {
            super(Rarity.VERY_RARE, category, slots);
            this.maxLevel = maxLevel;
            this.compatible = compatible;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            return super.checkCompatibility(other) && compatible.test(other);
        }

        /** Подходит ли вещь — только по виду вещи: запрет стола зачарований ниже сюда не относится. */
        @Override
        public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
            return category.canEnchant(stack.getItem());
        }

        @Override
        public boolean canApplyAtEnchantingTable(net.minecraft.world.item.ItemStack stack) {
            return false;
        }

        @Override
        public boolean isAllowedOnBooks() {
            return false;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }

        @Override
        public boolean isDiscoverable() {
            return false;
        }
    }

    private TTEnchantments() {
    }
}
