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
 * Пустотная мантия — перенос ModelRobe оригинала (коробки переведены tools/convert_armor.py). Капюшон, наплечники
 * и полы; у кирасы спереди нагрудник со свитком и книгой, у поножей — свисающие полы, которые колышутся при ходьбе.
 */
public class VoidRobeArmorModel extends HumanoidModel<LivingEntity> {
    private static VoidRobeArmorModel outer;
    private static VoidRobeArmorModel inner;
    private static final String[] CLOTH = {"frontclothr1", "frontclothl1", "frontclothr2", "frontclothl2", "clothbackr1", "clothbackl1",
            "clothbackr2", "clothbackl2", "clothbackr3", "clothbackl3"};
    private final Map<String, ModelPart> cloth = new HashMap<>();

    private VoidRobeArmorModel(ModelPart root) {
        super(root);
        for (String name : CLOTH) {
            if (body.hasChild(name)) {
                cloth.put(name, body.getChild(name));
            }
        }
    }

    /** Для кирасы — с нагрудником (outer, ModelRobe(1.0)); для капюшона и поножей — с полами (inner, ModelRobe(0.5)). */
    public static VoidRobeArmorModel get(boolean chest) {
        if (chest) {
            if (outer == null) {
                outer = new VoidRobeArmorModel(createLayer(true).bakeRoot());
            }
            return outer;
        }
        if (inner == null) {
            inner = new VoidRobeArmorModel(createLayer(false).bakeRoot());
        }
        return inner;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float a = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        float b = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.4f * limbSwingAmount;
        float c = Math.min(a, b);
        rotate("frontclothr1", c - 0.1047198f);
        rotate("frontclothl1", c - 0.1047198f);
        rotate("frontclothr2", c - 0.3316126f);
        rotate("frontclothl2", c - 0.3316126f);
        rotate("clothbackr1", -c + 0.1047198f);
        rotate("clothbackl1", -c + 0.1047198f);
        for (String name : new String[]{"clothbackr2", "clothbackl2", "clothbackr3", "clothbackl3"}) {
            rotate(name, -c + 0.2268928f);
        }
    }

    private void rotate(String name, float angle) {
        ModelPart part = cloth.get(name);
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
        parts.get("head").addOrReplaceChild("hood1", CubeListBuilder.create().texOffs(16, 7).addBox(-4.5f, -9f, -4.6f, 9f, 9f, 9f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("head").addOrReplaceChild("hood2", CubeListBuilder.create().texOffs(52, 13).addBox(-4f, -9.7f, 2f, 8f, 9f, 3f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.2268928f, 0f, 0f));
        parts.get("head").addOrReplaceChild("hood3", CubeListBuilder.create().texOffs(52, 14).addBox(-3.5f, -10f, 3.5f, 7f, 8f, 3f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.3490659f, 0f, 0f));
        parts.get("head").addOrReplaceChild("hood4", CubeListBuilder.create().texOffs(53, 15).addBox(-3f, -10.7f, 3.5f, 6f, 7f, 3f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.5759587f, 0f, 0f));
        parts.get("body").addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(16, 55).addBox(-4f, 7f, -3f, 8f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("body").addOrReplaceChild("mbeltb", CubeListBuilder.create().texOffs(16, 55).addBox(-4f, 7f, -4f, 8f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 3.141593f, 0f));
        parts.get("body").addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(16, 36).addBox(4f, 8f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("body").addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(16, 36).addBox(-5f, 8f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-3.5f, -2.5f, -2.5f, 5f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("rarm1", CubeListBuilder.create().texOffs(88, 39).addBox(-3.5f, 2.5f, -2.5f, 5f, 7f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("rarm2", CubeListBuilder.create().texOffs(76, 32).addBox(-3f, 5.5f, 2.5f, 4f, 4f, 2f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("rarm3", CubeListBuilder.create().texOffs(88, 32).addBox(-2.5f, 3.5f, 2.5f, 3f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("shoulderplatetopr", CubeListBuilder.create().texOffs(56, 25).addBox(-5.5f, -2.5f, -3.5f, 2f, 1f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_arm").addOrReplaceChild("shoulderplater1", CubeListBuilder.create().texOffs(56, 33).addBox(-4.5f, -1.5f, -3.5f, 1f, 4f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_arm").addOrReplaceChild("shoulderplater2", CubeListBuilder.create().texOffs(40, 33).addBox(-3.5f, 1.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_arm").addOrReplaceChild("shoulderplater3", CubeListBuilder.create().texOffs(40, 33).addBox(-2.5f, 3.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-1.5f, -2.5f, -2.5f, 5f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("larm1", CubeListBuilder.create().texOffs(88, 39).mirror().addBox(-1.5f, 2.5f, -2.5f, 5f, 7f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("larm2", CubeListBuilder.create().texOffs(76, 32).addBox(-1f, 5.5f, 2.5f, 4f, 4f, 2f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("larm3", CubeListBuilder.create().texOffs(88, 32).addBox(-0.5f, 3.5f, 2.5f, 3f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatetopl", CubeListBuilder.create().texOffs(56, 25).addBox(3.5f, -2.5f, -3.5f, 2f, 1f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatel1", CubeListBuilder.create().texOffs(56, 33).addBox(3.5f, -1.5f, -3.5f, 1f, 4f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatel2", CubeListBuilder.create().texOffs(40, 33).addBox(2.5f, 1.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatel3", CubeListBuilder.create().texOffs(40, 33).addBox(1.5f, 3.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("right_leg").addOrReplaceChild("legpanelr4", CubeListBuilder.create().texOffs(76, 38).addBox(-3f, 0.5f, -3.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr5", CubeListBuilder.create().texOffs(76, 42).addBox(-3f, 2.5f, -2.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr6", CubeListBuilder.create().texOffs(82, 38).addBox(-3f, 4.5f, -1.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(116, 25).mirror().addBox(-2.5f, 0.5f, -2.5f, 1f, 4f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_leg").addOrReplaceChild("sideclothr1", CubeListBuilder.create().texOffs(116, 42).addBox(-2.5f, 0.5f, -2.5f, 1f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.122173f));
        parts.get("right_leg").addOrReplaceChild("sideclothr2", CubeListBuilder.create().texOffs(116, 34).addBox(-1.5f, 5.5f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.296706f));
        parts.get("right_leg").addOrReplaceChild("sideclothr3", CubeListBuilder.create().texOffs(116, 1).addBox(0.4f, 8.4f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.5235988f));
        parts.get("left_leg").addOrReplaceChild("legpanell4", CubeListBuilder.create().texOffs(76, 38).mirror().addBox(1f, 0.5f, -3.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell5", CubeListBuilder.create().texOffs(76, 42).mirror().addBox(1f, 2.5f, -2.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell6", CubeListBuilder.create().texOffs(82, 38).mirror().addBox(1f, 4.5f, -1.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(116, 25).addBox(1.5f, 0.5f, -2.5f, 1f, 4f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_leg").addOrReplaceChild("sideclothl1", CubeListBuilder.create().texOffs(116, 42).addBox(1.5f, 0.5f, -2.5f, 1f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.122173f));
        parts.get("left_leg").addOrReplaceChild("sideclothl2", CubeListBuilder.create().texOffs(116, 34).addBox(0.5f, 5.5f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.296706f));
        parts.get("left_leg").addOrReplaceChild("sideclothl3", CubeListBuilder.create().texOffs(116, 1).addBox(-1.4f, 8.4f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.5235988f));
        if (chest) {
            parts.get("body").addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(16, 25).addBox(-4f, 1f, -3f, 8f, 6f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("chestthing", CubeListBuilder.create().texOffs(56, 50).addBox(-2.5f, 1f, -4f, 5f, 7f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(78, 25).addBox(-2f, 9.5f, 4f, 8f, 3f, 3f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.1919862f));
            parts.get("body").addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4f, 1f, 1.9f, 8f, 11f, 2f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("book", CubeListBuilder.create().texOffs(81, 16).addBox(1f, 0f, 4f, 5f, 7f, 2f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.7679449f));
            parts.get("body").addOrReplaceChild("clothchestl", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(2.1f, 0.5f, -3.5f, 2f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothchestr", CubeListBuilder.create().texOffs(108, 38).addBox(-4.1f, 0.5f, -3.5f, 2f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        } else {
            parts.get("left_leg").addOrReplaceChild("focipouch", CubeListBuilder.create().texOffs(100, 20).addBox(3.5f, 0.5f, -2.5f, 3f, 6f, 5f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.122173f));
            parts.get("body").addOrReplaceChild("frontclothr1", CubeListBuilder.create().texOffs(108, 38).addBox(0f, 0f, 0f, 3f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-3f, 11f, -2.9f, -0.1047198f, 0f, 0f));
            parts.get("body").addOrReplaceChild("frontclothr2", CubeListBuilder.create().texOffs(108, 47).addBox(0f, 7.5f, 1.7f, 3f, 3f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-3f, 11f, -2.9f, -0.3316126f, 0f, 0f));
            parts.get("body").addOrReplaceChild("frontclothl1", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(0f, 0f, 0f, 3f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 11f, -2.9f, -0.1047198f, 0f, 0f));
            parts.get("body").addOrReplaceChild("frontclothl2", CubeListBuilder.create().texOffs(108, 47).mirror().addBox(0f, 7.5f, 1.7f, 3f, 3f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 11f, -2.9f, -0.3316126f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothbackr1", CubeListBuilder.create().texOffs(118, 16).mirror().addBox(0f, 0f, 0f, 4f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-4f, 11.5f, 2.9f, 0.1047198f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothbackr2", CubeListBuilder.create().texOffs(123, 9).addBox(0f, 7.8f, -0.9f, 1f, 2f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-4f, 11.5f, 2.9f, 0.2268928f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothbackr3", CubeListBuilder.create().texOffs(120, 12).mirror().addBox(1f, 7.8f, -0.9f, 3f, 3f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(-4f, 11.5f, 2.9f, 0.2268928f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothbackl1", CubeListBuilder.create().texOffs(118, 16).addBox(0f, 0f, 0f, 4f, 8f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 11.5f, 2.9f, 0.1047198f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothbackl2", CubeListBuilder.create().texOffs(123, 9).mirror().addBox(3f, 7.8f, -0.9f, 1f, 2f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 11.5f, 2.9f, 0.2268928f, 0f, 0f));
            parts.get("body").addOrReplaceChild("clothbackl3", CubeListBuilder.create().texOffs(120, 12).addBox(0f, 7.8f, -0.9f, 3f, 3f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 11.5f, 2.9f, 0.2268928f, 0f, 0f));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
