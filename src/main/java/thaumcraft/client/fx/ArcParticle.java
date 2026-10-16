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
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Дуга — перенос FXArc оригинала: искрящая лента, перекинутая параболой из точки в точку (летит вверх и падает,
 * как брошенный камень, с дрожью). Живёт один кадр; ею хранитель и портал культа «прошивают» пол.
 */
public class ArcParticle extends Particle {
    private final List<Vec3> points = new ArrayList<>();
    private final float length;

    ArcParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, double heightGain) {
        super(level, x, y, z);
        setColor(r, g, b);
        hasPhysics = false;
        lifetime = 1;
        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(tx - x, ty - y, tz - z);
        length = (float) end.length();
        double gravity = 0.115;
        double noise = 0.25;
        Vec3 velocity = calculateVelocity(start, end, heightGain, gravity);
        double step = velocity.lengthSqr();
        points.add(start);
        Vec3 current = start;
        double vy = velocity.y;
        for (int c = 0; end.distanceToSqr(current) > step && c < 50; c++) {
            current = current.add(velocity.x, vy, velocity.z);
            points.add(current.add((random.nextDouble() - random.nextDouble()) * noise, (random.nextDouble() - random.nextDouble()) * noise,
                    (random.nextDouble() - random.nextDouble()) * noise));
            vy -= gravity / 1.9;
        }
        points.add(end);
    }

    /** Utils.calculateVelocity оригинала: начальная скорость, с которой брошенное тело поднимется на heightGain и придёт в to. */
    private static Vec3 calculateVelocity(Vec3 from, Vec3 to, double heightGain, double gravity) {
        double endGain = to.y - from.y;
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double maxGain = heightGain > endGain + heightGain ? heightGain : endGain + heightGain;
        double a = -horiz * horiz / (4.0 * maxGain);
        double c = -endGain;
        double slope = -horiz / (2.0 * a) - Math.sqrt(horiz * horiz - 4.0 * a * c) / (2.0 * a);
        double vy = Math.sqrt(maxGain * gravity);
        double vh = vy / slope;
        double mag = Math.max(1.0e-6, horiz);
        return new Vec3(vh * dx / mag, vy, vh * dz / mag);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float ox = (float) (x - cam.x);
        float oy = (float) (y - cam.y);
        float oz = (float) (z - cam.z);
        float size = 0.25f;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Thaumcraft.id("textures/misc/beamh.png"));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        for (int pass = 0; pass < 2; pass++) {
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int c = 0; c < points.size(); c++) {
                Vec3 v = points.get(c);
                float u = c / Math.max(1f, length);
                float px = ox + (float) v.x;
                float py = oy + (float) v.y;
                float pz = oz + (float) v.z;
                if (pass == 0) {
                    buffer.vertex(px, py - size, pz).uv(u, 1f).color(rCol, gCol, bCol, 0.8f).endVertex();
                    buffer.vertex(px, py + size, pz).uv(u, 0f).color(rCol, gCol, bCol, 0.8f).endVertex();
                } else {
                    buffer.vertex(px - size, py, pz - size).uv(u, 1f).color(rCol, gCol, bCol, 0.8f).endVertex();
                    buffer.vertex(px + size, py, pz + size).uv(u, 0f).color(rCol, gCol, bCol, 0.8f).endVertex();
                }
            }
            Tesselator.getInstance().end();
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
