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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;

/**
 * Вспышка рунного щита на грани защищённого блока — перенос FXBlockWard оригинала:
 * шестнадцать кадров (hemis0..15), случайно повёрнутых, вспыхивают и гаснут там, куда пришёлся удар.
 */
public class BlockWardParticle extends Particle {
    private static final ResourceLocation[] FRAMES = new ResourceLocation[16];

    static {
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = Thaumcraft.id("textures/models/hemis" + i + ".png");
        }
    }

    private final Direction side;
    private final float rotation;
    private final float scale;
    private final Vec3 shift;

    BlockWardParticle(ClientLevel level, double x, double y, double z, Direction side, float fx, float fy, float fz) {
        super(level, x + 0.5, y + 0.5, z + 0.5);
        this.side = side;
        this.hasPhysics = false;
        this.lifetime = 12 + random.nextInt(5);
        this.scale = (float) (1.4 + random.nextGaussian() * 0.3);
        this.rotation = random.nextInt(360) * Mth.DEG_TO_RAD;
        // сдвиг к месту удара, но так, чтобы вспышка не уползала за край грани
        float sx = side.getStepX() != 0 ? 0f : Mth.clamp(fx - 0.6f + random.nextFloat() * 0.2f, -0.4f, 0.4f);
        float sy = side.getStepY() != 0 ? 0f : Mth.clamp(fy - 0.6f + random.nextFloat() * 0.2f, -0.4f, 0.4f);
        float sz = side.getStepZ() != 0 ? 0f : Mth.clamp(fz - 0.6f + random.nextFloat() * 0.2f, -0.4f, 0.4f);
        this.shift = new Vec3(sx, sy, sz);
        this.alpha = 0f;
    }

    @Override
    public void tick() {
        float threshold = lifetime / 5f;
        alpha = age <= threshold ? age / threshold : (float) (lifetime - age) / lifetime;
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        float fade = Math.min(1f, (age + partialTick) / lifetime);
        int frame = Math.min(15, (int) (15f * fade));
        Vec3 normal = new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());
        Vec3 center = new Vec3(x, y, z).add(shift).add(normal.scale(0.505)).subtract(camera.getPosition());
        Vec3 u = normal.cross(Math.abs(normal.y) > 0.5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = normal.cross(u);
        // поворот в плоскости грани
        Vec3 ru = u.scale(Mth.cos(rotation)).add(v.scale(Mth.sin(rotation))).scale(0.5 * scale);
        Vec3 rv = v.scale(Mth.cos(rotation)).subtract(u.scale(Mth.sin(rotation))).scale(0.5 * scale);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, FRAMES[frame]);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        put(buffer, center.subtract(ru).add(rv), 0f, 1f);
        put(buffer, center.add(ru).add(rv), 1f, 1f);
        put(buffer, center.add(ru).subtract(rv), 1f, 0f);
        put(buffer, center.subtract(ru).subtract(rv), 0f, 0f);
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void put(BufferBuilder buffer, Vec3 pos, float u, float v) {
        buffer.vertex(pos.x, pos.y, pos.z).uv(u, v).color(1f, 1f, 1f, alpha / 2f).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
