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
 * Частица общего назначения — перенос FXGeneric оригинала: кадры из листа частиц 16x16, которые либо проигрываются
 * один раз за жизнь частицы, либо крутятся по кругу. Цвета складываются с фоном, как у всего первого слоя оригинала.
 */
public class GenericParticle extends Particle {
    private static final ResourceLocation SHEET = Thaumcraft.id("textures/misc/particles.png");

    private final boolean loop;
    private final int start;
    private final int count;
    private final int step;
    private final int delay;
    private final float scale;

    GenericParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float r, float g, float b,
                    float alpha, boolean loop, int start, int count, int step, int age, int delay, float scale) {
        super(level, x, y, z);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.hasPhysics = false;
        this.lifetime = age + delay;
        this.delay = delay;
        this.loop = loop;
        this.start = start;
        this.count = Math.max(1, count);
        this.step = Math.max(1, step);
        this.scale = scale;
        this.alpha = alpha;
        setColor(r, g, b);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        x += xd;
        y += yd;
        z += zd;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        if (age < delay) {
            return;
        }
        int index = loop ? start + age / step % count
                : (int) (start + Math.min(count * ((float) age / lifetime), count - 1));
        float u0 = index % 16 / 16f;
        float u1 = u0 + 0.0624375f;
        float v0 = index / 16 / 16f;
        float v1 = v0 + 0.0624375f;
        // в первый и последний такт частица вдвое прозрачнее
        float a = age <= 1 || age >= lifetime - 1 ? alpha / 2f : alpha;
        float size = 0.1f * scale;
        Vec3 cam = camera.getPosition();
        Vec3 center = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z)).subtract(cam);
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(size);
        Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(size);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHEET);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        put(buffer, center.add(l).subtract(h), u1, v1, a);
        put(buffer, center.add(l).add(h), u1, v0, a);
        put(buffer, center.subtract(l).add(h), u0, v0, a);
        put(buffer, center.subtract(l).subtract(h), u0, v1, a);
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void put(BufferBuilder buffer, Vec3 pos, float u, float v, float a) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(rCol, gCol, bCol, a).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
