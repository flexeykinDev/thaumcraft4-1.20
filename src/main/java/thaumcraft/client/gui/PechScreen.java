package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.registry.ModSounds;

/** Окно торга с печом (GuiPech оригинала): ячейка для подарка, кости и четыре ячейки с тем, что печ дал взамен. */
public class PechScreen extends AbstractContainerScreen<PechMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_pech.png");

    public PechScreen(PechMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 175;
        imageHeight = 232;
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
        // кости загораются, когда можно торговать
        if (menu.canRoll()) {
            graphics.blit(TEXTURE, leftPos + 67, topPos + 24, 176, 0, 25, 25);
        }
    }

    /** Как в оригинале, подписей в окне нет. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double dx = mouseX - (leftPos + 67);
        double dy = mouseY - (topPos + 24);
        if (dx >= 0 && dy >= 0 && dx < 25 && dy < 25 && menu.canRoll()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    ModSounds.PECH_DICE.get(), SoundSource.NEUTRAL, 0.5f, 0.95f + minecraft.level.random.nextFloat() * 0.1f, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
