package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.CultistCleric;
import thaumcraft.common.entity.CultistLeader;
import thaumcraft.common.entity.GolemOrb;
import thaumcraft.common.entity.InhabitedZombie;

import java.util.Random;

/**
 * Рисование культа: RenderCultist (рыцарь, клирик, предводитель — в полтора раза крупнее), RenderInhabitedZombie
 * и RenderElectricOrb (сгусток) оригинала.
 */
public final class CultistRenderers {
    private CultistRenderers() {
    }

    private static HumanoidModel<Mob> biped(float grow) {
        return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(grow), 0f), 64, 32).bakeRoot());
    }

    /** Культист: обычный человек со скином культиста; клирик-обрядник парит и тянет к алтарю луч. */
    public static class Cultist extends HumanoidMobRenderer<Mob, HumanoidModel<Mob>> {
        private static final ResourceLocation SKIN = Thaumcraft.id("textures/models/cultist.png");

        public Cultist(EntityRendererProvider.Context context) {
            super(context, biped(0f), 0.5f);
            addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        }

        @Override
        public ResourceLocation getTextureLocation(Mob mob) {
            return SKIN;
        }

        @Override
        protected void scale(Mob mob, PoseStack pose, float partialTick) {
            if (mob instanceof CultistLeader) {
                pose.scale(1.25f, 1.25f, 1.25f);
            }
        }

        @Override
        public void render(Mob mob, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
            pose.pushPose();
            if (mob instanceof CultistCleric cleric && cleric.isRitualist()) {
                int offset = new Random(mob.getId()).nextInt(1000);
                float bob = Mth.sin((mob.tickCount + partialTick + offset) / 9f) * 0.1f + 0.21f;
                pose.translate(0.0, bob, 0.0);
            }
            super.render(mob, yaw, partialTick, pose, buffer, light);
            pose.popPose();
        }
    }

    /** Одержимый зомби: зомби с собственной шкурой (вдвое крупнее обычной текстуры) и в латах культа. */
    public static class Zombie extends HumanoidMobRenderer<InhabitedZombie, ZombieModel<InhabitedZombie>> {
        private static final ResourceLocation SKIN = Thaumcraft.id("textures/models/czombie.png");

        public Zombie(EntityRendererProvider.Context context) {
            super(context, new ZombieModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0f), 64, 64).bakeRoot()), 0.5f);
            addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                    new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
        }

        @Override
        public ResourceLocation getTextureLocation(InhabitedZombie zombie) {
            return SKIN;
        }
    }

    /** Сгусток: кадр искр из листа частиц, повёрнутый к камере, пульсирует; красный — из другого ряда. */
    public static class Orb extends EntityRenderer<GolemOrb> {
        private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

        public Orb(EntityRendererProvider.Context context) {
            super(context);
            shadowRadius = 0f;
        }

        @Override
        public void render(GolemOrb orb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
            float u0 = (1 + orb.tickCount % 6) / 8f;
            float u1 = u0 + 0.125f;
            float v0 = orb.isRed() ? 0.75f : 0.875f;
            float v1 = v0 + 0.125f;
            float bob = Mth.sin((orb.tickCount + partialTick) / 5f) * 0.2f + 0.2f;
            pose.pushPose();
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.scale(1f + bob, 1f + bob, 1f + bob);
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(PARTICLES));
            float[][] corners = {{-0.5f, -0.5f, u0, v1}, {0.5f, -0.5f, u1, v1}, {0.5f, 0.5f, u1, v0}, {-0.5f, 0.5f, u0, v0}};
            for (float[] p : corners) {
                consumer.vertex(matrix, p[0], p[1], 0f).color(1f, 1f, 1f, 0.8f).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(0xf000f0).normal(normal, 0f, 1f, 0f).endVertex();
            }
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(GolemOrb orb) {
            return PARTICLES;
        }
    }
}
