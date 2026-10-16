package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.BrainyZombie;

/** Злой и яростный зомби (RenderBrainyZombie оригинала): обычный зомби со своей текстурой; яростный растёт со злостью. */
public class BrainyZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/bzombie.png");

    public BrainyZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Zombie zombie, PoseStack pose, float partialTick) {
        if (zombie instanceof BrainyZombie.Giant giant) {
            pose.scale(giant.getAnger(), giant.getAnger(), giant.getAnger());
        }
        super.scale(zombie, pose, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return TEXTURE;
    }
}
