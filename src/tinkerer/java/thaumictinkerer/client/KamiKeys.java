package thaumictinkerer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.kami.IchorArmor;
import thaumictinkerer.common.network.TTNetwork;

/** Клавиша U (KamiArmorClientHandler): включает и выключает силы всей пробуждённой брони разом. */
public final class KamiKeys {
    public static final KeyMapping ARMOR = new KeyMapping("key.thaumictinkerer.armor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U,
            "key.categories.thaumictinkerer");

    private KamiKeys() {
    }

    @Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(ARMOR);
        }
    }

    @Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT)
    public static final class Ticks {
        private Ticks() {
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            while (event.phase == TickEvent.Phase.END && ARMOR.consumeClick()) {
                if (minecraft.player == null || minecraft.screen != null) {
                    continue;
                }
                IchorArmor.clientEnabled = !IchorArmor.clientEnabled;
                minecraft.player.displayClientMessage(Component.translatable(IchorArmor.clientEnabled ? "ttmisc.enableAllArmor"
                        : "ttmisc.disableAllArmor"), true);
                TTNetwork.sendArmorStatusToServer(IchorArmor.clientEnabled);
            }
        }
    }
}
