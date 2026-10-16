package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.AlchemicalFurnaceMenu;

/** Окно алхимической печи — перенос GuiAlchemyFurnace оригинала. */
public class AlchemicalFurnaceScreen extends AbstractContainerScreen<AlchemicalFurnaceMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_alchemyfurnace.png");

    public AlchemicalFurnaceScreen(AlchemicalFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.isBurning()) {
            int fire = menu.burnScaled(20);
            graphics.blit(TEXTURE, leftPos + 80, topPos + 26 + 20 - fire, 176, 20 - fire, 16, fire);
        }
        int cook = menu.cookScaled(46);
        graphics.blit(TEXTURE, leftPos + 106, topPos + 13 + 46 - cook, 216, 46 - cook, 9, cook);
        int contents = menu.contentsScaled(48);
        graphics.blit(TEXTURE, leftPos + 61, topPos + 12 + 48 - contents, 200, 48 - contents, 8, contents);
        // стеклянная колба поверх столбика эссенции
        graphics.blit(TEXTURE, leftPos + 60, topPos + 8, 232, 0, 10, 55);
    }

    /** В оригинале подписей в этом окне нет. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
