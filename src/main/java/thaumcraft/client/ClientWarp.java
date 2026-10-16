package thaumcraft.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModSounds;

/**
 * То, как безумие выглядит для самого игрока (ClientTickEventsFML и RenderEventHandler оригинала):
 * пульсирующая тьма по краям экрана и стук сердца в начале приступа, густой туман, а также искажения зрения
 * от эффектов — обесцвеченный мир при смертельном взгляде, размытый при помутнении, красноватый при голоде.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ClientWarp {
    private static final ResourceLocation VIGNETTE = Thaumcraft.id("textures/misc/vignette.png");
    private static final ResourceLocation DESATURATE = Thaumcraft.id("shaders/post/desaturate.json");
    private static final ResourceLocation BLUR = Thaumcraft.id("shaders/post/blur.json");
    private static final ResourceLocation HUNGER = Thaumcraft.id("shaders/post/hunger.json");

    /** Сколько тиков ещё держится тьма по краям. */
    private static int warpVignette;
    private static float vignetteBrightness;
    private static int fogDuration;
    private static float fogTarget;
    /** Шейдер, который включили мы сами (чтобы не трогать чужие). */
    @Nullable
    private static ResourceLocation activeShader;

    /** Начало приступа: тьма сгущается на пять секунд, слышен стук сердца. */
    public static void startVignette() {
        warpVignette = 100;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(ModSounds.HEARTBEAT.get(), 1f, 1f);
        }
    }

    public static void startMist(int duration) {
        fogDuration = Math.max(fogDuration, duration);
    }

    /** Снимает туман и тьму сразу (для проверок). */
    public static void reset() {
        fogDuration = 0;
        fogTarget = 0f;
        warpVignette = 0;
        vignetteBrightness = 0f;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.isPaused()) {
            return;
        }
        // смертельный взгляд держит тьму по краям, пока действует
        ResourceLocation wanted = null;
        if (mc.player.hasEffect(ModEffects.DEATH_GAZE.get())) {
            warpVignette = Math.max(warpVignette, 10);
            wanted = DESATURATE;
        } else if (mc.player.hasEffect(ModEffects.BLURRED_VISION.get())) {
            wanted = BLUR;
        } else if (mc.player.hasEffect(ModEffects.UNNATURAL_HUNGER.get())) {
            wanted = HUNGER;
        }
        if (wanted != activeShader) {
            if (wanted != null) {
                mc.gameRenderer.loadEffect(wanted);
            } else if (activeShader != null) {
                mc.gameRenderer.shutdownEffect();
            }
            activeShader = wanted;
        }
        if (warpVignette > 0) {
            warpVignette--;
        }
        if (fogDuration > 0) {
            // туман наползает постепенно и так же постепенно рассеивается в последние пять секунд
            if (fogDuration < 100) {
                fogTarget = 0.1f * (fogDuration / 100f);
            } else if (fogTarget < 0.1f) {
                fogTarget += 0.001f;
            }
            fogDuration--;
        } else {
            fogTarget = 0f;
        }
    }

    /** Оригинал включал экспоненциальный туман плотностью до 0.1: дальше двух десятков блоков ничего не видно. */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (fogTarget > 0f) {
            float strength = fogTarget / 0.1f;
            float far = Mth.lerp(strength, event.getFarPlaneDistance(), 20f);
            event.setNearPlaneDistance(Mth.lerp(strength, event.getNearPlaneDistance(), 0f));
            event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
            event.setCanceled(true);
        }
    }

    /** renderVignette оригинала: тьма по краям плавно сгущается и слегка пульсирует. */
    public static void renderVignette(GuiGraphics graphics, int width, int height) {
        float target = warpVignette > 0 ? 1f : 0f;
        vignetteBrightness += (target - vignetteBrightness) * 0.01f;
        Minecraft mc = Minecraft.getInstance();
        if (vignetteBrightness <= 0.001f || mc.player == null) {
            return;
        }
        float b = Mth.clamp(vignetteBrightness * (1f + Mth.sin(mc.player.tickCount / 2f) * 0.1f), 0f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        graphics.setColor(b, b, b, 1f);
        graphics.blit(VIGNETTE, 0, 0, -90, 0f, 0f, width, height, width, height);
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
    }

    private ClientWarp() {
    }
}
