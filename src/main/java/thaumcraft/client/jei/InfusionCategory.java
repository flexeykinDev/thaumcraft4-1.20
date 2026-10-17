package thaumcraft.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.InfusionRecipe;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Наполнение: центральный предмет на главном пьедестале, составляющие по кругу на пьедесталах вокруг,
 * эссенция из банок и нестабильность. Для зачарования наполнением центр — любой подходящий предмет.
 */
class InfusionCategory implements IRecipeCategory<InfusionCategory.Shown> {
    static final RecipeType<Shown> TYPE = RecipeType.create("thaumcraft", "infusion", Shown.class);
    private static final int CENTER_X = 50;
    private static final int CENTER_Y = 48;
    private static final int RADIUS = 38;
    private static final int ASPECTS_Y = 94;

    /** Рецепт с уже выбранными примерами центрального предмета (у зачарования их много). */
    record Shown(InfusionRecipe recipe, List<ItemStack> centres) {
        ItemStack sample() {
            return centres.isEmpty() ? ItemStack.EMPTY : centres.get(0);
        }

        AspectList aspects() {
            return centres.isEmpty() ? recipe.getAspects() : recipe.getAspects(sample());
        }
    }

    private final IDrawable background;
    private final IDrawable icon;
    private final JeiDraw parts;

    InfusionCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(150, 160);
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.INFUSION_MATRIX.get()));
        parts = new JeiDraw(gui);
    }

    @Override
    public RecipeType<Shown> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("recipe.type.infusion");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Shown shown, IFocusGroup focuses) {
        InfusionRecipe recipe = shown.recipe();
        IRecipeSlotBuilder centre = builder.addSlot(RecipeIngredientRole.INPUT, CENTER_X - 8, CENTER_Y - 8);
        IRecipeSlotBuilder result = builder.addSlot(RecipeIngredientRole.OUTPUT, 127, CENTER_Y - 8);
        List<Ingredient> components;
        if (shown.centres().isEmpty()) {
            centre.addIngredients(recipe.getCentral());
            result.addItemStack(recipe.getResult());
            components = recipe.getComponents();
        } else {
            // зачарование наполнением: каждому примеру центра — свой зачарованный результат
            List<ItemStack> results = new ArrayList<>();
            for (ItemStack stack : shown.centres()) {
                results.add(recipe.getResult(stack));
            }
            centre.addItemStacks(shown.centres());
            result.addItemStacks(results);
            builder.createFocusLink(centre, result);
            components = recipe.getComponents(shown.sample());
        }
        for (int i = 0; i < components.size(); i++) {
            float angle = (float) (i * Math.PI * 2 / components.size() - Math.PI / 2);
            int x = CENTER_X + Math.round(Mth.cos(angle) * RADIUS) - 8;
            int y = CENTER_Y + Math.round(Mth.sin(angle) * RADIUS) - 8;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addIngredients(components.get(i));
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 127, 2).addItemStack(new ItemStack(ModItems.INFUSION_MATRIX.get()));
        JeiDraw.aspectSlots(builder, shown.aspects(), 1, ASPECTS_Y);
    }

    @Override
    public void draw(Shown shown, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        InfusionRecipe recipe = shown.recipe();
        List<Ingredient> components = shown.centres().isEmpty() ? recipe.getComponents() : recipe.getComponents(shown.sample());
        for (int i = 0; i < components.size(); i++) {
            float angle = (float) (i * Math.PI * 2 / components.size() - Math.PI / 2);
            parts.slot.draw(graphics, CENTER_X + Math.round(Mth.cos(angle) * RADIUS) - 9, CENTER_Y + Math.round(Mth.sin(angle) * RADIUS) - 9);
        }
        parts.slot.draw(graphics, CENTER_X - 9, CENTER_Y - 9);
        parts.slot.draw(graphics, 126, CENTER_Y - 9);
        parts.arrow.draw(graphics, 98, CENTER_Y - 8);

        var font = Minecraft.getInstance().font;
        int instability = shown.centres().isEmpty() ? recipe.getInstability() : recipe.getInstability(shown.sample());
        Component line = Component.translatable("tc.inst").append(Component.translatable("tc.inst." + Math.min(5, instability / 2)));
        int y = ASPECTS_Y + (shown.aspects().size() + JeiDraw.PER_ROW - 1) / JeiDraw.PER_ROW * 18 + 1;
        graphics.drawString(font, line, 1, y, 0x404040, false);
        y += 10;
        int xp = shown.centres().isEmpty() ? 0 : recipe.getXpCost(shown.sample());
        if (xp > 0) {
            graphics.drawString(font, Component.translatable("tc.infusion.levels", xp), 1, y, 0x408010, false);
            y += 10;
        }
        JeiDraw.drawResearch(graphics, recipe.getResearch(), 1, y, 148);
    }
}
