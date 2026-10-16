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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.armor.FortressArmorItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Крепостные доспехи — перенос ModelFortressArmor оригинала (коробки переведены tools/convert_armor.py). Украшения
 * растут с комплектом: со второй частью появляются наплечники, клапаны шлема и книга на поясе, с третьей — свиток,
 * самоцвет и рога на шлеме. На шлеме видны очки и выбранная маска.
 */
public class FortressArmorModel extends HumanoidModel<LivingEntity> {
    private static FortressArmorModel outer;
    private static FortressArmorModel inner;
    private final Map<String, ModelPart> parts = new HashMap<>();

    private FortressArmorModel(ModelPart root) {
        super(root);
        for (String limb : new String[]{"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            collect(root.getChild(limb));
        }
    }

    private void collect(ModelPart part) {
        for (String name : NAMES) {
            if (part.hasChild(name)) {
                parts.put(name, part.getChild(name));
            }
        }
    }

    /** Модель для шлема и кирасы (outer) или для поножей (inner — с другим поясом). */
    public static FortressArmorModel get(boolean legs) {
        if (legs) {
            if (inner == null) {
                inner = new FortressArmorModel(createLayer(true).bakeRoot());
            }
            return inner;
        }
        if (outer == null) {
            outer = new FortressArmorModel(createLayer(false).bakeRoot());
        }
        return outer;
    }

    private void hide(String name, boolean hidden) {
        ModelPart part = parts.get(name);
        if (part != null) {
            part.visible = !hidden;
        }
    }

    /** checkSet оригинала: сколько частей комплекта надето, какая маска и есть ли очки. */
    public void prepare(LivingEntity entity) {
        int set = 0;
        int mask = -1;
        boolean goggles = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack piece = entity.getItemBySlot(slot);
            if (piece.getItem() instanceof FortressArmorItem) {
                set++;
                if (slot == EquipmentSlot.HEAD) {
                    mask = FortressArmorItem.getMask(piece);
                    goggles = FortressArmorItem.hasGoggles(piece);
                }
            }
        }
        hide("goggles", !goggles);
        for (int a = 0; a < 3; a++) {
            hide("mask_" + a, mask != a);
        }
        for (String name : new String[]{"scroll", "ornamentl", "ornamentl2", "ornamentr", "ornamentr2", "gemornament", "gem",
                "shoulderplatel2", "shoulderplatel3", "shoulderplater2", "shoulderplater3", "sidepanelr3", "sidepanell3"}) {
            hide(name, set < 3);
        }
        for (String name : new String[]{"book", "flapl", "flapr", "shoulderplateltop", "shoulderplatel1", "shoulderplatertop",
                "shoulderplater1", "sidepanelr2", "sidepanell2"}) {
            hide(name, set < 2);
        }
    }

    /** Голова рисуется чуть крупнее, как в оригинале, чтобы шлем не сливался с головой. */
    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
        pose.pushPose();
        if (young) {
            pose.scale(0.75f, 0.75f, 0.75f);
            pose.translate(0f, 1f, 0f);
        } else {
            pose.scale(1.01f, 1.01f, 1.01f);
        }
        head.render(pose, consumer, light, overlay, r, g, b, a);
        pose.popPose();
        pose.pushPose();
        if (young) {
            pose.scale(0.5f, 0.5f, 0.5f);
            pose.translate(0f, 1.5f, 0f);
        }
        for (ModelPart part : new ModelPart[]{body, rightArm, leftArm, rightLeg, leftLeg, hat}) {
            part.render(pose, consumer, light, overlay, r, g, b, a);
        }
        pose.popPose();
    }

    private static final String[] NAMES = {"ornamentl", "ornamentl2", "ornamentr", "ornamentr2", "helmet", "helmetr", "helmetl", "helmetb", "capsthingy", "flapr", "flapl", "gemornament", "gem", "goggles", "mask_0", "mask_1", "mask_2", "mbelt", "mbeltl", "mbeltr", "beltr", "beltl", "chestplate", "scroll", "backplate", "book", "shoulderr", "gauntletr", "gauntletstrapr1", "gauntletstrapr2", "shoulderplatertop", "shoulderplater1", "shoulderplater2", "shoulderplater3", "shoulderl", "gauntletl", "gauntletstrapl1", "gauntletstrapl2", "shoulderplateltop", "shoulderplatel1", "shoulderplatel2", "shoulderplatel3", "legpanelr1", "legpanelr2", "legpanelr3", "legpanelr4", "legpanelr5", "legpanelr6", "sidepanelr1", "sidepanelr2", "sidepanelr3", "backpanelr1", "backpanelr2", "backpanelr3", "backpanell3", "legpanell1", "legpanell2", "legpanell3", "legpanell4", "legpanell5", "legpanell6", "sidepanell1", "sidepanell2", "sidepanell3", "backpanell1", "backpanell2"};

    public static LayerDefinition createLayer(boolean inner) {
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
        parts.get("head").addOrReplaceChild("ornamentl", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(1.5f, -9f, -6.5f, 2f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.1396263f, 0f, 0f));
        parts.get("head").addOrReplaceChild("ornamentl2", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(3.5f, -10f, -6.5f, 1f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.1396263f, 0f, 0f));
        parts.get("head").addOrReplaceChild("ornamentr", CubeListBuilder.create().texOffs(78, 8).addBox(-3.5f, -9f, -6.5f, 2f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.1396263f, 0f, 0f));
        parts.get("head").addOrReplaceChild("ornamentr2", CubeListBuilder.create().texOffs(78, 8).addBox(-4.5f, -10f, -6.5f, 1f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.1396263f, 0f, 0f));
        parts.get("head").addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).addBox(-4.5f, -9f, -4.5f, 9f, 4f, 9f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("head").addOrReplaceChild("helmetr", CubeListBuilder.create().texOffs(21, 13).addBox(-6.5f, -3f, -4.5f, 1f, 5f, 9f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.5235988f));
        parts.get("head").addOrReplaceChild("helmetl", CubeListBuilder.create().texOffs(21, 13).mirror().addBox(5.5f, -3f, -4.5f, 1f, 5f, 9f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.5235988f));
        parts.get("head").addOrReplaceChild("helmetb", CubeListBuilder.create().texOffs(41, 21).addBox(-4.5f, -3f, 5.5f, 9f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.5235988f, 0f, 0f));
        parts.get("head").addOrReplaceChild("capsthingy", CubeListBuilder.create().texOffs(21, 0).addBox(-4.5f, -6f, -6.5f, 9f, 1f, 2f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("head").addOrReplaceChild("flapr", CubeListBuilder.create().texOffs(59, 10).addBox(-10f, -2f, -1f, 3f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, -0.5235988f, 0.5235988f));
        parts.get("head").addOrReplaceChild("flapl", CubeListBuilder.create().texOffs(59, 10).mirror().addBox(7f, -2f, -1f, 3f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0.5235988f, -0.5235988f));
        parts.get("head").addOrReplaceChild("gemornament", CubeListBuilder.create().texOffs(68, 11).addBox(-1.5f, -9f, -7f, 3f, 3f, 2f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.1396263f, 0f, 0f));
        parts.get("head").addOrReplaceChild("gem", CubeListBuilder.create().texOffs(72, 8).addBox(-1f, -8.5f, -7.5f, 2f, 2f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.1396263f, 0f, 0f));
        parts.get("head").addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(100, 18).addBox(-4.5f, -5f, -4.25f, 9f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("head").addOrReplaceChild("mask_0", CubeListBuilder.create().texOffs(52, 2).addBox(-4.5f, -5f, -4.6f, 9f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("head").addOrReplaceChild("mask_1", CubeListBuilder.create().texOffs(76, 2).addBox(-4.5f, -5f, -4.6f, 9f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("head").addOrReplaceChild("mask_2", CubeListBuilder.create().texOffs(100, 2).addBox(-4.5f, -5f, -4.6f, 9f, 5f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        if (inner) {
            parts.get("body").addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).addBox(-4f, 8f, -3f, 8f, 4f, 1f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (inner) {
            parts.get("body").addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(76, 44).addBox(4f, 8f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (inner) {
            parts.get("body").addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5f, 8f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (!inner) {
            parts.get("body").addOrReplaceChild("beltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5f, 4f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (!inner) {
            parts.get("body").addOrReplaceChild("beltl", CubeListBuilder.create().texOffs(76, 44).addBox(4f, 4f, -3f, 1f, 3f, 6f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (!inner) {
            parts.get("body").addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4f, 1f, -4f, 8f, 7f, 2f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (!inner) {
            parts.get("body").addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(34, 27).addBox(-2f, 9.5f, 4f, 8f, 3f, 3f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.1919862f));
        }
        if (!inner) {
            parts.get("body").addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4f, 1f, 2f, 8f, 11f, 2f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        }
        if (!inner) {
            parts.get("body").addOrReplaceChild("book", CubeListBuilder.create().texOffs(100, 8).addBox(1f, -0.3f, 4f, 5f, 7f, 2f, new CubeDeformation(0f)),
                    PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.7679449f));
        }
        parts.get("right_arm").addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(56, 35).addBox(-3.5f, -2.5f, -2.5f, 5f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("gauntletr", CubeListBuilder.create().texOffs(100, 26).addBox(-3.5f, 3.5f, -2.5f, 2f, 6f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("gauntletstrapr1", CubeListBuilder.create().texOffs(84, 31).addBox(-1.5f, 3.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("gauntletstrapr2", CubeListBuilder.create().texOffs(84, 31).addBox(-1.5f, 6.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("right_arm").addOrReplaceChild("shoulderplatertop", CubeListBuilder.create().texOffs(110, 37).addBox(-5.5f, -2.5f, -3.5f, 2f, 1f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_arm").addOrReplaceChild("shoulderplater1", CubeListBuilder.create().texOffs(110, 45).addBox(-4.5f, -1.5f, -3.5f, 1f, 4f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_arm").addOrReplaceChild("shoulderplater2", CubeListBuilder.create().texOffs(94, 45).addBox(-3.5f, 1.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_arm").addOrReplaceChild("shoulderplater3", CubeListBuilder.create().texOffs(94, 45).addBox(-2.5f, 3.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5f, -2.5f, -2.5f, 5f, 5f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("gauntletl", CubeListBuilder.create().texOffs(114, 26).addBox(1.5f, 3.5f, -2.5f, 2f, 6f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("gauntletstrapl1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("gauntletstrapl2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3f, 1f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
        parts.get("left_arm").addOrReplaceChild("shoulderplateltop", CubeListBuilder.create().texOffs(110, 37).mirror().addBox(3.5f, -2.5f, -3.5f, 2f, 1f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatel1", CubeListBuilder.create().texOffs(110, 45).mirror().addBox(3.5f, -1.5f, -3.5f, 1f, 4f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatel2", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(2.5f, 1.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_arm").addOrReplaceChild("shoulderplatel3", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(1.5f, 3.5f, -3.5f, 1f, 3f, 7f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("right_leg").addOrReplaceChild("legpanelr1", CubeListBuilder.create().texOffs(0, 51).addBox(-1f, 0.5f, -3.5f, 3f, 4f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr2", CubeListBuilder.create().texOffs(8, 51).addBox(-1f, 3.5f, -2.5f, 3f, 4f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr3", CubeListBuilder.create().texOffs(0, 56).addBox(-1f, 6.5f, -1.5f, 3f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr4", CubeListBuilder.create().texOffs(0, 43).addBox(-3f, 0.5f, -3.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr5", CubeListBuilder.create().texOffs(0, 47).addBox(-3f, 2.5f, -2.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("legpanelr6", CubeListBuilder.create().texOffs(6, 43).addBox(-3f, 4.5f, -1.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(0, 22).addBox(-2.5f, 0.5f, -2.5f, 1f, 4f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr2", CubeListBuilder.create().texOffs(0, 31).addBox(-1.5f, 3.5f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_leg").addOrReplaceChild("sidepanelr3", CubeListBuilder.create().texOffs(12, 31).addBox(-0.5f, 5.5f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0.4363323f));
        parts.get("right_leg").addOrReplaceChild("backpanelr1", CubeListBuilder.create().texOffs(0, 18).addBox(-3f, 0.5f, 2.5f, 5f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("backpanelr2", CubeListBuilder.create().texOffs(0, 18).addBox(-3f, 2.5f, 1.5f, 5f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.4363323f, 0f, 0f));
        parts.get("right_leg").addOrReplaceChild("backpanelr3", CubeListBuilder.create().texOffs(0, 18).addBox(-3f, 4.5f, 0.5f, 5f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("backpanell3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2f, 4.5f, 0.5f, 5f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell1", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(-2f, 0.5f, -3.5f, 3f, 4f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell2", CubeListBuilder.create().texOffs(8, 51).mirror().addBox(-2f, 3.5f, -2.5f, 3f, 4f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell3", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2f, 6.5f, -1.5f, 3f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell4", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(1f, 0.5f, -3.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell5", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(1f, 2.5f, -2.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("legpanell6", CubeListBuilder.create().texOffs(6, 43).mirror().addBox(1f, 4.5f, -1.5f, 2f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(1.5f, 0.5f, -2.5f, 1f, 4f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_leg").addOrReplaceChild("sidepanell2", CubeListBuilder.create().texOffs(0, 31).mirror().addBox(0.5f, 3.5f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_leg").addOrReplaceChild("sidepanell3", CubeListBuilder.create().texOffs(12, 31).mirror().addBox(-0.5f, 5.5f, -2.5f, 1f, 3f, 5f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, -0.4363323f));
        parts.get("left_leg").addOrReplaceChild("backpanell1", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2f, 0.5f, 2.5f, 5f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.4363323f, 0f, 0f));
        parts.get("left_leg").addOrReplaceChild("backpanell2", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2f, 2.5f, 1.5f, 5f, 3f, 1f, new CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.4363323f, 0f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
