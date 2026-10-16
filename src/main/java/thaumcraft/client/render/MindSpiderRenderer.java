package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.MindSpider;

/**
 * Паук разума (RenderMindSpider оригинала): уменьшенный втрое паук с текстурой порченого, почти прозрачный —
 * видны главным образом горящие глаза. Видение рисуется только тому игроку, которому оно явилось.
 */
public class MindSpiderRenderer extends MobRenderer<MindSpider, SpiderModel<MindSpider>> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/taint_spider.png");
    private static final ResourceLocation EYES = Thaumcraft.id("textures/models/taint_spider_eyes.png");

    public MindSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)) {
            @Override
            public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green,
                                       float blue, float alpha) {
                super.renderToBuffer(pose, consumer, light, overlay, red, green, blue, alpha * bodyAlpha);
            }
        }, 0f);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffer, int light, MindSpider spider, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(EYES));
                float body = bodyAlpha;
                bodyAlpha = 1f;
                getParentModel().renderToBuffer(pose, consumer, 0xf000f0, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
                bodyAlpha = body;
            }
        });
    }

    /** Непрозрачность тела текущего паука; у слоя глаз — полная. Рендер идёт в одном потоке. */
    private static float bodyAlpha = 1f;

    private static boolean visible(MindSpider spider) {
        String viewer = spider.getViewer();
        return viewer.isEmpty() || Minecraft.getInstance().player != null
                && viewer.equals(Minecraft.getInstance().player.getGameProfile().getName());
    }

    @Override
    public void render(MindSpider spider, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        if (visible(spider)) {
            bodyAlpha = Math.min(0.1f, spider.tickCount / 100f);
            super.render(spider, yaw, partialTick, pose, buffer, light);
            bodyAlpha = 1f;
        }
    }

    @Override
    protected void scale(MindSpider spider, PoseStack pose, float partialTick) {
        pose.scale(0.3f, 0.3f, 0.3f);
    }

    @Override
    protected float getFlipDegrees(MindSpider spider) {
        return 180f;
    }

    /** Тело полупрозрачное, как в оригинале: непрозрачность растёт первые десять тиков и останавливается на 10%. */
    @Nullable
    @Override
    protected RenderType getRenderType(MindSpider spider, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    public ResourceLocation getTextureLocation(MindSpider spider) {
        return TEXTURE;
    }
}
