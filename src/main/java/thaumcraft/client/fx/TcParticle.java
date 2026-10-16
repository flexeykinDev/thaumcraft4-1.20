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
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * Частицы оригинала: огонёк (FXWisp), искра (FXSparkle), пузырь (FXBubble) и руна (FXBlockRunes).
 * Все они рисуются с одного листа textures/misc/particles.png, как это делал ParticleEngine оригинала,
 * поэтому обходятся без атласа частиц игры и создаются прямо в коде (см. Fx).
 */
public class TcParticle extends Particle {
    public enum Kind { WISP, SPARKLE, BUBBLE, RUNE, SLIMY, ELDRITCH, VENT }

    private static final ResourceLocation SHEET = Thaumcraft.id("textures/misc/particles.png");
    private static final float CELL = 0.0624375f;

    /** Слой 0 оригинала: яркость складывается с фоном, тёмное не видно. */
    public static final ParticleRenderType ADDITIVE = sheet("thaumcraft_additive", GlStateManager.DestFactor.ONE);
    /** Слой 1 оригинала: обычная прозрачность (для тёмных частиц). */
    public static final ParticleRenderType NORMAL = sheet("thaumcraft_normal", GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

    private static ParticleRenderType sheet(String name, GlStateManager.DestFactor dest) {
        return new ParticleRenderType() {
            @Override
            public void begin(BufferBuilder builder, TextureManager textures) {
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, dest);
                RenderSystem.setShaderTexture(0, SHEET);
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public void end(Tesselator tesselator) {
                tesselator.end();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableCull();
                RenderSystem.depthMask(true);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private final Kind kind;
    /** Размер, как particleScale оригинала: у EntityFX он случаен от 1 до 2 и затем умножается. */
    private float scale;
    boolean additive = true;
    boolean shrink;
    boolean slowdown = true;
    /** Сколько тиков держится кадр искры. */
    private int multiplier = 2;
    private int halfLife;
    /** Номер ячейки листа для пузыря; перед смертью пузырь «лопается» следующими кадрами. */
    private int cell = 16;
    double bubbleSpeed = 0.002;
    // руна: плоскость на грани блока
    private float runeRotation;
    private float runeX;
    private float runeY;
    private net.minecraft.world.entity.Entity follow;
    private float ventMax = 1f;

    TcParticle(ClientLevel level, double x, double y, double z, Kind kind) {
        super(level, x, y, z);
        this.kind = kind;
        this.scale = (random.nextFloat() * 0.5f + 0.5f) * 2f;
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0f;
        this.alpha = 1f;
        setSize(0.02f, 0.02f);
    }

    // ---------- настройка (те же числа, что в конструкторах оригинала) ----------

    static TcParticle wisp(ClientLevel level, double x, double y, double z, float size, float r, float g, float b) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.WISP);
        p.setColor(r == 0f ? 1f : r, g, b);
        p.scale *= size;
        p.lifetime = (int) (36.0 / (Math.random() * 0.3 + 0.7));
        p.halfLife = p.lifetime / 2;
        p.alpha = 0.5f;
        p.setSize(0.1f, 0.1f);
        return p;
    }

    static TcParticle sparkle(ClientLevel level, double x, double y, double z, float size, float r, float g, float b, int multiplier) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.SPARKLE);
        p.setColor(r == 0f ? 1f : r, g, b);
        p.scale *= size;
        p.lifetime = 3 * multiplier;
        p.multiplier = multiplier;
        p.shrink = true;
        p.setSize(0.01f, 0.01f);
        return p;
    }

    static TcParticle bubble(ClientLevel level, double x, double y, double z, int age) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.BUBBLE);
        p.setColor(1f, 0f, 0.5f);
        p.hasPhysics = false;
        p.scale *= p.random.nextFloat() * 0.3f + 0.2f;
        p.xd = (Math.random() * 2.0 - 1.0) * 0.02;
        p.yd = Math.random() * 0.02;
        p.zd = (Math.random() * 2.0 - 1.0) * 0.02;
        p.lifetime = (int) (age + 2 + 8.0 / (Math.random() * 0.8 + 0.2));
        return p;
    }

    /** FXSlimyBubble оригинала: густой пузырь, который вспухает, поднимается и лопается. */
    static TcParticle slimy(ClientLevel level, double x, double y, double z, float size) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.SLIMY);
        p.scale = size;
        p.lifetime = 15 + p.random.nextInt(5);
        p.cell = 144;
        p.alpha = 0.8f;
        p.additive = false;
        p.hasPhysics = false;
        return p;
    }

    /** FXWispEG оригинала: тёмный дымок, стекающий с балахона стража Элдрича и следующий за ним. */
    static TcParticle eldritch(ClientLevel level, double x, double y, double z, net.minecraft.world.entity.Entity follow) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.ELDRITCH);
        p.follow = follow;
        p.xd = p.random.nextGaussian() * 0.03;
        p.yd = -0.05;
        p.zd = p.random.nextGaussian() * 0.03;
        p.scale *= 0.4f;
        p.lifetime = (int) (40.0 / (Math.random() * 0.3 + 0.7));
        p.setColor(p.random.nextFloat() * 0.05f, p.random.nextFloat() * 0.05f, p.random.nextFloat() * 0.05f);
        p.additive = false;
        return p;
    }

    /** FXVent оригинала: клуб пара, вылетающий в заданную сторону; растёт и тает, пока не станет в size раз больше. */
    static TcParticle vent(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, int color, float size) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.VENT);
        p.scale = (p.random.nextFloat() * 0.1f + 0.05f) * size;
        p.ventMax = size;
        p.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
        p.hasPhysics = false;
        p.additive = false;
        p.alpha = 0.4f;
        p.lifetime = 200;
        // setHeading оригинала: направление с небольшим разбросом, скорость 0.125
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }
        dx += p.random.nextGaussian() * (p.random.nextBoolean() ? -1 : 1) * 0.0075 * 5.0;
        dy += p.random.nextGaussian() * (p.random.nextBoolean() ? -1 : 1) * 0.0075 * 5.0;
        dz += p.random.nextGaussian() * (p.random.nextBoolean() ? -1 : 1) * 0.0075 * 5.0;
        p.xd = dx * 0.125;
        p.yd = dy * 0.125;
        p.zd = dz * 0.125;
        return p;
    }

    /** Пена на кипящей воде (setFroth) и пена, стекающая по стенкам (setFroth2). */
    TcParticle froth(boolean down) {
        scale *= 0.75f;
        lifetime = down ? 12 + random.nextInt(12) : 4 + random.nextInt(3);
        bubbleSpeed = down ? -0.005 : -0.001;
        xd /= 5.0;
        yd /= 10.0;
        zd /= 5.0;
        return this;
    }

    static TcParticle rune(ClientLevel level, double x, double y, double z, float r, float g, float b, int duration) {
        TcParticle p = new TcParticle(level, x, y, z, Kind.RUNE);
        p.setColor(r == 0f ? 1f : r, g, b);
        p.hasPhysics = false;
        p.lifetime = 3 * duration;
        p.cell = 224 + p.random.nextInt(16);
        p.runeRotation = p.random.nextInt(4) * 90f;
        p.runeX = p.random.nextFloat() * 0.2f;
        p.runeY = -0.3f + p.random.nextFloat() * 0.6f;
        p.scale = (float) (1.0 + p.random.nextGaussian() * 0.1);
        p.alpha = 0f;
        return p;
    }

    TcParticle gravity(float value) {
        gravity = value;
        return this;
    }

    TcParticle noClip() {
        hasPhysics = false;
        return this;
    }

    TcParticle shrink(boolean value) {
        shrink = value;
        return this;
    }

    TcParticle normalBlend() {
        additive = false;
        return this;
    }

    /** Летит по прямой и исчезает ровно в точке назначения. */
    TcParticle target(double tx, double ty, double tz) {
        if (lifetime > 0) {
            xd = (tx - x) / lifetime;
            yd = (ty - y) / lifetime;
            zd = (tz - z) / lifetime;
            slowdown = false;
        }
        return this;
    }

    // ---------- жизнь ----------

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        switch (kind) {
            case BUBBLE -> {
                yd += bubbleSpeed;
                if (bubbleSpeed > 0.0) {
                    xd += (random.nextFloat() - random.nextFloat()) * 0.01f;
                    zd += (random.nextFloat() - random.nextFloat()) * 0.01f;
                }
                setPos(x + xd, y + yd, z + zd);
                xd *= 0.85;
                yd *= 0.85;
                zd *= 0.85;
                if (lifetime-- <= 0) {
                    remove();
                } else if (lifetime <= 2) {
                    cell++;
                }
            }
            case SLIMY -> {
                xo = x;
                yo = y;
                zo = z;
                if (age++ >= lifetime) {
                    remove();
                }
                if (age - 1 < 6) {
                    cell = 144 + age / 2;
                    if (age == 5) {
                        y += 0.1;
                    }
                } else if (age < lifetime - 4) {
                    yd += 0.005;
                    cell = 147 + age % 4 / 2;
                } else {
                    yd /= 2.0;
                    cell = 150 - (lifetime - age) / 2;
                }
                setPos(x, y + yd, z);
            }
            case VENT -> {
                age++;
                if (scale > ventMax) {
                    remove();
                }
                yd += 0.0025;
                setPos(x + xd, y + yd, z + zd);
                xd *= 0.85;
                yd *= 0.85;
                zd *= 0.85;
                if (scale < ventMax) {
                    scale *= 1.15f;
                }
            }
            case ELDRITCH -> {
                if (follow != null && !onGround) {
                    setPos(x + follow.getDeltaMovement().x, y, z + follow.getDeltaMovement().z);
                }
                move(xd, yd, zd);
                xd *= 0.98;
                yd *= 0.98;
                zd *= 0.98;
                if (onGround) {
                    xd *= 0.85;
                    zd *= 0.85;
                }
                if (age++ >= lifetime) {
                    remove();
                }
            }
            case RUNE -> {
                float threshold = lifetime / 5f;
                alpha = age <= threshold ? age / threshold : (lifetime - age) / (float) lifetime;
                if (age++ >= lifetime) {
                    remove();
                }
                yd -= 0.04 * gravity;
                setPos(x + xd, y + yd, z + zd);
            }
            default -> {
                if (age++ >= lifetime) {
                    remove();
                }
                yd -= 0.04 * gravity;
                move(xd, yd, zd);
                if (slowdown) {
                    double drag = kind == Kind.SPARKLE ? 0.908 : 0.98;
                    xd *= drag;
                    yd *= drag;
                    zd *= drag;
                    if (onGround) {
                        xd *= 0.7;
                        zd *= 0.7;
                    }
                }
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return additive ? ADDITIVE : NORMAL;
    }

    /** Как в оригинале, частицы светятся сами и не темнеют в тени. */
    @Override
    protected int getLightColor(float partialTick) {
        if (kind == Kind.VENT) {
            // пар, в отличие от огоньков, освещён как всё вокруг
            return super.getLightColor(partialTick);
        }
        return 0xf000f0;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - cam.x());
        float py = (float) (Mth.lerp(partialTick, yo, y) - cam.y());
        float pz = (float) (Mth.lerp(partialTick, zo, z) - cam.z());

        float u0;
        float v0;
        float u1;
        float v1;
        float half;
        float a = alpha;
        switch (kind) {
            case WISP -> {
                float ageScale = shrink ? (lifetime - (float) age) / lifetime : age / (float) Math.max(1, halfLife);
                if (!shrink && ageScale > 1f) {
                    ageScale = 2f - ageScale;
                }
                half = 0.5f * scale * Math.max(0f, ageScale);
                u0 = 0f;
                u1 = 0.125f;
                v0 = 0.875f;
                v1 = 1f;
            }
            case SPARKLE -> {
                int frame = 16 + age / multiplier;
                u0 = (frame % 4) / 16f;
                u1 = u0 + CELL;
                v0 = 0.25f;
                v1 = v0 + CELL;
                half = 0.1f * scale * (shrink ? (lifetime - age + 1) / (float) lifetime : 1f);
            }
            case BUBBLE -> {
                u0 = (cell % 16) / 16f;
                u1 = u0 + CELL;
                v0 = (cell / 16) / 16f;
                v1 = v0 + CELL;
                half = 0.1f * scale;
            }
            case VENT -> {
                int part = (int) (1f + scale / ventMax * 4f);
                u0 = (part % 16) / 16f;
                u1 = u0 + CELL;
                v0 = (part / 16) / 16f;
                v1 = v0 + CELL;
                half = 0.3f * scale;
                a = alpha * Math.max(0f, (ventMax - scale) / ventMax);
            }
            case ELDRITCH -> {
                u0 = (age % 13) / 16f;
                u1 = u0 + CELL;
                v0 = 0.1875f;
                v1 = v0 + CELL;
                half = 0.5f * scale;
                // вблизи дымок гуще, дальше 32 блоков не виден
                float base = (float) (1.0 - Math.min(1024.0, camera.getPosition().distanceToSqr(x, y, z)) / 1024.0);
                a = 0.2f * (1f - age / (float) lifetime) * base;
            }
            case SLIMY -> {
                u0 = (cell % 16) / 16f;
                u1 = u0 + CELL;
                v0 = (cell / 16) / 16f;
                v1 = v0 + CELL;
                half = scale;
            }
            default -> {
                u0 = (cell % 16) / 16f;
                u1 = u0 + CELL;
                v0 = 0.375f;
                v1 = v0 + CELL;
                half = 0.15f * scale;
                a = alpha / 2f;
            }
        }

        Vector3f[] corners = {new Vector3f(-1f, -1f, 0f), new Vector3f(-1f, 1f, 0f), new Vector3f(1f, 1f, 0f), new Vector3f(1f, -1f, 0f)};
        if (kind == Kind.RUNE) {
            // руна лежит на одной из четырёх боковых граней блока, чуть снаружи
            Quaternionf turn = new Quaternionf().rotationY(runeRotation * Mth.DEG_TO_RAD);
            for (Vector3f corner : corners) {
                corner.mul(half);
                corner.add(runeY, runeX, -0.51f);
                corner.rotate(turn);
                corner.add(px, py, pz);
            }
        } else {
            Quaternionf rotation = camera.rotation();
            for (Vector3f corner : corners) {
                corner.rotate(rotation);
                corner.mul(half);
                corner.add(px, py, pz);
            }
        }
        int light = getLightColor(partialTick);
        float[][] uv = {{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};
        for (int pass = 0; pass < (kind == Kind.RUNE ? 2 : 1); pass++) {
            for (int k = 0; k < 4; k++) {
                // руну видно с обеих сторон
                int i = pass == 0 ? k : 3 - k;
                consumer.vertex(corners[i].x(), corners[i].y(), corners[i].z()).uv(uv[i][0], uv[i][1])
                        .color(rCol, gCol, bCol, a).uv2(light).endVertex();
            }
        }
    }
}
