package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.TravelingTrunk;
import thaumcraft.common.menu.TrunkMenu;

/** Окно дорожного сундука — перенос GuiTravelingTrunk оригинала: ячейки, полоска здоровья и кнопка «стоять». */
public class TrunkScreen extends AbstractContainerScreen<TrunkMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/guitrunkbase.png");
    private final TravelingTrunk trunk;

    public TrunkScreen(TrunkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        trunk = menu.trunk;
        imageWidth = 176;
        imageHeight = 200;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (!trunk.isAlive()) {
            onClose();
            return;
        }
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int health = Math.round(trunk.getHealth() / trunk.getMaxHealth() * 39f);
        graphics.blit(TEXTURE, leftPos + 134, topPos + 2, 176, 16, health, 6);
        if (trunk.getUpgrade() == Golem.UPGRADE_EARTH) {
            graphics.blit(TEXTURE, leftPos, topPos + 80, 0, 206, imageWidth, 27);
        }
        if (trunk.getStay()) {
            graphics.blit(TEXTURE, leftPos + 112, topPos, 176, 0, 10, 10);
        }
        RenderSystem.disableBlend();
    }

    /** Мелкая подпись «Сундук такого-то» в левом верхнем углу. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        LivingEntity owner = trunk.getOwner();
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1f);
        graphics.drawString(font, Component.literal(owner == null ? "" : owner.getName().getString())
                .append(Component.translatable("entity.trunk.guiname")), 8, 4, 0xC0A030, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double dx = mouseX - (leftPos + 112);
        double dy = mouseY - topPos;
        if (dx >= 0 && dy >= 0 && dx < 10 && dy <= 10) {
            minecraft.level.playLocalSound(trunk.getX(), trunk.getY(), trunk.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.3f,
                    0.6f + (trunk.getStay() ? 0f : 0.2f), false);
            minecraft.player.displayClientMessage(Component.translatable(trunk.getStay() ? "entity.trunk.move" : "entity.trunk.stay"), false);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
