package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ArcaneSpaBlock;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/** Окно магической купальни — перенос GuiSpa оригинала: ячейка для солей, переключатель смешивания и бак. */
public class SpaScreen extends AbstractContainerScreen<SpaMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_spa.png");

    public SpaScreen(SpaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (over(mouseX, mouseY, 104, 10, 10, 55)) {
            FluidStack fluid = menu.spa.tank.getFluid();
            if (!fluid.isEmpty()) {
                graphics.renderComponentTooltip(font, List.of(fluid.getDisplayName(), Component.literal(fluid.getAmount() + " mb")),
                        mouseX, mouseY);
            }
        }
        if (over(mouseX, mouseY, 88, 34, 10, 10)) {
            graphics.renderTooltip(font, Component.translatable(menu.spa.getMix() ? "text.spa.mix.true" : "text.spa.mix.false"), mouseX, mouseY);
        }
    }

    private boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        double dx = mouseX - (leftPos + x);
        double dy = mouseY - (topPos + y);
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEXTURE, leftPos + 89, topPos + 35, 208, menu.spa.getMix() ? 16 : 32, 8, 8);
        ArcaneSpaBlock.Spa spa = menu.spa;
        FluidStack fluid = spa.tank.getFluid();
        if (!fluid.isEmpty()) {
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
            int tint = extensions.getTintColor(fluid);
            graphics.setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, 1f);
            // столбик жидкости во весь бак, затем его верх закрывается фоном по уровню
            for (int tile = 0; tile < 6; tile++) {
                graphics.blit(leftPos + 107, topPos + 15 + tile * 8, 0, 8, 8, sprite);
            }
            graphics.setColor(1f, 1f, 1f, 1f);
            float bar = spa.tank.getFluidAmount() / (float) spa.tank.getCapacity();
            graphics.blit(TEXTURE, leftPos + 107, topPos + 15, 107, 15, 10, (int) (48f - 48f * bar));
        }
        // стеклянная колба поверх
        graphics.blit(TEXTURE, leftPos + 106, topPos + 11, 232, 0, 10, 55);
        RenderSystem.disableBlend();
    }

    /** В оригинале подписей в этом окне нет. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (over(mouseX, mouseY, 89, 35, 8, 8)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    ModSounds.CAMERA_CLACK.get(), SoundSource.BLOCKS, 0.4f, 1f, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
