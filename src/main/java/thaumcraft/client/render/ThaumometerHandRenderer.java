package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientHudState;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.network.ScanTargetPacket;

/**
 * Таумометр от первого лица, как в оригинале (ItemThaumometerRenderer): держится двумя руками, как карта,
 * а в линзе показано название цели и, если она изучена, её аспекты.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHandRenderer {
    /** Размер прибора в руках: по скриншоту оригинала рамка занимает около 70% высоты экрана. */
    private static final float SCALE = 0.86f;
    /** Модель при импорте ужата в 0.35 раза; этот множитель возвращает единицы оригинальной модели. */
    private static final float MODEL_UNIT = 0.35f;
    /**
     * В оригинале содержимое линзы рисуется внутри матрицы её квадрата, растянутого в 2.5 раза
     * (renderQuadCenteredFromTexture не сбрасывает масштаб), поэтому все размеры текста умножаются на это.
     */
    private static final float LENS_SCALE = 2.5f;
    /** Сдвиг рук по вертикали относительно того, как они держат карту. */
    private static final float ARMS_LIFT = 0f;
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof ThaumometerItem)) {
            return;
        }
        // обе руки заняты прибором, поэтому вторая рука отдельно не рисуется
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        pose.pushPose();
        // положение и взмах — как у карты в двух руках (ItemInHandRenderer.renderTwoHandedMap)
        float swing = event.getSwingProgress();
        float swingRoot = Mth.sqrt(swing);
        pose.translate(0f, 0.1f * Mth.sin(swing * Mth.PI), -0.4f * Mth.sin(swingRoot * Mth.PI));
        // по центру экрана: прицел смотрит сквозь середину линзы, как в оригинале
        pose.translate(0f, event.getEquipProgress() * -1.2f, -0.72f);

        if (!player.isInvisible()) {
            pose.pushPose();
            pose.translate(0f, ARMS_LIFT, 0f);
            pose.mulPose(Axis.YP.rotationDegrees(90f));
            renderArm(mc, player, pose, buffer, light, HumanoidArm.RIGHT);
            renderArm(mc, player, pose, buffer, light, HumanoidArm.LEFT);
            pose.popPose();
        }
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(swingRoot * Mth.PI) * 20f));
        pose.scale(SCALE, SCALE, SCALE);

        // Модель лежит плашмя (верх по Y); разворачиваем лицом к игроку: X модели — вверх, Z — вправо
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(90f));
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        ItemStack stack = event.getItemStack();
        BakedModel model = mc.getItemRenderer().getModel(stack, player.level(), player, 0);
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffer, light,
                OverlayTexture.NO_OVERLAY, model);
        pose.popPose();

        ScanTargetPacket target = ClientHudState.getScanTarget();
        if (target != null) {
            pose.pushPose();
            // чуть перед линзой; дальше координаты как в оригинале: единицы модели, Y вниз
            pose.translate(0f, 0f, 0.02f);
            float unit = MODEL_UNIT * LENS_SCALE;
            pose.scale(unit, -unit, unit);
            renderReadout(mc.font, pose, buffer, target);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderArm(Minecraft mc, AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffer,
                                  int light, HumanoidArm arm) {
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(player);
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(92f));
        pose.mulPose(Axis.XP.rotationDegrees(45f));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -41f));
        pose.translate(side * 0.3f, -1.1f, 0.45f);
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(pose, buffer, light, player);
        } else {
            renderer.renderLeftHand(pose, buffer, light, player);
        }
        pose.popPose();
    }

    private static void renderReadout(Font font, PoseStack pose, MultiBufferSource buffer, ScanTargetPacket target) {
        // название цели над аспектами; длинные названия ужимаются, чтобы влезть в линзу
        Component name = target.name();
        int nameWidth = font.width(name);
        float nameScale = 0.005f;
        if (nameWidth > 90) {
            nameScale -= 2.5e-5f * (nameWidth - 90);
        }
        pose.pushPose();
        pose.translate(0f, -0.25f, 0f);
        pose.scale(nameScale, nameScale, nameScale);
        font.drawInBatch(name, -nameWidth / 2f, 0f, 0xffffffff, false, pose.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
        pose.popPose();

        if (!target.scanned()) {
            pose.pushPose();
            pose.scale(0.02f, 0.02f, 0.02f);
            font.drawInBatch("?", -font.width("?") / 2f, -2f, 0xffffffff, false, pose.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            pose.popPose();
            return;
        }

        // иконки рядами по 5, 4, 3..., каждый ряд по центру — раскладка оригинала
        AspectList aspects = target.aspects();
        pose.pushPose();
        pose.scale(0.0075f, 0.0075f, 0.0075f);
        int column = 0;
        int row = 0;
        int left = aspects.size();
        int baseX = Math.min(5, left) * 8;
        for (Aspect aspect : aspects.getAspectsSortedByName()) {
            drawAspect(font, pose, buffer, aspect, aspects.getAmount(aspect), -baseX + column * 16, -8 + row * 16);
            column++;
            if (column >= 5 - row) {
                column = 0;
                row++;
                left -= 5 - row;
                baseX = Math.min(5 - row, left) * 8;
            }
        }
        pose.popPose();
    }

    private static void drawAspect(Font font, PoseStack pose, MultiBufferSource buffer, Aspect aspect, int amount,
                                   float x, float y) {
        int color = aspect.getColor();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.text(aspect.getImage()));
        consumer.vertex(matrix, x, y, 0f).color(r, g, b, 255).uv(0f, 0f).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x, y + 16f, 0f).color(r, g, b, 255).uv(0f, 1f).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x + 16f, y + 16f, 0f).color(r, g, b, 255).uv(1f, 1f).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x + 16f, y, 0f).color(r, g, b, 255).uv(1f, 0f).uv2(FULL_BRIGHT).endVertex();

        String text = Integer.toString(amount);
        pose.pushPose();
        pose.translate(x + 16f, y + 16f, 0.5f);
        pose.scale(0.5f, 0.5f, 0.5f);
        font.drawInBatch(text, -font.width(text), -font.lineHeight, 0xffffffff, false, pose.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
        pose.popPose();
    }

    private ThaumometerHandRenderer() {
    }
}
