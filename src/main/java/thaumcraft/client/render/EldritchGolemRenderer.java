package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.EldritchGolem;

import java.util.List;

/**
 * Голем Элдрича — перенос ModelEldritchGolem и RenderEldritchGolem оригинала: голем в плаще и высоком воротнике,
 * вдвое с лишним больше обычной модели. Пока пробуждается — голова опущена; без головы видна шея-обрубок.
 */
public class EldritchGolemRenderer extends MobRenderer<EldritchGolem, EldritchGolemRenderer.Model> {
    private static final ResourceLocation SKIN = Thaumcraft.id("textures/models/eldritch_golem.png");

    public EldritchGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 1.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(EldritchGolem golem) {
        return SKIN;
    }

    @Override
    protected void scale(EldritchGolem golem, PoseStack pose, float partialTick) {
        pose.scale(2.15f, 2.15f, 2.15f);
    }

    public static class Model extends EntityModel<EldritchGolem> {
        final ModelPart cloak1 = ModelParts.part(0, 47, false, 128f, 64f, 0f, 0f, -2.5f, 0.1396263f, 0f, 0f, -5f, 1.5f, 4f, 10f, 12f, 1f, 0f);
        final ModelPart cloak3 = ModelParts.part(0, 37, false, 128f, 64f, 0f, 0f, -2.5f, 0.4465716f, 0f, 0f, -5f, 17.5f, -0.8f, 10f, 4f, 1f, 0f);
        final ModelPart cloak2 = ModelParts.part(0, 59, false, 128f, 64f, 0f, 0f, -2.5f, 0.3069452f, 0f, 0f, -5f, 13.5f, 1.7f, 10f, 4f, 1f, 0f);
        final ModelPart cloakCL = ModelParts.part(0, 43, false, 128f, 64f, 0f, 0f, -2.5f, 0.1396263f, 0f, 0f, 3f, 0.5f, 2f, 2f, 1f, 3f, 0f);
        final ModelPart cloakCR = ModelParts.part(0, 43, false, 128f, 64f, 0f, 0f, -2.5f, 0.1396263f, 0f, 0f, -5f, 0.5f, 2f, 2f, 1f, 3f, 0f);
        final ModelPart head = ModelParts.part(47, 12, false, 128f, 64f, 0f, 4.5f, -3.8f, -0.1047198f, 0f, 0f, -3.5f, -6f, -2.5f, 7f, 7f, 5f, 0f);
        final ModelPart head2 = ModelParts.part(26, 16, false, 128f, 64f, 0f, 0f, -5f, -0.1047198f, 0f, 0f, -2f, -2f, -2f, 4f, 4f, 4f, 0f);
        final ModelPart collarL = ModelParts.part(75, 50, false, 128f, 64f, 0f, 0f, -2.5f, 0.837758f, 0f, 0f, 3.5f, -0.5f, -7f, 1f, 4f, 10f, 0f);
        final ModelPart collarR = ModelParts.part(67, 50, false, 128f, 64f, 0f, 0f, -2.5f, 0.837758f, 0f, 0f, -4.5f, -0.5f, -7f, 1f, 4f, 10f, 0f);
        final ModelPart collarB = ModelParts.part(77, 59, false, 128f, 64f, 0f, 0f, -2.5f, 0.837758f, 0f, 0f, -3.5f, -0.5f, 2f, 7f, 4f, 1f, 0f);
        final ModelPart collarF = ModelParts.part(77, 59, false, 128f, 64f, 0f, 0f, -2.5f, 0.837758f, 0f, 0f, -3.5f, -0.5f, -7f, 7f, 4f, 1f, 0f);
        final ModelPart collarBlack = ModelParts.part(22, 0, false, 128f, 64f, 0f, 0f, -2.5f, 0.837758f, 0f, 0f, -3.5f, 0f, -6f, 7f, 1f, 8f, 0f);
        final ModelPart frontcloth0 = ModelParts.part(114, 52, false, 128f, 64f, 0f, 0f, -2.5f, 0.1745329f, 0f, 0f, -3f, 3.2f, -3.5f, 6f, 10f, 1f, 0f);
        final ModelPart frontcloth1 = ModelParts.part(114, 39, false, 128f, 64f, -2f, 12f, 0f, -0.1047198f, 0f, 0f, -1f, 1.5f, -3.5f, 6f, 6f, 1f, 0f);
        final ModelPart frontcloth2 = ModelParts.part(114, 47, false, 128f, 64f, -2f, 11f, 0f, -0.3316126f, 0f, 0f, -1f, 8.5f, -1.5f, 6f, 3f, 1f, 0f);
        final ModelPart torso = ModelParts.part(34, 45, true, 128f, 64f, 0f, 0f, -2.5f, 0.1745329f, 0f, 0f, -5f, 2.5f, -3f, 10f, 10f, 6f, 0f);
        final ModelPart armR = ModelParts.part(78, 32, false, 128f, 64f, -5f, 3f, -2f, 0f, 0f, 0.1047198f, -3.5f, 1.5f, -2f, 4f, 13f, 5f, 0f);
        final ModelPart shoulderR1 = ModelParts.part(0, 23, false, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 1.186824f, -3.3f, 4f, -2.5f, 1f, 2f, 6f, 0f);
        final ModelPart shoulderR = ModelParts.part(0, 0, false, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 1.186824f, -4.3f, -1f, -3f, 4f, 5f, 7f, 0f);
        final ModelPart shoulderR2 = ModelParts.part(0, 12, false, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 1.186824f, -2.3f, 4f, -3f, 2f, 3f, 7f, 0f);
        final ModelPart shoulderR0 = ModelParts.part(56, 31, false, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -4.5f, -1.5f, -2.5f, 5f, 6f, 6f, 0f);
        final ModelPart armL = ModelParts.part(78, 32, true, 128f, 64f, 5f, 3f, -2f, 0f, 0f, -0.1047198f, -0.5f, 1.5f, -2f, 4f, 13f, 5f, 0f);
        final ModelPart shoulderL1 = ModelParts.part(0, 23, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, -1.186824f, 2.3f, 4f, -2.5f, 1f, 2f, 6f, 0f);
        final ModelPart shoulderL0 = ModelParts.part(56, 31, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -0.5f, -1.5f, -2.5f, 5f, 6f, 6f, 0f);
        final ModelPart shoulderL = ModelParts.part(0, 0, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, -1.186824f, 0.3f, -1f, -3f, 4f, 5f, 7f, 0f);
        final ModelPart shoulderL2 = ModelParts.part(0, 12, true, 128f, 64f, 0f, 0f, 0f, 0f, 0f, -1.186824f, 0.3f, 4f, -3f, 2f, 3f, 7f, 0f);
        final ModelPart backpanelR1 = ModelParts.part(96, 7, false, 128f, 64f, -2f, 12f, 0f, 0f, 0f, 0.1396263f, 0f, 2.5f, -2.5f, 2f, 2f, 5f, 0f);
        final ModelPart waistR1 = ModelParts.part(96, 14, false, 128f, 64f, -2f, 12f, 0f, 0f, 0f, 0.1396263f, -3f, -0.5f, -2.5f, 5f, 3f, 5f, 0f);
        final ModelPart waistR2 = ModelParts.part(116, 13, false, 128f, 64f, -2f, 12f, 0f, 0f, 0f, 0.1396263f, -3f, 2.5f, -2.5f, 1f, 4f, 5f, 0f);
        final ModelPart waistR3 = ModelParts.part(114, 5, true, 128f, 64f, -2f, 12f, 0f, 0f, 0f, 0.1396263f, -2f, 2.5f, -2.5f, 2f, 3f, 5f, 0f);
        final ModelPart legR = ModelParts.part(79, 19, false, 128f, 64f, -2f, 12.5f, 0f, 0f, 0f, 0f, -2.5f, 2.5f, -2f, 4f, 9f, 4f, 0f);
        final ModelPart waistL1 = ModelParts.part(96, 14, true, 128f, 64f, 2f, 12f, 0f, 0f, 0f, -0.1396263f, -2f, -0.5f, -2.5f, 5f, 3f, 5f, 0f);
        final ModelPart waistL2 = ModelParts.part(116, 13, true, 128f, 64f, 2f, 12f, 0f, 0f, 0f, -0.1396263f, 2f, 2.5f, -2.5f, 1f, 4f, 5f, 0f);
        final ModelPart waistL3 = ModelParts.part(114, 5, true, 128f, 64f, 2f, 12f, 0f, 0f, 0f, -0.1396263f, 0f, 2.5f, -2.5f, 2f, 3f, 5f, 0f);
        final ModelPart backpanelL1 = ModelParts.part(96, 7, true, 128f, 64f, 2f, 12f, 0f, 0f, 0f, -0.1396263f, -2f, 2.5f, -2.5f, 2f, 2f, 5f, 0f);
        final ModelPart legL = ModelParts.part(79, 19, true, 128f, 64f, 2f, 12.5f, 0f, 0f, 0f, 0f, -1.5f, 2.5f, -2f, 4f, 9f, 4f, 0f);

        {
            ModelParts.attach(armL, "shoulderl", shoulderL);
            ModelParts.attach(armL, "shoulderl0", shoulderL0);
            ModelParts.attach(armL, "shoulderl1", shoulderL1);
            ModelParts.attach(armL, "shoulderl2", shoulderL2);
            ModelParts.attach(armR, "shoulderr", shoulderR);
            ModelParts.attach(armR, "shoulderr0", shoulderR0);
            ModelParts.attach(armR, "shoulderr1", shoulderR1);
            ModelParts.attach(armR, "shoulderr2", shoulderR2);
        }

        private boolean headless;

        @Override
        public void prepareMobModel(EldritchGolem golem, float limbSwing, float limbSwingAmount, float partialTick) {
            int i = golem.getAttackTimer();
            if (i > 0) {
                armR.xRot = -2f + 1.5f * doAbs(i - partialTick, 10f);
                armL.xRot = -2f + 1.5f * doAbs(i - partialTick, 10f);
            } else {
                armR.xRot = Mth.cos(limbSwing * 0.4f + Mth.PI) * 2f * limbSwingAmount * 0.5f;
                armL.xRot = Mth.cos(limbSwing * 0.4f) * 2f * limbSwingAmount * 0.5f;
            }
        }

        private static float doAbs(float a, float b) {
            return (Math.abs(a % b - b * 0.5f) - b * 0.25f) / (b * 0.25f);
        }

        @Override
        public void setupAnim(EldritchGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            headless = golem.isHeadless();
            if (golem.getSpawnTimer() > 0) {
                head.yRot = 0f;
                head.xRot = (golem.getSpawnTimer() / 2) * Mth.DEG_TO_RAD;
            } else {
                head.yRot = netHeadYaw / 4f * Mth.DEG_TO_RAD;
                head.xRot = headPitch / 2f * Mth.DEG_TO_RAD;
                head2.yRot = netHeadYaw * Mth.DEG_TO_RAD;
                head2.xRot = headPitch * Mth.DEG_TO_RAD;
            }
            legR.xRot = Mth.cos(limbSwing * 0.4662f) * 1.4f * limbSwingAmount;
            legL.xRot = Mth.cos(limbSwing * 0.4662f + Mth.PI) * 1.4f * limbSwingAmount;
            float a = Mth.cos(limbSwing * 0.44f) * 1.4f * limbSwingAmount;
            float b = Mth.cos(limbSwing * 0.44f + Mth.PI) * 1.4f * limbSwingAmount;
            float c = Math.min(a, b);
            frontcloth1.xRot = c - 0.1047198f;
            frontcloth2.xRot = c - 0.3316126f;
            cloak1.xRot = -c / 3f + 0.1396263f;
            cloak2.xRot = -c / 3f + 0.3069452f;
            cloak3.xRot = -c / 3f + 0.4465716f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            for (ModelPart part : List.of(frontcloth1, collarL, collarBlack, cloak1, cloakCL, cloakCR, cloak3, cloak2, headless ? head2 : head,
                    frontcloth0, collarB, torso, collarR, collarF, armL, armR, backpanelR1, waistR1, waistR2, waistR3, legR, waistL1, waistL2,
                    waistL3, frontcloth2, backpanelL1, legL)) {
                part.render(pose, consumer, light, overlay, r, g, b, a);
            }
        }
    }
}
