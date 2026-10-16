"""Строит Java-модели брони культистов из моделей 1.7.10 (ModelKnightArmor, ModelLeaderArmor).

Запуск из корня проекта:  python tools/gen_armor_models.py

Коробки переводит tools/convert_armor.py. Детали, которые в оригинале добавлялись только в модель кирасы
(блок if (!(f < 1.0F)) или if (f < 1.0F) ... else), попадают в соответствующую половину; колыхание плаща и пол
при ходьбе переносится формулами из render() оригинала.
"""
import glob
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import convert_armor as ca  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'java', 'thaumcraft', 'client', 'render')

MODELS = {
    'CultistKnightArmorModel': ('ModelKnightArmor', 'Латы рыцаря культа (ModelKnightArmor оригинала): шлем, наплечники, '
                                'латные рукавицы, нагрудник с табардом и плащ.', [
        ('frontcloth1', 'c - 0.1047198f'), ('frontcloth2', 'c - 0.3316126f'),
        ('cloak1', '-c / 2f + 0.1396263f'), ('cloak2', '-c / 2f + 0.3069452f'), ('cloak3', '-c / 2f + 0.4465716f')]),
    'CultistLeaderArmorModel': ('ModelLeaderArmor', 'Латы предводителя культа (ModelLeaderArmor оригинала): рогатый шлем, '
                                'воротник, украшенный нагрудник, набедренные полотнища и плащ.', [
        ('legclothr', 'a - 0.1047198f'), ('legclothl', 'b - 0.1047198f'),
        ('cloak1', '-c / 2f + 0.1396263f'), ('cloak2', '-c / 2f + 0.3069452f'), ('cloak3', '-c / 2f + 0.4465716f')]),
}

TEMPLATE = '''package thaumcraft.client.render;

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
 * %(doc)s Коробки переведены tools/gen_armor_models.py. Для кирасы — модель с нагрудником (outer), для шлема
 * и поножей — без него (inner).
 */
public class %(cls)s extends HumanoidModel<LivingEntity> {
    private static %(cls)s outer;
    private static %(cls)s inner;
    private final Map<String, ModelPart> swinging = new HashMap<>();

    private %(cls)s(ModelPart root) {
        super(root);
        for (String name : new String[]{%(names)s}) {
            if (body.hasChild(name)) {
                swinging.put(name, body.getChild(name));
            }
        }
    }

    public static %(cls)s get(boolean chest) {
        if (chest) {
            if (outer == null) {
                outer = new %(cls)s(createLayer(true).bakeRoot());
            }
            return outer;
        }
        if (inner == null) {
            inner = new %(cls)s(createLayer(false).bakeRoot());
        }
        return inner;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float a = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        float b = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.4f * limbSwingAmount;
        float c = Math.min(a, b);
%(rotations)s
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
%(common)s
        if (chest) {
%(outer)s
        }%(inner_block)s
        return LayerDefinition.create(mesh, 128, 64);
    }
}
'''


def indent(code):
    return '\n'.join('    ' + line for line in code.splitlines())


def build(cls, source_name, doc, rotations):
    path = glob.glob(os.path.join(ROOT, 'reference', '**', source_name + '.java'), recursive=True)[0]
    text = open(path, encoding='utf-8').read()
    parts, children = ca.parse(text)
    outer_only, inner_only = set(), set()
    m = re.search(r'if \(!\(f < 1\.0F\)\) \{(.*?)\n      \}', text, re.S)
    if m:
        outer_only = set(re.findall(r'addChild\(this\.(\w+)\)', m.group(1)))
    m = re.search(r'if \(f < 1\.0F\) \{(.*?)\} else \{(.*?)\n      \}', text, re.S)
    if m:
        inner_only = set(re.findall(r'addChild\(this\.(\w+)\)', m.group(1)))
        outer_only = set(re.findall(r'addChild\(this\.(\w+)\)', m.group(2)))
    common = [c for c in children if c[1] not in outer_only and c[1] not in inner_only]
    outer = [c for c in children if c[1] in outer_only]
    inner = [c for c in children if c[1] in inner_only]
    inner_block = ''
    if inner:
        inner_block = ' else {\n%s\n        }' % indent(ca.java(parts, inner))
    java = TEMPLATE % {
        'cls': cls, 'doc': doc,
        'names': ', '.join('"%s"' % n for n, _ in rotations),
        'rotations': '\n'.join('        rotate("%s", %s);' % (n, f) for n, f in rotations),
        'common': ca.java(parts, common), 'outer': indent(ca.java(parts, outer)), 'inner_block': inner_block,
    }
    with open(os.path.join(OUT, cls + '.java'), 'w', encoding='utf-8') as fh:
        fh.write(java)
    print(cls, 'деталей:', len(children), 'общих', len(common), 'кираса', len(outer), 'поножи', len(inner))


if __name__ == '__main__':
    for name, (source, doc, rot) in MODELS.items():
        build(name, source, doc, rot)
