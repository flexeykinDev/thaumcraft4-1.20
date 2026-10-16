package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.items.golem.GolemCoreItem;
import thaumcraft.common.items.golem.GolemUpgradeItem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Голем — перенос ModelGolem и RenderGolemBase оригинала: у каждого материала своя текстура, на спине видны ядро и
 * улучшения, а раны проступают трещинами тем сильнее, чем меньше осталось здоровья.
 */
public class GolemRenderer extends MobRenderer<Golem, GolemRenderer.Model> {
    private static final ResourceLocation DAMAGE = Thaumcraft.id("textures/models/golem_damage.png");
    /** Ведро голема-водоноса: модель bucket.obj оригинала. */
    public static final ResourceLocation BUCKET = Thaumcraft.id("block/golem_bucket");
    private static final ResourceLocation UPGRADE_EMPTY = Thaumcraft.id("item/golem_upgrade_empty");
    private static final ResourceLocation[] SKINS = new ResourceLocation[8];
    private static final ResourceLocation[] CORES = new ResourceLocation[GolemCoreItem.NAMES.length];
    private static final ResourceLocation[] UPGRADES = new ResourceLocation[GolemUpgradeItem.NAMES.length];
    /** Текстуры ядер оригинала по номеру ядра. */
    private static final String[] CORE_TEXTURES = {"fill", "empty", "gather", "harvest", "guard", "liquid", "essentia", "lumber", "use",
            "butcher", "sorting", "fish"};

    static {
        String[] skins = {"straw", "wood", "tallow", "clay", "flesh", "stone", "iron", "thaumium"};
        for (int i = 0; i < skins.length; i++) {
            SKINS[i] = Thaumcraft.id("textures/models/golem_" + skins[i] + ".png");
        }
        for (int i = 0; i < CORES.length; i++) {
            CORES[i] = Thaumcraft.id("item/golem_core_" + CORE_TEXTURES[i]);
        }
        for (int i = 0; i < UPGRADES.length; i++) {
            UPGRADES[i] = Thaumcraft.id("item/golem_upgrade_" + GolemUpgradeItem.NAMES[i]);
        }
    }

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 0.25f);
        addLayer(new Badges(this));
        addLayer(new Carried(this, context.getItemRenderer()));
        addLayer(new Rod(this, context.getItemInHandRenderer()));
        addLayer(new Accessories(this));
        addLayer(new Cracks(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Golem golem) {
        return SKINS[golem.getGolemType().ordinal()];
    }

    /** На ходу голем переваливается с боку на бок. */
    @Override
    protected void setupRotations(Golem golem, PoseStack pose, float ageInTicks, float bodyYaw, float partialTick) {
        super.setupRotations(golem, pose, ageInTicks, bodyYaw, partialTick);
        float amount = golem.walkAnimation.speed(partialTick);
        if (amount >= 0.01f) {
            float swing = golem.walkAnimation.position(partialTick) + 6f;
            pose.mulPose(Axis.ZP.rotationDegrees(6.5f * Model.wave(swing, 13f)));
        }
    }

    /** Ядро и улучшения на спине голема. */
    private static class Badges extends RenderLayer<Golem, Model> {
        Badges(RenderLayerParent<Golem, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Golem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (golem.isInvisible()) {
                return;
            }
            int core = golem.getCore();
            if (core >= 0 && core < CORES.length) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(180f));
                pose.translate(0.0875f, -0.96f, 0.15f + (golem.getGolemDecoration().contains("P") ? 0.03f : 0f));
                pose.scale(0.175f, 0.175f, 0.175f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                quad(pose, buffer, CORES[core], light);
                pose.popPose();
            }
            int slots = golem.getUpgradeSlots();
            float shift = 0.08f;
            for (int slot = 0; slot < slots; slot++) {
                int upgrade = golem.getUpgrade(slot);
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(180f));
                pose.translate(-0.05f - shift * (slots - 1) / 2f + shift * slot, -1.106f, 0.099f);
                pose.scale(0.1f, 0.1f, 0.1f);
                quad(pose, buffer, upgrade >= 0 && upgrade < UPGRADES.length ? UPGRADES[upgrade] : UPGRADE_EMPTY, light);
                pose.popPose();
            }
        }

        /**
         * Значок 1×1 в плоскости XY, как renderItemIn2D оригинала. Берётся из общего атласа, а не из файла текстуры:
         * значки ядер анимированы, и в файле их кадры лежат столбиком.
         */
        private static void quad(PoseStack pose, MultiBufferSource buffer, ResourceLocation icon, int light) {
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(icon);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            vertex(consumer, matrix, normal, 0f, 0f, sprite.getU1(), sprite.getV1(), light);
            vertex(consumer, matrix, normal, 1f, 0f, sprite.getU0(), sprite.getV1(), light);
            vertex(consumer, matrix, normal, 1f, 1f, sprite.getU0(), sprite.getV0(), light);
            vertex(consumer, matrix, normal, 0f, 1f, sprite.getU1(), sprite.getV0(), light);
        }

        private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v, int light) {
            consumer.vertex(matrix, x, y, 0f).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(normal, 0f, 0f, 1f).endVertex();
        }
    }

    /** Ноша в руках голема: предмет, блок или ведро с жидкостью (renderCarriedItems оригинала). */
    private static class Carried extends RenderLayer<Golem, Model> {
        private final net.minecraft.client.renderer.entity.ItemRenderer items;

        Carried(RenderLayerParent<Golem, Model> parent, net.minecraft.client.renderer.entity.ItemRenderer items) {
            super(parent);
            this.items = items;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Golem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            net.minecraft.world.item.ItemStack carried = golem.getCarriedForDisplay();
            if (golem.isInvisible() || golem.deathTime > 0) {
                return;
            }
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (golem.getCore() == GolemCoreItem.LIQUID) {
                pose.pushPose();
                pose.scale(0.4f, 0.4f, 0.4f);
                pose.translate(0f, 3f, -1.1f);
                pose.mulPose(Axis.XP.rotationDegrees(90f));
                mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutoutNoCull(
                        net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)), null, mc.getModelManager().getModel(BUCKET), 1f, 1f, 1f,
                        light, OverlayTexture.NO_OVERLAY);
                net.minecraftforge.fluids.FluidStack fluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(carried)
                        .orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
                if (!fluid.isEmpty() && carried.hasTag()) {
                    // зеркало жидкости поднимается по мере наполнения ведра
                    int amount = carried.getTag().getInt(Golem.FLUID_AMOUNT);
                    float fill = (float) amount / Math.max(amount, golem.getFluidCarryLimit());
                    var extensions = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
                    var sprite = mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
                    int tint = extensions.getTintColor(fluid);
                    int glow = Math.max(light & 0xFFFF, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4) | light & 0xFFFF0000;
                    pose.translate(0f, 0f, 0.2f + 0.8f * fill);
                    pose.scale(0.8f, 0.8f, 0.8f);
                    VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
                    Matrix4f matrix = pose.last().pose();
                    Matrix3f normal = pose.last().normal();
                    float[][] corners = {{-0.5f, 0.5f, sprite.getU1(), sprite.getV1()}, {0.5f, 0.5f, sprite.getU0(), sprite.getV1()},
                            {0.5f, -0.5f, sprite.getU0(), sprite.getV0()}, {-0.5f, -0.5f, sprite.getU1(), sprite.getV0()}};
                    for (int pass = 0; pass < 2; pass++) {
                        for (int i = 0; i < 4; i++) {
                            float[] c = corners[pass == 0 ? i : 3 - i];
                            consumer.vertex(matrix, c[0], c[1], 0f).color(tint >> 16 & 255, tint >> 8 & 255, tint & 255, 255).uv(c[2], c[3])
                                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(glow).normal(normal, 0f, 0f, 1f).endVertex();
                        }
                    }
                }
                pose.popPose();
                return;
            }
            if (carried.isEmpty()) {
                return;
            }
            pose.pushPose();
            pose.scale(0.4f, 0.4f, 0.4f);
            pose.translate(0f, 2.5f, -1.25f);
            pose.mulPose(Axis.XP.rotationDegrees(180f));
            boolean block = items.getModel(carried, golem.level(), golem, 0).isGui3d();
            if (block) {
                // блок голем несёт перед собой целиком; банка алхимика тем больше, чем больше он может унести
                float size = golem.getCore() == GolemCoreItem.ALCHEMY ? 0.5f + Math.min(64, golem.getCarryLimit()) / 128f : 1f;
                pose.scale(2f * size, 2f * size, 2f * size);
            } else {
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            items.renderStatic(carried, net.minecraft.world.item.ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer,
                    golem.level(), golem.getId());
            pose.popPose();
        }
    }

    /** Удочка в правой руке голема-рыбака. */
    private static class Rod extends RenderLayer<Golem, Model> {
        private final net.minecraft.client.renderer.ItemInHandRenderer items;
        private final net.minecraft.world.item.ItemStack rod = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD);

        Rod(RenderLayerParent<Golem, Model> parent, net.minecraft.client.renderer.ItemInHandRenderer items) {
            super(parent);
            this.items = items;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Golem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (golem.getCore() != GolemCoreItem.FISHING || golem.isInvisible()) {
                return;
            }
            pose.pushPose();
            // модель голема уменьшена в два с половиной раза; удочка крепится к кисти правой руки
            pose.scale(0.4f, 0.4f, 0.4f);
            getParentModel().rightArm.translateAndRotate(pose);
            pose.translate(-10f / 16f, 21f / 16f, -1f / 16f);
            pose.mulPose(Axis.XP.rotationDegrees(-90f));
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.scale(1.6f, 1.6f, 1.6f);
            items.renderItem(golem, rod, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffer, light);
            pose.popPose();
        }
    }

    /**
     * Украшения и банка с мозгом у продвинутого голема — перенос ModelGolemAccessories оригинала. Головные уборы
     * поворачиваются вместе с головой, дротикомёт и булава — вместе с руками.
     */
    private static class Accessories extends RenderLayer<Golem, Model> {
        private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/golem_decoration.png");
        private static final float LIFT = 30f;
        private final ModelPart fez = head(Model.cube(0, 94, -4.5f, -15f, -6f, 9, 7, 9, 0f, false));
        private final ModelPart hat = head(Model.cube(0, 110, -4.5f, -17f, -6f, 9, 9, 9, 0f, false));
        private final ModelPart hatRim = head(Model.cube(36, 114, -6.5f, -9f, -8f, 13, 1, 13, 0f, false));
        private final ModelPart glasses = head(Model.cube(0, 80, -4.5f, -8f, -6f, 9, 4, 9, 0f, false));
        private final ModelPart visor = head(Model.cube(0, 70, -5f, -8f, -6f, 10, 5, 5, 0f, false));
        private final ModelPart jar = head(Model.cube(96, 56, -4f, -15f, -5.5f, 8, 4, 8, 0f, false));
        private final ModelPart brain = head(Model.cube(96, 70, -3.5f, -14f, -5f, 7, 3, 7, 0f, false));
        private final ModelPart evilHead = head(Model.cube(64, 65, -4f, -9f, -5.5f, 8, 7, 8, 0f, false));
        private final ModelPart plate = body(Model.cube(32, 40, -6.5f, -1f, -7f, 13, 12, 13, 0f, false));
        private final ModelPart plateLeft = body(Model.cube(0, 44, -8.5f, -4f, -6.5f, 3, 6, 12, 0f, false));
        private final ModelPart plateRight = body(Model.cube(0, 44, 5.5f, -4f, -6.5f, 3, 6, 12, 0f, true));
        private final ModelPart bowtie = body(Model.cube(0, 0, -8.5f, -2f, -6.5f, 17, 4, 12, 0f, false));
        private final ModelPart dartgun = body(Model.cube(80, 80, 7.9f, 7.5f, -3.5f, 6, 16, 7, 0f, false));
        private final ModelPart mace = body(Model.cube(80, 26, -13f, 15f, -5f, 6, 8, 10, 0f, false));

        Accessories(RenderLayerParent<Golem, Model> parent) {
            super(parent);
        }

        private static ModelPart head(ModelPart.Cube cube) {
            return Model.part(0f, LIFT, -2f, false, cube);
        }

        private static ModelPart body(ModelPart.Cube cube) {
            return Model.part(0f, LIFT, 0f, false, cube);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Golem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            String deco = golem.getGolemDecoration();
            boolean advanced = golem.isAdvanced();
            if (golem.isInvisible() || deco.isEmpty() && !advanced) {
                return;
            }
            Model model = getParentModel();
            for (ModelPart part : List.of(fez, hat, hatRim, glasses, visor, jar, brain, evilHead)) {
                part.xRot = model.head.xRot;
                part.yRot = model.head.yRot;
            }
            // оружие качается вместе с руками, но при ударе — вдвое медленнее, как в оригинале
            int timer = golem.getActionTimer();
            if (timer > 0) {
                dartgun.xRot = -2f + 1.5f * Model.wave(timer, 10f);
                mace.xRot = dartgun.xRot;
            } else if (golem.leftArm <= 0 && golem.rightArm <= 0) {
                if (!golem.getCarriedForDisplay().isEmpty()) {
                    dartgun.xRot = -1f;
                    mace.xRot = -1f;
                } else {
                    dartgun.xRot = (-0.2f - 1.5f * Model.wave(limbSwing, 13f)) * limbSwingAmount;
                    mace.xRot = (-0.2f + 1.5f * Model.wave(limbSwing, 13f)) * limbSwingAmount;
                }
            } else {
                if (golem.leftArm > 0) {
                    dartgun.xRot = -2f + 1.5f * Model.wave(golem.leftArm, 10f);
                }
                if (golem.rightArm > 0) {
                    mace.xRot = -2f + 1.5f * Model.wave(golem.rightArm, 10f);
                }
            }
            int overlay = LivingEntityRenderer.getOverlayCoords(golem, 0f);
            VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            pose.pushPose();
            pose.scale(0.4f, 0.4f, 0.4f);
            if (deco.contains("R")) {
                dartgun.render(pose, solid, light, overlay);
            }
            pose.pushPose();
            if (advanced) {
                // головной убор садится поверх банки с мозгом
                pose.translate(0f, -0.01f, 0f);
            }
            if (deco.contains("F")) {
                fez.render(pose, solid, light, overlay);
            }
            if (deco.contains("H")) {
                hat.render(pose, solid, light, overlay);
                hatRim.render(pose, solid, light, overlay);
            }
            pose.popPose();
            if (deco.contains("P")) {
                plate.render(pose, solid, light, overlay);
                plateLeft.render(pose, solid, light, overlay);
                plateRight.render(pose, solid, light, overlay);
            }
            if (deco.contains("G")) {
                glasses.render(pose, solid, light, overlay);
            }
            if (deco.contains("V")) {
                visor.render(pose, solid, light, overlay);
            }
            if (deco.contains("M")) {
                mace.render(pose, solid, light, overlay);
            }
            if (advanced) {
                brain.render(pose, solid, light, overlay);
                if (golem.getCore() >= 0) {
                    pose.pushPose();
                    pose.scale(1.01f, 1f, 1.01f);
                    evilHead.render(pose, solid, light, overlay);
                    pose.popPose();
                }
            }
            // полупрозрачное рисуется последним: бабочка и стекло банки
            VertexConsumer glass = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            if (deco.contains("B")) {
                bowtie.render(pose, glass, light, overlay);
            }
            if (advanced) {
                jar.render(pose, glass, light, overlay);
            }
            pose.popPose();
        }
    }

    /** Трещины на раненом големе. */
    private static class Cracks extends RenderLayer<Golem, Model> {
        Cracks(RenderLayerParent<Golem, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Golem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float health = golem.getHealthPercentage();
            if (health >= 1f || golem.isInvisible()) {
                return;
            }
            getParentModel().renderToBuffer(pose, buffer.getBuffer(RenderType.entityTranslucent(DAMAGE)), light,
                    LivingEntityRenderer.getOverlayCoords(golem, 0f), 1f, 1f, 1f, Mth.clamp(1f - health, 0f, 1f));
        }
    }

    /** ModelGolem оригинала: модель нарисована в два с половиной раза крупнее и уменьшается при отрисовке. */
    public static class Model extends EntityModel<Golem> {
        private static final float LIFT = 30f;
        final ModelPart head = part(0f, LIFT, -2f, false, cube(0, 0, -4f, -11f, -5.5f, 8, 9, 8, 0f, false));
        private final ModelPart body = part(0f, LIFT, 0f, false, cube(0, 40, -8f, -2f, -6f, 16, 12, 11, 0f, false),
                cube(0, 70, -4.5f, 10f, -3f, 9, 5, 6, 0.5f, false));
        final ModelPart rightArm = part(0f, LIFT, 0f, false, cube(60, 21, -12f, -2.5f, -3f, 4, 25, 6, 0f, false));
        private final ModelPart leftArm = part(0f, LIFT, 0f, true, cube(60, 21, 8f, -2.5f, -3f, 4, 25, 6, 0f, true));
        private final ModelPart rightLeg = part(-4f, 18f + LIFT, 0f, false, cube(37, 0, -3.5f, -3f, -3f, 6, 16, 5, 0f, false));
        private final ModelPart leftLeg = part(5f, 18f + LIFT, 0f, true, cube(37, 0, -3.5f, -3f, -3f, 6, 16, 5, 0f, true));
        private float red = 1f;
        private float green = 1f;
        private float blue = 1f;

        static ModelPart.Cube cube(int u, int v, float x, float y, float z, int w, int h, int d, float grow, boolean mirror) {
            return new ModelPart.Cube(u, v, x, y, z, w, h, d, grow, grow, grow, mirror, 128f, 128f, EnumSet.allOf(Direction.class));
        }

        static ModelPart part(float px, float py, float pz, boolean mirror, ModelPart.Cube... cubes) {
            ModelPart part = new ModelPart(new ArrayList<>(List.of(cubes)), Map.of());
            part.setPos(px, py, pz);
            return part;
        }

        /** Пилообразная волна шага оригинала: от -1 до 1 с периодом period. */
        static float wave(float value, float period) {
            value = (value % period + period) % period;
            return (Math.abs(value - period * 0.5f) - period * 0.25f) / (period * 0.25f);
        }

        @Override
        public void setupAnim(Golem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            int core = golem.getCore();
            // вспышка зелёным, когда голем восстанавливает здоровье
            if (golem.healing > 0) {
                red = Math.min(1f, 0.5f + golem.healing / 10f);
                green = 1f;
                blue = Math.min(1f, 0.5f + golem.healing / 10f);
            } else {
                red = green = blue = 1f;
            }
            leftArm.zRot = 0f;
            rightArm.zRot = 0f;
            rightLeg.yRot = 0f;
            leftLeg.yRot = 0f;
            if (core == -1 || golem.bootup < 0f) {
                // без ядра голем стоит, уронив голову
                head.yRot = 0f;
                head.xRot = 0.57595867f;
                rightLeg.xRot = 0f;
                leftLeg.xRot = 0f;
                rightArm.xRot = 0f;
                leftArm.xRot = 0f;
                return;
            }
            if (golem.inactive) {
                head.yRot = 0f;
                head.xRot = 0.57595867f;
            } else if (golem.bootup > 0f) {
                head.yRot = 0f;
                head.xRot = golem.bootup * Mth.DEG_TO_RAD;
            } else {
                head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
                head.xRot = headPitch * Mth.DEG_TO_RAD;
            }
            rightLeg.xRot = -1.5f * wave(limbSwing, 13f) * limbSwingAmount;
            leftLeg.xRot = 1.5f * wave(limbSwing, 13f) * limbSwingAmount;
            if (core == GolemCoreItem.ALCHEMY) {
                // банку с эссенцией голем держит, расставив руки по её размеру
                float spread = (1f - (0.5f + Math.min(64, golem.getCarryLimit()) / 128f)) * 25f;
                leftArm.zRot = spread * Mth.DEG_TO_RAD;
                rightArm.zRot = -spread * Mth.DEG_TO_RAD;
            }
            int timer = golem.getActionTimer();
            boolean holding = !golem.getCarriedForDisplay().isEmpty() || core == GolemCoreItem.LIQUID;
            if (timer > 0) {
                rightArm.xRot = -2f + 1.5f * wave(timer, 5f);
                leftArm.xRot = -2f + 1.5f * wave(timer, 5f);
            } else if (golem.leftArm <= 0 && golem.rightArm <= 0) {
                if (holding) {
                    rightArm.xRot = -1f;
                    leftArm.xRot = -1f;
                } else {
                    rightArm.xRot = (-0.2f + 1.5f * wave(limbSwing, 13f)) * limbSwingAmount;
                    leftArm.xRot = (-0.2f - 1.5f * wave(limbSwing, 13f)) * limbSwingAmount;
                }
            } else {
                if (golem.leftArm > 0) {
                    leftArm.xRot = -2f + 1.5f * wave(golem.leftArm, 20f);
                }
                if (golem.rightArm > 0) {
                    rightArm.xRot = -2f + 1.5f * wave(golem.rightArm, 20f);
                }
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            pose.pushPose();
            pose.scale(0.4f, 0.4f, 0.4f);
            for (ModelPart part : List.of(head, body, rightLeg, leftLeg, rightArm, leftArm)) {
                part.render(pose, consumer, light, overlay, r * red, g * green, b * blue, a);
            }
            pose.popPose();
        }
    }
}
