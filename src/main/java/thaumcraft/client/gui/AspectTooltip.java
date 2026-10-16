package thaumcraft.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import thaumcraft.api.aspects.AspectList;

/** Строка иконок аспектов с количествами в подсказке предмета. */
public record AspectTooltip(AspectList aspects) implements TooltipComponent {

    public static class Renderer implements ClientTooltipComponent {
        private final AspectList aspects;

        public Renderer(AspectTooltip tooltip) {
            this.aspects = tooltip.aspects();
        }

        @Override
        public int getHeight() {
            return AspectRenderer.ICON + 4;
        }

        @Override
        public int getWidth(Font font) {
            return AspectRenderer.rowWidth(aspects);
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            AspectRenderer.drawRow(graphics, font, aspects, x, y);
        }
    }
}
