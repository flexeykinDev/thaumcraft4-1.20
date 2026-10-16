package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.menu.DeconstructionTableMenu;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/** Окно стола разборки — перенос GuiDeconstructionTable оригинала. */
public class DeconstructionTableScreen extends AbstractContainerScreen<DeconstructionTableMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_decontable.png");

    public DeconstructionTableScreen(DeconstructionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    private boolean overAspect(double mouseX, double mouseY) {
        double dx = mouseX - (leftPos + 64);
        double dy = mouseY - (topPos + 48);
        return dx >= 0 && dy >= 0 && dx < 16 && dy < 16;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        Aspect aspect = menu.table.aspect;
        if (aspect != null && overAspect(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(aspect.getName(), aspect.getDescription().copy().withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int left = menu.breakScaled(46);
        if (left > 0) {
            graphics.blit(TEXTURE, leftPos + 93, topPos + 15 + 46 - left, 176, 46 - left, 9, left);
        }
        if (menu.table.aspect != null) {
            AspectRenderer.draw(graphics, font, menu.table.aspect, 0, leftPos + 64, topPos + 48, 1f);
        }
    }

    /** В оригинале подписей в этом окне нет. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.table.aspect != null && overAspect(mouseX, mouseY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    ModSounds.HHOFF.get(), SoundSource.BLOCKS, 0.2f, 1f + minecraft.level.random.nextFloat() * 0.1f, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
