package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.ThaumatoriumBlock;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Окно тауматория — перенос GuiThaumatorium оригинала: стрелками листают подходящие рецепты, щелчок по предмету
 * задаёт рецепт или снимает его; под ним — нужные аспекты и то, сколько каждого уже набрано.
 */
public class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_thaumatorium.png");

    private int index;
    private int lastSize;
    private int startAspect;

    public ThaumatoriumScreen(ThaumatoriumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        menu.updateRecipes();
        lastSize = menu.recipes.size();
        refreshIndex();
    }

    private ThaumatoriumBlock.Core core() {
        return menu.thaumatorium;
    }

    private boolean selected(CrucibleRecipe recipe) {
        return core().recipes.contains(recipe.getId());
    }

    /** Открываем окно на первом из уже заданных рецептов. */
    private void refreshIndex() {
        for (int i = 0; i < menu.recipes.size(); i++) {
            if (selected(menu.recipes.get(i))) {
                index = i;
                break;
            }
        }
        startAspect = 0;
    }

    private boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        double dx = mouseX - (leftPos + x);
        double dy = mouseY - (topPos + y);
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }

    @Override
    protected void containerTick() {
        // список зависит от катализатора и от заданных рецептов, которые приходят с сервера
        menu.updateRecipes();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!menu.recipes.isEmpty() && over(mouseX, mouseY, 112, 16, 16, 16)) {
            graphics.renderTooltip(font, menu.recipes.get(index).getResult(), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int k = leftPos;
        int l = topPos;
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, k, l, 0, 0, imageWidth, imageHeight);
        List<CrucibleRecipe> recipes = menu.recipes;
        if (index >= recipes.size()) {
            index = recipes.size() - 1;
        }
        if (recipes.isEmpty()) {
            return;
        }
        if (lastSize != recipes.size()) {
            lastSize = recipes.size();
            refreshIndex();
        }
        if (index < 0) {
            index = 0;
        }
        CrucibleRecipe recipe = recipes.get(index);
        AspectList aspects = recipe.getAspects();
        if (recipes.size() > 1) {
            graphics.blit(TEXTURE, k + 128, l + 16, index > 0 ? 192 : 176, 16, 16, 8);
            graphics.blit(TEXTURE, k + 128, l + 24, index < recipes.size() - 1 ? 192 : 176, 24, 16, 8);
        }
        if (aspects.size() > 6) {
            graphics.blit(TEXTURE, k + 32, l + 40, startAspect > 0 ? 192 : 176, 32, 8, 16);
            graphics.blit(TEXTURE, k + 136, l + 40, startAspect < aspects.size() - 1 ? 200 : 184, 32, 8, 16);
        } else {
            startAspect = 0;
        }
        int ticks = minecraft.player.tickCount;
        if (!core().recipes.isEmpty()) {
            if (over(mouseX, mouseY, 112, 16, 16, 16) || selected(recipe)) {
                graphics.blit(TEXTURE, k + 104, l + 8, 176, 96, 48, 48);
            }
            // стрелка от катализатора к результату мерцает, пока тауматорию есть что делать
            float alpha = Mth.clamp(0.6f + Mth.sin(ticks / 5f) * 0.4f + 0.4f, 0f, 1f);
            graphics.setColor(1f, 1f, 1f, alpha);
            graphics.blit(TEXTURE, k + 88, l + 16, 176, 56, 24, 24);
            graphics.setColor(1f, 1f, 1f, 1f);
        }
        drawAspects(graphics, recipe, k, l);
        drawOutput(graphics, recipe, k, l, ticks);
        if (core().maxRecipes > 1) {
            String text = core().recipes.size() + "/" + core().maxRecipes;
            graphics.pose().pushPose();
            graphics.pose().translate(k + 136, l + 33, 0);
            graphics.pose().scale(0.5f, 0.5f, 1f);
            graphics.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFF, false);
            graphics.pose().popPose();
        }
    }

    private void drawAspects(GuiGraphics graphics, CrucibleRecipe recipe, int k, int l) {
        AspectList aspects = recipe.getAspects();
        List<Aspect> sorted = aspects.getAspectsSortedByName();
        if (selected(recipe)) {
            int pos = 0;
            for (int count = startAspect; count < sorted.size() && pos < 6; count++, pos++) {
                Aspect aspect = sorted.get(count);
                graphics.blit(TEXTURE, k + 41 + 16 * pos, l + 57, 176, 8, 14, 6);
                int filled = (int) (Math.min(1f, core().essentia.getAmount(aspect) / (float) aspects.getAmount(aspect)) * 12f);
                int color = aspect.getColor();
                graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1f);
                graphics.blit(TEXTURE, k + 42 + 16 * pos, l + 58, 176, 0, filled, 4);
                graphics.setColor(1f, 1f, 1f, 1f);
            }
        }
        int pos = 0;
        for (int count = startAspect; count < sorted.size() && pos < 6; count++, pos++) {
            Aspect aspect = sorted.get(count);
            AspectRenderer.draw(graphics, font, aspect, aspects.getAmount(aspect), k + 40 + 16 * pos, l + 40, 1f);
        }
    }

    private void drawOutput(GuiGraphics graphics, CrucibleRecipe recipe, int k, int l, int ticks) {
        ItemStack output = recipe.getResult();
        // рецепт, который можно задать или снять, мерцает
        boolean dull = core().recipes.size() < core().maxRecipes || selected(recipe);
        if (dull) {
            float alpha = Mth.clamp(0.3f + Mth.sin(ticks / 4f) * 0.3f + 0.3f, 0f, 1f);
            graphics.setColor(0.5f + alpha * 0.5f, 0.5f + alpha * 0.5f, 0.5f + alpha * 0.5f, 1f);
        }
        graphics.renderItem(output, k + 112, l + 16);
        graphics.renderItemDecorations(font, output, k + 112, l + 16);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** В оригинале подписей в этом окне нет. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<CrucibleRecipe> recipes = menu.recipes;
        if (!recipes.isEmpty() && index >= 0 && index < recipes.size()) {
            CrucibleRecipe recipe = recipes.get(index);
            if (over(mouseX, mouseY, 112, 16, 16, 16) && (core().recipes.size() < core().maxRecipes || selected(recipe))) {
                int global = ThaumatoriumBlock.allRecipes(minecraft.level).indexOf(recipe);
                if (global >= 0) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, global);
                    play(ModSounds.COMBINE.get(), 0.3f);
                }
                return true;
            }
            if (recipes.size() > 1) {
                if (index > 0 && over(mouseX, mouseY, 128, 16, 16, 8)) {
                    index--;
                    startAspect = 0;
                    play(ModSounds.CAMERA_CLACK.get(), 0.4f);
                    return true;
                }
                if (index < recipes.size() - 1 && over(mouseX, mouseY, 128, 24, 16, 8)) {
                    index++;
                    startAspect = 0;
                    play(ModSounds.CAMERA_CLACK.get(), 0.4f);
                    return true;
                }
            }
            int aspects = recipe.getAspects().size();
            if (aspects > 6) {
                if (startAspect > 0 && over(mouseX, mouseY, 32, 40, 8, 16)) {
                    startAspect--;
                    play(ModSounds.CAMERA_CLACK.get(), 0.4f);
                    return true;
                }
                if (startAspect < aspects - 1 && over(mouseX, mouseY, 136, 40, 8, 16)) {
                    startAspect++;
                    play(ModSounds.CAMERA_CLACK.get(), 0.4f);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void play(SoundEvent sound, float volume) {
        minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), sound, SoundSource.BLOCKS,
                volume, 1f, false);
    }
}
