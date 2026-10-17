package thaumcraft.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.registry.ModItems;

import java.util.List;

/** Тигель: катализатор, растворённые в воде аспекты и то, во что превращается катализатор. */
class CrucibleCategory implements IRecipeCategory<CrucibleRecipe> {
    static final RecipeType<CrucibleRecipe> TYPE = RecipeType.create("thaumcraft", "crucible", CrucibleRecipe.class);
    private static final int ASPECTS_Y = 26;

    private final IDrawable background;
    private final IDrawable icon;
    private final JeiDraw parts;

    CrucibleCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(116, 74);
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.CRUCIBLE.get()));
        parts = new JeiDraw(gui);
    }

    @Override
    public RecipeType<CrucibleRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("recipe.type.crucible");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).addIngredients(recipe.getCatalyst());
        builder.addSlot(RecipeIngredientRole.CATALYST, 33, 1).addItemStack(new ItemStack(ModItems.CRUCIBLE.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 1).addItemStack(recipe.getResult());
        JeiDraw.aspectSlots(builder, recipe.getAspects(), 1, ASPECTS_Y);
    }

    @Override
    public void draw(CrucibleRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        parts.slot.draw(graphics, 0, 0);
        parts.slot.draw(graphics, 94, 0);
        parts.arrow.draw(graphics, 60, 0);
        JeiDraw.drawResearch(graphics, recipe.getResearch(), 1, 26 + (recipe.getAspects().size() + 5) / 6 * 18 + 2, 116);
    }
}
