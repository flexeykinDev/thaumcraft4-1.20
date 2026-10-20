package thaumictinkerer.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.WarpGateBlock;
import thaumictinkerer.common.item.kami.SkyPearlItem;
import thaumictinkerer.common.network.TTNetwork;
import thaumictinkerer.common.registry.TTItems;

import java.util.ArrayList;
import java.util.List;

/** Окна врат искривления: настройка (GuiWarpGate) и карта направлений (GuiWarpGateDestinations). */
public final class WarpGateScreens {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/warp_gate.png");

    private WarpGateScreens() {
    }

    public static class Config extends AbstractContainerScreen<WarpGateBlock.Menu> {
        public Config(WarpGateBlock.Menu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageWidth = 176;
            imageHeight = 166;
        }

        private boolean locked() {
            return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof WarpGateBlock.Tile tile
                    && tile.locked;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            renderTooltip(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            // галочка «принимать гостей» стоит, когда врата не заперты
            graphics.blit(TEXTURE, leftPos + 5, topPos + 5, 176, locked() ? 0 : 9, 9, 9);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(font, Component.translatable("ttmisc.lockedGate"), 20, 6, 0x999999);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= leftPos + 5 && mouseX < leftPos + 14 && mouseY >= topPos + 5 && mouseY < topPos + 14 && minecraft != null
                    && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WarpGateBlock.Menu.LOCK);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    /** Карта: один блок — одна точка; тащите мышью, пробел — к вратам, цифры — к направлению, Shift+щелчок — перенос. */
    public static class Destinations extends Screen {
        private final WarpGateBlock.Tile gate;
        private double x;
        private double y;
        private int ticks;

        public Destinations(WarpGateBlock.Tile gate) {
            super(Component.translatable("ttmisc.destinations"));
            this.gate = gate;
        }

        @Override
        protected void init() {
            recenter(gate.getBlockPos());
        }

        private void recenter(BlockPos pos) {
            x = pos.getX() - width / 2.0;
            y = pos.getZ() - height / 2.0;
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private List<Integer> destinations() {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < WarpGateBlock.Tile.SLOTS; i++) {
                ItemStack pearl = gate.items.getItem(i);
                if (SkyPearlItem.isAttuned(pearl) && gate.getLevel() != null && gate.getLevel().dimension().equals(SkyPearlItem.dimension(pearl))) {
                    out.add(i);
                }
            }
            return out;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xE0100818);
            int gx = (int) (gate.getBlockPos().getX() - x);
            int gy = (int) (gate.getBlockPos().getZ() - y);
            int alpha = (int) (((Math.sin(ticks / 10.0) + 1) / 4 + 0.25) * 255);
            List<Component> tooltip = new ArrayList<>();
            for (int index : destinations()) {
                BlockPos to = SkyPearlItem.pos(gate.items.getItem(index));
                line(graphics, gx, gy, (int) (to.getX() - x), (int) (to.getZ() - y), alpha << 24 | 0xFFFFFF);
            }
            graphics.drawString(font, Component.translatable("ttmisc.destinations").withStyle(ChatFormatting.UNDERLINE), 3, 40, 0xFFFFFF);
            pearl(graphics, -1, gx, gy, mouseX, mouseY, tooltip);
            for (int index : destinations()) {
                BlockPos to = SkyPearlItem.pos(gate.items.getItem(index));
                pearl(graphics, index, (int) (to.getX() - x), (int) (to.getZ() - y), mouseX, mouseY, tooltip);
            }
            graphics.drawCenteredString(font, Component.translatable("ttmisc.numberKeys"), width / 2, 5, 0xFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("ttmisc.spaceToReset"), width / 2, 16, 0xFFFFFF);
            if (!tooltip.isEmpty()) {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        private void pearl(GuiGraphics graphics, int index, int px, int py, int mouseX, int mouseY, List<Component> tooltip) {
            ItemStack stack = index < 0 ? new ItemStack(TTItems.SKY_PEARL.get()) : gate.items.getItem(index);
            graphics.pose().pushPose();
            graphics.pose().translate(px - 4, py - 4, 0);
            graphics.pose().scale(0.5f, 0.5f, 1f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
            Component name = index < 0 ? Component.translatable("ttmisc.entrancePoint")
                    : stack.hasCustomHoverName() ? stack.getHoverName() : Component.translatable("ttmisc.destination");
            if (index >= 0) {
                graphics.drawString(font, (index + 1) + ": " + name.getString(), 5, 54 + index * 11, 0xFFFFFF);
            }
            if (mouseX >= px - 4 && mouseX <= px + 4 && mouseY >= py - 4 && mouseY < py + 4) {
                tooltip.add(name.copy().withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(" " + Component.translatable("ttmisc.destinationInd", index + 1).getString()).withStyle(ChatFormatting.ITALIC)));
                if (index >= 0) {
                    BlockPos to = SkyPearlItem.pos(stack);
                    tooltip.add(Component.literal("X: " + to.getX() + "  Z: " + to.getZ()));
                    tooltip.add(Component.translatable("ttmisc.clickToTeleport"));
                } else {
                    tooltip.add(Component.literal("X: " + gate.getBlockPos().getX() + "  Z: " + gate.getBlockPos().getZ()));
                }
            }
        }

        /** Отрезок по точкам (Брезенхем). */
        private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
            int dx = Math.abs(x1 - x0);
            int dy = -Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int error = dx + dy;
            for (int steps = 0; steps < 4096; steps++) {
                graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
                if (x0 == x1 && y0 == y1) {
                    return;
                }
                int e2 = 2 * error;
                if (e2 >= dy) {
                    error += dy;
                    x0 += sx;
                }
                if (e2 <= dx) {
                    error += dx;
                    y0 += sy;
                }
            }
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            x -= dragX;
            y -= dragY;
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hasShiftDown()) {
                for (int index : destinations()) {
                    BlockPos to = SkyPearlItem.pos(gate.items.getItem(index));
                    int px = (int) (to.getX() - x);
                    int py = (int) (to.getZ() - y);
                    if (mouseX >= px - 4 && mouseX <= px + 4 && mouseY >= py - 4 && mouseY < py + 4) {
                        TTNetwork.sendWarpTeleport(gate.getBlockPos(), index);
                        onClose();
                        return true;
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if (key == GLFW.GLFW_KEY_SPACE) {
                recenter(gate.getBlockPos());
                return true;
            }
            if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 || key == GLFW.GLFW_KEY_0) {
                int index = key == GLFW.GLFW_KEY_0 ? 9 : key - GLFW.GLFW_KEY_1;
                BlockPos to = SkyPearlItem.pos(gate.items.getItem(index));
                if (to != null) {
                    recenter(to);
                }
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }
    }
}
