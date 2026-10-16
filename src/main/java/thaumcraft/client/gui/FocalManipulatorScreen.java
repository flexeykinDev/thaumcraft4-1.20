package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.blocks.FocalManipulatorBlock;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.registry.ModSounds;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Окно фокального манипулятора — перенос GuiFocalManipulator оригинала. Сверху уже поставленные улучшения,
 * снизу — доступные на очередном ранге; выбранное показывает цену в вис и уровнях опыта, полоска запускает работу
 * и потом показывает, сколько вис каждого аспекта ещё осталось набрать из сети.
 */
public class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_wandtable.png");
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.#");

    private static final class Sparkle {
        float x;
        float y;
        final float mx;
        final float my;
        final float r;
        final float g;
        final float b;
        long nextFrame = System.currentTimeMillis() + 50L;
        int frame;

        Sparkle(float x, float y, float mx, float my, float r, float g, float b) {
            this.x = x;
            this.y = y;
            this.mx = mx;
            this.my = my;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private final FocalManipulatorBlock.Manipulator table;
    private final List<FocusUpgrade> possible = new ArrayList<>();
    private final List<FocusUpgrade> applied = new ArrayList<>();
    private final List<Sparkle> sparkles = new ArrayList<>();
    private AspectList cost = new AspectList();
    private ItemStack lastFocus = ItemStack.EMPTY;
    private int selected = -1;
    private int rank;
    private long nextSparkle;

    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.table = menu.getTable();
        imageWidth = 192;
        imageHeight = 233;
    }

    /** Перечитывает фокус: какие улучшения стоят, какой ранг следующий и что на нём предлагается. */
    private void gatherInfo() {
        possible.clear();
        applied.clear();
        cost = new AspectList();
        rank = 0;
        selected = -1;
        ItemStack focus = table.getItem(0);
        lastFocus = focus.copy();
        if (!(focus.getItem() instanceof FocusItem item)) {
            return;
        }
        for (short id : FocusItem.getAppliedUpgrades(focus)) {
            FocusUpgrade type = FocusUpgrade.byId(id);
            if (type != null) {
                applied.add(type);
            }
        }
        rank = FocalManipulatorBlock.Manipulator.nextRank(focus);
        if (rank > 0 && minecraft != null && minecraft.player != null) {
            for (FocusUpgrade type : item.getPossibleUpgradesByRank(focus, rank)) {
                if (item.canApplyUpgrade(focus, minecraft.player, type, rank)) {
                    possible.add(type);
                }
            }
        }
        if (table.getSize() > 0) {
            selected = table.getUpgrade();
        }
    }

    private void select(int id) {
        selected = id;
        FocusUpgrade type = FocusUpgrade.byId(id);
        cost = type == null ? new AspectList() : type.getVisCost(rank);
    }

    private boolean canStart() {
        return minecraft != null && minecraft.player != null && table.getSize() == 0 && selected >= 0 && rank > 0
                && (rank * FocalManipulatorBlock.Manipulator.XP_MULT <= minecraft.player.experienceLevel || minecraft.player.getAbilities().instabuild);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (rank > 0) {
            for (int a = 0; a < possible.size(); a++) {
                if (inside(mouseX, mouseY, leftPos + 48 + a * 16, topPos + 104, 16, 16)) {
                    upgradeTooltip(graphics, possible.get(a));
                }
            }
        }
        if (selected >= 0) {
            if (inside(mouseX, mouseY, leftPos + 48, topPos + 48, 36, 36)) {
                graphics.renderTooltip(font, Component.translatable("wandtable.text1"), mouseX, mouseY);
            }
            if (inside(mouseX, mouseY, leftPos + 108, topPos + 58, 36, 16)) {
                graphics.renderTooltip(font, Component.translatable("wandtable.text2"), mouseX, mouseY);
            }
            if (canStart() && inside(mouseX, mouseY, leftPos + 48, topPos + 88, 96, 8)) {
                graphics.renderTooltip(font, Component.translatable("wandtable.text3"), mouseX, mouseY);
            }
        }
        for (int a = 0; a < applied.size(); a++) {
            if (inside(mouseX, mouseY, leftPos + 56 + a * 16, topPos + 32, 16, 16)) {
                upgradeTooltip(graphics, applied.get(a));
            }
        }
    }

    /** Описание улучшения: всегда справа от окна, с переносом строк по оставшейся ширине экрана. */
    private void upgradeTooltip(GuiGraphics graphics, FocusUpgrade type) {
        int x = leftPos + imageWidth - 36;
        int room = Math.max(60, width - (leftPos + imageWidth - 16) - 12);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(type.getName().copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE).getVisualOrderText());
        lines.addAll(font.split(type.getText(), room));
        graphics.renderTooltip(font, lines, x, topPos + 24);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        long time = System.currentTimeMillis();
        int k = leftPos;
        int l = topPos;
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, k, l, 0, 0, imageWidth, imageHeight);
        if (!ItemStack.matches(lastFocus, table.getItem(0))) {
            gatherInfo();
        }
        if (table.getSize() > 0) {
            selected = table.getUpgrade();
        }
        if (rank > 0) {
            for (int a = 0; a < possible.size(); a++) {
                if (selected == possible.get(a).id) {
                    graphics.blit(TEXTURE, k + 48 + a * 16, l + 104, 200, 0, 16, 16);
                }
            }
        }
        if (rank > 0 && selected >= 0 && !table.getItem(0).isEmpty() && minecraft != null && minecraft.player != null) {
            int xp = rank * FocalManipulatorBlock.Manipulator.XP_MULT;
            if (canStart()) {
                graphics.blit(TEXTURE, k + 48, l + 88, 8, 240, 96, 8);
            }
            graphics.blit(TEXTURE, k + 108, l + 59, 200, 16, 16, 16);
            AspectList remaining = table.getRemaining();
            if (table.getSize() > 0) {
                // полоска: цветные отрезки по числу вис, которые ещё предстоит набрать
                int start = 0;
                for (Aspect aspect : remaining.getAspectsSortedByAmount()) {
                    if (remaining.getAmount(aspect) == 0) {
                        continue;
                    }
                    int size = (int) ((float) remaining.getAmount(aspect) / table.getSize() * 96f);
                    int c = aspect.getColor();
                    float r = (c >> 16 & 255) / 255f;
                    float g = (c >> 8 & 255) / 255f;
                    float b = (c & 255) / 255f;
                    graphics.setColor(r, g, b, 0.9f);
                    graphics.blit(TEXTURE, k + 48 + start, l + 88, 112 + start, 240, size, 8);
                    graphics.setColor(1f, 1f, 1f, 1f);
                    start += size;
                    if (minecraft.player.getRandom().nextInt(66) == 0) {
                        float x = 48 + start;
                        float y = 92f;
                        sparkles.add(new Sparkle(x, y, (46 + rank * 16 - x) / 9f, (38f - y) / 9f, r, g, b));
                    }
                }
            }
            boolean enough = xp <= minecraft.player.experienceLevel || minecraft.player.getAbilities().instabuild;
            graphics.drawString(font, String.valueOf(xp), k + 125, l + 64, enough ? 0x99ff8d : 0xf67278, true);
            AspectList shown = table.getSize() > 0 ? remaining : cost;
            int q = 0;
            graphics.pose().pushPose();
            graphics.pose().translate(k + 49, l + 68 - shown.size() * 2.5f, 0f);
            graphics.pose().scale(0.5f, 0.5f, 0.5f);
            for (Aspect aspect : shown.getAspectsSortedByAmount()) {
                graphics.drawString(font, aspect.getName(), 0, q * 10, aspect.getColor(), true);
                graphics.drawString(font, FORMAT.format(shown.getAmount(aspect) / 100f), 48, q * 10, aspect.getColor(), true);
                q++;
            }
            graphics.pose().popPose();
        }
        if (rank > 0 && minecraft != null && minecraft.player != null) {
            if (nextSparkle < time) {
                var random = minecraft.player.getRandom();
                nextSparkle = time + (table.getSize() > 0 ? 10 : 500) + random.nextInt(200);
                sparkles.add(new Sparkle(42 + rank * 16 + random.nextInt(12), 34 + random.nextInt(12), 0f, 0f,
                        0.5f + random.nextFloat() * 0.4f, 1f - random.nextFloat() * 0.4f, 1f - random.nextFloat() * 0.4f));
            }
            for (int a = 0; a < possible.size(); a++) {
                graphics.blit(possible.get(a).getIcon(), k + 48 + a * 16, l + 104, 0, 0f, 0f, 16, 16, 16, 16);
            }
        }
        for (int a = 0; a < applied.size(); a++) {
            graphics.blit(applied.get(a).getIcon(), k + 56 + a * 16, l + 32, 0, 0f, 0f, 16, 16, 16, 16);
        }
        RenderSystem.disableBlend();
    }

    /** Подписей в окне нет; поверх всего летают искры. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        long time = System.currentTimeMillis();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        Iterator<Sparkle> iterator = sparkles.iterator();
        while (iterator.hasNext()) {
            Sparkle s = iterator.next();
            graphics.setColor(s.r, s.g, s.b, 0.9f);
            graphics.pose().pushPose();
            graphics.pose().translate(s.x - 4f, s.y - 4f, 200f);
            // кадры искры: восьмая строка листа частиц, 16 пикселей на кадр при листе 256x256
            graphics.blit(PARTICLES, 0, 0, 8, 8, s.frame * 16f, 112f, 16, 16, 256, 256);
            graphics.pose().popPose();
            if (s.nextFrame < time) {
                s.frame++;
                s.nextFrame = time + 50L;
                s.x += s.mx;
                s.y += s.my;
            }
            if (s.frame >= 9) {
                iterator.remove();
            }
        }
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CAMERA_CLACK.get(), 1f, 0.4f));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (canStart() && inside(mouseX, mouseY, leftPos + 48, topPos + 88, 96, 8) && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
            click();
            return true;
        }
        if (table.getSize() == 0) {
            for (int a = 0; a < possible.size(); a++) {
                if (inside(mouseX, mouseY, leftPos + 48 + a * 16, topPos + 104, 16, 16)) {
                    if (selected == possible.get(a).id) {
                        selected = -1;
                        cost = new AspectList();
                    } else {
                        select(possible.get(a).id);
                    }
                    click();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
