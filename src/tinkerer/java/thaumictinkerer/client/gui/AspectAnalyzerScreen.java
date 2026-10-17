package thaumictinkerer.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientItemAspects;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.client.gui.AspectRenderer;
import thaumcraft.common.research.ScanManager;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.AspectAnalyzerBlock;

import java.util.List;

/** GuiAspectAnalyzer оригинала: под ячейкой — аспекты положенной вещи, если игрок её уже изучал. */
public class AspectAnalyzerScreen extends AbstractContainerScreen<AspectAnalyzerBlock.Menu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/aspect_analyzer.png");

    public AspectAnalyzerScreen(AspectAnalyzerBlock.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        Aspect hovered = null;
        AspectList aspects = shownAspects();
        int i = 0;
        for (Aspect aspect : aspects.getAspectsSortedByAmount()) {
            int x = leftPos + 20 + i * 18;
            int y = topPos + 58;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                hovered = aspect;
            }
            i++;
        }
        if (hovered != null) {
            graphics.renderComponentTooltip(font, List.of(hovered.getName().copy().withStyle(ChatFormatting.AQUA),
                    hovered.getDescription().copy().withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private AspectList shownAspects() {
        ItemStack stack = menu.analyzed();
        if (stack.isEmpty() || !ClientKnowledge.get().hasScanned(ScanManager.itemKey(stack))) {
            return new AspectList();
        }
        return ClientItemAspects.get(stack.getItem());
    }

    /** Подписей окна в оригинале нет: рамка ячейки маленькая, и надписи налезали бы на неё. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        AspectList aspects = shownAspects();
        int i = 0;
        for (Aspect aspect : aspects.getAspectsSortedByAmount()) {
            AspectRenderer.draw(graphics, font, aspect, aspects.getAmount(aspect), leftPos + 20 + i * 18, topPos + 58, 1f);
            i++;
        }
    }
}
