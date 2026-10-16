package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.crafting.ArcaneWrappedRecipe;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.ResearchEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Страницы исследования — перенос GuiResearchRecipe оригинала: разворот книги, две страницы за раз.
 * Страницы бывают текстовые и с рецептами; рецепты, которых в порте ещё нет, показаны заглушкой.
 */
public class ResearchPageScreen extends Screen {
    private static final ResourceLocation BOOK = Thaumcraft.id("textures/gui/gui_researchbook.png");
    private static final ResourceLocation OVERLAY = Thaumcraft.id("textures/gui/gui_researchbook_overlay.png");
    private static final int PANE_WIDTH = 256;
    private static final int PANE_HEIGHT = 181;
    /** На столько вторая страница разворота правее первой. */
    private static final int PAGE_SHIFT = 152;
    private static final int TEXT_WIDTH = 139;

    /** Высота области текста на странице; на первой странице часть занята заголовком исследования. */
    private static final int PAGE_HEIGHT = 186;
    private static final int TITLE_HEIGHT = 25;

    /** Страница разворота: либо свёрстанный текст, либо рецепт. */
    private record DisplayPage(@Nullable List<BookLayout.Element> elements, @Nullable ResearchEntry.Page recipe) {
    }

    private final ResearchEntry research;
    private final List<ResearchEntry.Page> sourcePages = new ArrayList<>();
    private final List<DisplayPage> pages = new ArrayList<>();
    private int page;
    @Nullable
    private ItemStack tooltipStack;

    public ResearchPageScreen(ResearchEntry research) {
        super(research.getName());
        this.research = research;
        for (ResearchEntry.Page candidate : research.pages) {
            // скрытые страницы открываются после другого исследования
            if (!candidate.type().equals("text_concealed") || candidate.research() == null
                    || ClientKnowledge.get().isResearchComplete(candidate.research())) {
                sourcePages.add(candidate);
            }
        }
    }

    /** Вёрстка зависит от шрифта и языка, поэтому делается при открытии экрана. */
    @Override
    protected void init() {
        pages.clear();
        // Идущие подряд текстовые страницы оригинала сливаются в один поток: иначе короткий «хвост»
        // занимал бы целую страницу, а следующий текст начинался бы с нового листа.
        StringBuilder text = new StringBuilder();
        for (ResearchEntry.Page source : sourcePages) {
            if (source.isText()) {
                if (text.length() > 0) {
                    text.append("<BR>");
                }
                // без String.format: в текстах встречается голый «%» («5% знижки»), и I18n.get выдавал «Format error»
                text.append(source.text() == null ? "" : net.minecraft.locale.Language.getInstance().getOrDefault(source.text()));
            } else {
                flushText(text);
                pages.add(new DisplayPage(null, source));
            }
        }
        flushText(text);
        page = Math.min(page, Math.max(0, (pages.size() - 1) / 2 * 2));
    }

    private void flushText(StringBuilder text) {
        if (text.length() == 0) {
            return;
        }
        int firstHeight = pages.isEmpty() ? PAGE_HEIGHT - TITLE_HEIGHT : PAGE_HEIGHT;
        for (List<BookLayout.Element> elements : BookLayout.layout(font, text.toString(), TEXT_WIDTH, firstHeight, PAGE_HEIGHT)) {
            pages.add(new DisplayPage(elements, null));
        }
        text.setLength(0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - PANE_WIDTH) / 2;
        int top = (height - PANE_HEIGHT) / 2;
        PoseStack pose = graphics.pose();

        // разворот книги нарисован в 1.3 раза крупнее и выходит за рамки области страниц
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate((width - PANE_WIDTH * 1.3f) / 2f, (height - PANE_HEIGHT * 1.3f) / 2f, 0f);
        pose.scale(1.3f, 1.3f, 1f);
        graphics.blit(BOOK, 0, 0, 0, 0, PANE_WIDTH, PANE_HEIGHT);
        pose.popPose();

        tooltipStack = null;
        for (int side = 0; side < 2; side++) {
            int index = page + side;
            if (index < pages.size()) {
                drawPage(graphics, pages.get(index), side, left, top, mouseX, mouseY);
            }
        }

        float bob = Mth.sin(minecraft.player.tickCount / 3f) * 0.2f + 0.1f;
        if (page > 0) {
            drawArrow(graphics, left - 16, top + 190, 0, bob);
        }
        if (page < pages.size() - 2) {
            drawArrow(graphics, left + 262, top + 190, 12, bob);
        }
        if (tooltipStack != null) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        }
    }

    private void drawArrow(GuiGraphics graphics, int x, int y, int u, float bob) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 6, y + 4, 0f);
        pose.scale(1f + bob, 1f + bob, 1f);
        RenderSystem.enableBlend();
        graphics.blit(BOOK, -6, -4, u, 184, 12, 8);
        pose.popPose();
    }

    private void drawPage(GuiGraphics graphics, DisplayPage display, int side, int x, int y, int mouseX, int mouseY) {
        if (page == 0 && side == 0) {
            // заголовок исследования между двумя линейками, только на первой странице
            RenderSystem.enableBlend();
            graphics.blit(BOOK, x + 4, y - 13, 24, 184, 96, 4);
            graphics.blit(BOOK, x + 4, y + 4, 24, 184, 96, 4);
            // заголовок тем же книжным шрифтом, что и текст
            Component name = research.getName().copy().withStyle(BookLayout.BODY.withBold(true));
            int nameWidth = font.width(name);
            if (nameWidth <= 130) {
                graphics.drawString(font, name, x + 52 - nameWidth / 2, y - 6, 0x303030, false);
            } else {
                float scale = 130f / nameWidth;
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(x + 52 - nameWidth / 2f * scale, y - 6f * scale, 0f);
                pose.scale(scale, scale, 1f);
                graphics.drawString(font, name, 0, 0, 0x303030, false);
                pose.popPose();
            }
            y += 25;
        }

        if (display.elements() != null) {
            drawElements(graphics, display.elements(), x - 15 + side * PAGE_SHIFT, y - 10);
            return;
        }
        ResearchEntry.Page content = display.recipe();
        ResourceLocation recipeId = content.recipe();
        if (!content.recipes().isEmpty()) {
            // несколько рецептов на одной странице сменяют друг друга раз в две секунды
            recipeId = content.recipes().get((int) (System.currentTimeMillis() / 2000L % content.recipes().size()));
        }
        Recipe<?> recipe = recipeId == null ? null : minecraft.level.getRecipeManager().byKey(recipeId).orElse(null);
        if (recipe != null && (content.type().equals("crafting") || content.type().equals("arcane"))) {
            drawCraftingPage(graphics, recipe, side, x - 4, y - 8, mouseX, mouseY);
        } else if (recipe instanceof CrucibleRecipe crucible) {
            drawCruciblePage(graphics, crucible, side, x - 4, y - 8, mouseX, mouseY);
        } else if (recipe instanceof thaumcraft.common.crafting.InfusionEnchantRecipe enchant) {
            drawEnchantPage(graphics, enchant, side, x - 4, y - 8, mouseX, mouseY);
        } else if (recipe instanceof thaumcraft.common.crafting.InfusionRecipe infusion) {
            drawInfusionPage(graphics, infusion, side, x - 4, y - 8, mouseX, mouseY);
        } else if (content.type().equals("sceptre")) {
            // три примера скипетра сменяют друг друга раз в две секунды
            List<Recipe<?>> examples = sceptreExamples();
            drawCraftingPage(graphics, examples.get((int) (System.currentTimeMillis() / 2000L % examples.size())), side, x - 4, y - 8,
                    mouseX, mouseY);
        } else if (content.construct() != null) {
            drawConstructPage(graphics, content.construct(), side, x - 4, y - 8, mouseX, mouseY);
        } else if (content.type().equals("smelting")) {
            drawSmeltingPage(graphics, content.item(), side, x - 4, y - 8, mouseX, mouseY);
        } else {
            List<List<BookLayout.Element>> placeholder = BookLayout.layout(font, I18n.get("tc.research.notported"),
                    TEXT_WIDTH, PAGE_HEIGHT, PAGE_HEIGHT);
            drawElements(graphics, placeholder.get(0), x - 15 + side * PAGE_SHIFT, y + 40);
        }
    }

    private void drawElements(GuiGraphics graphics, List<BookLayout.Element> elements, int x, int y) {
        for (BookLayout.Element element : elements) {
            if (element instanceof BookLayout.TextLine line) {
                int lineX = line.centered() ? x + (TEXT_WIDTH - font.width(line.text())) / 2 : x;
                graphics.drawString(font, line.text(), lineX, y + line.y(), 0x000000, false);
            } else if (element instanceof BookLayout.Rule rule) {
                RenderSystem.enableBlend();
                graphics.blit(BOOK, x + (TEXT_WIDTH - 96) / 2, y + rule.y(), 24, 184, 96, 4);
            } else if (element instanceof BookLayout.Image image) {
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(x + (TEXT_WIDTH - image.drawnWidth()) / 2f, y + image.y(), 0f);
                pose.scale(image.scale(), image.scale(), 1f);
                RenderSystem.enableBlend();
                // координаты картинок в оригинале заданы для листа 256x256, какого бы размера ни была текстура
                graphics.blit(image.texture(), 0, 0, image.u(), image.v(), image.width(), image.height(), 256, 256);
                pose.popPose();
            }
        }
    }

    private void drawCraftingPage(GuiGraphics graphics, Recipe<?> recipe, int side, int x, int y, int mouseX, int mouseY) {
        int start = side * PAGE_SHIFT;
        PoseStack pose = graphics.pose();
        boolean arcane = recipe instanceof ArcaneRecipe;

        // сетка и рамка результата из листа-накладки, вдвое крупнее
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(x + start, y, 0f);
        pose.scale(2f, 2f, 1f);
        graphics.blit(OVERLAY, 2, 27, arcane ? 112 : 60, 15, 52, 52);
        graphics.blit(OVERLAY, 20, 7, 20, 3, 16, 16);
        pose.popPose();

        Component title = Component.translatable(arcane ? "recipe.type.arcane" : "recipe.type.workbench");
        graphics.drawString(font, title, x + start + 56 - font.width(title) / 2, y, 0x505050, false);

        ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
        drawStack(graphics, result, x + 48 + start, y + 22, mouseX, mouseY);

        Recipe<?> layout = recipe instanceof ArcaneWrappedRecipe wrapped ? wrapped.getInner() : recipe;
        List<Ingredient> ingredients = layout.getIngredients();
        int recipeWidth = layout instanceof IShapedRecipe<?> shaped ? shaped.getRecipeWidth() : 3;
        // раз в секунду показывается следующий подходящий предмет (для тегов вроде «любые доски»)
        int cycle = (int) (System.currentTimeMillis() / 1000L);
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            ItemStack[] options = ingredients.get(i).getItems();
            if (options.length == 0) {
                continue;
            }
            int column = i % recipeWidth;
            int row = i / recipeWidth;
            drawStack(graphics, options[cycle % options.length], x + start + 16 + column * 32, y + 66 + row * 32, mouseX, mouseY);
        }

        if (recipe instanceof ArcaneRecipe arcaneRecipe) {
            AspectList cost = arcaneRecipe.getAspects(EMPTY_GRID);
            List<Aspect> aspects = cost.getAspectsSortedByAmount();
            int index = 0;
            for (Aspect aspect : aspects) {
                AspectRenderer.draw(graphics, font, aspect, cost.getAmount(aspect),
                        x + start + 14 + 18 * index + (5 - aspects.size()) * 8, y + 172, 1f);
                index++;
            }
        }
    }

    /** Страница инфузии (drawInfusionPage оригинала): результат, центральный предмет в кругу компонентов, эссенция. */
    private void drawInfusionPage(GuiGraphics graphics, thaumcraft.common.crafting.InfusionRecipe recipe, int side, int x, int y,
                                  int mouseX, int mouseY) {
        int start = side * PAGE_SHIFT;
        PoseStack pose = graphics.pose();
        Component title = Component.translatable("recipe.type.infusion");
        graphics.drawString(font, title, x + start + 56 - font.width(title) / 2, y, 0x505050, false);
        Component inst = Component.translatable("tc.inst").append(" ")
                .append(Component.translatable("tc.inst." + Math.min(5, recipe.getInstability() / 2)));
        graphics.drawString(font, inst, x + start + 56 - font.width(inst) / 2, y + 194, 0x505050, false);

        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(x + start, y + 20, 0f);
        pose.scale(2f, 2f, 1f);
        graphics.blit(OVERLAY, 0, 0, 0, 3, 56, 17);
        graphics.blit(OVERLAY, 0, 19, 200, 77, 60, 44);
        pose.popPose();

        AspectList cost = recipe.getAspects();
        List<Aspect> aspects = cost.getAspectsSortedByName();
        int rows = (aspects.size() - 1) / 5;
        int shift = (5 - aspects.size() % 5) * 10;
        int total = 0;
        for (Aspect aspect : aspects) {
            boolean lastRow = total / 5 >= rows && (rows > 1 || aspects.size() < 5);
            AspectRenderer.draw(graphics, font, aspect, cost.getAmount(aspect),
                    x + start + 8 + total % 5 * 20 + (lastRow ? shift : 0), y + 164 - 10 * rows + total / 5 * 20, 1f);
            total++;
        }

        int cycle = (int) (System.currentTimeMillis() / 1000L);
        drawStack(graphics, recipe.getResult(), x + 48 + start, y + 28, mouseX, mouseY);
        ItemStack[] centre = recipe.getCentral().getItems();
        if (centre.length > 0) {
            drawStack(graphics, centre[cycle % centre.length], x + 48 + start, y + 94, mouseX, mouseY);
        }
        List<Ingredient> components = recipe.getComponents();
        float slice = 360f / Math.max(1, components.size());
        float rotation = -90f;
        for (Ingredient component : components) {
            ItemStack[] options = component.getItems();
            int dx = (int) (Mth.cos(rotation / 180f * Mth.PI) * 40f) - 8;
            int dy = (int) (Mth.sin(rotation / 180f * Mth.PI) * 40f) - 8;
            rotation += slice;
            if (options.length > 0) {
                drawStack(graphics, options[cycle % options.length], x + 56 + start + dx, y + 102 + dy, mouseX, mouseY);
            }
        }
    }

    /** Страница тигля (drawCruciblePage оригинала): результат сверху, катализатор над котлом, аспекты в котле. */
    private void drawCruciblePage(GuiGraphics graphics, CrucibleRecipe recipe, int side, int x, int y, int mouseX, int mouseY) {
        int start = side * PAGE_SHIFT;
        PoseStack pose = graphics.pose();
        Component title = Component.translatable("recipe.type.crucible");
        graphics.drawString(font, title, x + start + 56 - font.width(title) / 2, y, 0x505050, false);

        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(x + start, y + 28, 0f);
        pose.scale(2f, 2f, 1f);
        graphics.blit(OVERLAY, 0, 0, 0, 3, 56, 17);
        graphics.blit(OVERLAY, 0, 32, 0, 20, 56, 48);
        graphics.blit(OVERLAY, 21, 24, 100, 84, 11, 13);
        pose.popPose();

        AspectList cost = recipe.getAspects();
        List<Aspect> aspects = cost.getAspectsSortedByName();
        int rows = (aspects.size() - 1) / 3;
        int shift = (3 - aspects.size() % 3) * 10;
        int total = 0;
        for (Aspect aspect : aspects) {
            // неполный последний ряд стоит по центру
            boolean lastRow = total / 3 >= rows && (rows > 1 || aspects.size() < 3);
            AspectRenderer.draw(graphics, font, aspect, cost.getAmount(aspect),
                    x + start + 28 + total % 3 * 20 + (lastRow ? shift : 0), y + 128 - 10 * rows + total / 3 * 20, 1f);
            total++;
        }

        drawStack(graphics, recipe.getResult(), x + 48 + start, y + 36, mouseX, mouseY);
        ItemStack[] options = recipe.getCatalyst().getItems();
        if (options.length > 0) {
            drawStack(graphics, options[(int) (System.currentTimeMillis() / 1000L % options.length)],
                    x + 26 + start, y + 72, mouseX, mouseY);
        }
    }

    /**
     * Страница-чертёж составной конструкции (drawCompoundCraftingPage оригинала): блоки нарисованы послойно,
     * верхний слой сверху, каждый слой — ромбом, как видно сбоку-сверху. Внизу — сколько вис возьмёт жезл.
     */
    private void drawConstructPage(GuiGraphics graphics, ResearchEntry.Construct construct, int side, int x, int y, int mouseX, int mouseY) {
        int start = side * PAGE_SHIFT;
        PoseStack pose = graphics.pose();
        int dx = construct.dx();
        int dy = construct.dy();
        int dz = construct.dz();
        int xoff = 64 - (dx * 16 + dz * 16) / 2;
        int yoff = -dy * 25;
        Component title = Component.translatable("recipe.type.construct");
        graphics.drawString(font, title, x + start + 56 - font.width(title) / 2, y, 0x505050, false);

        AspectList cost = construct.aspects();
        RenderSystem.enableBlend();
        if (!cost.isEmpty()) {
            pose.pushPose();
            pose.translate(x + start, y + 174, 0f);
            pose.scale(2f, 2f, 1f);
            RenderSystem.enableBlend();
            graphics.setColor(1f, 1f, 1f, 0.4f);
            graphics.blit(OVERLAY, 0, 0, 68, 76, 12, 12);
            graphics.setColor(1f, 1f, 1f, 1f);
            pose.popPose();
            int count = 0;
            for (Aspect aspect : cost.getAspectsSortedByAmount()) {
                AspectRenderer.draw(graphics, font, aspect, cost.getAmount(aspect),
                        x + start + 14 + 18 * count + (5 - cost.size()) * 8, y + 182, 1f);
                count++;
            }
        }

        // высокие конструкции (выше трёх слоёв) рисуются мельче, чтобы уместиться на странице
        float shrink = dy > 3 ? (dy - 3) * 0.2f : 0f;
        float originX = x + start + xoff * (1f + shrink);
        float originY = y + 108 + yoff * (1f - shrink);
        pose.pushPose();
        pose.translate(originX, originY, 0f);
        pose.scale(1f - shrink, 1f - shrink, 1f);

        pose.pushPose();
        pose.translate(-8 - xoff, -119 + Math.max(3 - dx, 3 - dz) * 8 + dx * 4 + dz * 4 + dy * 50, 0f);
        pose.scale(2f, 2f, 1f);
        RenderSystem.enableBlend();
        graphics.setColor(1f, 1f, 1f, 0.5f);
        graphics.blit(OVERLAY, 0, 0, 0, 72, 64, 44);
        graphics.setColor(1f, 1f, 1f, 1f);
        pose.popPose();

        int index = 0;
        for (int j = 0; j < dy; j++) {
            for (int k = dz - 1; k >= 0; k--) {
                for (int i = dx - 1; i >= 0; i--) {
                    ItemStack stack = constructStack(construct, index++);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    int px = i * 16 + k * 16;
                    int py = -i * 8 + k * 8 + j * 50;
                    pose.pushPose();
                    pose.translate(0f, 0f, 60 - j * 10);
                    graphics.renderItem(stack, px, py);
                    pose.popPose();
                    float sx = originX + px * (1f - shrink);
                    float sy = originY + py * (1f - shrink);
                    if (mouseX >= sx && mouseY >= sy && mouseX < sx + 16f * (1f - shrink) && mouseY < sy + 16f * (1f - shrink)) {
                        tooltipStack = stack;
                    }
                }
            }
        }
        pose.popPose();
    }

    private static ItemStack constructStack(ResearchEntry.Construct construct, int index) {
        if (index >= construct.blocks().size() || construct.blocks().get(index).isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(construct.blocks().get(index).get()));
    }

    /**
     * Страница зачарования наполнением (drawInfusionEnchantingPage оригинала): название зачарования по очереди на
     * каждом уровне, цена этого уровня в опыте и эссенции, предметы вокруг алтаря.
     */
    private void drawEnchantPage(GuiGraphics graphics, thaumcraft.common.crafting.InfusionEnchantRecipe recipe, int side, int x, int y,
                                 int mouseX, int mouseY) {
        int start = side * PAGE_SHIFT;
        PoseStack pose = graphics.pose();
        int level = (int) (1L + System.currentTimeMillis() / 1000L % recipe.getEnchantment().getMaxLevel());
        Component title = Component.translatable("recipe.type.infusionenchantment");
        graphics.drawString(font, title, x + start + 56 - font.width(title) / 2, y, 0x505050, false);
        Component inst = Component.translatable("tc.inst").append(" ")
                .append(Component.translatable("tc.inst." + Math.min(5, recipe.getInstability() / 2)));
        graphics.drawString(font, inst, x + start + 56 - font.width(inst) / 2, y + 194, 0x505050, false);
        String name = recipe.getEnchantment().getFullname(level).getString();
        graphics.drawString(font, name, x + start + 56 - font.width(name) / 2, y + 24, 0x705090, false);
        Component xp = Component.translatable("tc.infusion.levels", recipe.getBaseXp() * level);
        graphics.drawString(font, xp, x + start + 56 - font.width(xp) / 2, y + 40, 0x508850, false);

        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(x + start, y + 20, 0f);
        pose.scale(2f, 2f, 1f);
        graphics.blit(OVERLAY, 0, 19, 200, 77, 60, 44);
        pose.popPose();

        AspectList cost = recipe.getAspects();
        List<Aspect> aspects = cost.getAspectsSortedByName();
        int rows = (aspects.size() - 1) / 5;
        int shift = (5 - aspects.size() % 5) * 10;
        int total = 0;
        for (Aspect aspect : aspects) {
            boolean lastRow = total / 5 >= rows && (rows > 1 || aspects.size() < 5);
            AspectRenderer.draw(graphics, font, aspect, cost.getAmount(aspect) * level,
                    x + start + 8 + total % 5 * 20 + (lastRow ? shift : 0), y + 164 - 10 * rows + total / 5 * 20, 1f);
            total++;
        }

        List<net.minecraft.world.item.crafting.Ingredient> components = recipe.getComponents();
        float slice = 360f / components.size();
        float angle = -90f;
        for (net.minecraft.world.item.crafting.Ingredient component : components) {
            int cx = (int) (Math.cos(angle / 180f * Math.PI) * 40f) - 8;
            int cy = (int) (Math.sin(angle / 180f * Math.PI) * 40f) - 8;
            angle += slice;
            ItemStack[] options = component.getItems();
            if (options.length > 0) {
                drawStack(graphics, options[(int) (System.currentTimeMillis() / 1000L % options.length)],
                        x + 56 + start + cx, y + 102 + cy, mouseX, mouseY);
            }
        }
    }

    private static List<Recipe<?>> sceptres;

    /**
     * Примеры для страницы «Скипетр»: настоящий рецепт собирает скипетр из любых набалдашников и стержня, поэтому книга,
     * как и в оригинале, показывает три образца — простой, золотой с великим деревом и тауминиевый с серебряным.
     */
    private static List<Recipe<?>> sceptreExamples() {
        if (sceptres == null) {
            List<Recipe<?>> list = new java.util.ArrayList<>();
            thaumcraft.api.wands.WandCap[] caps = {thaumcraft.api.wands.WandCap.IRON, thaumcraft.api.wands.WandCap.GOLD,
                    thaumcraft.api.wands.WandCap.THAUMIUM};
            thaumcraft.api.wands.WandRod[] rods = {thaumcraft.api.wands.WandRod.WOOD, thaumcraft.api.wands.WandRod.GREATWOOD,
                    thaumcraft.api.wands.WandRod.SILVERWOOD};
            for (int i = 0; i < caps.length; i++) {
                Ingredient cap = Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(caps[i].getItemId()));
                Ingredient rod = Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rods[i].getItemId()));
                Ingredient charm = Ingredient.of(thaumcraft.common.registry.ModItems.PRIMAL_CHARM.get());
                net.minecraft.core.NonNullList<Ingredient> grid = net.minecraft.core.NonNullList.of(Ingredient.EMPTY,
                        Ingredient.EMPTY, cap, charm, Ingredient.EMPTY, rod, cap, cap, Ingredient.EMPTY, Ingredient.EMPTY);
                ItemStack result = thaumcraft.common.items.wands.WandItem.create(thaumcraft.common.registry.ModItems.WAND.get(), rods[i], caps[i], false);
                thaumcraft.common.items.wands.WandItem.setSceptre(result);
                AspectList cost = new AspectList();
                for (Aspect aspect : Aspect.primals()) {
                    cost.add(aspect, (int) (caps[i].getCraftCost() * rods[i].getCraftCost() * 1.5f));
                }
                ResourceLocation id = thaumcraft.Thaumcraft.id("sceptre_example_" + i);
                list.add(new ArcaneWrappedRecipe(new net.minecraft.world.item.crafting.ShapedRecipe(id, "",
                        net.minecraft.world.item.crafting.CraftingBookCategory.MISC, 3, 3, grid, result), cost, "SCEPTRE", () -> null));
            }
            sceptres = list;
        }
        return sceptres;
    }

    /** Страница переплавки (drawSmeltingPage оригинала): что положить в печь и что из неё выйдет. */
    private void drawSmeltingPage(GuiGraphics graphics, @org.jetbrains.annotations.Nullable ResourceLocation input, int side, int x, int y,
                                  int mouseX, int mouseY) {
        if (input == null) {
            return;
        }
        ItemStack in = new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(input));
        ItemStack out = ItemStack.EMPTY;
        for (net.minecraft.world.item.crafting.SmeltingRecipe recipe : minecraft.level.getRecipeManager()
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
            if (!recipe.getIngredients().isEmpty() && recipe.getIngredients().get(0).test(in)) {
                out = recipe.getResultItem(minecraft.level.registryAccess());
                break;
            }
        }
        // как и в оригинале, страница пуста, если предмет в печи ни во что не переплавляется
        if (in.isEmpty() || out.isEmpty()) {
            return;
        }
        int start = side * PAGE_SHIFT;
        PoseStack pose = graphics.pose();
        Component title = Component.translatable("recipe.type.smelting");
        graphics.drawString(font, title, x + start + 56 - font.width(title) / 2, y, 0x505050, false);
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(x + start, y + 28, 0f);
        pose.scale(2f, 2f, 1f);
        graphics.blit(OVERLAY, 0, 0, 0, 192, 56, 64);
        pose.popPose();
        drawStack(graphics, in, x + 48 + start, y + 64, mouseX, mouseY);
        drawStack(graphics, out, x + 48 + start, y + 144, mouseX, mouseY);
    }

    /** Цена форменных рецептов от раскладки не зависит, поэтому её можно спросить у пустой сетки. */
    private static final CraftingContainer EMPTY_GRID = new net.minecraft.world.inventory.TransientCraftingContainer(
            new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                    return false;
                }
            }, 3, 3);

    private void drawStack(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 16) {
            tooltipStack = stack;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - PANE_WIDTH) / 2;
        int top = (height - PANE_HEIGHT) / 2;
        double nextX = mouseX - (left + 261);
        double prevX = mouseX - (left - 17);
        double arrowY = mouseY - (top + 189);
        if (arrowY >= 0 && arrowY < 10) {
            if (page < pages.size() - 2 && nextX >= 0 && nextX < 14) {
                page += 2;
                minecraft.player.playSound(ModSounds.PAGE.get(), 0.66f, 1f);
                return true;
            }
            if (page >= 2 && prevX >= 0 && prevX < 14) {
                page -= 2;
                minecraft.player.playSound(ModSounds.PAGE.get(), 0.66f, 1f);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Колесо мыши тоже листает страницы. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0 && page < pages.size() - 2) {
            page += 2;
        } else if (delta > 0 && page >= 2) {
            page -= 2;
        } else {
            return false;
        }
        minecraft.player.playSound(ModSounds.PAGE.get(), 0.66f, 1f);
        return true;
    }

    /** Выход из страниц возвращает на карту исследований, а не закрывает книгу. */
    @Override
    public void onClose() {
        minecraft.setScreen(new ResearchBrowserScreen());
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
