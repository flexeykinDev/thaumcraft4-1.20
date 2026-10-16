package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ManaPodBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Стручок маны — перенос TileManaPodRenderer оригинала. С третьей стадии под черенком висит полупрозрачная оболочка,
 * которая растёт и постепенно окрашивается в цвет своего аспекта, а внутри неё пульсирует светящееся зерно.
 */
public class ManaPodRenderer implements BlockEntityRenderer<ManaPodBlock.Pod> {
    private static final ResourceLocation SEED = Thaumcraft.id("textures/models/manapod_0.png");
    private static final ResourceLocation SHELL = Thaumcraft.id("textures/models/manapod_2.png");
    private final ModelPart seed = box(-2f, 0f, -2f, 4f, 5f, 4f);
    private final ModelPart shell = box(-3.5f, 0f, -3.5f, 7f, 9f, 7f);

    public ManaPodRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart box(float x, float y, float z, float w, float h, float d) {
        return new ModelPart(List.of(new ModelPart.Cube(0, 0, x, y, z, w, h, d, 0f, 0f, 0f, true, 32f, 32f, EnumSet.allOf(Direction.class))), Map.of());
    }

    @Override
    public void render(ManaPodBlock.Pod pod, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        int age = pod.getAge();
        if (age <= 1) {
            return;
        }
        // незрелый стручок бирюзовый; чем он старше, тем ближе к цвету своего аспекта
        float r = 0.14509805f;
        float g = 0.6156863f;
        float b = 0.45882353f;
        if (pod.aspect != null) {
            int color = pod.aspect.getColor();
            float ar = (color >> 16 & 255) / 255f;
            float ag = (color >> 8 & 255) / 255f;
            float ab = (color & 255) / 255f;
            if (age == 7) {
                r = ar;
                g = ag;
                b = ab;
            } else {
                float m = age - 2;
                r = (r + ar * m) / (m + 1f);
                g = (g + ag * m) / (m + 1f);
                b = (b + ab * m) / (m + 1f);
            }
        }
        pose.pushPose();
        pose.translate(0.5, 0.75, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        if (age > 2) {
            float time = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount + partialTick;
            float pulse = Mth.sin((time + pod.getBlockPos().hashCode() % 100) / 8f) * 0.1f + 0.9f;
            pose.pushPose();
            pose.translate(0.0, 0.1, 0.0);
            float size = 0.125f * age * pulse;
            pose.scale(size, size, size);
            // зерно светится само, тем ярче, чем стручок старше
            int glow = Math.min(240, age * 10 + (int) (150f * pulse));
            seed.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(SEED)), glow | glow << 16, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        float size = 0.15f * age;
        pose.scale(size, size, size);
        shell.render(pose, buffer.getBuffer(RenderType.entityTranslucent(SHELL)), light, OverlayTexture.NO_OVERLAY, r, g, b, 0.9f);
        pose.popPose();
    }
}
