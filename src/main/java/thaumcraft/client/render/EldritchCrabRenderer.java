package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.EldritchCrab;

import java.util.List;

/** Краб Элдрича — перенос ModelEldritchCrab и RenderEldritchCrab оригинала: панцирь с клешнями, светящиеся пятна поверх. */
public class EldritchCrabRenderer extends MobRenderer<EldritchCrab, EldritchCrabRenderer.Model> {
    private static final ResourceLocation SKIN = Thaumcraft.id("textures/models/crab.png");
    private static final ResourceLocation OVERLAY = Thaumcraft.id("textures/models/craboverlay.png");

    public EldritchCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 1f);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(EldritchCrab crab) {
        return SKIN;
    }

    /** Второй проход оригинала: светящийся рисунок поверх панциря при яркости 200. */
    private static class Glow extends RenderLayer<EldritchCrab, Model> {
        Glow(RenderLayerParent<EldritchCrab, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, EldritchCrab crab, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(OVERLAY));
            getParentModel().renderToBuffer(pose, consumer, 200 | (light & 0xffff0000), OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        }
    }

    public static class Model extends EntityModel<EldritchCrab> {
        final ModelPart tailHelm = ModelParts.part(0, 0, false, 128f, 64f, 0f, 18f, 0f, 0.1047198f, 0f, 0f, -4.5f, -4.5f, -0.4f, 9f, 9f, 9f, 0f);
        final ModelPart tailBare = ModelParts.part(64, 0, false, 128f, 64f, 0f, 18f, 0f, 0.1047198f, 0f, 0f, -4f, -4f, -0.4f, 8f, 8f, 8f, 0f);
        final ModelPart rClaw1 = ModelParts.part(0, 47, false, 128f, 64f, -6f, 15.5f, -10f, 0f, 0f, 0f, -2f, -1f, -5.066667f, 4f, 3f, 5f, 0f);
        final ModelPart head1 = ModelParts.part(0, 38, false, 128f, 64f, 0f, 18f, 0f, 0f, 0f, 0f, -2f, -1.5f, -9.066667f, 4f, 4f, 1f, 0f);
        final ModelPart rClaw0 = ModelParts.part(0, 55, false, 128f, 64f, -6f, 17f, -7f, 0f, 0f, 0f, -2f, -2.5f, -3.066667f, 4f, 5f, 3f, 0f);
        final ModelPart rClaw2 = ModelParts.part(14, 54, false, 128f, 64f, -6f, 18.5f, -10f, 0.3141593f, 0f, 0f, -1.5f, -1f, -4.066667f, 3f, 2f, 5f, 0f);
        final ModelPart rArm = ModelParts.part(44, 4, false, 128f, 64f, -3f, 17f, -4f, 0f, 0.7504916f, 0f, -1f, -1f, -5.066667f, 2f, 2f, 6f, 0f);
        final ModelPart lClaw2 = ModelParts.part(14, 54, false, 128f, 64f, 6f, 18.5f, -10f, 0.3141593f, 0f, 0f, -1.5f, -1f, -4.066667f, 3f, 2f, 5f, 0f);
        final ModelPart lClaw1 = ModelParts.part(0, 47, true, 128f, 64f, 6f, 15.5f, -10f, 0f, 0f, 0f, -2f, -1f, -5.066667f, 4f, 3f, 5f, 0f);
        final ModelPart lClaw0 = ModelParts.part(0, 55, true, 128f, 64f, 6f, 17f, -7f, 0f, 0f, 0f, -2f, -2.5f, -3.066667f, 4f, 5f, 3f, 0f);
        final ModelPart lArm = ModelParts.part(44, 4, false, 128f, 64f, 4f, 17f, -5f, 0f, -0.7504916f, 0f, -1f, -1f, -4.066667f, 2f, 2f, 6f, 0f);
        final ModelPart torso = ModelParts.part(0, 18, false, 128f, 64f, 0f, 18f, 0f, 0.0523599f, 0f, 0f, -3.5f, -3.5f, -6.066667f, 7f, 7f, 6f, 0f);
        final ModelPart head0 = ModelParts.part(0, 31, false, 128f, 64f, 0f, 18f, 0f, 0f, 0f, 0f, -2.5f, -2f, -8.066667f, 5f, 5f, 2f, 0f);
        final ModelPart rRLeg1 = ModelParts.part(36, 4, false, 128f, 64f, -4f, 20f, -1.5f, 0f, 0.2094395f, 0.4363323f, -4.5f, 1f, -0.9f, 2f, 5f, 2f, 0f);
        final ModelPart rFLeg1 = ModelParts.part(36, 4, false, 128f, 64f, -4f, 20f, -3.5f, 0f, -0.2094395f, 0.4363323f, -5f, 1f, -1.066667f, 2f, 5f, 2f, 0f);
        final ModelPart lRLeg1 = ModelParts.part(36, 4, false, 128f, 64f, 4f, 20f, -1.5f, 0f, -0.2094395f, -0.4363323f, 2.5f, 1f, -0.9f, 2f, 5f, 2f, 0f);
        final ModelPart lFLeg1 = ModelParts.part(36, 4, false, 128f, 64f, 4f, 20f, -3.5f, 0f, 0.2094395f, -0.4363323f, 3f, 1f, -1.066667f, 2f, 5f, 2f, 0f);
        final ModelPart rRLeg0 = ModelParts.part(36, 0, false, 128f, 64f, -4f, 20f, -1.5f, 0f, 0.2094395f, 0.4363323f, -4.5f, -1f, -0.9f, 6f, 2f, 2f, 0f);
        final ModelPart rFLeg0 = ModelParts.part(36, 0, false, 128f, 64f, -4f, 20f, -3.5f, 0f, -0.2094395f, 0.4363323f, -5f, -1f, -1.066667f, 6f, 2f, 2f, 0f);
        final ModelPart lFLeg0 = ModelParts.part(36, 0, false, 128f, 64f, 4f, 20f, -3.5f, 0f, 0.2094395f, -0.4363323f, -1f, -1f, -1.066667f, 6f, 2f, 2f, 0f);
        final ModelPart lRLeg0 = ModelParts.part(36, 0, false, 128f, 64f, 4f, 20f, -1.5f, 0f, -0.2094395f, -0.4363323f, -1.5f, -1f, -0.9f, 6f, 2f, 2f, 0f);
        private boolean helm;

        private List<ModelPart> body() {
            return List.of(rFLeg1, rClaw1, head1, rClaw0, rClaw2, lClaw2, lClaw1, rArm, torso, rRLeg1, head0, lRLeg1, lFLeg1, rRLeg0, rFLeg0,
                    lFLeg0, lRLeg0, lClaw0, lArm);
        }

        @Override
        public void setupAnim(EldritchCrab crab, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            helm = crab.hasHelm();
            for (ModelPart part : List.of(rRLeg1, rFLeg1, lRLeg1, lFLeg1, rRLeg0, rFLeg0, lFLeg0, lRLeg0)) {
                part.resetPose();
            }
            float f9 = -(Mth.cos(limbSwing * 0.6662f * 2f) * 0.4f) * limbSwingAmount;
            float f10 = -(Mth.cos(limbSwing * 0.6662f * 2f + Mth.PI) * 0.4f) * limbSwingAmount;
            rRLeg1.yRot += f9;
            rRLeg0.yRot += f9;
            lRLeg1.yRot += -f9;
            lRLeg0.yRot += -f9;
            rFLeg1.yRot += f10;
            rFLeg0.yRot += f10;
            lFLeg1.yRot += -f10;
            lFLeg0.yRot += -f10;
            rRLeg1.zRot += f9;
            rRLeg0.zRot += f9;
            lRLeg1.zRot += -f9;
            lRLeg0.zRot += -f9;
            rFLeg1.zRot += f10;
            rFLeg0.zRot += f10;
            lFLeg1.zRot += -f10;
            lFLeg0.zRot += -f10;
            tailBare.yRot = tailHelm.yRot = Mth.cos(limbSwing * 0.6662f) * 2f * limbSwingAmount * 0.125f;
            tailBare.zRot = tailHelm.zRot = Mth.cos(limbSwing * 0.6662f) * limbSwingAmount * 0.125f;
            rClaw2.xRot = 0.3141593f - Mth.sin(ageInTicks / 4f) * 0.25f;
            lClaw2.xRot = 0.3141593f + Mth.sin(ageInTicks / 4.1f) * 0.25f;
            rClaw1.xRot = Mth.sin(ageInTicks / 4f) * 0.125f;
            lClaw1.xRot = -Mth.sin(ageInTicks / 4.1f) * 0.125f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            (helm ? tailHelm : tailBare).render(pose, consumer, light, overlay, r, g, b, a);
            for (ModelPart part : body()) {
                part.render(pose, consumer, light, overlay, r, g, b, a);
            }
        }
    }
}
