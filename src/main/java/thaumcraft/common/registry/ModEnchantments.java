package thaumcraft.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

/**
 * Зачарования мода (EnchantmentRepair и EnchantmentHaste оригинала). Оба можно получить и на столе зачарований,
 * и наполнением в алтаре. Что они делают — в {@link thaumcraft.common.enchant.EnchantEvents}.
 */
public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Thaumcraft.MODID);

    /** Вещи мода, которые умеют чиниться за вис (IRepairable оригинала). */
    public static final TagKey<Item> REPAIRABLE = TagKey.create(Registries.ITEM, Thaumcraft.id("repairable"));

    /** Починка: вещь понемногу восстанавливает прочность, расходуя вис из жезла владельца. */
    public static final RegistryObject<Enchantment> REPAIR = ENCHANTMENTS.register("repair",
            () -> new Enchantment(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE, EquipmentSlot.values()) {
                @Override
                public int getMinCost(int level) {
                    return 20 + (level - 1) * 10;
                }

                @Override
                public int getMaxCost(int level) {
                    return 1 + level * 10 + 50;
                }

                @Override
                public int getMaxLevel() {
                    return 2;
                }

                @Override
                public boolean canEnchant(ItemStack stack) {
                    return stack.isDamageableItem() && stack.is(REPAIRABLE) || stack.is(Items.BOOK) || stack.getItem() instanceof EnchantedBookItem;
                }

                @Override
                public boolean canApplyAtEnchantingTable(ItemStack stack) {
                    return canEnchant(stack);
                }

                @Override
                protected boolean checkCompatibility(Enchantment other) {
                    return super.checkCompatibility(other) && other != Enchantments.UNBREAKING;
                }
            });

    /** Спешка: обувь, в которой быстрее бегается. */
    public static final RegistryObject<Enchantment> HASTE = ENCHANTMENTS.register("haste",
            () -> new Enchantment(Enchantment.Rarity.RARE, EnchantmentCategory.ARMOR_FEET, new EquipmentSlot[]{EquipmentSlot.FEET}) {
                @Override
                public int getMinCost(int level) {
                    return 15 + (level - 1) * 9;
                }

                @Override
                public int getMaxCost(int level) {
                    return 1 + level * 10 + 50;
                }

                @Override
                public int getMaxLevel() {
                    return 3;
                }

                @Override
                public boolean canEnchant(ItemStack stack) {
                    return stack.getItem() instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.BOOTS
                            || stack.is(Items.BOOK) || stack.getItem() instanceof EnchantedBookItem;
                }

                @Override
                public boolean canApplyAtEnchantingTable(ItemStack stack) {
                    return canEnchant(stack);
                }
            });

    private ModEnchantments() {
    }
}
