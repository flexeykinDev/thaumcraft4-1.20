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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModSounds;

import java.util.HashSet;
import java.util.Set;

/**
 * Мошка роя — перенос FXSwarm оригинала: летит по кругу возле своего существа, поворачивая к нему с ограниченной
 * скоростью, а когда хозяин ранен — шарахается прочь. Потеряв хозяина, медленно падает и гаснет.
 */
public class SwarmParticle extends Particle {
    private static final ResourceLocation SHEET = Thaumcraft.id("textures/misc/particles.png");
    /** Существа, чьи мошки отпущены: их частицы гаснут, как при гибели хозяина. */
    private static final Set<Integer> RELEASED = new HashSet<>();

    private final Entity target;
    private final float speed;
    private final float turnSpeed;
    private final float pull;
    private final float scale;
    private float yaw;
    private float pitch;
    private int deathTimer;
    private boolean released;

    SwarmParticle(ClientLevel level, Entity target, float speed, float turnSpeed, float pull) {
        super(level, target.getX() + (level.random.nextFloat() - level.random.nextFloat()) * 2f,
                target.getY() + (level.random.nextFloat() - level.random.nextFloat()) * 2f,
                target.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * 2f);
        this.target = target;
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.pull = pull;
        this.scale = random.nextFloat() * 0.5f + 1f;
        this.xd = (random.nextFloat() - random.nextFloat()) * 0.2f;
        this.yd = (random.nextFloat() - random.nextFloat()) * 0.2f;
        this.zd = (random.nextFloat() - random.nextFloat()) * 0.2f;
        this.lifetime = Integer.MAX_VALUE;
        setColor(0.8f + random.nextFloat() * 0.2f, random.nextFloat() * 0.4f, 1f - random.nextFloat() * 0.2f);
        RELEASED.remove(target.getId());
    }

    /** Мошки этого существа улетают: гаснут, не дожидаясь его гибели. */
    static void release(int entityId) {
        RELEASED.add(entityId);
    }

    private boolean orphaned() {
        return released || !target.isAlive() || target instanceof LivingEntity living && living.deathTime > 0;
    }

    private static float approach(float from, float to, float max) {
        return from + Mth.clamp(Mth.wrapDegrees(to - from), -max, max);
    }

    /** Поворот к хозяину (или от него при отрицательной скорости), не быстрее заданного. */
    private void face(float yawStep, float pitchStep) {
        double dx = target.getX() - x;
        double dz = target.getZ() - z;
        double dy = target.getBoundingBox().getCenter().y - y;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float wantPitch = (float) -(Math.atan2(dy, flat) * 180.0 / Math.PI);
        if (yawStep >= 0) {
            pitch = approach(pitch, wantPitch, pitchStep);
            yaw = approach(yaw, wantYaw, yawStep);
        } else {
            // с отрицательным шагом расчёт оригинала всегда давал поворот на полный шаг в одну сторону:
            // мошка, подлетевшая вплотную или вспугнутая, закладывает вираж
            pitch -= pitchStep;
            yaw -= yawStep;
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        age++;
        if (RELEASED.contains(target.getId())) {
            released = true;
        }
        boolean orphan = orphaned();
        if (orphan) {
            deathTimer++;
            yd -= pull / 2f;
            if (deathTimer > 50) {
                remove();
                return;
            }
        } else {
            yd += pull;
        }
        move(xd, yd, zd);
        xd *= 0.985;
        yd *= 0.985;
        zd *= 0.985;
        if (!orphan) {
            boolean hurt = target instanceof LivingEntity living && living.hurtTime > 0;
            float step = turnSpeed / 2f + random.nextInt(Math.max(1, (int) (turnSpeed / 2f)));
            double dx = target.getX() - x;
            double dy = target.getY() - y;
            double dz = target.getZ() - z;
            if (dx * dx + dy * dy + dz * dz > target.getBbWidth() && !hurt) {
                face(step, step);
            } else {
                face(-step, -step);
            }
            double mx = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            double mz = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            double my = -Mth.sin(pitch * Mth.DEG_TO_RAD);
            double length = Math.sqrt(mx * mx + my * my + mz * mz);
            if (length > 1.0e-4) {
                // небольшой случайный увод, чтобы мошки не летели строем
                xd = (mx / length + random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.0075 * 15) * speed;
                yd = (my / length + random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.0075 * 15) * speed;
                zd = (mz / length + random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.0075 * 15) * speed;
            }
        }
        if (random.nextInt(50) == 0 && level.getNearestPlayer(x, y, z, 8.0, false) != null) {
            level.playLocalSound(x, y, z, ModSounds.FLY.get(), SoundSource.HOSTILE, 0.03f, 0.5f + random.nextFloat() * 0.4f, false);
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        float bob = Mth.sin(age / 3f) * 0.25f + 1f;
        int part = 7 + age % 8;
        float u0 = part / 16f;
        float u1 = u0 + 0.0624375f;
        float v0 = 0.25f;
        float v1 = v0 + 0.0624375f;
        float size = 0.1f * scale * bob;
        float alpha = (50f - deathTimer) / 50f;
        boolean hurt = !(target instanceof LivingEntity living) || living.hurtTime > 0;
        float g = hurt ? gCol / 2f : gCol;
        float b = hurt ? bCol / 2f : bCol;
        Vec3 cam = camera.getPosition();
        Vec3 center = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z)).subtract(cam);
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(size);
        Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(size);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHEET);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        put(buffer, center.add(l).subtract(h), u1, v1, g, b, alpha);
        put(buffer, center.add(l).add(h), u1, v0, g, b, alpha);
        put(buffer, center.subtract(l).add(h), u0, v0, g, b, alpha);
        put(buffer, center.subtract(l).subtract(h), u0, v1, g, b, alpha);
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void put(BufferBuilder buffer, Vec3 pos, float u, float v, float g, float b, float alpha) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(rCol, g, b, alpha).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
