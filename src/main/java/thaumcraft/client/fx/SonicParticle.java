package thaumcraft.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;

/**
 * Вопль стража Элдрича — перенос FXSonic оригинала: вытянутая вперёд полусфера ряби бьёт из головы существа
 * туда, куда оно смотрит, и расходится за пятнадцать кадров.
 */
public class SonicParticle extends Particle {
    private final Entity target;
    private final float yaw;
    private final float pitch;

    SonicParticle(ClientLevel level, Entity target, int age) {
        super(level, target.getX(), target.getEyeY(), target.getZ());
        this.target = target;
        this.yaw = target.getYHeadRot();
        this.pitch = target.getXRot();
        this.lifetime = age + random.nextInt(age / 2);
        this.hasPhysics = false;
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
        x = target.getX();
        y = target.getEyeY();
        z = target.getZ();
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        float[] vertices = ShieldParticle.mesh();
        if (vertices.length == 0) {
            return;
        }
        float fade = (age + partialTick) / lifetime;
        int frame = Math.min(15, (int) (14f * fade) + 1);
        Vec3 cam = camera.getPosition();
        float h = target.getBbHeight();
        Matrix4f matrix = new Matrix4f()
                .translate((float) (Mth.lerp(partialTick, xo, x) - cam.x), (float) (Mth.lerp(partialTick, yo, y) - cam.y),
                        (float) (Mth.lerp(partialTick, zo, z) - cam.z))
                .rotate(Axis.YP.rotationDegrees(-yaw))
                .rotate(Axis.XP.rotationDegrees(pitch))
                .translate(0f, 0f, 2f * h + target.getBbWidth() / 2f)
                .scale(0.25f * h, 0.25f * h, -h);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Thaumcraft.id("textures/models/ripple" + frame + ".png"));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < vertices.length; i += 5) {
            buffer.vertex(matrix, vertices[i], vertices[i + 1], vertices[i + 2]).uv(vertices[i + 3], vertices[i + 4])
                    .color(0.5f, 0.5f, 0.5f, 1f).endVertex();
        }
        Tesselator.getInstance().end();
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
