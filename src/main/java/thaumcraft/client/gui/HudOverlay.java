package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.WandItem;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import thaumcraft.client.ClientHudState;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.network.ScanTargetPacket;

import java.util.List;

/** Экранные элементы мода: полоса сканирования у прицела и уведомления справа. */
public class HudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        renderThaumometer(mc, graphics, width, height);
        renderWandDial(mc, graphics);
        renderHoverHarness(mc, graphics, height);
        if (mc.player.getMainHandItem().is(thaumcraft.common.registry.ModItems.SANITY_CHECKER.get())) {
            renderSanity(mc, graphics);
        }
        // при открытом интерфейсе уведомления рисует InventoryScanHandler, поверх него
        if (mc.screen == null) {
            renderNotifications(mc.font, graphics, width, height);
        }
    }

    private static final ResourceLocation HUD = Thaumcraft.id("textures/gui/hud.png");
    private static final ResourceLocation PARTICLE_SHEET = Thaumcraft.id("textures/misc/particles.png");

    /**
     * Шкала рассудка (renderSanityHud оригинала) в левом верхнем углу: сверху вниз — временное искажение (светлое),
     * липкое и постоянное (тёмное). Шкала рассчитана на 100 единиц; если больше — всё сжимается, а сверху загорается знак.
     */
    private static void renderSanity(Minecraft mc, GuiGraphics graphics) {
        var knowledge = thaumcraft.client.ClientKnowledge.get();
        float total = knowledge.getWarpTotal();
        int perm = knowledge.getWarpPerm();
        int sticky = knowledge.getWarpSticky();
        int temp = knowledge.getWarpTemp();
        float mod = 1f;
        if (total > 100f) {
            mod = 100f / total;
            total = 100f;
        }
        int gap = (int) ((100f - total) / 100f * 48f);
        int wt = (int) (temp / 100f * 48f * mod);
        int ws = (int) (sticky / 100f * 48f * mod);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.blit(HUD, 1, 1, 152, 0, 20, 76);
        if (temp > 0) {
            graphics.setColor(1f, 0.5f, 1f, 1f);
            graphics.blit(HUD, 7, 21 + gap, 200, gap, 8, wt + gap);
        }
        if (sticky > 0) {
            graphics.setColor(0.75f, 0f, 0.75f, 1f);
            graphics.blit(HUD, 7, 21 + wt + gap, 200, wt + gap, 8, wt + ws + gap);
        }
        if (perm > 0) {
            graphics.setColor(0.5f, 0f, 0.5f, 1f);
            graphics.blit(HUD, 7, 21 + wt + ws + gap, 200, wt + ws + gap, 8, 48);
        }
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.blit(HUD, 1, 1, 176, 0, 20, 76);
        if (total >= 100f) {
            graphics.blit(HUD, 1, 1, 216, 0, 20, 16);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /**
     * Запас Potentia летательной упряжи у левого края экрана (renderHoverHUD оригинала): шкала в рамке, над ней
     * значок упряжи, а в парении вокруг него вьётся искра.
     */
    private static void renderHoverHarness(Minecraft mc, GuiGraphics graphics, int height) {
        ItemStack armor = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(armor.getItem() instanceof thaumcraft.common.items.armor.HoverHarnessItem) || mc.screen != null) {
            return;
        }
        int fuel = thaumcraft.common.items.armor.HoverHarnessItem.getFuel(armor);
        int level = Math.round(fuel / 64f * 48f);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.setColor(0f, 1f, 0.75f, 1f);
        graphics.blit(PARTICLE_SHEET, 6, height / 2 + 24 - level, 224, 48 - level, 8, level, 256, 256);
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.blit(PARTICLE_SHEET, 5, height / 2 - 28, 240, 0, 10, 56, 256, 256);
        if (thaumcraft.common.items.armor.HoverHarnessItem.isHovering(mc.player)) {
            graphics.setColor(1f, 1f, 1f, 0.66f);
            graphics.blit(PARTICLE_SHEET, 2, height / 2 - 43, 16 * (int) (System.currentTimeMillis() % 700L / 50L), 32, 16, 16, 256, 256);
            graphics.setColor(1f, 1f, 1f, 1f);
        }
        graphics.renderItem(armor, 2, height / 2 - 43);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private final Map<Aspect, Integer> previousVis = new HashMap<>();
    private long nextSnapshot;

    /**
     * Шкала вис жезла в левом верхнем углу — перенос renderCastingWandHud оригинала: шесть делений веером,
     * стрелка вверх или вниз у аспекта, запас которого меняется; при приседании видны числа.
     */
    private void renderWandDial(Minecraft mc, GuiGraphics graphics) {
        LocalPlayer player = mc.player;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WandItem) || mc.screen != null) {
            previousVis.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (previousVis.isEmpty() || nextSnapshot <= now) {
            for (Aspect aspect : Aspect.primals()) {
                previousVis.put(aspect, WandItem.getVis(stack, aspect));
            }
            nextSnapshot = now + 1000L;
        }

        PoseStack pose = graphics.pose();
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 0.5f);
        graphics.blit(HUD, 0, 0, 0, 0, 64, 64);
        pose.popPose();
        pose.translate(16f, 16f, 0f);

        int max = WandItem.getMaxVis(stack);
        int index = 0;
        for (Aspect aspect : Aspect.primals()) {
            int amount = WandItem.getVis(stack, aspect);
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(90f));
            pose.mulPose(Axis.ZP.rotationDegrees(-15 + index * 24));
            pose.translate(0f, -32f, 0f);
            pose.scale(0.5f, 0.5f, 0.5f);

            int fill = (int) (30f * amount / max);
            int color = aspect.getColor();
            graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 0.8f);
            graphics.blit(HUD, -4, 35 - fill, 104, 0, 8, fill);
            graphics.setColor(1f, 1f, 1f, 1f);
            graphics.blit(HUD, -8, -3, 72, 0, 16, 42);

            int previous = previousVis.getOrDefault(aspect, amount);
            if (previous > amount) {
                graphics.blit(HUD, -4, -8, 128, 0, 8, 8);
            } else if (previous < amount) {
                graphics.blit(HUD, -4, -8, 120, 0, 8, 8);
            }
            if (player.isShiftKeyDown()) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(-90f));
                graphics.drawString(mc.font, Integer.toString(amount / 100), -32, -4, 0xffffff, true);
                pose.popPose();
            }
            pose.popPose();
            index++;
        }
        pose.popPose();
    }

    private void renderThaumometer(Minecraft mc, GuiGraphics graphics, int width, int height) {
        LocalPlayer player = mc.player;
        ScanTargetPacket target = ClientHudState.getScanTarget();
        boolean holding = player.getMainHandItem().getItem() instanceof ThaumometerItem
                || player.getOffhandItem().getItem() instanceof ThaumometerItem;
        if (!holding || target == null || mc.screen != null) {
            return;
        }
        int centerX = width / 2;
        int y = height / 2 + 12;

        // название и аспекты цели рисуются в линзе самого прибора (ThaumometerHandRenderer); здесь только ход сканирования
        if (!target.scanned() && player.isUsingItem() && player.getUseItem().getItem() instanceof ThaumometerItem) {
            // сканирование завершается, когда остаётся 5 тиков из полного времени
            float total = ThaumometerItem.SCAN_TICKS - 5f;
            float progress = Mth.clamp((ThaumometerItem.SCAN_TICKS - player.getUseItemRemainingTicks()) / total, 0f, 1f);
            int barWidth = 40;
            int left = centerX - barWidth / 2;
            graphics.fill(left - 1, y - 1, left + barWidth + 1, y + 3, 0xa0000000);
            graphics.fill(left, y, left + (int) (barWidth * progress), y + 2, 0xffc080ff);
        }
    }

    public static void renderNotifications(Font font, GuiGraphics graphics, int width, int height) {
        List<ClientHudState.Notification> notifications = ClientHudState.notifications();
        long now = System.currentTimeMillis();
        int y = height / 3;
        for (ClientHudState.Notification notification : notifications) {
            long age = now - notification.created();
            // плавное появление и исчезновение
            float alpha = Mth.clamp(Math.min(age / 250f, (ClientHudState.NOTIFICATION_MILLIS - age) / 1000f), 0f, 1f);
            if (alpha > 0.05f) {
                int textWidth = font.width(notification.text());
                int right = width - 4;
                int left = right - textWidth - (notification.aspect() != null ? AspectRenderer.STEP : 0);
                graphics.fill(left - 3, y - 2, right + 2, y + AspectRenderer.ICON + 2, (int) (alpha * 110f) << 24);
                if (notification.aspect() != null) {
                    AspectRenderer.draw(graphics, font, notification.aspect(), 0, left, y, alpha);
                }
                int color = 0xffffff | Math.max(4, (int) (alpha * 255f)) << 24;
                graphics.drawString(font, notification.text(), right - textWidth, y + 4, color, true);
            }
            y += AspectRenderer.ICON + 5;
        }
    }
}
