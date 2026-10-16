package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.TallowCandleBlock;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.items.golem.GolemCoreItem;
import thaumcraft.common.menu.GolemMenu;

/**
 * Окно голема — перенос GuiGolem оригинала: слева сам голем и его реплика, справа ячейки-образцы с цветами,
 * а под ними и рядом — переключатели, которые зависят от ядра и улучшений.
 */
public class GolemScreen extends AbstractContainerScreen<GolemMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/guigolem.png");
    private static final int TEXT = 0xFDFDFD;
    private static final int TEXT_OFF = 0x666666;

    private final Golem golem;
    /** Продвинутый голем иногда вместо обычной реплики говорит что-нибудь зловещее. */
    private final int threat;

    public GolemScreen(GolemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        golem = menu.golem;
        imageWidth = 176;
        imageHeight = 166;
        threat = golem.isAdvanced() && golem.getRandom().nextInt(4) == 0 ? golem.getRandom().nextInt(9) : -1;
    }

    private int slots() {
        return golem.inventory.getContainerSize();
    }

    private boolean hasSamples() {
        return golem.getCore() > -1 && GolemCoreItem.hasInventory(golem.getCore());
    }

    private boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        double dx = mouseX - (leftPos + x);
        double dy = mouseY - (topPos + y);
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }

    private static Component colorName(int color) {
        return color < 0 ? Component.translatable("gui.thaumcraft.golem.any_color")
                : Component.translatable("color.minecraft." + DyeColor.byId(color).getName());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        // название цвета над ячейками, когда курсор на полоске цвета
        if (hasSamples() && golem.getUpgradeAmount(Golem.UPGRADE_ORDER) > 0) {
            for (int cell = 0; cell < Math.min(6, slots()); cell++) {
                if (over(mouseX, mouseY, 96 + cell / 2 * 28, 4 + cell % 2 * 31, 24, 12)) {
                    Component text = colorName(golem.getColors(cell + menu.currentScroll * 6));
                    graphics.drawString(font, text, leftPos + 133 - font.width(text) / 2, topPos - 6, TEXT, false);
                }
            }
        }
    }

    private void box(GuiGraphics graphics, int x, int y, boolean on) {
        graphics.blit(TEXTURE, leftPos + x, topPos + y, 8, 168, 8, 8);
        if (on) {
            graphics.blit(TEXTURE, leftPos + x, topPos + y, 8, 176, 8, 8);
        }
    }

    private void small(GuiGraphics graphics, Component text, int x, int y, int row, int color, boolean centred) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + x, topPos + y, 0);
        graphics.pose().scale(0.5f, 0.5f, 1f);
        graphics.drawString(font, text, centred ? -font.width(text) / 2 : 0, row * 20, color, false);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int baseX = leftPos;
        int baseY = topPos;
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, baseX, baseY, 0, 0, imageWidth, imageHeight);
        // сам голем слева, крупно; подписи и переключатели рисуются поверх него
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, baseX + 51, baseY + 76, 64, baseX + 51 - mouseX, baseY + 25 - mouseY, golem);
        RenderSystem.enableBlend();
        int core = golem.getCore();
        int slots = slots();
        int typeLoc = golem.getGolemType().ordinal() * 24;
        if (hasSamples()) {
            for (int cell = 0; cell < Math.min(6, slots); cell++) {
                graphics.blit(TEXTURE, baseX + 96 + cell / 2 * 28, baseY + 12 + cell % 2 * 31, 184, typeLoc, 24, 24);
                if (golem.getUpgradeAmount(Golem.UPGRADE_ORDER) > 0) {
                    graphics.blit(TEXTURE, baseX + 96 + cell / 2 * 28, baseY + 4 + cell % 2 * 31, 72, 168, 24, 12);
                    int color = golem.getColors(cell + menu.currentScroll * 6);
                    if (color > -1) {
                        int rgb = TallowCandleBlock.COLORS[color];
                        graphics.setColor((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1f);
                        graphics.blit(TEXTURE, baseX + 105 + cell / 2 * 28, baseY + 7 + cell % 2 * 31, 0, 176, 6, 6);
                        graphics.setColor(1f, 1f, 1f, 1f);
                    }
                }
            }
            if (slots > 6) {
                graphics.blit(TEXTURE, baseX + 111, baseY + 68, 0, menu.currentScroll > 0 ? 200 : 208, 24, 8);
                graphics.blit(TEXTURE, baseX + 135, baseY + 68, 24, menu.currentScroll < menu.maxScroll ? 200 : 208, 24, 8);
            }
        }
        if (core == GolemCoreItem.GUARD && golem.getUpgradeAmount(Golem.UPGRADE_ORDER) > 0) {
            box(graphics, 104, 5, golem.canAttackHostiles());
            box(graphics, 104, 21, golem.canAttackAnimals());
            box(graphics, 104, 37, golem.canAttackPlayers());
            box(graphics, 104, 53, golem.canAttackCreepers());
            graphics.drawString(font, Component.translatable("gui.thaumcraft.golem.monsters"), baseX + 122, baseY + 6, 0xFFCCCC, false);
            graphics.drawString(font, Component.translatable("gui.thaumcraft.golem.animals"), baseX + 122, baseY + 22, 0xFFFFCC, false);
            graphics.drawString(font, Component.translatable("gui.thaumcraft.golem.players"), baseX + 122, baseY + 38, 0xCCCCFF, false);
            graphics.drawString(font, Component.translatable("gui.thaumcraft.golem.creepers"), baseX + 122, baseY + 54, 0xCCFFCC, false);
        }
        boolean[] toggles = golem.getToggles();
        if (core == GolemCoreItem.FILL) {
            box(graphics, 62, 54, !toggles[0]);
            small(graphics, Component.translatable(toggles[0] ? "gui.thaumcraft.golem.any_amount" : "gui.thaumcraft.golem.precise"), 66, 48, 0,
                    TEXT, true);
        }
        if (core == GolemCoreItem.USE) {
            box(graphics, 42, 40, !toggles[0]);
            box(graphics, 42, 50, !toggles[1]);
            box(graphics, 42, 60, !toggles[2]);
            small(graphics, Component.translatable(toggles[0] ? "gui.thaumcraft.golem.empty_space" : "gui.thaumcraft.golem.block"), 53, 42, 0, TEXT, false);
            small(graphics, Component.translatable(toggles[1] ? "gui.thaumcraft.golem.left_click" : "gui.thaumcraft.golem.right_click"), 53, 42, 1, TEXT, false);
            small(graphics, Component.translatable(toggles[2] ? "gui.thaumcraft.golem.sneaking" : "gui.thaumcraft.golem.not_sneaking"), 53, 42, 2, TEXT, false);
        }
        if (golem.getUpgradeAmount(Golem.UPGRADE_ENTROPY) > 0 && GolemCoreItem.canSort(core)) {
            int shiftX = core == GolemCoreItem.SORTING ? 66 : 180;
            int shiftY = core == GolemCoreItem.SORTING ? 12 : 0;
            box(graphics, shiftX, 24 + shiftY, golem.checkOreDict());
            box(graphics, shiftX, 34 + shiftY, golem.ignoreDamage());
            box(graphics, shiftX, 44 + shiftY, golem.ignoreNBT());
            small(graphics, Component.translatable("gui.thaumcraft.golem.ore_dict"), shiftX + 10, 26 + shiftY, 0, golem.checkOreDict() ? TEXT : TEXT_OFF, false);
            small(graphics, Component.translatable("gui.thaumcraft.golem.ignore_damage"), shiftX + 10, 26 + shiftY, 1, golem.ignoreDamage() ? TEXT : TEXT_OFF, false);
            small(graphics, Component.translatable("gui.thaumcraft.golem.ignore_nbt"), shiftX + 10, 26 + shiftY, 2, golem.ignoreNBT() ? TEXT : TEXT_OFF, false);
        }
        RenderSystem.disableBlend();
    }

    /** Реплика голема мелким шрифтом в левом верхнем углу; подписей «Инвентарь» в оригинале нет. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1f);
        Component blurb = Component.translatable(threat >= 0 ? "golemthreat." + threat + ".text" : "golemblurb." + golem.getCore() + ".text");
        graphics.drawWordWrap(font, blurb, 80, 22, 110, 0xDDDDDD);
        if (menu.maxScroll > 0) {
            graphics.drawString(font, (menu.currentScroll + 1) + "/" + (menu.maxScroll + 1), 323, 140, 0xDDDDDD, false);
        }
        graphics.pose().popPose();
    }

    private boolean press(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        // сервер ответит синхронизацией данных голема; страницу образцов листаем сразу
        menu.clickMenuButton(minecraft.player, button >= GolemMenu.SCROLL_UP ? button : -1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int core = golem.getCore();
        int slots = slots();
        if (hasSamples()) {
            if (golem.getUpgradeAmount(Golem.UPGRADE_ORDER) > 0) {
                for (int cell = 0; cell < Math.min(6, slots); cell++) {
                    if (over(mouseX, mouseY, 96 + cell / 2 * 28, 4 + cell % 2 * 31, 8, 12)) {
                        return press(cell + menu.currentScroll * 6);
                    }
                    if (over(mouseX, mouseY, 112 + cell / 2 * 28, 4 + cell % 2 * 31, 8, 12)) {
                        return press(cell + slots + menu.currentScroll * 6);
                    }
                }
            }
            if (slots > 6) {
                if (over(mouseX, mouseY, 111, 68, 24, 8) && menu.currentScroll > 0) {
                    return press(GolemMenu.SCROLL_UP);
                }
                if (over(mouseX, mouseY, 135, 68, 24, 8) && menu.currentScroll < menu.maxScroll) {
                    return press(GolemMenu.SCROLL_DOWN);
                }
            }
        }
        if (core == GolemCoreItem.GUARD) {
            for (int row = 0; row < 4; row++) {
                if (over(mouseX, mouseY, 104, 5 + 16 * row, 8, 8)) {
                    return press(GolemMenu.TOGGLE + 1 + row);
                }
            }
        }
        if (core == GolemCoreItem.FILL && over(mouseX, mouseY, 62, 54, 8, 8)) {
            return press(GolemMenu.TOGGLE);
        }
        if (core == GolemCoreItem.USE) {
            for (int row = 0; row < 3; row++) {
                if (over(mouseX, mouseY, 42, 40 + row * 10, 8, 8)) {
                    return press(GolemMenu.TOGGLE + row);
                }
            }
        }
        if (golem.getUpgradeAmount(Golem.UPGRADE_ENTROPY) > 0 && GolemCoreItem.canSort(core)) {
            int shiftX = core == GolemCoreItem.SORTING ? 66 : 180;
            int shiftY = core == GolemCoreItem.SORTING ? 12 : 0;
            for (int row = 0; row < 3; row++) {
                if (over(mouseX, mouseY, shiftX, 24 + row * 10 + shiftY, 64, 8)) {
                    return press(GolemMenu.TOGGLE + 5 + row);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
