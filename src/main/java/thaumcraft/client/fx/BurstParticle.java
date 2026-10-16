package thaumcraft.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * Вспышка (FXBurst оригинала): белое кольцо, которое за полторы секунды проигрывает 32 кадра из нижней строки листа
 * узлов. Ею появляется и гибнет висп.
 */
public class BurstParticle extends Particle {
    private static final ResourceLocation SHEET = Thaumcraft.id("textures/misc/nodes.png");
    private final float scale;

    BurstParticle(ClientLevel level, double x, double y, double z, float size) {
        super(level, x, y, z);
        this.scale = 0.1f * (random.nextFloat() * 0.5f + 0.5f) * 2f * size;
        this.lifetime = 31;
        this.gravity = 0f;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        float u0 = (age % 32) / 32f;
        float u1 = u0 + 0.03125f;
        float v0 = 0.96875f;
        float v1 = 1f;
        Vec3 cam = camera.getPosition();
        Vec3 centre = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z)).subtract(cam);
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(scale);
        Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(scale);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHEET);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        put(buffer, centre.add(l).subtract(h), u1, v1);
        put(buffer, centre.add(l).add(h), u1, v0);
        put(buffer, centre.subtract(l).add(h), u0, v0);
        put(buffer, centre.subtract(l).subtract(h), u0, v1);
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void put(BufferBuilder buffer, Vec3 pos, float u, float v) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(1f, 1f, 1f, 1f).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
