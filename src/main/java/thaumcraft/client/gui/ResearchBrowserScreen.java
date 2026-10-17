package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.PurchaseResearchPacket;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.research.ResearchRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Карта исследований таумономикона — перенос GuiResearchBrowser оригинала: прокручиваемое поле со значками
 * исследований и связями между ними, слева вкладки категорий.
 */
public class ResearchBrowserScreen extends Screen {
    private static final ResourceLocation FRAME = Thaumcraft.id("textures/gui/gui_research.png");
    private static final ResourceLocation UNKNOWN = Thaumcraft.id("textures/aspects/_unknown.png");
    private static final ResourceLocation GALACTIC = thaumcraft.common.Res.of("minecraft", "alt");
    private static final int PANE_WIDTH = 256;
    private static final int PANE_HEIGHT = 230;
    private static final int MAP_WIDTH = 224;
    private static final int MAP_HEIGHT = 196;

    /** Категория и положение карты запоминаются между открытиями книги. */
    private static String selectedCategory;

    /** Открыть книгу на этой вкладке (самосъёмка и аддоны). */
    public static void selectCategory(String category) {
        selectedCategory = category;
    }
    private static double savedX = -100;
    private static double savedY = -86;

    private double mapX;
    private double mapY;
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private List<ResearchEntry> research = List.of();
    @Nullable
    private ResearchEntry highlight;
    private long popupUntil;
    private Component popupText = Component.empty();

    public ResearchBrowserScreen() {
        super(Component.translatable("item.thaumcraft.thaumonomicon"));
        mapX = savedX;
        mapY = savedY;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ResearchBrowserScreen());
    }

    /** Есть ли у игрока бумага и письменные принадлежности с чернилами, чтобы записать заметку. */
    private boolean hasScribeStuff() {
        return ResearchHelper.findScribingTools(minecraft.player) >= 0 && ResearchHelper.findPaper(minecraft.player) >= 0;
    }

    private PlayerKnowledge knowledge() {
        return ClientKnowledge.get();
    }

    @Override
    protected void init() {
        updateResearch();
    }

    private void updateResearch() {
        if (selectedCategory == null || ResearchRegistry.inCategory(selectedCategory).isEmpty()) {
            selectedCategory = ResearchRegistry.categories().stream().findFirst().map(ResearchRegistry.Category::key).orElse("");
        }
        research = ResearchRegistry.inCategory(selectedCategory);
        int minColumn = 0;
        int maxColumn = 0;
        int minRow = 0;
        int maxRow = 0;
        for (ResearchEntry entry : research) {
            if (!entry.virtual) {
                minColumn = Math.min(minColumn, entry.column);
                maxColumn = Math.max(maxColumn, entry.column);
                minRow = Math.min(minRow, entry.row);
                maxRow = Math.max(maxRow, entry.row);
            }
        }
        // границы прокрутки — формулы оригинала
        minX = minColumn * 24 - 85;
        minY = minRow * 24 - 112;
        maxX = maxColumn * 24 - 112;
        maxY = maxRow * 24 - 61;
        clampMap();
    }

    private void clampMap() {
        mapX = Mth.clamp(mapX, minX, Math.max(minX, maxX - 1));
        mapY = Mth.clamp(mapY, minY, Math.max(minY, maxY - 1));
    }

    @Override
    public void onClose() {
        savedX = mapX;
        savedY = mapY;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isComplete(ResearchEntry entry) {
        return knowledge().isResearchComplete(entry.key);
    }

    /** Показывается ли незавершённое исследование на карте. */
    private boolean isVisible(ResearchEntry entry) {
        if (knowledge().isResearchComplete("@" + entry.key)) {
            return true;
        }
        return !entry.lost && !entry.hidden && (!entry.concealed || ResearchHelper.canUnlock(knowledge(), entry));
    }

    private List<ResearchRegistry.Category> visibleCategories() {
        List<ResearchRegistry.Category> out = new ArrayList<>();
        for (ResearchRegistry.Category category : ResearchRegistry.categories()) {
            // запретные знания появляются только после первого шага в них
            if (!category.key().equals("ELDRITCH") || knowledge().isResearchComplete("ELDRITCHMINOR")) {
                out.add(category);
            }
        }
        return out;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - PANE_WIDTH) / 2;
        int top = (height - PANE_HEIGHT) / 2;
        int mapLeft = left + 16;
        int mapTop = top + 17;
        int scrollX = Mth.floor(mapX);
        int scrollY = Mth.floor(mapY);
        float ticks = minecraft.player.tickCount + partialTick;
        float pulse = (float) Math.sin(System.currentTimeMillis() % 600L / 600.0 * Math.PI * 2.0);

        graphics.enableScissor(mapLeft, mapTop, mapLeft + MAP_WIDTH, mapTop + MAP_HEIGHT);
        drawBackground(graphics, mapLeft, mapTop, scrollX, scrollY);
        drawConnections(graphics, mapLeft, mapTop, scrollX, scrollY, ticks);

        highlight = null;
        for (ResearchEntry entry : research) {
            int x = entry.column * 24 - scrollX;
            int y = entry.row * 24 - scrollY;
            if (entry.virtual || x < -24 || y < -24 || x > MAP_WIDTH || y > MAP_HEIGHT) {
                continue;
            }
            boolean complete = isComplete(entry);
            if (!complete && !isVisible(entry)) {
                continue;
            }
            boolean unlockable = ResearchHelper.canUnlock(knowledge(), entry);
            float brightness = complete ? 1f : unlockable ? pulse * 0.25f + 0.75f : 0.3f;
            int px = mapLeft + x;
            int py = mapTop + y;

            RenderSystem.enableBlend();
            graphics.setColor(brightness, brightness, brightness, 1f);
            int frameU = entry.round ? 54 : entry.hidden ? (entry.secondary ? 230 : 86) : entry.secondary ? 110 : 0;
            graphics.blit(FRAME, px - 2, py - 2, frameU, 230, 26, 26);
            if (entry.special) {
                graphics.blit(FRAME, px - 2, py - 2, 26, 230, 26, 26);
            }
            graphics.setColor(1f, 1f, 1f, 1f);

            // недоступное исследование видно тёмным силуэтом самого значка, как в оригинале (цвет 0.1), без подложки
            if (!complete && !unlockable) {
                graphics.setColor(0.1f, 0.1f, 0.1f, 1f);
            }
            drawIcon(graphics, entry, px + 3, py + 3);
            graphics.setColor(1f, 1f, 1f, 1f);
            if (mouseX >= mapLeft && mouseY >= mapTop && mouseX < mapLeft + MAP_WIDTH && mouseY < mapTop + MAP_HEIGHT
                    && mouseX >= px && mouseX <= px + 22 && mouseY >= py && mouseY <= py + 22) {
                highlight = entry;
            }
        }
        graphics.disableScissor();

        drawCategoryTabs(graphics, left, top);
        graphics.pose().pushPose();
        graphics.pose().translate(0f, 0f, 250f);
        RenderSystem.enableBlend();
        graphics.blit(FRAME, left, top, 0, 0, PANE_WIDTH, PANE_HEIGHT);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(0f, 0f, 400f);
        if (highlight != null) {
            drawResearchTooltip(graphics, highlight, mouseX + 6, mouseY - 4, pulse);
        }
        drawCategoryTooltip(graphics, left, top, mouseX, mouseY);
        if (popupUntil > System.currentTimeMillis()) {
            int centerX = left + 128;
            int centerY = top + 128;
            int half = font.wordWrapHeight(popupText, 150) / 2;
            graphics.fill(centerX - 78, centerY - half - 3, centerX + 78, centerY + half + 3, 0xc0000000);
            graphics.drawWordWrap(font, popupText, centerX - 75, centerY - half, 150, 0x9090ff);
        }
        graphics.pose().popPose();
    }

    /** Фон категории нарисован вдвое крупнее и прокручивается медленнее карты. */
    private void drawBackground(GuiGraphics graphics, int mapLeft, int mapTop, int scrollX, int scrollY) {
        ResearchRegistry.Category category = currentCategory();
        if (category == null) {
            return;
        }
        int vx = (int) ((float) (scrollX - minX) / Math.max(1, Math.abs(minX - maxX)) * 288f);
        int vy = (int) ((float) (scrollY - minY) / Math.max(1, Math.abs(minY - maxY)) * 316f);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(2f, 2f, 1f);
        graphics.blit(category.background(), mapLeft / 2, mapTop / 2, vx / 2f, vy / 2f, 112, 98, 256, 256);
        pose.popPose();
    }

    @Nullable
    private ResearchRegistry.Category currentCategory() {
        for (ResearchRegistry.Category category : ResearchRegistry.categories()) {
            if (category.key().equals(selectedCategory)) {
                return category;
            }
        }
        return null;
    }

    private void drawConnections(GuiGraphics graphics, int mapLeft, int mapTop, int scrollX, int scrollY, float ticks) {
        for (ResearchEntry entry : research) {
            if (entry.virtual) {
                continue;
            }
            int x1 = entry.column * 24 - scrollX + 11 + mapLeft;
            int y1 = entry.row * 24 - scrollY + 11 + mapTop;
            boolean complete = isComplete(entry);
            boolean visible = complete || isVisible(entry);
            for (String parentKey : entry.parents) {
                ResearchEntry parent = ResearchRegistry.get(parentKey);
                if (parent == null || parent.virtual || !parent.category.equals(selectedCategory)) {
                    continue;
                }
                drawLink(graphics, x1, y1, parent, mapLeft, mapTop, scrollX, scrollY, complete, visible, 0.1f, ticks);
            }
            for (String siblingKey : entry.siblings) {
                ResearchEntry sibling = ResearchRegistry.get(siblingKey);
                if (sibling == null || sibling.virtual || !sibling.category.equals(selectedCategory)
                        || sibling.parents.contains(entry.key)) {
                    continue;
                }
                drawLink(graphics, x1, y1, sibling, mapLeft, mapTop, scrollX, scrollY, complete, visible, 0.2f, ticks);
            }
        }
    }

    /** Связь тёмная, если исследование завершено; зелёная, если завершён сосед; синяя, если оба ещё впереди. */
    private void drawLink(GuiGraphics graphics, int x1, int y1, ResearchEntry other, int mapLeft, int mapTop,
                          int scrollX, int scrollY, boolean complete, boolean visible, float doneBlue, float ticks) {
        int x2 = other.column * 24 - scrollX + 11 + mapLeft;
        int y2 = other.row * 24 - scrollY + 11 + mapTop;
        if (complete) {
            drawLine(graphics, x1, y1, x2, y2, 0.1f, 0.1f, doneBlue, ticks, false);
        } else if (visible) {
            if (isComplete(other)) {
                drawLine(graphics, x1, y1, x2, y2, 0f, 1f, 0f, ticks, true);
            } else if (isVisible(other)) {
                drawLine(graphics, x1, y1, x2, y2, 0f, 0f, 1f, ticks, true);
            }
        }
    }

    /** Линия из коротких отрезков; у незавершённых связей она колышется, затухая к концу. */
    private void drawLine(GuiGraphics graphics, int x, int y, int x2, int y2, float r, float g, float b, float ticks,
                          boolean wiggle) {
        double dx = x - x2;
        double dy = y - y2;
        float distance = Mth.sqrt((float) (dx * dx + dy * dy));
        int steps = Math.max(1, (int) (distance / 2f));
        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float previousX = 0f;
        float previousY = 0f;
        for (int i = 0; i <= steps; i++) {
            float phase = (float) i / steps;
            float offsetX = 0f;
            float offsetY = 0f;
            if (wiggle) {
                offsetX = Mth.sin((ticks + i) / 7f) * 5f * (1f - phase);
                offsetY = Mth.sin((ticks + i) / 5f) * 5f * (1f - phase);
            }
            float pointX = (float) (x - dx * phase) + offsetX;
            float pointY = (float) (y - dy * phase) + offsetY;
            if (i > 0) {
                // отрезок толщиной полтора пикселя
                float normalX = -(pointY - previousY);
                float normalY = pointX - previousX;
                float length = Math.max(0.001f, Mth.sqrt(normalX * normalX + normalY * normalY));
                normalX = normalX / length * 0.75f;
                normalY = normalY / length * 0.75f;
                builder.vertex(matrix, previousX - normalX, previousY - normalY, 0f).color(r, g, b, 0.6f).endVertex();
                builder.vertex(matrix, pointX - normalX, pointY - normalY, 0f).color(r, g, b, 0.6f).endVertex();
                builder.vertex(matrix, pointX + normalX, pointY + normalY, 0f).color(r, g, b, 0.6f).endVertex();
                builder.vertex(matrix, previousX + normalX, previousY + normalY, 0f).color(r, g, b, 0.6f).endVertex();
            }
            previousX = pointX;
            previousY = pointY;
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
    }

    private void drawIcon(GuiGraphics graphics, ResearchEntry entry, int x, int y) {
        if (entry.iconItem != null) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.iconItem);
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                if (entry.iconNbt != null) {
                    try {
                        stack.setTag(net.minecraft.nbt.TagParser.parseTag(entry.iconNbt));
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {
                        // значок без меток всё равно лучше знака вопроса
                    }
                }
                graphics.renderItem(stack, x, y);
                return;
            }
        }
        RenderSystem.enableBlend();
        // у исследований, чей предмет ещё не перенесён, вместо значка знак вопроса
        graphics.blit(entry.iconTexture != null ? entry.iconTexture : UNKNOWN, x, y, 16, 16, 0f, 0f, 1, 1, 1, 1);
    }

    private void drawCategoryTabs(GuiGraphics graphics, int left, int top) {
        int index = 0;
        for (ResearchRegistry.Category category : visibleCategories()) {
            boolean selected = category.key().equals(selectedCategory);
            int y = top + index * 24;
            RenderSystem.enableBlend();
            graphics.blit(FRAME, left - 24, y, selected ? 152 : 176, 232, 24, 24);
            graphics.blit(category.icon(), left - 19 + (selected ? 0 : 8), y + 4, 16, 16, 0f, 0f, 1, 1, 1, 1);
            if (!selected) {
                graphics.blit(FRAME, left - 24, y, 200, 232, 24, 24);
            }
            index++;
        }
    }

    private void drawCategoryTooltip(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int index = 0;
        for (ResearchRegistry.Category category : visibleCategories()) {
            int x = mouseX - (left - 24);
            int y = mouseY - (top + index * 24);
            if (x >= 0 && x < 24 && y >= 0 && y < 24) {
                graphics.drawString(font, category.getName(), mouseX, mouseY - 8, 0xffffff, true);
            }
            index++;
        }
    }

    private static final String CYRILLIC = "абвгґдеєёжзиіїйклмнопрстуфхцчшщъыьэюя";
    private static final String[] LATIN = {"a", "b", "v", "g", "g", "d", "e", "ye", "yo", "zh", "z", "i", "i", "yi", "y", "k", "l", "m",
            "n", "o", "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya"};

    /**
     * Рунный шрифт игры (minecraft:alt) знает только латиницу, и кириллица в нём превращалась в квадраты.
     * Название переводится в латиницу по звучанию, остальные письменности — в буквы по коду символа.
     */
    public static String runes(String text) {
        StringBuilder out = new StringBuilder();
        for (char ch : text.toCharArray()) {
            char lower = Character.toLowerCase(ch);
            int index = CYRILLIC.indexOf(lower);
            if (index >= 0) {
                String latin = LATIN[index];
                out.append(ch != lower && !latin.isEmpty() ? Character.toUpperCase(latin.charAt(0)) + latin.substring(1) : latin);
            } else if (ch < 128 || !Character.isLetter(ch)) {
                out.append(ch);
            } else {
                out.append((char) ('a' + ch % 26));
            }
        }
        return out.toString();
    }

    private void drawResearchTooltip(GuiGraphics graphics, ResearchEntry entry, int x, int y, float pulse) {
        boolean complete = isComplete(entry);
        boolean unlockable = ResearchHelper.canUnlock(knowledge(), entry);
        // название недоступного исследования написано рунами
        Component name = !complete && !unlockable
                ? Component.literal(runes(entry.getName().getString())).withStyle(Style.EMPTY.withFont(GALACTIC)) : entry.getName();
        int nameColor = unlockable ? (entry.special ? 0xffff80 : 0xffffff) : (entry.special ? 0x808000 : 0x808040);
        PoseStack pose = graphics.pose();

        if (!unlockable) {
            Component missing = Component.translatable("tc.researchmissing");
            int boxWidth = (int) Math.max(font.width(name), font.width(missing) / 1.5f);
            graphics.fill(x - 3, y - 3, x + boxWidth + 3, y + 20, 0xc0000000);
            pose.pushPose();
            pose.translate(x, y + 12, 0f);
            pose.scale(0.5f, 0.5f, 1f);
            graphics.drawString(font, missing, 0, 0, 0x705050, false);
            pose.popPose();
            graphics.drawString(font, name, x, y, nameColor, true);
            return;
        }

        boolean purchasable = !complete && entry.isPurchasable();
        boolean primary = !complete && !purchasable;
        Component hint = null;
        int hintColor = 0;
        boolean affordable = ResearchHelper.canAfford(knowledge(), entry);
        if (purchasable) {
            hint = Component.translatable(affordable ? "tc.research.purchase" : "tc.research.short");
            hintColor = affordable ? 0x87ceeb : 0xdc143c;
        } else if (primary) {
            if (ResearchHelper.findNote(minecraft.player, entry.key) >= 0) {
                hint = Component.translatable("tc.research.hasnote");
                hintColor = 0xffa500;
            } else if (hasScribeStuff()) {
                hint = Component.translatable("tc.research.getprim");
                hintColor = 0x87ceeb;
            } else {
                hint = Component.translatable("tc.research.shortprim");
                hintColor = 0xdc143c;
            }
        }

        int boxWidth = (int) Math.max(font.width(name), font.width(entry.getText()) / 1.9f);
        if (hint != null) {
            boxWidth = (int) Math.max(boxWidth, font.width(hint) / 1.9f);
        }
        if (purchasable) {
            boxWidth = Math.max(boxWidth, entry.aspects.size() * 16);
        }
        int extra = (primary ? 9 : 0) + (purchasable ? 29 : 0);
        graphics.fill(x - 3, y - 3, x + boxWidth + 3, y + 20 + extra, 0xc0000000);

        pose.pushPose();
        pose.translate(x, y + 13, 0f);
        pose.scale(0.5f, 0.5f, 1f);
        graphics.drawString(font, entry.getText(), 0, 0, 0x9090ff, true);
        pose.popPose();

        if (purchasable) {
            int index = 0;
            for (Aspect aspect : entry.aspects.getAspectsSortedByAmount()) {
                if (knowledge().hasDiscovered(aspect)) {
                    float alpha = knowledge().getPool(aspect) < entry.aspects.getAmount(aspect) ? pulse * 0.25f + 0.75f : 1f;
                    AspectRenderer.draw(graphics, font, aspect, entry.aspects.getAmount(aspect), x + index * 16, y + 22, alpha);
                } else {
                    graphics.setColor(0.5f, 0.5f, 0.5f, 0.5f);
                    graphics.blit(UNKNOWN, x + index * 16, y + 22, 16, 16, 0f, 0f, 1, 1, 1, 1);
                    graphics.setColor(1f, 1f, 1f, 1f);
                }
                index++;
            }
        }
        if (hint != null) {
            pose.pushPose();
            pose.translate(x, y + (purchasable ? 41 : 22), 0f);
            pose.scale(0.5f, 0.5f, 1f);
            graphics.drawString(font, hint, 0, 0, hintColor, true);
            pose.popPose();
        }
        graphics.drawString(font, name, x, y, nameColor, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        popupUntil = 0;
        if (highlight != null) {
            if (isComplete(highlight)) {
                savedX = mapX;
                savedY = mapY;
                minecraft.setScreen(new ResearchPageScreen(highlight));
            } else if (ResearchHelper.canUnlock(knowledge(), highlight)) {
                if (highlight.isPurchasable()) {
                    if (ResearchHelper.canAfford(knowledge(), highlight)) {
                        ModNetwork.CHANNEL.sendToServer(new PurchaseResearchPacket(highlight.key));
                    }
                } else if (hasScribeStuff() && ResearchHelper.findNote(minecraft.player, highlight.key) < 0) {
                    ModNetwork.CHANNEL.sendToServer(new PurchaseResearchPacket(highlight.key));
                    popupText = Component.translatable("tc.research.popup", highlight.getName()).withStyle(ChatFormatting.WHITE);
                    popupUntil = System.currentTimeMillis() + 3000L;
                }
            }
            return true;
        }
        int left = (width - PANE_WIDTH) / 2;
        int top = (height - PANE_HEIGHT) / 2;
        int index = 0;
        for (ResearchRegistry.Category category : visibleCategories()) {
            double x = mouseX - (left - 24);
            double y = mouseY - (top + index * 24);
            if (x >= 0 && x < 24 && y >= 0 && y < 24) {
                selectedCategory = category.key();
                updateResearch();
                minecraft.player.playSound(ModSounds.CAMERA_CLACK.get(), 0.4f, 1f);
                return true;
            }
            index++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int mapLeft = (width - PANE_WIDTH) / 2 + 16;
        int mapTop = (height - PANE_HEIGHT) / 2 + 17;
        if (button == 0 && mouseX >= mapLeft && mouseX < mapLeft + MAP_WIDTH && mouseY >= mapTop && mouseY < mapTop + MAP_HEIGHT) {
            mapX -= dragX;
            mapY -= dragY;
            clampMap();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
