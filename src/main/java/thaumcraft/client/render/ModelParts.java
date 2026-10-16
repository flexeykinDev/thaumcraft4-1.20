package thaumcraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Детали моделей существ, переведённых из 1.7.10 (tools/convert_model.py): ModelRenderer оригинала с точкой вращения,
 * начальным поворотом и одной или несколькими коробками. Начальная поза запоминается, её возвращает {@link ModelPart#resetPose()}.
 */
public final class ModelParts {
    /** Дочерние детали (addChild оригинала) — у ModelPart список детей закрыт, поэтому держим его здесь. */
    private static final Map<ModelPart, Map<String, ModelPart>> CHILDREN = new WeakHashMap<>();

    private ModelParts() {
    }

    /** addChild оригинала: деталь child двигается вместе с parent. */
    public static void attach(ModelPart parent, String name, ModelPart child) {
        CHILDREN.get(parent).put(name, child);
    }

    /** boxes — по семь чисел на коробку: x, y, z, ширина, высота, глубина, утолщение. */
    public static ModelPart part(int u, int v, boolean mirror, float texWidth, float texHeight, float px, float py, float pz,
                                 float rx, float ry, float rz, float... boxes) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        for (int i = 0; i + 6 < boxes.length; i += 7) {
            float grow = boxes[i + 6];
            cubes.add(new ModelPart.Cube(u, v, boxes[i], boxes[i + 1], boxes[i + 2], boxes[i + 3], boxes[i + 4], boxes[i + 5], grow, grow, grow,
                    mirror, texWidth, texHeight, EnumSet.allOf(Direction.class)));
        }
        Map<String, ModelPart> children = new HashMap<>();
        ModelPart part = new ModelPart(cubes, children);
        CHILDREN.put(part, children);
        part.setPos(px, py, pz);
        part.xRot = rx;
        part.yRot = ry;
        part.zRot = rz;
        part.setInitialPose(PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
        return part;
    }
}
