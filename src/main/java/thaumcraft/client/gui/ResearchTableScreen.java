package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.common.blockentity.ResearchTableBlockEntity;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.ResearchTablePacket;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchNote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Окно стола исследований — перенос GuiResearchTable оригинала. Слева палитра известных аспектов и место,
 * где два аспекта соединяются в составной; справа лист заметки с полем из шестиугольников. Аспект
 * перетаскивается из палитры на клетку; клик по своему аспекту на листе стирает его.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/guiresearchtable2.png");
    private static final ResourceLocation PARCHMENT = Thaumcraft.id("textures/misc/parchment3.png");
    private static final ResourceLocation HEX = Thaumcraft.id("textures/gui/hex1.png");
    private static final ResourceLocation HEX_HIGHLIGHT = Thaumcraft.id("textures/gui/hex2.png");
    private static final ResourceLocation UNKNOWN = Thaumcraft.id("textures/aspects/_unknown.png");
    private static final ResourceLocation ASPECT_BACK = Thaumcraft.id("textures/aspects/_back.png");
    /** Радиус клетки и положение центра поля на листе. */
    private static final int HEX_SIZE = 9;
    private static final int SHEET_X = 169;
    private static final int SHEET_Y = 83;
    /** Палитра аспектов: пять столбцов по пять, прокрутка по столбцу. */
    private static final int PALETTE_X = 10;
    private static final int PALETTE_Y = 40;

    private int page;
    private int lastPage;
    @Nullable
    private Aspect select1;
    @Nullable
    private Aspect select2;
    @Nullable
    private Aspect dragged;
    private long combinePressedUntil;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 255;
        imageHeight = 255;
    }

    private PlayerKnowledge knowledge() {
        return ClientKnowledge.get();
    }

    private ResearchTableBlockEntity table() {
        return menu.getTable();
    }

    @Nullable
    private ResearchNote note() {
        return ResearchNote.read(table().getItems().get(ResearchTableBlockEntity.NOTES_SLOT));
    }

    /** Открытые аспекты в том порядке, в каком они показаны в палитре. */
    private List<Aspect> knownAspects() {
        List<Aspect> out = new ArrayList<>();
        for (Aspect aspect : Aspect.all()) {
            if (knowledge().hasDiscovered(aspect)) {
                out.add(aspect);
            }
        }
        out.sort((a, b) -> a.getTag().compareTo(b.getTag()));
        return out;
    }

    /** «Копирование исследований»: над готовым открытием появляется кнопка снятия копии. */
    private boolean canDuplicate() {
        ResearchNote note = note();
        return note != null && note.complete && knowledge().isResearchComplete("RESEARCHDUPE");
    }

    private void renderCopyCost(GuiGraphics graphics) {
        ResearchNote note = note();
        thaumcraft.common.research.ResearchEntry research = thaumcraft.common.research.ResearchRegistry.get(note.key);
        if (research == null) {
            return;
        }
        AspectList cost = ResearchTableBlockEntity.copyCost(research, note);
        graphics.pose().pushPose();
        graphics.pose().translate(0f, 0f, 400f);
        graphics.drawString(font, Component.translatable("tc.research.copy"), leftPos + 100, topPos + 12, 0xffffff, true);
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos + 100, topPos + 21, 184, 224, 48, 16);
        int x = leftPos + 148;
        for (Aspect aspect : cost.getAspectsSortedByName()) {
            AspectRenderer.draw(graphics, font, aspect, cost.getAmount(aspect), x, topPos + 21, 1f);
            x += AspectRenderer.ICON;
        }
        graphics.pose().popPose();
    }

    private boolean available(Aspect aspect) {
        return knowledge().getPool(aspect) > 0 || table().getBonusAspects().getAmount(aspect) > 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (dragged != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 300f);
            AspectRenderer.draw(graphics, font, dragged, 0, mouseX - 8, mouseY - 8, 1f);
            graphics.pose().popPose();
        } else {
            if (canDuplicate() && inside(mouseX, mouseY, 37, 5, 24, 24)) {
                renderCopyCost(graphics);
            }
            renderAspectTooltip(graphics, mouseX, mouseY);
            renderTooltip(graphics, mouseX, mouseY);
        }
        ItemStack tools = table().getItems().get(ResearchTableBlockEntity.TOOLS_SLOT);
        if (note() != null && !ScribingToolsItem.hasInk(tools)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tile.researchtable.noink.0"),
                    Component.translatable("tile.researchtable.noink.1")), leftPos + 100, topPos + 60);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 255, 167);
        graphics.blit(TEXTURE, leftPos + 40, topPos + 167, 0, 166, 184, 88);
        if (canDuplicate()) {
            graphics.blit(TEXTURE, leftPos + 37, topPos + 5, 232, 200, 24, 24);
        }

        List<Aspect> known = knownAspects();
        lastPage = Math.max(0, (known.size() - 20) / 5);
        page = Mth.clamp(page, 0, lastPage);
        if (page < lastPage) {
            graphics.blit(TEXTURE, leftPos + 51, topPos + 121, 208, 208, 24, 8);
        }
        if (page > 0) {
            graphics.blit(TEXTURE, leftPos + 27, topPos + 121, 184, 208, 24, 8);
        }

        if (select1 != null && !available(select1)) {
            select1 = null;
        }
        if (select2 != null && !available(select2)) {
            select2 = null;
        }
        if (select1 != null && select2 != null) {
            graphics.blit(TEXTURE, leftPos + 35, topPos + 139, 184, 184, 32, 16);
            if (combinePressedUntil > System.currentTimeMillis()) {
                graphics.blit(TEXTURE, leftPos + 35, topPos + 139, 184, 168, 32, 16);
            }
        }

        // палитра
        AspectList bonus = table().getBonusAspects();
        int drawn = 0;
        for (int i = page * 5; i < known.size() && drawn < 25; i++) {
            Aspect aspect = known.get(i);
            int amount = knowledge().getPool(aspect);
            int x = leftPos + PALETTE_X + drawn / 5 * 16;
            int y = topPos + PALETTE_Y + drawn % 5 * 16;
            AspectRenderer.draw(graphics, font, aspect, amount, x, y, available(aspect) ? 1f : 0.33f);
            if (bonus.getAmount(aspect) > 0) {
                // бесплатный аспект от окружения отмечен плюсом
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 20f);
                graphics.pose().scale(0.5f, 0.5f, 1f);
                graphics.drawString(font, "+" + bonus.getAmount(aspect), 0, 0, 0xffff80, true);
                graphics.pose().popPose();
            }
            drawn++;
        }
        if (select1 != null) {
            AspectRenderer.draw(graphics, font, select1, 0, leftPos + 13, topPos + 139, 1f);
        }
        if (select2 != null) {
            AspectRenderer.draw(graphics, font, select2, 0, leftPos + 71, topPos + 139, 1f);
        }

        drawSheet(graphics, mouseX, mouseY);
    }

    private void drawSheet(GuiGraphics graphics, int mouseX, int mouseY) {
        ResearchNote note = note();
        if (note == null) {
            return;
        }
        RenderSystem.enableBlend();
        graphics.blit(PARCHMENT, leftPos + 94, topPos + 8, 0, 0, 150, 150);
        int centerX = leftPos + SHEET_X;
        int centerY = topPos + SHEET_Y;
        ResearchNote.Hex hovered = ResearchNote.Hex.fromPixel(mouseX - centerX, mouseY - centerY, HEX_SIZE);

        // сеть уже найденных связей: линии между соседними родственными аспектами
        Set<ResearchNote.Hex> linked = new HashSet<>();
        for (Map.Entry<ResearchNote.Hex, ResearchNote.Entry> e : note.hexes.entrySet()) {
            if (e.getValue().type() != ResearchNote.FIXED) {
                continue;
            }
            for (ResearchNote.Hex reached : note.connected(e.getKey(), knowledge())) {
                if (!reached.equals(e.getKey())) {
                    linked.add(reached);
                }
            }
        }
        float pulse = 0.6f + Mth.sin(minecraft.player.tickCount / 3f) * 0.3f;
        for (Map.Entry<ResearchNote.Hex, ResearchNote.Entry> e : note.hexes.entrySet()) {
            ResearchNote.Hex hex = e.getKey();
            ResearchNote.Entry entry = e.getValue();
            if (entry.aspect() == null || !knowledge().hasDiscovered(entry.aspect())) {
                continue;
            }
            for (int n = 0; n < 3; n++) {
                ResearchNote.Hex other = hex.neighbour(n);
                ResearchNote.Entry otherEntry = note.hexes.get(other);
                if (otherEntry != null && otherEntry.aspect() != null && knowledge().hasDiscovered(otherEntry.aspect())
                        && ResearchNote.related(entry.aspect(), otherEntry.aspect())
                        && (linked.contains(hex) || entry.type() == ResearchNote.FIXED)
                        && (linked.contains(other) || otherEntry.type() == ResearchNote.FIXED)) {
                    drawLine(graphics, centerX + hex.pixelX(HEX_SIZE), centerY + hex.pixelY(HEX_SIZE),
                            centerX + other.pixelX(HEX_SIZE), centerY + other.pixelY(HEX_SIZE), pulse);
                }
            }
        }

        for (Map.Entry<ResearchNote.Hex, ResearchNote.Entry> e : note.hexes.entrySet()) {
            ResearchNote.Hex hex = e.getKey();
            ResearchNote.Entry entry = e.getValue();
            int x = (int) Math.round(centerX + hex.pixelX(HEX_SIZE)) - 8;
            int y = (int) Math.round(centerY + hex.pixelY(HEX_SIZE)) - 8;
            if (!note.complete && entry.type() != ResearchNote.FIXED) {
                RenderSystem.enableBlend();
                if (hex.equals(hovered)) {
                    graphics.blit(HEX_HIGHLIGHT, x, y, 16, 16, 0f, 0f, 1, 1, 1, 1);
                }
                graphics.setColor(1f, 1f, 1f, 0.25f);
                graphics.blit(HEX, x, y, 16, 16, 0f, 0f, 1, 1, 1, 1);
                graphics.setColor(1f, 1f, 1f, 1f);
            }
            if (entry.aspect() == null) {
                continue;
            }
            if (!knowledge().hasDiscovered(entry.aspect())) {
                // аспект, которого игрок ещё не знает, показан тёмным знаком вопроса
                graphics.setColor(0f, 0f, 0f, 0.5f);
                graphics.blit(UNKNOWN, x, y, 16, 16, 0f, 0f, 1, 1, 1, 1);
                graphics.setColor(1f, 1f, 1f, 1f);
            } else {
                boolean bright = entry.type() == ResearchNote.FIXED || linked.contains(hex);
                AspectRenderer.draw(graphics, font, entry.aspect(), 0, x, y, bright ? 1f : 0.66f);
            }
        }
    }

    private void drawLine(GuiGraphics graphics, double x1, double y1, double x2, double y2, float alpha) {
        Matrix4f matrix = graphics.pose().last().pose();
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float length = Math.max(0.001f, Mth.sqrt(dx * dx + dy * dy));
        float nx = -dy / length * 1.2f;
        float ny = dx / length * 1.2f;
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, (float) x1 - nx, (float) y1 - ny, 0f).color(0f, 0.6f, 0.8f, alpha).endVertex();
        builder.vertex(matrix, (float) x2 - nx, (float) y2 - ny, 0f).color(0f, 0.6f, 0.8f, alpha).endVertex();
        builder.vertex(matrix, (float) x2 + nx, (float) y2 + ny, 0f).color(0f, 0.6f, 0.8f, alpha).endVertex();
        builder.vertex(matrix, (float) x1 + nx, (float) y1 + ny, 0f).color(0f, 0.6f, 0.8f, alpha).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
    }

    @Nullable
    private Aspect paletteAspectAt(double mouseX, double mouseY) {
        double x = mouseX - (leftPos + PALETTE_X);
        double y = mouseY - (topPos + PALETTE_Y);
        if (x < 0 || y < 0 || x >= 80 || y >= 80) {
            return null;
        }
        List<Aspect> known = knownAspects();
        int index = page * 5 + (int) (x / 16) * 5 + (int) (y / 16);
        return index < known.size() ? known.get(index) : null;
    }

    private void renderAspectTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Aspect aspect = paletteAspectAt(mouseX, mouseY);
        if (aspect != null) {
            graphics.renderComponentTooltip(font, List.of(aspect.getName(), aspect.getDescription()), mouseX, mouseY - 8);
            // «Экспертиза»: под курсором видно, из каких двух аспектов составлен наведённый (drawAspectText оригинала)
            if (!aspect.isPrimal() && knowledge().isResearchComplete("RESEARCHER1")) {
                graphics.pose().pushPose();
                graphics.pose().translate(0f, 0f, 500f);
                drawComponent(graphics, aspect.getFirstComponent(), mouseX + 6, mouseY + 6);
                drawComponent(graphics, aspect.getSecondComponent(), mouseX + 24, mouseY + 6);
                graphics.pose().popPose();
            }
            return;
        }
        if (aspect == null && select1 != null && inside(mouseX, mouseY, 13, 139, 16, 16)) {
            aspect = select1;
        }
        if (aspect == null && select2 != null && inside(mouseX, mouseY, 71, 139, 16, 16)) {
            aspect = select2;
        }
        if (aspect == null) {
            ResearchNote note = note();
            if (note != null) {
                ResearchNote.Entry entry = note.hexes.get(ResearchNote.Hex.fromPixel(
                        mouseX - (leftPos + SHEET_X), mouseY - (topPos + SHEET_Y), HEX_SIZE));
                if (entry != null && entry.aspect() != null && knowledge().hasDiscovered(entry.aspect())) {
                    aspect = entry.aspect();
                }
            }
        }
        if (aspect != null) {
            graphics.renderComponentTooltip(font, List.of(aspect.getName(), aspect.getDescription()), mouseX, mouseY - 8);
        }
    }

    private void drawComponent(GuiGraphics graphics, Aspect component, int x, int y) {
        RenderSystem.enableBlend();
        graphics.blit(ASPECT_BACK, x, y, 20, 20, 0f, 0f, 1, 1, 1, 1);
        AspectRenderer.draw(graphics, font, component, 0, x + 2, y + 2, 1f);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        double dx = mouseX - (leftPos + x);
        double dy = mouseY - (topPos + y);
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }

    private void click() {
        minecraft.player.playSound(ModSounds.CAMERA_CLACK.get(), 0.4f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && canDuplicate() && inside(mouseX, mouseY, 37, 5, 24, 24)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchTableMenu.DUPLICATE);
            click();
            return true;
        }
        Aspect aspect = paletteAspectAt(mouseX, mouseY);
        // «Мастерство»: Shift+щелчок собирает аспект из его составляющих, если они есть в запасе или на столе
        if (button == 0 && aspect != null && !aspect.isPrimal() && hasShiftDown()
                && knowledge().isResearchComplete("RESEARCHER2")
                && available(aspect.getFirstComponent()) && available(aspect.getSecondComponent())) {
            ModNetwork.CHANNEL.sendToServer(ResearchTablePacket.combine(aspect.getFirstComponent(), aspect.getSecondComponent()));
            minecraft.player.playSound(ModSounds.COMBINE.get(), 0.3f, 1f);
            click();
            return true;
        }
        if (button == 0 && aspect != null && available(aspect)) {
            dragged = aspect;
            click();
            return true;
        }
        if (select1 != null && select2 != null && inside(mouseX, mouseY, 35, 139, 32, 16)) {
            ModNetwork.CHANNEL.sendToServer(ResearchTablePacket.combine(select1, select2));
            minecraft.player.playSound(ModSounds.COMBINE.get(), 0.3f, 1f);
            combinePressedUntil = System.currentTimeMillis() + 200L;
            click();
            return true;
        }
        if (page > 0 && inside(mouseX, mouseY, 27, 121, 24, 8)) {
            page--;
            click();
            return true;
        }
        if (page < lastPage && inside(mouseX, mouseY, 51, 121, 24, 8)) {
            page++;
            click();
            return true;
        }
        if (select1 != null && inside(mouseX, mouseY, 13, 139, 16, 16)) {
            select1 = null;
            click();
            return true;
        }
        if (select2 != null && inside(mouseX, mouseY, 71, 139, 16, 16)) {
            select2 = null;
            click();
            return true;
        }
        ResearchNote note = note();
        if (note != null && !note.complete) {
            ResearchNote.Hex hex = ResearchNote.Hex.fromPixel(mouseX - (leftPos + SHEET_X), mouseY - (topPos + SHEET_Y), HEX_SIZE);
            ResearchNote.Entry entry = note.hexes.get(hex);
            if (entry != null && entry.type() == ResearchNote.PLACED) {
                ModNetwork.CHANNEL.sendToServer(ResearchTablePacket.erase(hex.q(), hex.r()));
                minecraft.player.playSound(ModSounds.ERASE.get(), 0.2f, 1f + minecraft.player.getRandom().nextFloat() * 0.1f);
                click();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragged != null) {
            Aspect aspect = dragged;
            dragged = null;
            ResearchNote note = note();
            if (note != null && !note.complete) {
                ResearchNote.Hex hex = ResearchNote.Hex.fromPixel(mouseX - (leftPos + SHEET_X), mouseY - (topPos + SHEET_Y), HEX_SIZE);
                ResearchNote.Entry entry = note.hexes.get(hex);
                if (entry != null && entry.type() == ResearchNote.EMPTY) {
                    ModNetwork.CHANNEL.sendToServer(ResearchTablePacket.place(hex.q(), hex.r(), aspect));
                    minecraft.player.playSound(ModSounds.WRITE.get(), 0.2f, 1f);
                    click();
                    return true;
                }
            }
            // отпустили над одним из двух мест соединения или просто щёлкнули по аспекту в палитре
            if (inside(mouseX, mouseY, 4, 130, 32, 32)) {
                select1 = aspect;
            } else if (inside(mouseX, mouseY, 63, 130, 32, 32)) {
                select2 = aspect;
            } else if (paletteAspectAt(mouseX, mouseY) == aspect) {
                if (select1 == null) {
                    select1 = aspect;
                } else if (select2 == null) {
                    select2 = aspect;
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
