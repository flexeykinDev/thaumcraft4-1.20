package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.ArcaneBoreMenu;

/** GuiArcaneBore оригинала: ячейки фокуса и кирки, а справа — ширина, скорость и особые свойства бура. */
public class ArcaneBoreScreen extends AbstractContainerScreen<ArcaneBoreMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_arcanebore.png");

    public ArcaneBoreScreen(ArcaneBoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 141;
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
        var bore = menu.bore;
        if (bore.pickaxeWorn()) {
            graphics.blit(TEXTURE, leftPos + 74, topPos + 18, 184, 0, 16, 16);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + 112, topPos + 8, 505f);
        graphics.pose().scale(0.5f, 0.5f, 1f);
        graphics.drawString(font, Component.translatable("thaumcraft.bore.width", 1 + (bore.area + bore.maxRadius) * 2), 0, 0, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("thaumcraft.bore.speed", bore.speed), 0, 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("thaumcraft.bore.other"), 0, 24, 0xFFFFFF);
        int line = 0;
        if (bore.nativeClusters()) {
            graphics.drawString(font, Component.translatable("thaumcraft.bore.clusters"), 4, 34 + line, 0xC0C0C0);
            line += 9;
        }
        if (bore.fortune > 0) {
            graphics.drawString(font, Component.translatable("thaumcraft.bore.fortune", bore.fortune), 4, 34 + line, 0xEEC54A);
            line += 9;
        }
        if (bore.silkTouch()) {
            graphics.drawString(font, Component.translatable("thaumcraft.bore.silk"), 4, 34 + line, 0x8080FF);
        }
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
