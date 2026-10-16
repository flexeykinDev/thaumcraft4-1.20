package thaumcraft.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
 * Луч между звеньями сети вис — перенос FXBeamPower оригинала: две скрещённые ленты с бегущей текстурой.
 * В покое луч едва заметен; когда по нему идёт вис, он вспыхивает цветом аспекта и медленно гаснет.
 */
public class PowerBeamParticle extends Particle {
    private static final ResourceLocation BEAM = Thaumcraft.id("textures/misc/beam1.png");
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

    private Vec3 from;
    private Vec3 to;
    private float opacity = 0.3f;

    PowerBeamParticle(ClientLevel level, Vec3 from, Vec3 to, float r, float g, float b) {
        super(level, from.x, from.y, from.z);
        this.from = from;
        this.to = to;
        this.hasPhysics = false;
        this.lifetime = 8;
        setColor(r, g, b);
    }

    void update(Vec3 newFrom, Vec3 newTo, float r, float g, float b, boolean pulse) {
        from = newFrom;
        to = newTo;
        setColor(r, g, b);
        if (pulse) {
            opacity = 0.8f;
        }
        while (lifetime - age < 4) {
            lifetime++;
        }
    }

    @Override
    public void tick() {
        opacity = Math.max(0.3f, opacity - 0.025f);
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    /** Без очков откровения лучи сети почти не видны, в очках — яркие, как в оригинале. */
    private static float visibility(float withoutGoggles) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && thaumcraft.api.nodes.Revealer.wornBy(mc.player) ? 1f : withoutGoggles;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        Vec3 start = from.subtract(cam);
        Vec3 end = to.subtract(cam);
        Vec3 axis = end.subtract(start);
        float length = (float) axis.length();
        if (length < 1.0e-3f) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        Vec3 u = dir.cross(Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = dir.cross(u);
        Minecraft mc = Minecraft.getInstance();
        float slide = (mc.player == null ? 0 : mc.player.tickCount) + partialTick;
        float shift = -slide * 0.2f - Mth.floor(-slide * 0.1f);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BEAM);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float alpha = opacity * visibility(0.1f);
        for (int t = 0; t < 2; t++) {
            Vec3 side = (t == 0 ? u : v).scale(0.15 * 0.7);
            float v0 = -1f + shift + t / 3f;
            float v1 = length + v0;
            put(buffer, end.subtract(side), 1f, v1, alpha);
            put(buffer, start.subtract(side), 1f, v0, alpha);
            put(buffer, start.add(side), 0f, v0, alpha);
            put(buffer, end.add(side), 0f, v1, alpha);
        }
        Tesselator.getInstance().end();

        // блик на дальнем конце луча
        RenderSystem.setShaderTexture(0, PARTICLES);
        float u0 = age % 16 / 16f;
        float u1 = u0 + 0.0624375f;
        float w0 = 0.3125f;
        float w1 = w0 + 0.0624375f;
        float half = 0.66f * opacity;
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(half);
        Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(half);
        float flare = opacity * visibility(0.2f);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        put(buffer, end.add(l).subtract(h), u1, w1, flare);
        put(buffer, end.add(l).add(h), u1, w0, flare);
        put(buffer, end.subtract(l).add(h), u0, w0, flare);
        put(buffer, end.subtract(l).subtract(h), u0, w1, flare);
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void put(BufferBuilder buffer, Vec3 pos, float u, float v, float alpha) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(rCol, gCol, bCol, alpha).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
