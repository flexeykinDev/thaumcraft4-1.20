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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * Клуб дыма, летящий по спирали вокруг точки, — перенос FXSmokeSpiral оригинала (вихрь меча зефира). За свою жизнь
 * клуб делает два оборота и опускается с макушки вихря до его подножия, но не ниже уровня земли floor.
 */
public class SmokeSpiralParticle extends Particle {
    private static final ResourceLocation SHEET = Thaumcraft.id("textures/misc/particles.png");

    private final float radius;
    private final int start;
    private final int floor;

    SmokeSpiralParticle(ClientLevel level, double x, double y, double z, float radius, int start, int floor) {
        super(level, x, y, z);
        this.radius = radius;
        this.start = start;
        this.floor = floor;
        this.lifetime = 20 + level.random.nextInt(10);
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        alpha = (float) (lifetime - age) / lifetime;
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        float progress = Math.min(1f, (age + partialTick) / lifetime);
        int index = (int) (1f + (float) age / lifetime * 4f);
        float around = (start + 720f * progress) * Mth.DEG_TO_RAD;
        float down = (90f - 180f * progress) * Mth.DEG_TO_RAD;
        double px = x - Mth.sin(around) * Mth.cos(down) * radius;
        double py = Math.max(y - Mth.sin(down) * radius, floor + 0.1f);
        double pz = z + Mth.cos(around) * Mth.cos(down) * radius;
        float u0 = index % 16 / 16f;
        float u1 = u0 + 0.0624375f;
        float v0 = index / 16 / 16f;
        float v1 = v0 + 0.0624375f;
        float size = 0.15f;
        Vec3 center = new Vec3(px, py, pz).subtract(camera.getPosition());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(size);
        Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(size);
        float a = 0.66f * alpha;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHEET);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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
