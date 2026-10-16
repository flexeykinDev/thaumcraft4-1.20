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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * Непрерывный луч от жезла к цели — перенос FXBeamWand оригинала: три скрещённые полупрозрачные ленты
 * с бегущей текстурой, которые вращаются вокруг оси луча, и вспышка в точке удара.
 * Пока заклинание длится, луч не создаётся заново, а продлевается ({@link #update}).
 */
public class WandBeamParticle extends Particle {
    private static final ResourceLocation[] TEXTURES = {
            Thaumcraft.id("textures/misc/beam.png"), Thaumcraft.id("textures/misc/beam1.png"),
            Thaumcraft.id("textures/misc/beam2.png"), Thaumcraft.id("textures/misc/beam3.png")};
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

    private final Player player;
    /** Начало луча, если он идёт не от жезла игрока, а из устройства (луч чародейского бура). */
    private final Vec3 fixedOrigin;
    private final int type;
    private final boolean reverse;
    private Vec3 target;
    private Vec3 prevTarget;
    private float endMod = 1f;
    private int impact;
    private float prevSize;

    WandBeamParticle(ClientLevel level, Player player, Vec3 target, int type, int color, boolean reverse) {
        super(level, player.getX(), player.getY(), player.getZ());
        this.player = player;
        this.fixedOrigin = null;
        this.type = Mth.clamp(type, 0, TEXTURES.length - 1);
        this.reverse = reverse;
        this.target = target;
        this.prevTarget = target;
        this.hasPhysics = false;
        this.lifetime = 8;
        setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
    }

    /** Луч из неподвижной точки (FXBeamBore оригинала). */
    WandBeamParticle(ClientLevel level, Vec3 origin, Vec3 target, int type, int color, boolean reverse) {
        super(level, origin.x, origin.y, origin.z);
        this.player = null;
        this.fixedOrigin = origin;
        this.type = Mth.clamp(type, 0, TEXTURES.length - 1);
        this.reverse = reverse;
        this.target = target;
        this.prevTarget = target;
        this.hasPhysics = false;
        this.lifetime = 8;
        setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
    }

    /** Продлевает луч и переносит его конец. */
    void update(Vec3 newTarget, float newEndMod, int newImpact) {
        target = newTarget;
        endMod = newEndMod;
        impact = newImpact;
        while (lifetime - age < 4) {
            lifetime++;
        }
    }

    @Override
    public void tick() {
        prevTarget = target;
        if (impact > 0) {
            impact--;
        }
        if (age++ >= lifetime || player != null && !player.isAlive()) {
            remove();
        }
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    /** Вершина жезла: у себя в первом лице — чуть впереди глаз, у других игроков — на уровне груди. */
    private Vec3 origin(float partialTick) {
        if (fixedOrigin != null) {
            return fixedOrigin;
        }
        // если жезл только что рисовался, луч выходит точно из его вершины
        Vec3 tip = thaumcraft.client.render.WandItemRenderer.tipOf(player.getId());
        if (tip != null) {
            return tip;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean own = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        Vec3 base = own ? player.getEyePosition(partialTick)
                : player.getPosition(partialTick).add(0, player.getBbHeight() / 2f + 0.25, 0);
        float yaw = Mth.lerp(partialTick, player.yRotO, player.getYRot()) * Mth.DEG_TO_RAD;
        return base.add(-Mth.cos(yaw) * 0.066, -0.06, -Mth.sin(yaw) * 0.04).add(player.getViewVector(partialTick).scale(0.3));
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        Vec3 start = origin(partialTick).subtract(cam);
        Vec3 end = prevTarget.lerp(target, partialTick).subtract(cam);
        Vec3 axis = end.subtract(start);
        float length = (float) axis.length();
        if (length < 1.0e-3f) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        Vec3 u = dir.cross(Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = dir.cross(u);

        float size = Math.min(age / 4f, 1f);
        size = prevSize + (size - prevSize) * partialTick;
        float opacity = 0.4f;
        if (lifetime - age <= 4) {
            opacity = 0.4f - (4 - (lifetime - age)) * 0.1f;
        }
        float slide = (player != null ? player.tickCount : (float) level.getGameTime()) + partialTick;
        if (reverse) {
            slide = -slide;
        }
        float shift = -slide * 0.2f - Mth.floor(-slide * 0.1f);
        float spin = (level.getGameTime() % 72 * 5 + 5 * partialTick) * Mth.DEG_TO_RAD;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURES[type]);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vec3 far = start.add(dir.scale(length * size));
        for (int t = 0; t < 3; t++) {
            float angle = spin + (t + 1) * 60f * Mth.DEG_TO_RAD;
            Vec3 side = u.scale(Mth.cos(angle)).add(v.scale(Mth.sin(angle)));
            Vec3 near = side.scale(0.15 * size);
            Vec3 wide = side.scale(0.15 * size * endMod);
            float v0 = -1f + shift + t / 3f;
            float v1 = length * size + v0;
            put(buffer, far.subtract(wide), 1f, v1, opacity);
            put(buffer, start.subtract(near), 1f, v0, opacity);
            put(buffer, start.add(near), 0f, v0, opacity);
            put(buffer, far.add(wide), 0f, v1, opacity);
        }
        tesselator.end();

        if (impact > 0) {
            // вспышка в точке удара: кадр из листа частиц, развёрнутый к камере
            RenderSystem.setShaderTexture(0, PARTICLES);
            float u0 = age % 16 / 16f;
            float u1 = u0 + 0.0624375f;
            float w0 = 0.3125f;
            float w1 = w0 + 0.0624375f;
            float half = endMod / 2f / (6 - impact);
            Vector3f left = camera.getLeftVector();
            Vector3f up = camera.getUpVector();
            Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(half);
            Vec3 h = new Vec3(up.x(), up.y(), up.z()).scale(half);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            put(buffer, end.add(l).subtract(h), u1, w1, 0.66f);
            put(buffer, end.add(l).add(h), u1, w0, 0.66f);
            put(buffer, end.subtract(l).add(h), u0, w0, 0.66f);
            put(buffer, end.subtract(l).subtract(h), u0, w1, 0.66f);
            tesselator.end();
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        prevSize = Math.min(age / 4f, 1f);
    }

    private void put(BufferBuilder buffer, Vec3 pos, float u, float v, float alpha) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(rCol, gCol, bCol, alpha).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
