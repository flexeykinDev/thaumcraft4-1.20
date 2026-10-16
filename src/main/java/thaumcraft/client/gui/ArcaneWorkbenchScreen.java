package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;

import java.text.DecimalFormat;

/**
 * Окно магического верстака (GuiArcaneWorkbench оригинала): вокруг сетки шесть мест под первичные аспекты,
 * где показана цена рецепта. Цена мерцает, пока в жезле не хватает вис.
 */
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_arcaneworkbench.png");
    /** Места шести первичных аспектов вокруг сетки, в порядке: воздух, земля, огонь, вода, порядок, энтропия. */
    private static final int[][] ASPECT_SPOTS = {{72, 21}, {24, 43}, {24, 102}, {72, 124}, {120, 102}, {120, 43}};
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.#");

    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 190;
        imageHeight = 234;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // в оригинале подписей в этом окне нет
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ArcaneRecipe recipe = menu.findArcaneRecipe();
        if (recipe == null || minecraft == null || minecraft.player == null) {
            return;
        }
        ItemStack wand = menu.getWand();
        boolean hasWand = wand.getItem() instanceof WandItem;
        AspectList cost = recipe.getAspects(menu.getGrid());

        int index = 0;
        for (Aspect primal : Aspect.primals()) {
            float amount = cost.getAmount(primal);
            if (amount > 0 && index < ASPECT_SPOTS.length) {
                float alpha = 0.5f + (Mth.sin((minecraft.player.tickCount + index * 10) / 2f) * 0.2f - 0.2f);
                if (hasWand) {
                    amount *= WandItem.getConsumptionModifier(wand, minecraft.player, primal, true);
                    if (amount * 100f <= WandItem.getVis(wand, primal)) {
                        alpha = 1f;
                    }
                }
                AspectRenderer.draw(graphics, font, primal, FORMAT.format(amount),
                        leftPos + ASPECT_SPOTS[index][0] - 8, topPos + ASPECT_SPOTS[index][1] - 8, alpha);
            }
            index++;
        }

        if (hasWand && !menu.payVis(recipe, false)) {
            // вис не хватает: результат показан затемнённым призраком с пояснением
            ItemStack ghost = recipe.assemble(menu.getGrid(), minecraft.player.level().registryAccess());
            graphics.renderItem(ghost, leftPos + 160, topPos + 64);
            graphics.renderItemDecorations(font, ghost, leftPos + 160, topPos + 64);
            graphics.fill(leftPos + 160, topPos + 64, leftPos + 176, topPos + 80, 0xaa202020);

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(leftPos + 168, topPos + 46, 300f);
            pose.scale(0.5f, 0.5f, 1f);
            Component text = Component.translatable("tc.arcane.insufficient");
            graphics.drawString(font, text, -font.width(text) / 2, 0, 0xee6e6e, false);
            pose.popPose();
        }
    }
}
