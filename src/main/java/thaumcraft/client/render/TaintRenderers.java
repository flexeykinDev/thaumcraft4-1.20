package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.TaintSpider;
import thaumcraft.common.entity.TaintSpore;
import thaumcraft.common.entity.TaintSporeSwarmer;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Отрисовка порождений порчи: паучок, спора и рой спор (RenderTaintSpider, RenderTaintSpore, RenderTaintSporeSwarmer оригинала). */
public final class TaintRenderers {
    private static final ResourceLocation SPIDER = Thaumcraft.id("textures/models/taint_spider.png");
    private static final ResourceLocation SPIDER_EYES = Thaumcraft.id("textures/models/taint_spider_eyes.png");
    private static final ResourceLocation SPORE = Thaumcraft.id("textures/models/taint_spore.png");

    private TaintRenderers() {
    }

    private static ModelPart.Cube cube(int u, int v, float x, float y, float z, float w, float h, float d) {
        return new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, false, 64f, 64f, EnumSet.allOf(Direction.class));
    }

    /** Обычный паук, уменьшенный до двух пятых, с горящими глазами. */
    public static class Spider extends MobRenderer<TaintSpider, SpiderModel<TaintSpider>> {
        public Spider(EntityRendererProvider.Context context) {
            super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.1f);
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffer, int light, TaintSpider spider, float limbSwing,
                                   float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                    getParentModel().renderToBuffer(pose, buffer.getBuffer(RenderType.eyes(SPIDER_EYES)), 0xf000f0, OverlayTexture.NO_OVERLAY,
                            1f, 1f, 1f, 1f);
                }
            });
        }

        @Override
        protected void scale(TaintSpider spider, PoseStack pose, float partialTick) {
            pose.scale(0.4f, 0.5f, 0.4f);
        }

        @Override
        protected float getFlipDegrees(TaintSpider spider) {
            return 180f;
        }

        @Override
        public ResourceLocation getTextureLocation(TaintSpider spider) {
            return SPIDER;
        }
    }

    /** ModelTaintSpore оригинала: полупрозрачный пузырь с ядром внутри, слегка колышется. */
    private static class SporeModel extends EntityModel<TaintSpore> {
        private final ModelPart body = new ModelPart(List.of(cube(0, 0, -6f, 2f, -6f, 12f, 12f, 12f), cube(0, 0, -8f, 0f, -8f, 16f, 16f, 16f)), Map.of());

        SporeModel() {
            super(RenderType::entityTranslucent);
            body.setPos(0f, 24f, 0f);
        }

        @Override
        public void setupAnim(TaintSpore spore, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float intensity = spore.hurtTime > 0 ? 0.04f : 0.02f;
            body.xRot = intensity * Mth.sin(ageInTicks * 0.05f);
            body.zRot = intensity * Mth.sin(ageInTicks * 0.1f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            body.render(pose, consumer, light, overlay, r, g, b, a);
        }
    }

    public static class Spore extends MobRenderer<TaintSpore, EntityModel<TaintSpore>> {
        public Spore(EntityRendererProvider.Context context) {
            super(context, new SporeModel(), 0.25f);
        }

        /** Размер растёт вместе со спорой, а сама она дышит: то шире, то выше. */
        @Override
        protected void scale(TaintSpore spore, PoseStack pose, float partialTick) {
            float size = spore.displaySize;
            if (size < spore.getSporeSize()) {
                size += 0.02f * partialTick;
            }
            float pulse = 0.025f * Mth.sin(spore.tickCount * 0.075f);
            pose.scale(-0.12f * size - pulse, -0.12f * size + pulse, -0.12f * size - pulse);
        }

        @Override
        protected int getBlockLightLevel(TaintSpore spore, BlockPos pos) {
            return 15;
        }

        @Override
        protected float getFlipDegrees(TaintSpore spore) {
            return 0f;
        }

        @Override
        public ResourceLocation getTextureLocation(TaintSpore spore) {
            return SPORE;
        }
    }

    /** ModelTaintSporeSwarmer оригинала: плотное основание и светящийся пузырь над ним. */
    private static class SwarmerModel extends EntityModel<TaintSporeSwarmer> {
        private final ModelPart bubble = new ModelPart(List.of(cube(0, 0, -8f, 0f, -8f, 16f, 16f, 16f)), Map.of());
        private final ModelPart base = new ModelPart(List.of(cube(0, 32, -8f, -8f, -8f, 16f, 16f, 16f)), Map.of());
        private float size;
        private float pulse;

        SwarmerModel() {
            super(RenderType::entityTranslucent);
            base.setPos(0f, 16f, 0f);
        }

        @Override
        public void setupAnim(TaintSporeSwarmer spore, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                              float headPitch) {
            float intensity = spore.hurtTime > 0 ? 0.04f : 0.02f;
            bubble.xRot = intensity * Mth.sin(ageInTicks * 0.05f);
            bubble.zRot = intensity * Mth.sin(ageInTicks * 0.1f);
            size = spore.displaySize;
            pulse = 0.025f * Mth.sin(spore.tickCount * 0.075f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            pose.pushPose();
            pose.translate(0f, 1.6f, 0f);
            float sy = -0.07f * size + pulse;
            pose.scale(-0.07f * size - pulse, sy, -0.07f * size - pulse);
            pose.translate(0f, -sy / 2f, 0f);
            // пузырь светится сам, основание освещено как обычный блок
            bubble.render(pose, consumer, 0xf000f0, overlay, r, g, b, a);
            pose.popPose();
            base.render(pose, consumer, light, overlay, r, g, b, a);
        }
    }

    public static class Swarmer extends MobRenderer<TaintSporeSwarmer, EntityModel<TaintSporeSwarmer>> {
        public Swarmer(EntityRendererProvider.Context context) {
            super(context, new SwarmerModel(), 0.25f);
        }

        @Override
        protected float getFlipDegrees(TaintSporeSwarmer spore) {
            return 0f;
        }

        @Override
        public ResourceLocation getTextureLocation(TaintSporeSwarmer spore) {
            return SPORE;
        }
    }
}
