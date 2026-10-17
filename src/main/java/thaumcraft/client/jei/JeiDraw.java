package thaumcraft.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.client.gui.AspectRenderer;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchRegistry;


/** Общие кусочки отрисовки категорий: ячейки, стрелка, ряды аспектов с подсказками и строка исследования. */
final class JeiDraw {
    static final ResourceLocation CRAFTING = thaumcraft.common.Res.of("minecraft", "textures/gui/container/crafting_table.png");
    static final int PER_ROW = 6;

    final IDrawable slot;
    final IDrawable arrow;

    JeiDraw(IGuiHelper gui) {
        slot = gui.getSlotDrawable();
        // стрелка из окна обычного верстака: есть в любой версии JEI
        arrow = gui.createDrawable(CRAFTING, 89, 34, 24, 17);
    }

    /** Аспекты рецепта ячейками-ингредиентами рядами по шесть: по ним работают подсказки и клавиши R/U. */
    static void aspectSlots(IRecipeLayoutBuilder builder, AspectList aspects, int x, int y) {
        int index = 0;
        for (AspectIngredient.AspectStack stack : AspectIngredient.of(aspects)) {
            builder.addSlot(RecipeIngredientRole.INPUT, x + index % PER_ROW * AspectRenderer.STEP, y + index / PER_ROW * AspectRenderer.STEP)
                    .addIngredient(AspectIngredient.TYPE, stack);
            index++;
        }
    }

    /** «Исследование: …» — серым, если оно уже изучено, и красным, если ещё нет. */
    static void drawResearch(GuiGraphics graphics, String research, int x, int y, int width) {
        if (research == null || research.isEmpty()) {
            return;
        }
        ResearchEntry entry = ResearchRegistry.get(research);
        if (entry == null) {
            return;
        }
        boolean known = ClientKnowledge.get().isResearchComplete(research);
        Component text = Component.translatable("jei.thaumcraft.research", entry.getName());
        var font = Minecraft.getInstance().font;
        // мельче обычного: длинные названия исследований не помещаются в ширину рецепта
        String line = text.getString();
        int room = (int) (width / 0.75f);
        if (font.width(line) > room) {
            line = font.plainSubstrByWidth(line, room - font.width("…")) + "…";
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0f);
        graphics.pose().scale(0.75f, 0.75f, 1f);
        graphics.drawString(font, line, 0, 0, known ? 0x707070 : 0xa04040, false);
        graphics.pose().popPose();
    }
}
