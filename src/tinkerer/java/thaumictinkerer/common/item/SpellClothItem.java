package thaumictinkerer.common.item;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumictinkerer.common.registry.TTRecipes;

/**
 * Заклинательная ткань (ItemSpellCloth оригинала): в сетке крафта вместе с зачарованной вещью снимает с неё все чары.
 * Ткань остаётся в сетке и изнашивается: её хватает на 35 раз.
 */
public class SpellClothItem extends Item {
    public SpellClothItem() {
        super(new Item.Properties().durability(35).setNoRepair().rarity(Rarity.UNCOMMON));
    }

    /** Оттенок ткани бледнеет по мере износа (getColorFromItemStack оригинала). */
    public static int color(ItemStack stack) {
        float left = (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage();
        return java.awt.Color.HSBtoRGB(0.75f, left * 0.5f, 1f);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    /** SpellClothRecipe оригинала: одна зачарованная вещь и одна ткань — больше ничего. */
    public static class Disenchant extends CustomRecipe {
        public Disenchant(ResourceLocation id, CraftingBookCategory category) {
            super(id, category);
        }

        private static ItemStack enchanted(CraftingContainer grid) {
            ItemStack found = ItemStack.EMPTY;
            boolean cloth = false;
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack stack = grid.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() instanceof SpellClothItem && !cloth) {
                    cloth = true;
                } else if (stack.isEnchanted() && found.isEmpty()) {
                    found = stack;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            return cloth ? found : ItemStack.EMPTY;
        }

        @Override
        public boolean matches(CraftingContainer grid, Level level) {
            return !enchanted(grid).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
            ItemStack result = enchanted(grid).copyWithCount(1);
            result.removeTagKey("Enchantments");
            return result;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
            NonNullList<ItemStack> left = NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack stack = grid.getItem(i);
                if (stack.getItem() instanceof SpellClothItem) {
                    ItemStack worn = stack.copyWithCount(1);
                    worn.setDamageValue(worn.getDamageValue() + 1);
                    left.set(i, worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn);
                }
            }
            return left;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TTRecipes.SPELL_CLOTH.get();
        }
    }
}
