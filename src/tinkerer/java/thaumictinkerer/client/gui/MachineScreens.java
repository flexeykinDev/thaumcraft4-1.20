package thaumictinkerer.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.AnimationTabletBlock;
import thaumictinkerer.common.block.MagnetBlock;
import thaumictinkerer.common.block.RemotePlacerBlock;
import thaumictinkerer.common.item.SoulMouldItem;

/**
 * Окна магнита для существ, удалённого установщика и анимационной таблички (GuiMobMagnet, GuiRemotePlacer и
 * GuiAnimationTablet оригинала): фон, подписи и переключатели 13×13, нарисованные в правой части текстуры.
 */
public final class MachineScreens {
    private MachineScreens() {
    }

    private abstract static class Base<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
        private final ResourceLocation texture;

        Base(M menu, Inventory inventory, Component title, String texture) {
            super(menu, inventory, title);
            this.texture = ThaumicTinkerer.id("textures/gui/" + texture + ".png");
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            renderTooltip(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            drawExtra(graphics);
        }

        abstract void drawExtra(GuiGraphics graphics);

        /** Переключатель: включённый рисуется кадром (176, 13). */
        void toggle(GuiGraphics graphics, int x, int y, boolean on) {
            if (on) {
                graphics.blit(texture, leftPos + x, topPos + y, 176, 13, 13, 13);
            }
        }

        boolean inside(double mx, double my, int x, int y) {
            return mx >= leftPos + x && my >= topPos + y && mx < leftPos + x + 13 && my < topPos + y + 13;
        }

        boolean press(int id) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return true;
        }

        void label(GuiGraphics graphics, Component text, int x, int y) {
            graphics.drawString(font, text, leftPos + x, topPos + y, 0x999999, false);
        }
    }

    public static class MobMagnet extends Base<MagnetBlock.Menu> {
        public MobMagnet(MagnetBlock.Menu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "mob_magnet");
        }

        @Override
        void drawExtra(GuiGraphics graphics) {
            MagnetBlock.Tile tile = menu.tile;
            if (tile == null) {
                return;
            }
            ItemStack mould = tile.mould();
            EntityType<?> pattern = SoulMouldItem.pattern(mould);
            Component filter = mould.isEmpty() ? Component.translatable("ttmisc.all")
                    : pattern == null ? Component.translatable("ttmisc.none") : pattern.getDescription();
            label(graphics, filter, imageWidth / 2 - font.width(filter) / 2 - 26, 16);
            label(graphics, Component.translatable("ttmisc.adult"), 120, 30);
            label(graphics, Component.translatable("ttmisc.child"), 120, 50);
            toggle(graphics, 100, 28, tile.adult);
            toggle(graphics, 100, 48, !tile.adult);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (inside(mouseX, mouseY, 100, 28)) {
                return press(0);
            }
            if (inside(mouseX, mouseY, 100, 48)) {
                return press(1);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public static class RemotePlacer extends Base<RemotePlacerBlock.Menu> {
        public RemotePlacer(RemotePlacerBlock.Menu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "remote_placer");
        }

        @Override
        void drawExtra(GuiGraphics graphics) {
            if (menu.tile == null) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                toggle(graphics, 100, i * 13, menu.tile.distance == i + 1);
                label(graphics, Component.literal(String.valueOf(i + 1)), 116, i * 13 + 3);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (int i = 0; i < 4; i++) {
                if (inside(mouseX, mouseY, 100, i * 13)) {
                    return press(i + 1);
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public static class AnimationTablet extends Base<AnimationTabletBlock.Menu> {
        public AnimationTablet(AnimationTabletBlock.Menu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "animation_tablet");
        }

        @Override
        void drawExtra(GuiGraphics graphics) {
            if (menu.tile == null) {
                return;
            }
            Component left = Component.translatable("ttmisc.leftClick");
            Component redstone = Component.translatable("ttmisc.redstoneControl");
            label(graphics, left, 48 - font.width(left), 18);
            label(graphics, Component.translatable("ttmisc.rightClick"), 128, 18);
            label(graphics, redstone, imageWidth / 2 - font.width(redstone) / 2, 50);
            toggle(graphics, imageWidth / 2 - 7, 60, menu.tile.redstone);
            toggle(graphics, 52, 15, menu.tile.leftClick);
            toggle(graphics, 111, 15, !menu.tile.leftClick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (inside(mouseX, mouseY, imageWidth / 2 - 7, 60)) {
                return press(0);
            }
            if (inside(mouseX, mouseY, 52, 15)) {
                return press(1);
            }
            if (inside(mouseX, mouseY, 111, 15)) {
                return press(2);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
