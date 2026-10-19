package thaumictinkerer.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.ClientKnowledge;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.EnchanterBlock;
import thaumictinkerer.common.enchantment.EnchanterData;

import java.util.ArrayList;
import java.util.List;

/**
 * GuiEnchanting оригинала. Под вещью — до 16 значков доступных чар (по 8 в ряд); справа от окна — выбранные чары
 * в рамках с кнопками «−» и «+» уровня (щелчок по рамке убирает чары); столбики над значками показывают, сколько
 * вис каждой стихии уже собрано; круглая кнопка справа запускает зачарование.
 */
public class EnchanterScreen extends AbstractContainerScreen<EnchanterBlock.Menu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/enchanter.png");

    private final List<Integer> available = new ArrayList<>();

    public EnchanterScreen(EnchanterBlock.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private EnchanterBlock.Tile tile() {
        return menu.tile;
    }

    private void collectAvailable() {
        available.clear();
        EnchanterBlock.Tile tile = tile();
        if (tile == null) {
            return;
        }
        ItemStack stack = tile.tool();
        for (int i = 0; i < EnchanterData.all().size() && available.size() < 16; i++) {
            EnchanterData.Entry entry = EnchanterData.get(i);
            if (EnchanterData.isUnlocked(ClientKnowledge.get(), entry)
                    && EnchanterData.canApply(stack, entry.enchantment().get(), tile.enchantments)) {
                available.add(i);
            }
        }
    }

    /** Ряды значков: если чар больше восьми, первый ряд поднимается на 24 точки (как в оригинале). */
    private int buttonX(int i) {
        return leftPos + 34 + i % 8 * 16;
    }

    private int buttonY(int i) {
        return topPos + 54 + (i < 8 && available.size() > 8 ? -24 : 0);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        collectAvailable();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        EnchanterBlock.Tile tile = tile();
        if (tile == null) {
            return;
        }
        List<Component> tooltip = new ArrayList<>();
        for (int i = 0; i < available.size(); i++) {
            EnchanterData.Entry entry = EnchanterData.get(available.get(i));
            graphics.blit(entry.icon(), buttonX(i), buttonY(i), 0, 0, 16, 16, 16, 16);
            if (inside(mouseX, mouseY, buttonX(i), buttonY(i), 16, 16)) {
                tooltip.add(Component.translatable(entry.enchantment().get().getDescriptionId()));
            }
        }
        // выбранные чары справа от окна
        int right = leftPos + imageWidth + 4;
        for (int k = 0; k < tile.enchantments.size(); k++) {
            EnchanterData.Entry entry = EnchanterData.get(tile.enchantments.get(k));
            int y = topPos + k * 26;
            graphics.blit(TEXTURE, right - 4, y - 4, 176, 0, 24, 24);
            graphics.blit(entry.icon(), right, y, 0, 0, 16, 16, 16, 16);
            if (!tile.working) {
                graphics.blit(TEXTURE, right + 20, y - 4, 218, 0, 7, 7);
                graphics.blit(TEXTURE, right + 27, y - 4, 225, 0, 7, 7);
            }
            graphics.drawString(font, String.valueOf(tile.levels.get(k)), right + 22, y + 6, 0xffffff, true);
            if (inside(mouseX, mouseY, right, y, 16, 16)) {
                tooltip.add(entry.enchantment().get().getFullname(tile.levels.get(k)));
            }
        }
        // столбики собранной вис
        if (!tile.enchantments.isEmpty()) {
            int base = topPos + (available.size() > 8 ? 26 : 50);
            int x = leftPos + 55;
            for (Aspect aspect : Aspect.primals()) {
                int total = tile.totalAspects.getAmount(aspect);
                int current = tile.currentAspects.getAmount(aspect);
                int height = total == 0 ? 11 : 59;
                if (total == 0) {
                    graphics.blit(TEXTURE, x, base - height, 200, 0, 10, 4);
                    graphics.blit(TEXTURE, x, base - height + 4, 200, 52, 10, 10);
                } else {
                    int filled = (int) (48.0 * current / total);
                    int color = aspect.getColor();
                    graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1f);
                    graphics.blit(TEXTURE, x + 1, base - height + 4 + 48 - filled, 210, 48 - filled, 8, filled);
                    graphics.setColor(1f, 1f, 1f, 1f);
                    graphics.blit(TEXTURE, x, base - height, 200, 0, 10, height);
                }
                if (inside(mouseX, mouseY, x, base - height, 10, height)) {
                    tooltip.add(aspect.getName().copy().withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.literal(current + "/" + total));
                }
                x += 15;
            }
        }
        if (!tile.enchantments.isEmpty() && !tile.working && inside(mouseX, mouseY, leftPos + 151, topPos + 33, 15, 15)) {
            tooltip.add(Component.translatable("ttmisc.startEnchant").withStyle(ChatFormatting.AQUA));
        }
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        EnchanterBlock.Tile tile = tile();
        if (tile == null) {
            return;
        }
        boolean fresh = !tile.tool().isEmpty() && !tile.tool().isEnchanted();
        if (fresh && available.size() > 8) {
            graphics.blit(TEXTURE, leftPos + 30, topPos + 26, 0, imageHeight, 147, 24);
        }
        if (fresh) {
            graphics.blit(TEXTURE, leftPos + 30, topPos + 50, 0, imageHeight, 147, 24);
        }
        // кнопка запуска видна, пока выбраны чары; во время работы — другим кадром
        if (!tile.enchantments.isEmpty()) {
            graphics.blit(TEXTURE, leftPos + 151, topPos + 33, 176, tile.working ? 39 : 24, 15, 15);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        EnchanterBlock.Tile tile = tile();
        if (button == 0 && tile != null && !tile.working) {
            if (inside(mouseX, mouseY, leftPos + 151, topPos + 33, 15, 15) && !tile.enchantments.isEmpty()) {
                return press(EnchanterBlock.START);
            }
            for (int i = 0; i < available.size(); i++) {
                if (inside(mouseX, mouseY, buttonX(i), buttonY(i), 16, 16)) {
                    return press(EnchanterBlock.ADD + available.get(i));
                }
            }
            int right = leftPos + imageWidth + 4;
            for (int k = 0; k < tile.enchantments.size(); k++) {
                int y = topPos + k * 26;
                if (inside(mouseX, mouseY, right, y, 16, 16)) {
                    return press(EnchanterBlock.EDIT + k * 3);
                }
                if (inside(mouseX, mouseY, right + 20, y - 4, 7, 7)) {
                    return press(EnchanterBlock.EDIT + k * 3 + 1);
                }
                if (inside(mouseX, mouseY, right + 27, y - 4, 7, 7)) {
                    return press(EnchanterBlock.EDIT + k * 3 + 2);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
        return true;
    }
}
