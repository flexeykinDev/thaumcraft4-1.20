package thaumcraft.common.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

/** Материалы инструментов и брони — числа из ThaumcraftApi оригинала. */
public final class ModMaterials {
    /** Тауминий: уровень добычи алмаза, прочность 400, скорость 7, урон +2, зачаровываемость 22. */
    public static final Tier THAUMIUM_TOOL = new ForgeTier(3, 400, 7f, 2f, 22, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()));

    /** Стихийный тауминий (toolMatElemental): уровень алмаза, прочность 1500, скорость 10, урон +3, зачаровываемость 18. */
    public static final Tier ELEMENTAL_TOOL = new ForgeTier(3, 1500, 10f, 3f, 18, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()));

    /** «Особый» материал оригинала (armorMatSpecial): защита 1/3/2/1, зачаровываемость 25; из него сделаны очки откровения. */
    public static final ArmorMaterial SPECIAL_ARMOR = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 25 * switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 1;
                case CHESTPLATE -> 3;
                case LEGGINGS -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 25;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_GENERIC;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "thaumcraft:special";
        }

        @Override
        public float getToughness() {
            return 0f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    };

    /** Тауминий: прочность x25, защита 2/6/5/2 (шлем/нагрудник/поножи/ботинки), зачаровываемость 25. */
    /**
     * Крепостной тауминий (armorMatThaumiumFortress): прочность 40, защита 3/7/6, зачаровываемость 25. Твёрдость 2 —
     * как у алмазной брони 1.20: по книге оригинала каждая часть защищает так же, как алмазная.
     */
    public static final ArmorMaterial FORTRESS_ARMOR = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 40 * switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 3;
                case CHESTPLATE -> 7;
                case LEGGINGS -> 6;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 25;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get());
        }

        @Override
        public String getName() {
            return "thaumcraft:fortress";
        }

        @Override
        public float getToughness() {
            return 2f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    };

    public static final ArmorMaterial THAUMIUM_ARMOR = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 25 * switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 2;
                case CHESTPLATE -> 6;
                case LEGGINGS -> 5;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 25;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get());
        }

        @Override
        public String getName() {
            return "thaumcraft:thaumium";
        }

        @Override
        public float getToughness() {
            return 0f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    };

    /** Пустотный металл (toolMatVoid): уровень добычи 4 (как незерит), прочность 150, скорость 8, урон +3, зачаровываемость 10. */
    public static final Tier VOID_TOOL = new ForgeTier(4, 150, 8f, 3f, 10, net.minecraftforge.common.Tags.Blocks.NEEDS_NETHERITE_TOOL,
            () -> Ingredient.of(ModItems.VOID_METAL_INGOT.get()));

    /** Пустотная броня (armorMatVoid): прочность x10, защита 3/7/6/3, зачаровываемость 10. */
    public static final ArmorMaterial VOID_ARMOR = armor("void", 10, 3, 7, 6, 3, 10, 0f, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.VOID_METAL_INGOT.get()));

    /** Пустотная крепостная мантия (armorMatVoidFortress): прочность x18, защита 4/8/7/4, зачаровываемость 10, твёрдость 2. */
    public static final ArmorMaterial VOID_FORTRESS_ARMOR = armor("void_fortress", 18, 4, 8, 7, 4, 10, 2f, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ModItems.VOID_METAL_INGOT.get()));

    private static ArmorMaterial armor(String name, int durability, int helmet, int chest, int legs, int boots, int enchantment,
                                       float toughness, SoundEvent sound, java.util.function.Supplier<Ingredient> repair) {
        return new ArmorMaterial() {
            @Override
            public int getDurabilityForType(ArmorItem.Type type) {
                return durability * switch (type) {
                    case HELMET -> 11;
                    case CHESTPLATE -> 16;
                    case LEGGINGS -> 15;
                    case BOOTS -> 13;
                };
            }

            @Override
            public int getDefenseForType(ArmorItem.Type type) {
                return switch (type) {
                    case HELMET -> helmet;
                    case CHESTPLATE -> chest;
                    case LEGGINGS -> legs;
                    case BOOTS -> boots;
                };
            }

            @Override
            public int getEnchantmentValue() {
                return enchantment;
            }

            @Override
            public SoundEvent getEquipSound() {
                return sound;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repair.get();
            }

            @Override
            public String getName() {
                return "thaumcraft:" + name;
            }

            @Override
            public float getToughness() {
                return toughness;
            }

            @Override
            public float getKnockbackResistance() {
                return 0f;
            }
        };
    }

    private ModMaterials() {
    }
}
