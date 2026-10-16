package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.EldritchGuardian;
import thaumcraft.common.entity.EldritchWarden;
import thaumcraft.common.world.outer.OuterLands;

import java.util.List;

/**
 * Страж Элдрича — перенос ModelEldritchGuardian и RenderEldritchGuardian оригинала: балахон с наплечниками, полы
 * и плащ колышутся, руки подняты для броска. В обычном мире страж полупрозрачен (не больше 60%) и тает с
 * расстоянием — на высокой сложности быстрее; в Запредельных землях виден целиком.
 */
public class EldritchGuardianRenderer extends MobRenderer<Mob, EldritchGuardianRenderer.Model> {
    private static final ResourceLocation SKIN = Thaumcraft.id("textures/models/eldritch_guardian.png");
    private static final ResourceLocation WARDEN = Thaumcraft.id("textures/models/eldritch_warden.png");

    public EldritchGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 0.5f);
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<>(this) {
            /** Глаз хранителя под капюшоном светится и мерцает (ModelEldritchGuardian.render оригинала). */
            @Override
            public void render(PoseStack pose, MultiBufferSource buffer, int light, Mob mob, float limbSwing, float limbSwingAmount, float partialTick,
                               float ageInTicks, float netHeadYaw, float headPitch) {
                if (mob instanceof EldritchWarden) {
                    int glow = (int) (195f + Mth.sin(ageInTicks / 3f) * 15f + 15f);
                    getParentModel().renderEye(pose, buffer.getBuffer(RenderType.eyes(WARDEN)), glow | glow << 16,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                }
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Mob mob) {
        return mob instanceof EldritchWarden ? WARDEN : SKIN;
    }

    @Override
    public void render(Mob mob, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        model.alpha = mob instanceof EldritchWarden ? 1f : visibility(mob);
        pose.pushPose();
        if (mob instanceof EldritchWarden warden) {
            // пока хранитель появляется, он поднимается из пола
            pose.translate(0.0, -warden.getBbHeight() * (warden.getSpawnTimer() / 150f), 0.0);
        }
        super.render(mob, yaw, partialTick, pose, buffer, light);
        pose.popPose();
    }

    @Override
    protected void scale(Mob mob, PoseStack pose, float partialTick) {
        if (mob instanceof EldritchWarden) {
            pose.scale(1.5f, 1.5f, 1.5f);
        }
    }

    static float visibility(Mob mob) {
        if (OuterLands.is(mob.level())) {
            return 1f;
        }
        Entity viewer = net.minecraft.client.Minecraft.getInstance().getCameraEntity();
        if (viewer == null) {
            return 0.6f;
        }
        float far = mob.level().getDifficulty() == Difficulty.HARD ? 576f : 1024f;
        float near = 256f;
        double distance = mob.distanceToSqr(viewer);
        if (distance < near) {
            return 0.6f;
        }
        return (float) (1.0 - Math.min(far - near, distance - near) / (far - near)) * 0.6f;
    }

    @Override
    protected RenderType getRenderType(Mob mob, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible || translucent ? RenderType.entityTranslucent(getTextureLocation(mob)) : super.getRenderType(mob, false, false, glowing);
    }

    public static class Model extends EntityModel<Mob> {
        final ModelPart beltR = ModelParts.part(76, 44, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -5f, 4f, -3f, 1f, 3f, 6f, 0f);
        final ModelPart mbelt = ModelParts.part(56, 55, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -4f, 8f, -3f, 8f, 4f, 1f, 0f);
        final ModelPart mbeltL = ModelParts.part(76, 44, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, 4f, 8f, -3f, 1f, 3f, 6f, 0f);
        final ModelPart mbeltR = ModelParts.part(76, 44, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -5f, 8f, -3f, 1f, 3f, 6f, 0f);
        final ModelPart beltL = ModelParts.part(76, 44, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, 4f, 4f, -3f, 1f, 3f, 6f, 0f);
        final ModelPart chestplate = ModelParts.part(56, 45, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -4f, 1f, -4f, 8f, 7f, 2f, 0f);
        final ModelPart hoodEye = ModelParts.part(0, 0, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -4f, -8f, -4f, 8f, 8f, 8f, 0f);
        final ModelPart hood1 = ModelParts.part(40, 12, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -4f, -8f, -4f, 8f, 8f, 8f, 0f);
        final ModelPart hood2 = ModelParts.part(36, 28, true, 128f, 64f, 0f, 0f, 0f, -0.2268928f, 0f, 0f, -3.5f, -8.7f, 2f, 7f, 7f, 3f, 0f);
        final ModelPart hood3 = ModelParts.part(22, 19, true, 128f, 64f, 0f, 0f, 0f, -0.3490659f, 0f, 0f, -3f, -9f, 2.5f, 6f, 6f, 3f, 0f);
        final ModelPart hood4 = ModelParts.part(40, 4, true, 128f, 64f, 0f, 0f, 0f, -0.5759587f, 0f, 0f, -2.5f, -9.7f, 3.5f, 5f, 5f, 3f, 0f);
        final ModelPart backplate = ModelParts.part(36, 45, true, 128f, 64f, 0f, -6f, 0f, 0f, 0f, 0f, -4f, 1f, 2f, 8f, 11f, 2f, 0f);
        final ModelPart shoulderplateTopR = ModelParts.part(110, 37, true, 128f, 64f, -5f, -4f, 0f, -0.3665191f, 0.3141593f, 0.4363323f, -5.5f, -2.5f, -3.5f, 2f, 1f, 7f, 0f);
        final ModelPart shoulderplateR1 = ModelParts.part(110, 45, true, 128f, 64f, 5f, -4f, 0f, -0.3665191f, -0.3141593f, -0.4363323f, 3.5f, -1.5f, -3.5f, 1f, 4f, 7f, 0f);
        final ModelPart shoulderplateR2 = ModelParts.part(94, 45, true, 128f, 64f, -5f, -4f, 0f, -0.3665191f, 0.3141593f, 0.4363323f, -3.5f, 1.5f, -3.5f, 1f, 3f, 7f, 0f);
        final ModelPart shoulderplateR3 = ModelParts.part(94, 45, true, 128f, 64f, -5f, -4f, 0f, -0.3665191f, 0.3141593f, 0.4363323f, -2.5f, 3.5f, -3.5f, 1f, 3f, 7f, 0f);
        final ModelPart shoulderR = ModelParts.part(56, 35, true, 128f, 64f, -5f, -4f, 0f, -0.3665191f, 0.122173f, 0.0349066f, -3.5f, -2.5f, -2.5f, 5f, 5f, 5f, 0f);
        final ModelPart armL1 = ModelParts.part(72, 8, true, 128f, 64f, 5f, -4f, 0f, -0.9599311f, -0.1047198f, -0.1919862f, -1f, 2.5f, -1.5f, 4f, 10f, 5f, 0f);
        final ModelPart armL2 = ModelParts.part(76, 28, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -1f, 9.5f, 3.5f, 4f, 3f, 3f, 0f);
        final ModelPart armL3 = ModelParts.part(76, 23, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -1f, 6.5f, 3.5f, 4f, 3f, 2f, 0f);
        final ModelPart armR1 = ModelParts.part(72, 8, true, 128f, 64f, -5f, -4f, 0f, -0.9599311f, 0.1047198f, 0.1919862f, -3f, 2.5f, -1.5f, 4f, 10f, 5f, 0f);
        final ModelPart armR2 = ModelParts.part(76, 28, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -3f, 9.5f, 3.5f, 4f, 3f, 3f, 0f);
        final ModelPart armR3 = ModelParts.part(76, 23, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -3f, 6.5f, 3.5f, 4f, 3f, 2f, 0f);
        final ModelPart shoulderL = ModelParts.part(56, 35, true, 128f, 64f, 5f, -4f, 0f, -0.3665191f, -0.122173f, -0.0349066f, -1.5f, -2.5f, -2.5f, 5f, 5f, 5f, 0f);
        final ModelPart shoulderplateLtop = ModelParts.part(110, 37, true, 128f, 64f, 5f, -4f, 0f, -0.3665191f, -0.3141593f, -0.4363323f, 3.5f, -2.5f, -3.5f, 2f, 1f, 7f, 0f);
        final ModelPart shoulderplateL1 = ModelParts.part(110, 45, true, 128f, 64f, -5f, -4f, 0f, -0.3665191f, 0.3141593f, 0.4363323f, -4.5f, -1.5f, -3.5f, 1f, 4f, 7f, 0f);
        final ModelPart shoulderplateL2 = ModelParts.part(94, 45, true, 128f, 64f, 5f, -4f, 0f, -0.3665191f, -0.3141593f, -0.4363323f, 2.5f, 1.5f, -3.5f, 1f, 3f, 7f, 0f);
        final ModelPart shoulderplateL3 = ModelParts.part(94, 45, true, 128f, 64f, 5f, -4f, 0f, -0.3665191f, -0.3141593f, -0.4363323f, 1.5f, 3.5f, -3.5f, 1f, 3f, 7f, 0f);
        final ModelPart legpanelR4 = ModelParts.part(0, 43, true, 128f, 64f, -2f, 6f, 0f, -0.4363323f, 0f, 0f, -3f, 0.5f, -3.5f, 2f, 3f, 1f, 0f);
        final ModelPart legpanelR5 = ModelParts.part(0, 47, true, 128f, 64f, -2f, 6f, 0f, -0.4363323f, 0f, 0f, -3f, 2.5f, -2.5f, 2f, 3f, 1f, 0f);
        final ModelPart legpanelR6 = ModelParts.part(6, 43, true, 128f, 64f, -2f, 6f, 0f, -0.4363323f, 0f, 0f, -3f, 4.5f, -1.5f, 2f, 3f, 1f, 0f);
        final ModelPart backpanelR1 = ModelParts.part(0, 18, true, 128f, 64f, -2f, 6f, 0f, 0.4363323f, 0f, 0f, -3f, 0.5f, 2.5f, 5f, 3f, 1f, 0f);
        final ModelPart backpanelR2 = ModelParts.part(0, 18, true, 128f, 64f, -2f, 6f, 0f, 0.4363323f, 0f, 0f, -3f, 2.5f, 1.5f, 5f, 3f, 1f, 0f);
        final ModelPart backpanelR3 = ModelParts.part(0, 18, true, 128f, 64f, -2f, 6f, 0f, 0.4363323f, 0f, 0f, -3f, 4.5f, 0.5f, 5f, 3f, 1f, 0f);
        final ModelPart backpanelL3 = ModelParts.part(0, 18, true, 128f, 64f, 2f, 6f, 0f, 0.4363323f, 0f, 0f, -2f, 4.5f, 0.5f, 5f, 3f, 1f, 0f);
        final ModelPart legpanelL4 = ModelParts.part(0, 43, true, 128f, 64f, 2f, 6f, 0f, -0.4363323f, 0f, 0f, 1f, 0.5f, -3.5f, 2f, 3f, 1f, 0f);
        final ModelPart legpanelL5 = ModelParts.part(0, 47, true, 128f, 64f, 2f, 6f, 0f, -0.4363323f, 0f, 0f, 1f, 2.5f, -2.5f, 2f, 3f, 1f, 0f);
        final ModelPart legpanelL6 = ModelParts.part(6, 43, true, 128f, 64f, 2f, 6f, 0f, -0.4363323f, 0f, 0f, 1f, 4.5f, -1.5f, 2f, 3f, 1f, 0f);
        final ModelPart backpanelL1 = ModelParts.part(0, 18, true, 128f, 64f, 2f, 6f, 0f, 0.4363323f, 0f, 0f, -2f, 0.5f, 2.5f, 5f, 3f, 1f, 0f);
        final ModelPart backpanelL2 = ModelParts.part(0, 18, true, 128f, 64f, 2f, 6f, 0f, 0.4363323f, 0f, 0f, -2f, 2.5f, 1.5f, 5f, 3f, 1f, 0f);
        final ModelPart sidepanelL1 = ModelParts.part(0, 22, true, 128f, 64f, 2f, 6f, 0f, 0f, 0f, -0.4363323f, 1.5f, 0.5f, -2.5f, 1f, 4f, 5f, 0f);
        final ModelPart sidepanelR1 = ModelParts.part(0, 22, true, 128f, 64f, -2f, 6f, 0f, 0f, 0f, 0.4363323f, -2.5f, 0.5f, -2.5f, 1f, 4f, 5f, 0f);
        final ModelPart sidepanelR2 = ModelParts.part(0, 54, true, 128f, 64f, -4.5f, 9.5f, -2f, 0f, 0f, 0.122173f, 0f, 0f, -0.5f, 1f, 5f, 5f, 0f);
        final ModelPart sidepanelR3 = ModelParts.part(0, 35, true, 128f, 64f, 0f, 5f, 0f, 0f, 0f, 0.296706f, 0f, 0f, -0.5f, 1f, 3f, 5f, 0f);
        final ModelPart sidepanelR4 = ModelParts.part(24, 35, true, 128f, 64f, 0f, 3f, 0f, 0f, 0f, 0.5235988f, 0f, 0f, -0.5f, 1f, 3f, 5f, 0f);
        final ModelPart sidepanelL2 = ModelParts.part(0, 54, true, 128f, 64f, 4.5f, 9.5f, -2f, 0f, 0f, -0.122173f, 0f, 0f, -0.5f, 1f, 5f, 5f, 0f);
        final ModelPart sidepanelL3 = ModelParts.part(0, 35, true, 128f, 64f, 0f, 5f, 0f, 0f, 0f, -0.296706f, 0f, 0f, -0.5f, 1f, 3f, 5f, 0f);
        final ModelPart sidepanelL4 = ModelParts.part(24, 35, true, 128f, 64f, 0f, 3f, 0f, 0f, 0f, -0.5235988f, 0f, 0f, -0.5f, 1f, 3f, 5f, 0f);
        final ModelPart legpanelC1 = ModelParts.part(16, 45, true, 128f, 64f, 0f, 5.5f, -3f, 0f, 0f, 0f, -3f, 0f, -0.5f, 6f, 8f, 1f, 0f);
        final ModelPart legpanelC2 = ModelParts.part(16, 54, true, 128f, 64f, 0f, 8f, 0f, 0f, 0f, 0f, -3f, 0f, -0.5f, 6f, 4f, 1f, 0f);
        final ModelPart legpanelC3 = ModelParts.part(32, 59, true, 128f, 64f, 0f, 4f, 0f, 0f, 0f, 0f, -3f, 0f, -0.5f, 6f, 4f, 1f, 0f);
        final ModelPart cloak1 = ModelParts.part(106, 0, true, 128f, 64f, -5f, -6f, 4f, 0f, 0f, 0f, 0f, 0f, -0.5f, 10f, 18f, 1f, 0f);
        final ModelPart cloak2 = ModelParts.part(106, 19, true, 128f, 64f, 0f, 18f, 0f, 0f, 0f, 0f, 0f, 0f, -0.5f, 10f, 4f, 1f, 0f);
        final ModelPart cloak3 = ModelParts.part(106, 24, true, 128f, 64f, 0f, 4f, 0f, 0f, 0f, 0f, 0f, 0f, -0.5f, 10f, 4f, 1f, 0f);

        {
            ModelParts.attach(hood1, "hood2", hood2);
            ModelParts.attach(hood1, "hood3", hood3);
            ModelParts.attach(hood1, "hood4", hood4);
            ModelParts.attach(armL1, "arml2", armL2);
            ModelParts.attach(armL1, "arml3", armL3);
            ModelParts.attach(armR1, "armr2", armR2);
            ModelParts.attach(armR1, "armr3", armR3);
            ModelParts.attach(legpanelC1, "legpanelc2", legpanelC2);
            ModelParts.attach(legpanelC2, "legpanelc3", legpanelC3);
            ModelParts.attach(sidepanelL2, "sidepanell3", sidepanelL3);
            ModelParts.attach(sidepanelL3, "sidepanell4", sidepanelL4);
            ModelParts.attach(sidepanelR2, "sidepanelr3", sidepanelR3);
            ModelParts.attach(sidepanelR3, "sidepanelr4", sidepanelR4);
            ModelParts.attach(cloak1, "cloak2", cloak2);
            ModelParts.attach(cloak2, "cloak3", cloak3);
        }

        /** Множитель прозрачности на этот кадр (doRenderLiving оригинала). */
        float alpha = 1f;

        private List<ModelPart> body() {
            return List.of(
                    beltR, mbelt, mbeltL, mbeltR, beltL, chestplate,
                    hood1, backplate, shoulderplateTopR, shoulderplateR1, shoulderplateR2, shoulderplateR3,
                    shoulderR, armL1, armR1, shoulderL, shoulderplateLtop, shoulderplateL1,
                    shoulderplateL2, shoulderplateL3, legpanelR4, legpanelR5, legpanelR6, backpanelR1,
                    backpanelR2, backpanelR3, backpanelL3, legpanelL4, legpanelL5, legpanelL6,
                    backpanelL1, backpanelL2, cloak1, sidepanelR1, sidepanelL1, sidepanelL2,
                    sidepanelR2, legpanelC1);
        }

        @Override
        public void setupAnim(Mob mob, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            hood1.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            hood1.xRot = headPitch * Mth.DEG_TO_RAD;
            hoodEye.xRot = hood1.xRot;
            hoodEye.yRot = hood1.yRot;
            float alr = 0f;
            float all = 0f;
            if (mob instanceof EldritchGuardian guardian) {
                alr = guardian.armLiftR;
                all = guardian.armLiftL;
            }
            if (mob instanceof EldritchWarden warden) {
                alr = warden.armLiftR;
                all = warden.armLiftL;
            }
            float t = ageInTicks;
            armL1.xRot = -1f - all + Mth.sin((t + 20f) / 10f) * 0.08f;
            armR1.xRot = -1f - alr + Mth.sin(t / 10f) * 0.08f;
            legpanelC1.xRot = -0.15f + Mth.sin(t / 8f) * 0.12f;
            legpanelC2.xRot = Mth.sin((t - 5f) / 8f) * 0.13f;
            legpanelC3.xRot = Mth.sin((t - 10f) / 8f) * 0.14f;
            cloak1.xRot = 0.2f + Mth.sin(t / 7f) * 0.08f;
            cloak2.xRot = Mth.sin((t - 5f) / 7f) * 0.1f;
            cloak3.xRot = Mth.sin((t - 10f) / 7f) * 0.12f;
            sidepanelL2.zRot = -0.2f + Mth.sin((t + 10f) / 8f) * 0.12f;
            sidepanelL3.zRot = Mth.sin((t + 5f) / 8f) * 0.13f;
            sidepanelL4.zRot = Mth.sin(t / 8f) * 0.14f;
            sidepanelR2.zRot = 0.2f + Mth.sin((t - 5f) / 8f) * 0.12f;
            sidepanelR3.zRot = Mth.sin((t - 10f) / 8f) * 0.13f;
            sidepanelR4.zRot = Mth.sin((t - 15f) / 8f) * 0.14f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            for (ModelPart part : body()) {
                part.render(pose, consumer, light, overlay, r, g, b, a * alpha);
            }
        }

        /** Светящийся глаз под капюшоном (у хранителя Элдрича). */
        public void renderEye(PoseStack pose, VertexConsumer consumer, int light, int overlay) {
            pose.pushPose();
            pose.scale(1.01f, 1.01f, 1.01f);
            hoodEye.render(pose, consumer, light, overlay, 1f, 1f, 1f, 1f);
            pose.popPose();
        }
    }
}
