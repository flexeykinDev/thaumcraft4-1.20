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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Молния — перенос FXLightningBolt и FXLightningBoltCommon оригинала. Отрезок между двумя точками семь раз
 * дробится пополам со случайным смещением середин, на первых четырёх проходах от него отходят ветки.
 * Молния «вырастает» от начала к концу и гаснет. Рисуется двумя слоями: широкое цветное свечение (p_large)
 * и яркая сердцевина (p_small) — это две частицы с одним и тем же зерном случайности.
 * <p>
 * Вторая роль класса — колышущаяся лента вис между узлом и жезлом (drawFloatyLine оригинала).
 */
public class BoltParticle extends Particle {
    private static final ParticleRenderType GLOW = layer("thaumcraft_bolt_glow", "textures/misc/p_large.png");
    private static final ParticleRenderType CORE = layer("thaumcraft_bolt_core", "textures/misc/p_small.png");
    /** Цвета свечения и сердцевины по типам оригинала: 0 лиловая, 1 жёлтая, 2 синяя, 3 зелёная, 4 красная. */
    private static final float[][] GLOW_COLORS = {{0.6f, 0.3f, 0.6f}, {0.6f, 0.6f, 0.1f}, {0.1f, 0.1f, 0.6f}, {0.1f, 1f, 0.1f}, {0.6f, 0.1f, 0.1f},
            {0.6f, 0.2f, 0.6f}, {0.75f, 1f, 1f}};
    private static final float[][] CORE_COLORS = {{1f, 0.6f, 1f}, {1f, 1f, 0.1f}, {0.1f, 0.1f, 1f}, {0.1f, 0.6f, 0.1f}, {1f, 0.1f, 0.1f},
            {0f, 0f, 0f}, {0.75f, 1f, 1f}};

    private static ParticleRenderType layer(String name, String texture) {
        ResourceLocation location = Thaumcraft.id(texture);
        return new ParticleRenderType() {
            @Override
            public void begin(BufferBuilder builder, TextureManager textures) {
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.setShaderTexture(0, location);
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

    /** Отрезок молнии: light — яркость (у веток вдвое меньше), number — порядковый номер вдоль ствола. */
    private record Segment(Vec3 from, Vec3 to, float light, int number, boolean last, boolean first) {
        Vec3 diff() {
            return to.subtract(from);
        }
    }

    private List<Segment> segments = new ArrayList<>();
    private final boolean floaty;
    private final boolean core;
    private float width = 0.03f;
    private int type;
    // молния
    private Random rand;
    private float length;
    private int increment = 1;
    private int segmentCount = 1;
    private int boltAge;
    private int boltMaxAge;

    /**
     * nodeBolt оригинала: молния между двумя точками с длительностью 10, размахом 4.0 и скоростью роста 5.
     *
     * @param core false — слой свечения, true — сердцевина; оба слоя создаются с одним seed
     */
    BoltParticle(ClientLevel level, Vec3 start, Vec3 end, long seed, int type, boolean core) {
        this(level, start, end, seed, type, core, 10, 4f, 5, 0.03f);
    }

    /**
     * Молния с параметрами FXLightningBolt оригинала.
     *
     * @param duration   сколько живёт (в шагах возраста)
     * @param multiplier размах изломов относительно длины
     * @param speed      на сколько шагов возраст растёт за тик
     * @param width      толщина линии
     */
    BoltParticle(ClientLevel level, Vec3 start, Vec3 end, long seed, int type, boolean core, int duration, float multiplier,
                 int speed, float width) {
        super(level, start.x, start.y, start.z);
        this.width = width;
        this.type = type;
        this.floaty = false;
        this.core = core;
        this.hasPhysics = false;
        this.rand = new Random(seed);
        this.length = (float) start.distanceTo(end);
        this.boltMaxAge = duration + rand.nextInt(duration) - duration / 2;
        this.increment = speed;
        this.boltAge = -(int) (length * 3f);
        this.lifetime = Integer.MAX_VALUE;
        float[] color = (core ? CORE_COLORS : GLOW_COLORS)[Math.floorMod(type, GLOW_COLORS.length)];
        setColor(color[0], color[1], color[2]);
        segments.add(new Segment(start, end, 1f, 0, true, true));
        // defaultFractal оригинала
        fractal(length * multiplier / 8f, 0.7f, 45f);
        fractal(length * multiplier / 12f, 0.5f, 50f);
        fractal(length * multiplier / 17f, 0.5f, 55f);
        fractal(length * multiplier / 23f, 0.5f, 60f);
        fractal(length * multiplier / 30f, 0f, 0f);
        fractal(length * multiplier / 34f, 0f, 0f);
        fractal(length * multiplier / 40f, 0f, 0f);
        setBoundingBox(new AABB(start, end).inflate(length * 0.5 + 1));
    }

    /** Колышущаяся лента: плавная волна вместо ломаной, живёт пару тиков и каждый тик рисуется заново. */
    BoltParticle(ClientLevel level, Vec3 start, Vec3 end, int color) {
        super(level, start.x, start.y, start.z);
        this.floaty = true;
        this.core = false;
        this.width = 0.07f;
        lifetime = 2;
        hasPhysics = false;
        setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
        double distance = start.distanceTo(end);
        int steps = Math.max(4, (int) (distance * 6));
        double time = level.getGameTime() / 3.0;
        Vec3 previous = start;
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            // волна затухает к обоим концам
            double swing = Math.sin(t * Math.PI) * 0.12;
            Vec3 point = start.lerp(end, t).add(Math.sin(t * 9.0 + time) * swing, Math.cos(t * 7.0 + time * 1.3) * swing,
                    Math.sin(t * 8.0 - time) * swing);
            segments.add(new Segment(previous, point, 1f, i, i == steps, i == 1));
            previous = point;
        }
        setBoundingBox(new AABB(start, end).inflate(1));
    }

    private static Vec3 perpendicular(Vec3 v) {
        return v.z == 0 ? new Vec3(v.y, -v.x, 0) : new Vec3(0, v.z, -v.y);
    }

    /** Поворот вектора вокруг оси на угол в градусах (формула Родрига). */
    private static Vec3 rotate(Vec3 v, float degrees, Vec3 axis) {
        Vec3 k = axis.normalize();
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(k.cross(v).scale(sin)).add(k.scale(k.dot(v) * (1 - cos)));
    }

    /**
     * Один проход дробления (fractal оригинала при splits = 2): каждый отрезок делится пополам, середина
     * смещается вбок не больше чем на amount / 2, и с шансом splitChance от середины уходит короткая ветка.
     */
    private void fractal(float amount, float splitChance, float splitAngle) {
        List<Segment> next = new ArrayList<>();
        for (Segment segment : segments) {
            Vec3 diff = segment.diff();
            Vec3 offset = rotate(perpendicular(diff).normalize(), rand.nextFloat() * 360f, diff).scale((rand.nextFloat() - 0.5f) * amount);
            Vec3 middle = segment.from.add(diff.scale(0.5)).add(offset);
            Segment head = new Segment(segment.from, middle, segment.light, segment.number * 2, false, segment.first);
            Segment tail = new Segment(middle, segment.to, segment.light, segment.number * 2 + 1, segment.last, false);
            next.add(head);
            if (rand.nextFloat() < splitChance) {
                Vec3 tailDiff = tail.diff();
                Vec3 axis = rotate(perpendicular(tailDiff), rand.nextFloat() * 360f, tailDiff);
                Vec3 branch = rotate(tailDiff, (rand.nextFloat() * 0.66f + 0.33f) * splitAngle, axis).scale(0.1);
                // ветка начинается в середине и идёт рядом со стволом, заметно тусклее его
                next.add(new Segment(middle, segment.to.add(branch), segment.light / 2f, tail.number, true, false));
            }
            next.add(tail);
        }
        segments = next;
        segmentCount *= 2;
    }

    @Override
    public void tick() {
        if (floaty) {
            if (age++ >= lifetime) {
                remove();
            }
            return;
        }
        boltAge += increment;
        if (boltAge >= boltMaxAge) {
            remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return floaty ? TcParticle.ADDITIVE : core ? CORE : GLOW;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        Vec3 look = new Vec3(camera.getLookVector());
        if (floaty) {
            float fade = 1f - (age + partialTick) / (lifetime + 1f);
            for (Segment segment : segments) {
                Vec3 a = segment.from.subtract(cam);
                Vec3 c = segment.to.subtract(cam);
                Vec3 side = c.subtract(a).cross(a).normalize().scale(width);
                // средняя полоска ячейки-свечения: вдоль ленты яркость ровная, к краям гаснет
                vertex(consumer, a.subtract(side), 0.0625f, 0.885f, fade);
                vertex(consumer, a.add(side), 0.0625f, 0.99f, fade);
                vertex(consumer, c.add(side), 0.0625f, 0.99f, fade);
                vertex(consumer, c.subtract(side), 0.0625f, 0.885f, fade);
            }
            return;
        }

        float progress = boltAge >= 0 ? boltAge / (float) boltMaxAge : 0f;
        float mainAlpha = core ? 1f - progress * 0.5f : (1f - progress) * 0.4f;
        // молнии упряжи (тип 6) почти прозрачны, как в оригинале
        if (type == 6) {
            mainAlpha *= 0.2f;
        }
        int grow = (int) (length * 3f);
        // сколько отрезков ствола уже «выросло»
        int renderLength = grow == 0 ? segmentCount : (int) ((boltAge + partialTick + grow) / grow * segmentCount);
        for (Segment segment : segments) {
            if (segment.number > renderLength) {
                continue;
            }
            Vec3 a = segment.from.subtract(cam);
            Vec3 c = segment.to.subtract(cam);
            // чем дальше молния, тем шире лента, чтобы её было видно издали
            float w = width * ((float) a.length() / 5f + 1f) * (1f + segment.light) * 0.5f;
            Vec3 direction = segment.diff().normalize();
            Vec3 side = look.cross(direction).normalize().scale(w);
            float alpha = mainAlpha * segment.light;
            vertex(consumer, c.subtract(side), 0.5f, 0f, alpha);
            vertex(consumer, a.subtract(side), 0.5f, 0f, alpha);
            vertex(consumer, a.add(side), 0.5f, 1f, alpha);
            vertex(consumer, c.add(side), 0.5f, 1f, alpha);
            // скруглённые концы
            if (segment.last) {
                Vec3 round = c.add(direction.scale(w));
                vertex(consumer, round.subtract(side), 0f, 0f, alpha);
                vertex(consumer, c.subtract(side), 0.5f, 0f, alpha);
                vertex(consumer, c.add(side), 0.5f, 1f, alpha);
                vertex(consumer, round.add(side), 0f, 1f, alpha);
            }
            if (segment.first) {
                Vec3 round = a.subtract(direction.scale(w));
                vertex(consumer, a.subtract(side), 0.5f, 0f, alpha);
                vertex(consumer, round.subtract(side), 0f, 0f, alpha);
                vertex(consumer, round.add(side), 0f, 1f, alpha);
                vertex(consumer, a.add(side), 0.5f, 1f, alpha);
            }
        }
    }

    private void vertex(VertexConsumer consumer, Vec3 pos, float u, float v, float a) {
        consumer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(rCol, gCol, bCol, a).uv2(0xf000f0).endVertex();
    }
}
