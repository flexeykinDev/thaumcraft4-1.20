package thaumictinkerer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.kami.SoulHearts;

/** SoulHeartClientHandler: сердца души — металлические сердечки над полоской сытости, по полсердца на единицу. */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulHeartsOverlay {
    private static final ResourceLocation HEARTS = ThaumicTinkerer.id("textures/gui/soul_hearts.png");

    private SoulHeartsOverlay() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), "soul_hearts", (gui, graphics, partialTick, width, height) -> {
            int hearts = SoulHearts.clientHearts;
            if (hearts <= 0 || Minecraft.getInstance().options.hideGui || !gui.shouldDrawSurvivalElements()) {
                return;
            }
            int x = width / 2 + 10;
            int y = height - gui.rightHeight;
            int slot = 0;
            for (int i = 0; i < hearts; i++) {
                boolean half = i == hearts - 1 && hearts % 2 != 0;
                if (half || i % 2 == 0) {
                    graphics.blit(HEARTS, x + slot * 8, y, half ? 9 : 0, 0, half ? 7 : 9, 9, 16, 16);
                    slot++;
                }
            }
            gui.rightHeight += 10;
        });
    }
}
