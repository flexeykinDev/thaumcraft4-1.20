package thaumictinkerer.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.kami.IchorPouchItem;

/** GuiIchorPouch: окно на всю текстуру 256×256, сама сумка в хотбаре затемнена. */
public class IchorPouchScreen extends AbstractContainerScreen<IchorPouchItem.Menu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/ichor_pouch.png");

    public IchorPouchScreen(IchorPouchItem.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 48 + menu.getPouchSlot() * 18;
        graphics.fill(x, 235, x + 16, 251, 0x90000000);
    }
}
