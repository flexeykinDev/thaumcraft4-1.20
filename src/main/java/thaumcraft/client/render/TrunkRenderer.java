package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.TravelingTrunk;
import thaumcraft.common.items.golem.GolemUpgradeItem;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Дорожный сундук — перенос ModelTrunk и RenderTravelingTrunk оригинала: сундук, хлопающий крышкой на скаку;
 * разозлившись, он меняет текстуру, а на замке виден значок его улучшения.
 */
public class TrunkRenderer extends MobRenderer<TravelingTrunk, TrunkRenderer.Model> {
    private static final ResourceLocation CALM = Thaumcraft.id("textures/models/trunk.png");
    private static final ResourceLocation ANGRY = Thaumcraft.id("textures/models/trunkangry.png");

    public TrunkRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 0.5f);
        addLayer(new Badge(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TravelingTrunk trunk) {
        return trunk.getAnger() > 0 ? ANGRY : CALM;
    }

    /** Сундук с улучшением земли чуть крупнее. */
    @Override
    protected void scale(TravelingTrunk trunk, PoseStack pose, float partialTick) {
        float size = trunk.getUpgrade() == Golem.UPGRADE_EARTH ? 2f / 1.33f : 2f / 1.5f;
        float squeeze = 1f / 1.4f;
        pose.scale(squeeze * size, 0.5f / squeeze * size, squeeze * size);
        pose.translate(-0.5f, 0.5f, -0.5f);
    }

    public static class Model extends EntityModel<TravelingTrunk> {
        final ModelPart lid = part(0, 0, 0f, -5f, -14f, 14, 5, 14, 1f, 7f, 15f);
        final ModelPart knob = part(0, 0, -1f, -2f, -15f, 2, 4, 1, 8f, 7f, 15f);
        private final ModelPart base = part(0, 19, 0f, 0f, 0f, 14, 10, 14, 1f, 6f, 1f);

        private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
            ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, false, 64f, 64f,
                    EnumSet.allOf(Direction.class))), Map.of());
            part.setPos(px, py, pz);
            return part;
        }

        @Override
        public void setupAnim(TravelingTrunk trunk, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float open = 1f - trunk.lidrot;
            open = 1f - open * open * open;
            lid.xRot = -(open * Mth.HALF_PI);
            knob.xRot = lid.xRot;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            lid.render(pose, consumer, light, overlay, r, g, b, a);
            base.render(pose, consumer, light, overlay, r, g, b, a);
            knob.render(pose, consumer, light, overlay, r, g, b, a);
        }
    }

    /** Значок улучшения на замке. */
    private static class Badge extends RenderLayer<TravelingTrunk, Model> {
        Badge(RenderLayerParent<TravelingTrunk, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, TravelingTrunk trunk, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            int upgrade = trunk.getUpgrade();
            if (trunk.isInvisible()) {
                return;
            }
            ResourceLocation icon = Thaumcraft.id(upgrade >= 0 && upgrade < GolemUpgradeItem.NAMES.length
                    ? "item/golem_upgrade_" + GolemUpgradeItem.NAMES[upgrade] : "item/golem_upgrade_empty");
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
            pose.pushPose();
            getParentModel().knob.translateAndRotate(pose);
            pose.translate(-0.075f, -0.115f, -0.94301f);
            pose.scale(0.15f, 0.15f, 0.15f);
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
            float[][] corners = {{0f, 0f, sprite.getU1(), sprite.getV1()}, {1f, 0f, sprite.getU0(), sprite.getV1()},
                    {1f, 1f, sprite.getU0(), sprite.getV0()}, {0f, 1f, sprite.getU1(), sprite.getV0()}};
            for (float[] c : corners) {
                consumer.vertex(matrix, c[0], c[1], 0f).color(255, 255, 255, 255).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light).normal(normal, 0f, 0f, 1f).endVertex();
            }
            pose.popPose();
        }
    }
}
