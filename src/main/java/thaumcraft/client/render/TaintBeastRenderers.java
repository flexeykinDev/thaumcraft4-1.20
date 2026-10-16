package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.TaintCreeper;
import thaumcraft.common.entity.Taintacle;
import thaumcraft.common.entity.TaintedBeasts;
import thaumcraft.common.entity.ThaumicSlime;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Отрисовка порченых животных, жителя, крипера, тентаклей и таумического слизня (RenderTaintCow, RenderTaintPig,
 * RenderTaintSheep, RenderTaintChicken, RenderTaintVillager, RenderTaintCreeper, RenderTaintacle и
 * RenderThaumicSlime оригинала). Животные рисуются обычными моделями игры со своими, порчеными, текстурами.
 */
public final class TaintBeastRenderers {
    private static ResourceLocation texture(String name) {
        return Thaumcraft.id("textures/models/" + name + ".png");
    }

    private TaintBeastRenderers() {
    }

    // ---------- корова, свинья, курица ----------

    public static class Cow extends MobRenderer<TaintedBeasts.Cow, CowModel<TaintedBeasts.Cow>> {
        private static final ResourceLocation TEXTURE = texture("cow");

        public Cow(EntityRendererProvider.Context context) {
            super(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), 0.7f);
        }

        @Override
        public ResourceLocation getTextureLocation(TaintedBeasts.Cow cow) {
            return TEXTURE;
        }
    }

    public static class Pig extends MobRenderer<TaintedBeasts.Pig, PigModel<TaintedBeasts.Pig>> {
        private static final ResourceLocation TEXTURE = texture("pig");

        public Pig(EntityRendererProvider.Context context) {
            super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.7f);
        }

        @Override
        public ResourceLocation getTextureLocation(TaintedBeasts.Pig pig) {
            return TEXTURE;
        }
    }

    public static class Chicken extends MobRenderer<TaintedBeasts.Chicken, ChickenModel<TaintedBeasts.Chicken>> {
        private static final ResourceLocation TEXTURE = texture("chicken");

        public Chicken(EntityRendererProvider.Context context) {
            super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.3f);
        }

        /** Взмах крыльев. */
        @Override
        protected float getBob(TaintedBeasts.Chicken chicken, float partialTick) {
            float flap = Mth.lerp(partialTick, chicken.oFlap, chicken.flap);
            float speed = Mth.lerp(partialTick, chicken.oFlapSpeed, chicken.flapSpeed);
            return (Mth.sin(flap) + 1f) * speed;
        }

        @Override
        public ResourceLocation getTextureLocation(TaintedBeasts.Chicken chicken) {
            return TEXTURE;
        }
    }

    // ---------- овца ----------

    /** ModelTaintSheep1 и ModelTaintSheep2 оригинала: обычная овца и её шерсть, голова опускается, когда овца пасётся. */
    private static class SheepModel extends QuadrupedModel<TaintedBeasts.Sheep> {
        private float headXRot;

        SheepModel(ModelPart root) {
            super(root, false, 8f, 4f, 2f, 2f, 24);
        }

        @Override
        public void prepareMobModel(TaintedBeasts.Sheep sheep, float limbSwing, float limbSwingAmount, float partialTick) {
            super.prepareMobModel(sheep, limbSwing, limbSwingAmount, partialTick);
            head.y = 6f + sheep.getHeadEatPositionScale(partialTick) * 9f;
            headXRot = sheep.getHeadEatAngleScale(partialTick);
        }

        @Override
        public void setupAnim(TaintedBeasts.Sheep sheep, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                              float headPitch) {
            super.setupAnim(sheep, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            head.xRot = headXRot;
        }
    }

    public static class Sheep extends MobRenderer<TaintedBeasts.Sheep, SheepModel> {
        private static final ResourceLocation TEXTURE = texture("sheep");
        private static final ResourceLocation FUR = texture("sheep_fur");

        public Sheep(EntityRendererProvider.Context context) {
            super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)), 0.7f);
            SheepModel fur = new SheepModel(context.bakeLayer(ModelLayers.SHEEP_FUR));
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffer, int light, TaintedBeasts.Sheep sheep, float limbSwing,
                                   float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                    if (sheep.isSheared() || sheep.isInvisible()) {
                        return;
                    }
                    getParentModel().copyPropertiesTo(fur);
                    fur.prepareMobModel(sheep, limbSwing, limbSwingAmount, partialTick);
                    fur.setupAnim(sheep, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                    fur.renderToBuffer(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(FUR)), light,
                            LivingEntityRenderer.getOverlayCoords(sheep, 0f), 1f, 1f, 1f, 1f);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(TaintedBeasts.Sheep sheep) {
            return TEXTURE;
        }
    }

    // ---------- житель ----------

    public static class Villager extends MobRenderer<TaintedBeasts.Villager, VillagerModel<TaintedBeasts.Villager>> {
        private static final ResourceLocation TEXTURE = texture("villager");

        public Villager(EntityRendererProvider.Context context) {
            super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
            // текстура оригинала нарисована под старую модель жителя, без шляпы
            getModel().root().getChild("head").getChild("hat").visible = false;
        }

        @Override
        protected void scale(TaintedBeasts.Villager villager, PoseStack pose, float partialTick) {
            pose.scale(0.9375f, 0.9375f, 0.9375f);
        }

        @Override
        public ResourceLocation getTextureLocation(TaintedBeasts.Villager villager) {
            return TEXTURE;
        }
    }

    // ---------- крипер ----------

    public static class Creeper extends MobRenderer<TaintCreeper, CreeperModel<TaintCreeper>> {
        private static final ResourceLocation TEXTURE = texture("creeper");

        public Creeper(EntityRendererProvider.Context context) {
            super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5f);
        }

        /** Перед взрывом раздувается и дрожит. */
        @Override
        protected void scale(TaintCreeper creeper, PoseStack pose, float partialTick) {
            float swelling = creeper.getSwelling(partialTick);
            float wobble = 1f + Mth.sin(swelling * 100f) * swelling * 0.01f;
            swelling = Mth.clamp(swelling, 0f, 1f);
            swelling *= swelling;
            swelling *= swelling;
            float wide = (1f + swelling * 0.4f) * wobble;
            float tall = (1f + swelling * 0.1f) / wobble;
            pose.scale(wide, tall, wide);
        }

        /** И мигает белым. */
        @Override
        protected float getWhiteOverlayProgress(TaintCreeper creeper, float partialTick) {
            float swelling = creeper.getSwelling(partialTick);
            return (int) (swelling * 10f) % 2 == 0 ? 0f : Mth.clamp(swelling, 0.5f, 1f);
        }

        @Override
        public ResourceLocation getTextureLocation(TaintCreeper creeper) {
            return TEXTURE;
        }
    }

    // ---------- тентакль ----------

    private static ModelPart box(int u, int v, float half) {
        return new ModelPart(List.of(new ModelPart.Cube(u, v, -half, -half, -half, half * 2f, half * 2f, half * 2f, 0f, 0f, 0f, false,
                64f, 64f, EnumSet.allOf(Direction.class))), Map.of());
    }

    /**
     * ModelTaintacle оригинала: цепочка кубов, каждый следующий чуть меньше предыдущего и качается относительно него.
     * На конце — светящийся шар и крупная «голова»; они светятся сами, остальное освещено как обычно.
     */
    private static class TaintacleModel extends EntityModel<Taintacle> {
        private static final float STEP = 0.88f;
        private final ModelPart root = box(0, 0, 4f);
        private final ModelPart segment = box(0, 16, 4f);
        private final ModelPart orb = box(0, 56, 2f);
        private final ModelPart head = box(0, 32, 6f);
        private final int length;
        private final float[] bendX;
        private final float[] bendZ;
        private float height;
        private float rise;

        TaintacleModel(int length) {
            super(RenderType::entityTranslucent);
            this.length = length;
            this.bendX = new float[length - 1];
            this.bendZ = new float[length - 1];
        }

        @Override
        public void prepareMobModel(Taintacle taintacle, float limbSwing, float limbSwingAmount, float partialTick) {
            height = taintacle.getBbHeight();
            float growTime = height * 10f;
            float age = taintacle.tickCount + partialTick;
            // первые секунды щупальце поднимается из земли
            rise = age < growTime ? (growTime - age) / growTime * height : 0f;
        }

        @Override
        public void setupAnim(Taintacle taintacle, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean agitated = taintacle.isAgitated();
            float mod = 0.0125f;
            float speed = agitated ? 3f : 1f - mod;
            float sway = taintacle.flailIntensity + (taintacle.hurtTime > 0 || taintacle.swinging ? mod : -mod);
            for (int k = 0; k < length - 1; k++) {
                bendX[k] = 0.15f * sway * Mth.sin(ageInTicks * 0.1f * speed - k / 2f);
                bendZ[k] = 0.1f / sway * Mth.sin(ageInTicks * 0.15f - k / 2f);
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            pose.pushPose();
            pose.translate(0f, (height == 3f ? 0.6f : 1.2f) + rise, 0f);
            float scale = height / 3f;
            pose.scale(scale, scale, scale);
            pose.translate(0f, 0.75f, 0f);
            root.render(pose, consumer, light, overlay, r, g, b, a);
            chain(0, pose, consumer, light, overlay, r, g, b, a);
            pose.popPose();
        }

        private void chain(int k, PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            pose.pushPose();
            pose.scale(STEP, STEP, STEP);
            pose.translate(0f, -0.5f, 0f);
            pose.mulPose(Axis.ZP.rotation(bendZ[k]));
            pose.mulPose(Axis.XP.rotation(bendX[k]));
            segment.render(pose, consumer, light, overlay, r, g, b, a);
            if (k < length - 2) {
                chain(k + 1, pose, consumer, light, overlay, r, g, b, a);
            } else {
                pose.scale(STEP, STEP, STEP);
                pose.translate(0f, -0.5f, 0f);
                orb.render(pose, consumer, 0xf000f0, overlay, r, g, b, a);
                head.render(pose, consumer, 0xf000f0, overlay, r, g, b, a);
            }
            pose.popPose();
        }
    }

    public static class TaintacleRenderer extends MobRenderer<Taintacle, EntityModel<Taintacle>> {
        private static final ResourceLocation TEXTURE = texture("taintacle");

        private final float scale;

        public TaintacleRenderer(EntityRendererProvider.Context context, float shadow, int length) {
            this(context, shadow, length, 1f);
        }

        public TaintacleRenderer(EntityRendererProvider.Context context, float shadow, int length, float scale) {
            super(context, new TaintacleModel(length), shadow);
            this.scale = scale;
        }

        /** Гигантское щупальце хранителя: длиннее и в полтора раза толще (RenderTaintacle(1.0, 14) и 1,33). */
        public static TaintacleRenderer giant(EntityRendererProvider.Context context) {
            return new TaintacleRenderer(context, 1.0f, 14, 1.33f);
        }

        @Override
        protected void scale(Taintacle taintacle, PoseStack pose, float partialTick) {
            if (scale != 1f) {
                pose.scale(scale, scale, scale);
            }
        }

        public static TaintacleRenderer big(EntityRendererProvider.Context context) {
            return new TaintacleRenderer(context, 0.6f, 10);
        }

        public static TaintacleRenderer small(EntityRendererProvider.Context context) {
            return new TaintacleRenderer(context, 0.2f, 6);
        }

        @Override
        protected float getFlipDegrees(Taintacle taintacle) {
            return 0f;
        }

        @Override
        public ResourceLocation getTextureLocation(Taintacle taintacle) {
            return TEXTURE;
        }
    }

    // ---------- слизень ----------

    public static class Slime extends MobRenderer<ThaumicSlime, SlimeModel<ThaumicSlime>> {
        private static final ResourceLocation TEXTURE = texture("tslime");

        public Slime(EntityRendererProvider.Context context) {
            super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25f);
            addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));
        }

        @Override
        public void render(ThaumicSlime slime, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
            this.shadowRadius = 0.25f * (float) Math.sqrt(slime.getSlimeSize());
            super.render(slime, yaw, partialTick, pose, buffer, light);
        }

        /** Размер растёт как корень из величины слизня; в прыжке вытягивается, приземлившись — плющится. */
        @Override
        protected void scale(ThaumicSlime slime, PoseStack pose, float partialTick) {
            pose.scale(0.999f, 0.999f, 0.999f);
            pose.translate(0f, 0.001f, 0f);
            float root = (float) Math.sqrt(slime.getSlimeSize());
            float squish = Mth.lerp(partialTick, slime.oSquish, slime.squish) / (root * 0.25f + 1f);
            float flat = 1f / (squish + 1f);
            pose.scale(flat * root + 0.1f, 1f / flat * root + 0.1f, flat * root + 0.1f);
        }

        @Override
        public ResourceLocation getTextureLocation(ThaumicSlime slime) {
            return TEXTURE;
        }
    }
}
