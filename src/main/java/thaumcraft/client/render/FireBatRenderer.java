package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.FireBat;

/**
 * Огненная летучая мышь — как RenderFireBat оригинала: модель обычной летучей мыши со своей текстурой,
 * светится в темноте; дьявольские и вампирские мыши почти вдвое крупнее.
 */
public class FireBatRenderer extends BatRenderer {
    private static final ResourceLocation FIRE = Thaumcraft.id("textures/models/firebat.png");
    private static final ResourceLocation VAMPIRE = Thaumcraft.id("textures/models/vampirebat.png");

    public FireBatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Bat bat) {
        return bat instanceof FireBat fire && fire.isVampire() ? VAMPIRE : FIRE;
    }

    @Override
    protected void scale(Bat bat, PoseStack pose, float partialTick) {
        float scale = bat instanceof FireBat fire && (fire.isDevil() || fire.isVampire()) ? 0.6f : 0.35f;
        pose.scale(scale, scale, scale);
    }

    @Override
    protected int getBlockLightLevel(Bat bat, BlockPos pos) {
        return 15;
    }
}
