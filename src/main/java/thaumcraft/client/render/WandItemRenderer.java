package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Трёхмерная модель жезла — перенос ModelWand оригинала: стержень 2x18x2 и два набалдашника 2x2x2,
 * каждый со своей текстурой в зависимости от частей жезла.
 */
public class WandItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final WandItemRenderer INSTANCE = new WandItemRenderer();

    // Развёртка рассчитана на лист 32x32. В оригинале после создания коробок вызывался setTextureSize(64, 32),
    // но на уже созданные коробки он не влиял — если взять 64, стержень попадает мимо своей текстуры.
    private static final ResourceLocation SCRIPT = Thaumcraft.id("textures/misc/script.png");

    private static final ResourceLocation FOCUS = Thaumcraft.id("textures/models/wand.png");
    private final ModelPart focus = new ModelPart(List.of(new ModelPart.Cube(0, 0, -3f, -6f, -3f, 6f, 6f, 6f, 0f, 0f, 0f, true,
            32f, 32f, EnumSet.allOf(Direction.class))), Map.of());
    private final ModelPart cap = part(0, 0, 2, 2, 2);
    private final ModelPart rod = part(0, 8, 2, 18, 2);

    private WandItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ModelPart part(int u, int v, float width, float height, float depth) {
        ModelPart.Cube cube = new ModelPart.Cube(u, v, -1f, -1f, -1f, width, height, depth, 0f, 0f, 0f, true,
                32f, 32f, EnumSet.allOf(Direction.class));
        return new ModelPart(List.of(cube), Map.of());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer,
                             int light, int overlay) {
        pose.pushPose();
        // Модель стоит вертикально по центру; поворот на 45 градусов кладёт её по диагонали, как обычный
        // предмет-инструмент, поэтому положения в руке берутся стандартные (см. models/item/wand.json).
        pose.translate(0.5f, 0.5f, 0.5f);
        // В интерфейсе жезл оригинала лежит от левого верхнего угла к правому нижнему — под него же нарисована
        // канавка в ячейке жезла магического верстака. В руках наклон обратный, как у обычных инструментов.
        pose.mulPose(Axis.ZP.rotationDegrees(context == ItemDisplayContext.GUI ? 45f : -45f));
        // у модели ось Y направлена вниз, верхний набалдашник в нуле, длина 22 пикселя
        if (context == ItemDisplayContext.GUI && WandItem.isStaff(stack)) {
            // посох вдвое длиннее жезла: в ячейке интерфейса он уменьшен и поставлен серединой в её центр,
            // чтобы не закрывать соседние предметы
            pose.scale(0.54f, 0.54f, 0.54f);
            pose.translate(0f, 1.4f, 0f);
        } else {
            pose.translate(0f, 10f / 16f, 0f);
        }
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        net.minecraft.world.entity.LivingEntity caster = applyCastingTilt(stack, context, pose);

        // ModelWand оригинала: посох вдвое длиннее и толще, у скипетра сверху двойной набалдашник
        boolean staff = WandItem.isStaff(stack);
        boolean sceptre = WandItem.isSceptre(stack);
        if (staff) {
            pose.translate(0f, 0.2f, 0f);
        }
        if (caster != null) {
            rememberTip(caster, context, pose);
        }
        var rodConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(WandItem.getRod(stack).getTexture()));
        pose.pushPose();
        if (staff) {
            pose.translate(0f, -0.1f, 0f);
            pose.scale(1.2f, 2f, 1.2f);
        }
        pose.translate(0f, 2f / 16f, 0f);
        // пламенный стержень светится сам
        rod.render(pose, rodConsumer, WandItem.getRod(stack).isGlowing() ? 0xf000f0 : light, overlay);
        pose.popPose();

        var capConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(WandItem.getCap(stack).getTexture()));
        pose.pushPose();
        if (staff) {
            pose.scale(1.3f, 1.1f, 1.3f);
        } else {
            pose.scale(1.2f, 1f, 1.2f);
        }
        if (sceptre) {
            pose.pushPose();
            pose.scale(1.3f, 1.3f, 1.3f);
            cap.render(pose, capConsumer, light, overlay);
            pose.popPose();
            pose.pushPose();
            pose.translate(0f, 0.3f, 0f);
            pose.scale(1f, 0.66f, 1f);
            cap.render(pose, capConsumer, light, overlay);
            pose.popPose();
        } else {
            cap.render(pose, capConsumer, light, overlay);
        }
        if (staff) {
            // кольцо под верхним набалдашником и нижний набалдашник на удлинённом древке
            pose.translate(0f, 0.225f, 0f);
            pose.pushPose();
            pose.scale(1f, 0.66f, 1f);
            cap.render(pose, capConsumer, light, overlay);
            pose.popPose();
            pose.translate(0f, 0.65f, 0f);
        }
        pose.translate(0f, 20f / 16f, 0f);
        cap.render(pose, capConsumer, light, overlay);
        pose.popPose();
        // кристалл фокуса на вершине: полупрозрачный, светится своим цветом
        ItemStack focusStack = WandItem.getFocusStack(stack);
        if (focusStack.getItem() instanceof FocusItem focusItem) {
            ResourceLocation ornament = focusItem.getOrnament(focusStack);
            if (ornament != null) {
                // украшение: значок в двух скрещённых плоскостях вокруг кристалла
                VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ornament));
                for (int turn = 0; turn < 2; turn++) {
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotationDegrees(90f * turn));
                    Matrix4f matrix = pose.last().pose();
                    Matrix3f normal = pose.last().normal();
                    float[][] corners = {{-0.25f, 0.1f, 0f, 1f}, {0.25f, 0.1f, 1f, 1f}, {0.25f, -0.4f, 1f, 0f}, {-0.25f, -0.4f, 0f, 0f}};
                    for (float[] c : corners) {
                        consumer.vertex(matrix, c[0], c[1], 0f).color(1f, 1f, 1f, 1f).uv(c[2], c[3])
                                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0f, 0f, 1f).endVertex();
                    }
                    pose.popPose();
                }
            }
            float alpha = 0.95f;
            ResourceLocation depth = focusItem.getDepthTexture(focusStack);
            if (depth != null) {
                // сердцевина: маленький куб с бегущей текстурой внутри кристалла
                int frames = Math.max(1, focusItem.getDepthFrames(focusStack));
                Minecraft client = Minecraft.getInstance();
                int frame = client.player == null ? 0 : client.player.tickCount % frames;
                pose.pushPose();
                if (staff) {
                    pose.translate(0f, -0.15f, 0f);
                    pose.scale(0.165f, 0.1765f, 0.165f);
                } else {
                    pose.translate(0f, -0.09f, 0f);
                    pose.scale(0.16f, 0.16f, 0.16f);
                }
                drawCube(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(depth)), (float) frame / frames, (frame + 1f) / frames);
                pose.popPose();
                alpha = 0.6f;
            }
            pose.pushPose();
            if (staff) {
                pose.translate(0f, -0.0475f, 0f);
                pose.scale(0.525f, 0.5525f, 0.525f);
            } else {
                pose.scale(0.5f, 0.5f, 0.5f);
            }
            int color = focusItem.getFocusColor(focusStack);
            focus.render(pose, buffer.getBuffer(RenderType.entityTranslucent(FOCUS)), 0xf000f0, overlay,
                    (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha);
            pose.popPose();
        }

        Minecraft mc = Minecraft.getInstance();
        int ticks = mc.player == null ? 0 : mc.player.tickCount;
        if (sceptre) {
            // десять рун кружат вокруг навершия скипетра
            for (int rot = 0; rot < 10; rot++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(36 * rot + ticks));
                drawRune(pose, buffer, 0.16f, -0.01f, -0.125f, rot, ticks);
                pose.popPose();
            }
        }
        if (WandItem.getRod(stack).hasRunes()) {
            // по четырём граням первозданного посоха — по четырнадцать рун
            pose.pushPose();
            for (int rot = 0; rot < 4; rot++) {
                pose.mulPose(Axis.YP.rotationDegrees(90f));
                for (int a = 0; a < 14; a++) {
                    drawRune(pose, buffer, 0.36f + a * 0.14f, -0.01f, -0.08f, (a + rot * 3) % 16, ticks);
                }
            }
            pose.popPose();
        }

        pose.popPose();
    }

    /** Кого сейчас рисует игра от третьего лица (см. ClientEvents): по нему жезл узнаёт, что его используют. */
    public static net.minecraft.world.entity.LivingEntity renderedEntity;

    /**
     * Наклон жезла во время использования — как в ItemWandRenderer оригинала: за три тика жезл наклоняется
     * вперёд на 60 градусов вокруг точки хвата, а потом покачивается (у узла и «плавных» фокусов) либо мелко дрожит.
     */
    /** Где в мире была вершина жезла в последнем кадре: от неё начинаются лучи заклинаний. */
    private record Tip(net.minecraft.world.phys.Vec3 position, long nanos) {
    }

    private static final java.util.Map<Integer, Tip> TIPS = new java.util.HashMap<>();

    /** Вершина жезла, которым прямо сейчас колдует это существо, либо null, если жезл не рисовался. */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.phys.Vec3 tipOf(int entityId) {
        Tip tip = TIPS.get(entityId);
        return tip != null && System.nanoTime() - tip.nanos() < 150_000_000L ? tip.position() : null;
    }

    /**
     * Переводит вершину модели из пространства кадра в мир. Матрица отрисовки уже повёрнута вместе с камерой,
     * поэтому поворот камеры снимается; рука от первого лица к тому же рисуется со своим углом обзора (70 градусов),
     * и точка поправляется так, чтобы на экране она осталась на вершине жезла.
     */
    private static void rememberTip(net.minecraft.world.entity.LivingEntity caster, ItemDisplayContext context, PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        org.joml.Vector4f point = new org.joml.Vector4f(0f, -0.12f, 0f, 1f).mul(pose.last().pose());
        org.joml.Vector3f view = new org.joml.Vector3f(point.x(), point.y(), point.z());
        if (context.firstPerson()) {
            double fov = mc.options.fov().get() * (mc.player == null ? 1f : mc.player.getFieldOfViewModifier());
            float fix = (float) (Math.tan(Math.toRadians(fov) / 2) / Math.tan(Math.toRadians(70) / 2));
            view.mul(fix, fix, 1f);
        }
        org.joml.Quaternionf rotation = Axis.XP.rotationDegrees(camera.getXRot()).mul(Axis.YP.rotationDegrees(camera.getYRot() + 180f));
        rotation.conjugate().transform(view);
        TIPS.put(caster.getId(), new Tip(camera.getPosition().add(view.x(), view.y(), view.z()), System.nanoTime()));
    }

    @org.jetbrains.annotations.Nullable
    private static net.minecraft.world.entity.LivingEntity applyCastingTilt(ItemStack stack, ItemDisplayContext context, PoseStack pose) {
        net.minecraft.world.entity.LivingEntity wielder;
        if (context.firstPerson()) {
            wielder = Minecraft.getInstance().player;
        } else if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            wielder = renderedEntity;
        } else {
            return null;
        }
        if (wielder == null || !wielder.isUsingItem() || !(wielder.getUseItem().getItem() instanceof WandItem)) {
            return null;
        }
        // жезл в другой руке не наклоняется
        boolean rightContext = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean usedRight = (wielder.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND)
                == (wielder.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        if (rightContext != usedRight) {
            return null;
        }
        float used = wielder.getTicksUsingItem() + Minecraft.getInstance().getFrameTime();
        float t = Math.min(used, 3f);
        // модель лежит в плоскости предмета, поэтому «вперёд» — это поворот вокруг оси Z; у левой руки он зеркальный
        float forward = TILT_SIGN * (rightContext ? 1f : -1f);
        pose.translate(0f, 1f, 0f);
        pose.mulPose(Axis.ZP.rotationDegrees(forward * (context.firstPerson() ? 45f : 60f) * (t / 3f)));
        ItemStack focus = WandItem.getFocusStack(stack);
        boolean charge = stack.hasTag() && !stack.getTag().contains("IIUX") && focus.getItem() instanceof FocusItem item
                && item.getAnimation(focus) == FocusItem.Animation.CHARGE;
        if (charge) {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(used / 0.8f)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(used / 0.7f)));
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(used / 10f) * 10f));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(used / 15f) * 10f));
        }
        pose.translate(0f, -1f, 0f);
        return wielder;
    }

    private static final float TILT_SIGN = -1f;

    /** Единичный куб, на каждой грани — один кадр ленты (v0..v1). Светится сам. */
    private static void drawCube(PoseStack pose, VertexConsumer consumer, float v0, float v1) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float h = 0.5f;
        float[][][] faces = {
                {{-h, -h, h}, {h, -h, h}, {h, h, h}, {-h, h, h}}, {{h, -h, -h}, {-h, -h, -h}, {-h, h, -h}, {h, h, -h}},
                {{h, -h, h}, {h, -h, -h}, {h, h, -h}, {h, h, h}}, {{-h, -h, -h}, {-h, -h, h}, {-h, h, h}, {-h, h, -h}},
                {{-h, h, h}, {h, h, h}, {h, h, -h}, {-h, h, -h}}, {{-h, -h, -h}, {h, -h, -h}, {h, -h, h}, {-h, -h, h}}};
        float[][] uv = {{0f, v1}, {1f, v1}, {1f, v0}, {0f, v0}};
        for (float[][] face : faces) {
            for (int i = 0; i < 4; i++) {
                consumer.vertex(matrix, face[i][0], face[i][1], face[i][2]).color(1f, 1f, 1f, 1f).uv(uv[i][0], uv[i][1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xf000f0).normal(normal, 0f, 1f, 0f).endVertex();
            }
        }
    }

    /** drawRune оригинала: светящийся знак из листа script.png (16 знаков в ряд), мерцающий в своём ритме. */
    private static void drawRune(PoseStack pose, MultiBufferSource buffer, float x, float y, float z, int rune, int ticks) {
        float r = Mth.sin((ticks + rune * 5) / 5f) * 0.1f + 0.88f;
        float g = Mth.sin((ticks + rune * 5) / 7f) * 0.1f + 0.63f;
        float pulse = Mth.sin((ticks + rune * 5) / 10f) * 0.2f;
        // свечение складывается с фоном, поэтому прозрачность заложена в яркость
        float alpha = pulse + 0.6f;
        float size = 0.06f + pulse / 40f;
        float u0 = 0.0625f * rune;
        float u1 = u0 + 0.0625f;
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(90f));
        pose.translate(x, y, z);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(SCRIPT));
        runeVertex(consumer, matrix, normal, -size, size, u1, 1f, r * alpha, g * alpha, 0.2f * alpha);
        runeVertex(consumer, matrix, normal, size, size, u1, 0f, r * alpha, g * alpha, 0.2f * alpha);
        runeVertex(consumer, matrix, normal, size, -size, u0, 0f, r * alpha, g * alpha, 0.2f * alpha);
        runeVertex(consumer, matrix, normal, -size, -size, u0, 1f, r * alpha, g * alpha, 0.2f * alpha);
        pose.popPose();
    }

    private static void runeVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v,
                                   float r, float g, float b) {
        consumer.vertex(matrix, x, y, 0f).color(r, g, b, 1f).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xf000f0)
                .normal(normal, 0f, 0f, 1f).endVertex();
    }
}
