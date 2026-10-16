package thaumcraft.client.fx;

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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * Электрическая искра — перенос FXSpark оригинала: короткая мультипликация из семи кадров
 * (три варианта в верхних рядах второго листа частиц, цвета складываются с фоном), иногда отражённая зеркально.
 */
public class SparkParticle extends Particle {
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles2.png");

    private final int first;
    private final boolean flip;
    private final float scale;

    SparkParticle(ClientLevel level, double x, double y, double z, float scale, float r, float g, float b, float alpha) {
        super(level, x, y, z);
        this.scale = scale;
        this.hasPhysics = false;
        this.lifetime = 5 + random.nextInt(5);
        this.first = random.nextInt(3) * 8;
        this.flip = random.nextBoolean();
        this.alpha = alpha;
        setColor(r, g, b);
    }

    @Override
    public void tick() {
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        int part = first + (int) ((float) age / lifetime * 7f);
        float u0 = part % 8 / 8f;
        float u1 = u0 + 0.125f;
        float v0 = part / 8 / 8f;
        float v1 = v0 + 0.125f;
        if (flip) {
            float t = u0;
            u0 = u1;
            u1 = t;
        }
        Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(scale);
        Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(scale);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PARTICLES);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        put(buffer, center.add(l).subtract(h), u1, v1);
        put(buffer, center.add(l).add(h), u1, v0);
        put(buffer, center.subtract(l).add(h), u0, v0);
        put(buffer, center.subtract(l).subtract(h), u0, v1);
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void put(BufferBuilder buffer, Vec3 pos, float u, float v) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(rCol, gCol, bCol, alpha).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
