package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Латы рыцаря культа (ModelKnightArmor оригинала): шлем, наплечники, латные рукавицы, нагрудник с табардом и плащ. Коробки переведены tools/gen_armor_models.py. Для кирасы — модель с нагрудником (outer), для шлема
 * и поножей — без него (inner).
 */
public class CultistKnightArmorModel extends HumanoidModel<LivingEntity> {
    private static CultistKnightArmorModel outer;
    private static CultistKnightArmorModel inner;
    private final Map<String, ModelPart> swinging = new HashMap<>();

    private CultistKnightArmorModel(ModelPart root) {
        super(root);
        for (String name : new String[]{"frontcloth1", "frontcloth2", "cloak1", "cloak2", "cloak3"}) {
            if (body.hasChild(name)) {
                swinging.put(name, body.getChild(name));
            }
        }
    }

    public static CultistKnightArmorModel get(boolean chest) {
        if (chest) {
            if (outer == null) {
                outer = new CultistKnightArmorModel(createLayer(true).bakeRoot());
            }
            return outer;
        }
        if (inner == null) {
            inner = new CultistKnightArmorModel(createLayer(false).bakeRoot());
        }
        return inner;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float a = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        float b = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.4f * limbSwingAmount;
        float c = Math.min(a, b);
        rotate("frontcloth1", c - 0.1047198f);
        rotate("frontcloth2", c - 0.3316126f);
        rotate("cloak1", -c / 2f + 0.1396263f);
        rotate("cloak2", -c / 2f + 0.3069452f);
        rotate("cloak3", -c / 2f + 0.4465716f);
    }

    private void rotate(String name, float angle) {
        ModelPart part = swinging.get(name);
        if (part != null) {
            part.xRot = angle;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
        if (young) {
            super.renderToBuffer(pose, consumer, light, overlay, r, g, b, a);
            return;
        }
        pose.pushPose();
        pose.scale(1.01f, 1.01f, 1.01f);
        head.render(pose, consumer, light, overlay, r, g, b, a);
        pose.popPose();
        body.render(pose, consumer, light, overlay, r, g, b, a);
        rightArm.render(pose, consumer, light, overlay, r, g, b, a);
        leftArm.render(pose, consumer, light, overlay, r, g, b, a);
        rightLeg.render(pose, consumer, light, overlay, r, g, b, a);
        leftLeg.render(pose, consumer, light, overlay, r, g, b, a);
    }

    public static LayerDefinition createLayer(boolean chest) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<String, PartDefinition> parts = new HashMap<>();
        parts.put("head", root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO));
        parts.put("hat", root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO));
        parts.put("body", root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO));
        parts.put("right_arm", root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5f, 2f, 0f)));
        parts.put("left_arm", root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5f, 2f, 0f)));
        parts.put("right_leg", root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12f, 0f)));
        parts.put("left_leg", root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12f, 0f)));
        parts.get("head").addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).addBox(-4.5f, -9f, -4.5f, 9f, 9f, 9f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("body").addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).addBox(-4f, 8f, -3f, 8f, 4f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("body").addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(76, 44).addBox(4f, 8f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("body").addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5f, 8f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-3.5f, -2.5f, -2.5f, 5f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("shoulderr0", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4.3f, -1.5f, -3f, 3f, 5f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.7853982f));
        parts.get("right_arm").addOrReplaceChild("shoulderr1", CubeListBuilder.create().texOffs(0, 19).addBox(-3.3f, 3.5f, -2.5f, 1f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.7853982f));
        parts.get("right_arm").addOrReplaceChild("shoulderr2", CubeListBuilder.create().texOffs(0, 11).mirror().addBox(-2.3f, 3.5f, -3f, 1f, 2f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.7853982f));
        parts.get("right_arm").addOrReplaceChild("gauntletr", CubeListBuilder.create().texOffs(100, 26).mirror().addBox(-3.5f, 3.5f, -2.5f, 2f, 6f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("gauntletstrapr1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("gauntletstrapr2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(56, 35).addBox(-1.5f, -2.5f, -2.5f, 5f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("shoulderl0", CubeListBuilder.create().texOffs(0, 0).addBox(1.3f, -1.5f, -3f, 3f, 5f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.7853982f));
        parts.get("left_arm").addOrReplaceChild("shoulderl1", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3f, 3.5f, -2.5f, 1f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.7853982f));
        parts.get("left_arm").addOrReplaceChild("shoulderl2", CubeListBuilder.create().texOffs(0, 11).addBox(1.3f, 3.5f, -3f, 1f, 2f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.7853982f));
        parts.get("left_arm").addOrReplaceChild("gauntletl", CubeListBuilder.create().texOffs(114, 26).addBox(1.5f, 3.5f, -2.5f, 2f, 6f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("gauntletstrapl1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("gauntletstrapl2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr0", CubeListBuilder.create().texOffs(96, 14).addBox(-3f, -0.5f, -2.5f, 5f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.1396263f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(96, 7).mirror().addBox(0f, 2.5f, -2.5f, 2f, 2f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.1396263f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr2", CubeListBuilder.create().texOffs(114, 5).mirror().addBox(-2f, 2.5f, -2.5f, 2f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.1396263f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr3", CubeListBuilder.create().texOffs(116, 13).addBox(-3f, 2.5f, -2.5f, 1f, 4f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.1396263f));
        parts.get("left_leg").addOrReplaceChild("sidepanell0", CubeListBuilder.create().texOffs(96, 14).addBox(-2f, -0.5f, -2.5f, 5f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.1396263f));
        parts.get("left_leg").addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(96, 7).addBox(-2f, 2.5f, -2.5f, 2f, 2f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.1396263f));
        parts.get("left_leg").addOrReplaceChild("sidepanell2", CubeListBuilder.create().texOffs(114, 5).addBox(0f, 2.5f, -2.5f, 2f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.1396263f));
        parts.get("left_leg").addOrReplaceChild("sidepanell3", CubeListBuilder.create().texOffs(116, 13).addBox(2f, 2.5f, -2.5f, 1f, 4f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.1396263f));
        if (chest) {
            parts.get("body").addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4f, 1f, -3f, 8f, 7f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("frontcloth1", CubeListBuilder.create().texOffs(120, 39).addBox(0f, 0f, 0f, 6f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-3f, 11f, -3.5f, -0.1047198f, 0f, 0f));
            parts.get("body").addOrReplaceChild("frontcloth2", CubeListBuilder.create().texOffs(100, 37).addBox(0f, 7.5f, 1.8f, 6f, 3f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-3f, 11f, -3.5f, -0.3316126f, 0f, 0f));
            parts.get("body").addOrReplaceChild("tabbard", CubeListBuilder.create().texOffs(114, 52).addBox(-3f, 1.2f, -3.5f, 6f, 10f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4f, 1f, 2f, 8f, 11f, 2f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(0, 47).addBox(0f, 0f, 0f, 9f, 12f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-4.5f, 1.3f, 4.2f, 0.1396263f, 0f, 0f));
            parts.get("body").addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(0, 59).addBox(0f, 11.7f, -2f, 9f, 4f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-4.5f, 1.3f, 4.2f, 0.3069452f, 0f, 0f));
            parts.get("body").addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(0, 59).addBox(0f, 15.2f, -4.2f, 9f, 4f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-4.5f, 1.3f, 4.2f, 0.4465716f, 0f, 0f));
            parts.get("body").addOrReplaceChild("cloakatl", CubeListBuilder.create().texOffs(0, 43).addBox(2.5f, 1f, 2f, 2f, 1f, 3f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0.1396263f, 0f, 0f));
            parts.get("body").addOrReplaceChild("cloakatr", CubeListBuilder.create().texOffs(0, 43).addBox(-4.5f, 1f, 2f, 2f, 1f, 3f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0.1396263f, 0f, 0f));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
