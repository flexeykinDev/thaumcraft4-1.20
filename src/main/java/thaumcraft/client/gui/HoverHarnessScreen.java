package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.HoverHarnessMenu;

/** GuiHoverHarness оригинала: ячейка банки, а ячейка с самой упряжью помечена замком. */
public class HoverHarnessScreen extends AbstractContainerScreen<HoverHarnessMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/guihoverharness.png");

    public HoverHarnessScreen(HoverHarnessMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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
        if (menu.harnessSlot < 9) {
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 200f);
            graphics.blit(TEXTURE, 8 + menu.harnessSlot * 18, 142, 240, 0, 16, 16);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
        }
    }
}
