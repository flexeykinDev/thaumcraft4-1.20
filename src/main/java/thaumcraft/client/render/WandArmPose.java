package thaumcraft.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Поза руки с работающим жезлом: рука вытянута вперёд и следует за взглядом.
 * Новую позу надо завести до того, как игра впервые нарисует модель игрока, поэтому она живёт в отдельном
 * классе, который загружается при старте клиента (см. ClientEvents.registerKeys).
 */
public final class WandArmPose {
    public static final HumanoidModel.ArmPose CASTING = HumanoidModel.ArmPose.create("THAUMCRAFT_WAND", false, (model, entity, arm) -> {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart part = right ? model.rightArm : model.leftArm;
        part.xRot = -Mth.HALF_PI * 0.85f + model.head.xRot;
        part.yRot = (right ? -0.1f : 0.1f) + model.head.yRot;
    });

    private WandArmPose() {
    }

    /** Ничего не делает: вызов нужен лишь затем, чтобы класс загрузился. */
    public static void init() {
    }
}
