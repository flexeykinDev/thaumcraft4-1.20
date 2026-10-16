package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.Pech;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Печ: перенос ModelPech и RenderPech оригинала. У каждого из трёх видов своя текстура; в правой руке — его орудие. */
public class PechRenderer extends MobRenderer<Pech, PechRenderer.Model> {
    private static final ResourceLocation[] SKINS = {Thaumcraft.id("textures/models/pech_forage.png"),
            Thaumcraft.id("textures/models/pech_thaum.png"), Thaumcraft.id("textures/models/pech_stalker.png")};

    public PechRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 0.25f);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Pech pech) {
        return SKINS[Mth.clamp(pech.getPechType(), 0, 2)];
    }

    /** Сгорбленное тело, голова с отвисшей челюстью, короткие руки и ноги и огромный тюк за спиной. */
    public static class Model extends EntityModel<Pech> implements ArmedModel {
        private final ModelPart body = part(34, 12, -3f, 0f, 0f, 6, 10, 6, false, 0f, 9f, -3f);
        private final ModelPart rightLeg = part(35, 1, -2.9f, 0f, 0f, 3, 6, 3, true, 0f, 18f, 0f);
        private final ModelPart leftLeg = part(35, 1, -0.1f, 0f, 0f, 3, 6, 3, false, 0f, 18f, 0f);
        private final ModelPart head = part(2, 11, -3.5f, -5f, -5f, 7, 5, 5, false, 0f, 8f, 0f);
        private final ModelPart jowls = part(1, 21, -4f, -1f, -6f, 8, 3, 5, false, 0f, 8f, 0f);
        private final ModelPart lowerPack = part(0, 0, -5f, 0f, 0f, 10, 5, 5, false, 0f, 10f, 3.5f);
        private final ModelPart upperPack = part(64, 1, -7.5f, -14f, 0f, 15, 14, 11, false, 0f, 10f, 3f);
        private final ModelPart rightArm = part(52, 2, -2f, 0f, -1f, 2, 6, 2, true, -3f, 10f, -1f);
        private final ModelPart leftArm = part(52, 2, 0f, 0f, -1f, 2, 6, 2, false, 3f, 10f, -1f);

        Model() {
            body.xRot = 0.3129957f;
            lowerPack.xRot = 0.3013602f;
            upperPack.xRot = 0.4537856f;
        }

        private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, boolean mirror, float px, float py, float pz) {
            ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, mirror, 128f, 64f,
                    EnumSet.allOf(Direction.class))), Map.of());
            part.setPos(px, py, pz);
            return part;
        }

        @Override
        public void setupAnim(Pech pech, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            // челюсть ходит при ходьбе и когда печ бормочет
            jowls.yRot = head.yRot;
            jowls.xRot = head.xRot + 0.2617994f + Mth.cos(limbSwing * 0.6662f) * limbSwingAmount * 0.25f
                    + 0.34906587f * Math.abs(Mth.sin(pech.mumble));
            rightArm.xRot = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 2f * limbSwingAmount * 0.5f;
            leftArm.xRot = Mth.cos(limbSwing * 0.6662f) * 2f * limbSwingAmount * 0.5f;
            rightArm.zRot = 0f;
            leftArm.zRot = 0f;
            rightArm.yRot = 0f;
            leftArm.yRot = 0f;
            rightLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
            leftLeg.xRot = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.4f * limbSwingAmount;
            rightLeg.yRot = 0f;
            leftLeg.yRot = 0f;
            lowerPack.yRot = Mth.cos(limbSwing * 0.6662f) * 2f * limbSwingAmount * 0.125f;
            lowerPack.zRot = Mth.cos(limbSwing * 0.6662f) * 2f * limbSwingAmount * 0.125f;
            if (riding) {
                rightArm.xRot -= Mth.PI / 5f;
                leftArm.xRot -= Mth.PI / 5f;
                rightLeg.xRot = -1.2566371f;
                leftLeg.xRot = -1.2566371f;
                rightLeg.yRot = Mth.PI / 10f;
                leftLeg.yRot = -Mth.PI / 10f;
            }
            if (attackTime > 0f) {
                float swing = 1f - attackTime;
                swing *= swing;
                swing *= swing;
                swing = 1f - swing;
                float lift = Mth.sin(swing * Mth.PI);
                float reach = Mth.sin(attackTime * Mth.PI) * -(head.xRot - 0.7f) * 0.75f;
                rightArm.xRot -= lift * 1.2f + reach;
                rightArm.zRot = Mth.sin(attackTime * Mth.PI) * -0.4f;
            }
            rightArm.zRot += Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f;
            leftArm.zRot -= Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f;
            rightArm.xRot += Mth.sin(ageInTicks * 0.067f) * 0.05f;
            leftArm.xRot -= Mth.sin(ageInTicks * 0.067f) * 0.05f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            for (ModelPart part : List.of(body, rightLeg, leftLeg, head, jowls, lowerPack, upperPack, rightArm, leftArm)) {
                part.render(pose, consumer, light, overlay, r, g, b, a);
            }
        }

        /** Рука у печа вдвое короче человеческой, поэтому предмет поднимается к кисти. */
        @Override
        public void translateToHand(HumanoidArm arm, PoseStack pose) {
            (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
            pose.translate(arm == HumanoidArm.RIGHT ? 0.0625f : -0.0625f, -0.2875f, 0f);
        }
    }
}
