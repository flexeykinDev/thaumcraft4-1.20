package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.runic.RunicShield;

/**
 * Рунный щит на экране (ClientTickEventsFML.renderRunicArmorBar оригинала): поверх полоски здоровья — до десяти
 * мерцающих золотых рун, по одной на каждую десятую долю заряда. Здесь же строка «Рунный щит +N» в подсказках.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class RunicOverlay implements IGuiOverlay {
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");
    /** Заряд и предел щита этого игрока: их присылает сервер. */
    public static int charge;
    public static int max;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.getAbilities().instabuild || mc.player.isSpectator()
                || charge <= 0 || max <= 0) {
            return;
        }
        int left = width / 2 - 91;
        int top = height - 39;
        float fill = (float) charge / max;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int a = 0; a < fill * 10f; a++) {
            graphics.setColor(1f, 1f, 1f, 1f);
            graphics.blit(PARTICLES, left + a * 8, top, 160, 16, 9, 9, 256, 256);
            graphics.pose().pushPose();
            graphics.pose().translate(left, top, 0);
            graphics.pose().scale(0.5f, 0.5f, 1f);
            graphics.setColor(1f, 0.75f, 0.24f, Mth.sin(mc.player.tickCount / 4f + a) * 0.4f + 0.6f);
            graphics.blit(PARTICLES, a * 16, 0, a * 16, 96, 16, 16, 256, 256);
            graphics.pose().popPose();
        }
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        int amount = RunicShield.getFinalCharge(event.getItemStack());
        if (amount > 0) {
            event.getToolTip().add(Component.translatable("item.runic.charge").append(" +" + amount).withStyle(ChatFormatting.GOLD));
        }
        if (event.getEntity() != null && event.getItemStack().getItem() instanceof thaumcraft.api.WarpingGear gear) {
            int warp = gear.getWarp(event.getItemStack(), event.getEntity());
            if (warp > 0) {
                event.getToolTip().add(Component.translatable("item.warping").append(" " + warp).withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }
}
