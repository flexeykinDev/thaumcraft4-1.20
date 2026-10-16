package thaumcraft.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.HudOverlay;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.ScanSlotPacket;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.ScanManager;

/**
 * Сканирование в инвентаре — порт аддона Thaumcraft Inventory Scanning (BlayTheNinth).
 * Возьми таумометр на курсор и подержи над предметом в любом слоте или над своей фигуркой в инвентаре.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class InventoryScanHandler {
    private static final int SCAN_TICKS = 50;

    @Nullable
    private static Slot hoveredSlot;
    @Nullable
    private static Slot lastScannedSlot;
    private static boolean hoveringSelf;
    private static boolean selfDone;
    private static int ticksHovered;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (event.phase != TickEvent.Phase.END || player == null) {
            return;
        }
        if (!supports(mc.screen) || !carryingThaumometer(player)) {
            ticksHovered = 0;
            lastScannedSlot = null;
            selfDone = false;
            return;
        }

        int target;
        if (hoveringSelf) {
            if (selfDone || ClientKnowledge.get().hasScanned(ScanManager.playerKey(player))) {
                return;
            }
            target = ScanSlotPacket.SELF;
        } else if (canScan(hoveredSlot, player)) {
            if (ClientKnowledge.get().hasScanned(ScanManager.itemKey(hoveredSlot.getItem()))) {
                lastScannedSlot = hoveredSlot;
                return;
            }
            target = hoveredSlot.index;
        } else {
            return;
        }

        ticksHovered++;
        if (ticksHovered > 5 && ticksHovered % 2 == 0) {
            player.playSound(ModSounds.CAMERA_TICKS.get(), 0.2f, 0.45f + player.getRandom().nextFloat() * 0.1f);
        }
        if (ticksHovered >= SCAN_TICKS) {
            ModNetwork.CHANNEL.sendToServer(new ScanSlotPacket(target));
            ticksHovered = 0;
            // один и тот же слот не сканируется повторно, пока курсор с него не уйдёт
            if (target == ScanSlotPacket.SELF) {
                selfDone = true;
            } else {
                lastScannedSlot = hoveredSlot;
            }
        }
    }

    /**
     * Творческий инвентарь не подходит: его слоты и предмет на курсоре существуют только на клиенте,
     * сервер их не видит и проверить сканирование не может.
     */
    private static boolean supports(@Nullable Screen screen) {
        return screen instanceof AbstractContainerScreen<?> && !(screen instanceof CreativeModeInventoryScreen);
    }

    private static boolean canScan(@Nullable Slot slot, LocalPlayer player) {
        return slot != null && slot != lastScannedSlot && slot.hasItem() && slot.mayPickup(player)
                && !(slot instanceof ResultSlot);
    }

    private static boolean carryingThaumometer(LocalPlayer player) {
        return player.containerMenu.getCarried().getItem() instanceof ThaumometerItem;
    }

    @SubscribeEvent
    public static void renderScreen(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        // интерфейс закрывает обычный HUD, поэтому уведомления дублируются поверх него
        HudOverlay.renderNotifications(mc.font, graphics, event.getScreen().width, event.getScreen().height);

        if (!supports(event.getScreen()) || mc.player == null) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        boolean wasHoveringSelf = hoveringSelf;
        // фигурка игрока в окне инвентаря
        hoveringSelf = screen instanceof InventoryScreen
                && mouseX >= screen.getGuiLeft() + 26 && mouseX < screen.getGuiLeft() + 78
                && mouseY >= screen.getGuiTop() + 8 && mouseY < screen.getGuiTop() + 78;
        Slot slot = hoveringSelf ? null : screen.getSlotUnderMouse();
        if (slot != hoveredSlot || hoveringSelf != wasHoveringSelf) {
            ticksHovered = 0;
            selfDone = false;
        }
        hoveredSlot = slot;

        if (!carryingThaumometer(mc.player)) {
            return;
        }
        if (ticksHovered > 0) {
            float progress = ticksHovered / (float) SCAN_TICKS;
            String dots = progress >= 0.75f ? "..." : progress >= 0.5f ? ".." : progress >= 0.25f ? "." : "";
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 400f);
            graphics.drawString(mc.font, Component.translatable("tc.invscan.scanning").append(dots)
                    .withStyle(ChatFormatting.GOLD), mouseX, mouseY - 30, 0xffffff, true);
            graphics.pose().popPose();
        }
        // с предметом на курсоре игра подсказку не показывает; рисуем её сами, чтобы были видны аспекты
        if (slot != null && slot.hasItem()) {
            graphics.renderTooltip(mc.font, slot.getItem(), mouseX, mouseY);
        }
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ThaumometerItem)) {
            return;
        }
        event.getToolTip().add(Component.translatable("tc.invscan.tooltip").withStyle(ChatFormatting.GOLD));
        if (Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tc.invscan.tooltip.more1").withStyle(ChatFormatting.DARK_AQUA));
            event.getToolTip().add(Component.translatable("tc.invscan.tooltip.more2").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private InventoryScanHandler() {
    }
}
