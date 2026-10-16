package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.FrostShardEntity;

import java.util.Random;

/**
 * Ледяной осколок — перенос RenderFrostShard оригинала: модель orb.obj с текстурой льда, развёрнутая по полёту
 * и неравномерно растянутая, чтобы каждый осколок был своей формы. Размер растёт с уроном.
 */
public class FrostShardRenderer extends EntityRenderer<FrostShardEntity> {
    /** Модель грузится отдельно (ModelEvent.RegisterAdditional в ClientEvents). */
    public static final ResourceLocation MODEL = Thaumcraft.id("block/frost_shard");

    public FrostShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FrostShardEntity shard, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(MODEL);
        Random random = new Random(shard.getId());
        float base = shard.getDamage() * 0.1f;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, shard.yRotO, shard.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, shard.xRotO, shard.getXRot())));
        pose.scale(base + random.nextFloat() * 0.1f, base + random.nextFloat() * 0.1f, base + random.nextFloat() * 0.1f);
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FrostShardEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
