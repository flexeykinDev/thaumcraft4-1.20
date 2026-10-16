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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Вспышка рунного щита — перенос FXShieldRunes оригинала: полусфера из рун, повёрнутая в сторону удара, проступает
 * вокруг существа и гаснет за пятнадцать кадров. У монстров вместо рун — рябь.
 */
public class ShieldParticle extends Particle {
    private static final ResourceLocation MODEL = Thaumcraft.id("textures/models/hemis.obj");
    /** Треугольники полусферы: по пять чисел на вершину (x, y, z, u, v). */
    private static float[] mesh;

    private final Entity target;
    private final float yaw;
    private final float pitch;

    ShieldParticle(ClientLevel level, Entity target, int age, float yaw, float pitch) {
        super(level, target.getX(), target.getBoundingBox().getCenter().y, target.getZ());
        this.target = target;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lifetime = age + random.nextInt(age / 2);
        this.hasPhysics = false;
    }

    /** Модель оригинала читается как есть: вершины, текстурные координаты и грани (многоугольники режутся веером). */
    static float[] mesh() {
        if (mesh != null) {
            return mesh;
        }
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<Float> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Minecraft.getInstance().getResourceManager().open(MODEL), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts[0].equals("v") && parts.length >= 4) {
                    positions.add(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                } else if (parts[0].equals("vt") && parts.length >= 3) {
                    uvs.add(new float[]{Float.parseFloat(parts[1]), 1f - Float.parseFloat(parts[2])});
                } else if (parts[0].equals("f") && parts.length >= 4) {
                    for (int i = 2; i < parts.length - 1; i++) {
                        for (String corner : new String[]{parts[1], parts[i], parts[i + 1]}) {
                            String[] index = corner.split("/");
                            float[] position = positions.get(Integer.parseInt(index[0]) - 1);
                            float[] uv = index.length > 1 && !index[1].isEmpty() ? uvs.get(Integer.parseInt(index[1]) - 1) : new float[]{0f, 0f};
                            out.add(position[0]);
                            out.add(position[1]);
                            out.add(position[2]);
                            out.add(uv[0]);
                            out.add(uv[1]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Не удалось прочитать модель рунного щита", e);
        }
        mesh = new float[out.size()];
        for (int i = 0; i < mesh.length; i++) {
            mesh[i] = out.get(i);
        }
        return mesh;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime || !target.isAlive()) {
            remove();
            return;
        }
        x = target.getX();
        y = target.getBoundingBox().getCenter().y;
        z = target.getZ();
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        float[] vertices = mesh();
        if (vertices.length == 0) {
            return;
        }
        float fade = Math.min(1f, (age + partialTick) / lifetime);
        int frame = Math.min(15, (int) (14f * fade) + 1);
        boolean monster = target instanceof Monster;
        float brightness = monster ? 0.5f : 1f;
        float alpha = Math.min(1f, (1f - fade) * 3f);
        Vec3 cam = camera.getPosition();
        float size = 0.4f * target.getBbHeight();
        Matrix4f matrix = new Matrix4f()
                .translate((float) (Mth.lerp(partialTick, xo, x) - cam.x), (float) (Mth.lerp(partialTick, yo, y) - cam.y),
                        (float) (Mth.lerp(partialTick, zo, z) - cam.z))
                .rotate(Axis.YP.rotationDegrees(180f - yaw))
                .rotate(Axis.XP.rotationDegrees(-pitch))
                .scale(size);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Thaumcraft.id("textures/models/" + (monster ? "ripple" : "hemis") + frame + ".png"));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < vertices.length; i += 5) {
            buffer.vertex(matrix, vertices[i], vertices[i + 1], vertices[i + 2]).uv(vertices[i + 3], vertices[i + 4])
                    .color(brightness, brightness, brightness, alpha).endVertex();
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
