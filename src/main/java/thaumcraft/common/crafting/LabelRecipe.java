package thaumcraft.common.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.LabelItem;
import thaumcraft.common.items.PhialItem;
import thaumcraft.common.registry.ModRecipes;

/**
 * Ярлык с аспектом: чистый ярлык и колба эссенции на обычном верстаке (рецепты JarLabel0..N оригинала —
 * там их было по одному на каждый аспект, здесь один рецепт читает аспект из колбы).
 */
public class LabelRecipe extends CustomRecipe {
    public LabelRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    /** @return аспект будущего ярлыка либо null, если раскладка не подходит */
    private static Aspect find(CraftingContainer container) {
        boolean label = false;
        Aspect aspect = null;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof LabelItem && LabelItem.getAspect(stack) == null && !label) {
                label = true;
            } else if (stack.getItem() instanceof PhialItem && PhialItem.getAspect(stack) != null && aspect == null) {
                aspect = PhialItem.getAspect(stack);
            } else {
                return null;
            }
        }
        return label ? aspect : null;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return find(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        Aspect aspect = find(container);
        if (aspect == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).getItem() instanceof LabelItem label) {
                return LabelItem.withAspect(label, aspect);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.LABEL.get();
    }
}
