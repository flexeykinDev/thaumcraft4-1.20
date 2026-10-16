package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.items.armor.HoverHarnessItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Летательная упряжь на игроке — перенос ModelHoverHarness оригинала: ремни на туловище, механизм на спине
 * (OBJ-модель оригинала), а в парении — светящееся кольцо за спиной и молнии, бьющие с упряжи в ближние блоки.
 */
public class HoverHarnessLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation BACK = Thaumcraft.id("block/hover_harness_back");
    private static final ResourceLocation RING = Thaumcraft.id("item/lightningring");
    private static HumanoidModel<LivingEntity> body;
    private static final Map<Integer, Long> SHOCKS = new HashMap<>();

    public HoverHarnessLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Ремни упряжи: одна коробка туловища, чуть шире самого туловища. */
    public static HumanoidModel<LivingEntity> bodyModel() {
        if (body == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            for (String part : new String[]{"head", "hat", "right_arm", "left_arm", "right_leg", "left_leg"}) {
                root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
            }
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4f, 0f, -2f, 8f, 12f, 4f,
                    new CubeDeformation(0.6f)), PartPose.ZERO);
            body = new HumanoidModel<>(LayerDefinition.create(mesh, 128, 64).bakeRoot());
        }
        return body;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HoverHarnessItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // механизм на спине
        pose.pushPose();
        pose.scale(0.1f, 0.1f, 0.1f);
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        pose.translate(0f, 0.33f, -3.7f);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                null, mc.getModelManager().getModel(BACK), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        if (HoverHarnessItem.isHovering(player)) {
            TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(RING);
            VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.additive(InventoryMenu.BLOCK_ATLAS));
            pose.pushPose();
            pose.translate(0f, 0.2f, 0.55f);
            ring(pose.last().pose(), consumer, sprite, 2.5f, 1f, 1f, 1f);
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.translate(0f, 0f, 0.03f);
            ring(pose.last().pose(), consumer, sprite, 1.5f, 1f, 0.5f, 1f);
            pose.popPose();
            shock(player);
        }
        pose.popPose();
    }

    private static void ring(Matrix4f matrix, VertexConsumer consumer, TextureAtlasSprite sprite, float size, float r, float g, float b) {
        float h = size / 2f;
        consumer.vertex(matrix, -h, h, 0f).uv(sprite.getU0(), sprite.getV1()).color(r, g, b, 1f).endVertex();
        consumer.vertex(matrix, h, h, 0f).uv(sprite.getU1(), sprite.getV1()).color(r, g, b, 1f).endVertex();
        consumer.vertex(matrix, h, -h, 0f).uv(sprite.getU1(), sprite.getV0()).color(r, g, b, 1f).endVertex();
        consumer.vertex(matrix, -h, -h, 0f).uv(sprite.getU0(), sprite.getV0()).color(r, g, b, 1f).endVertex();
    }

    /** Раз в 50-100 мс молния с упряжи бьёт в случайный блок в шести блоках за спиной и по бокам. */
    private static void shock(AbstractClientPlayer player) {
        long now = System.currentTimeMillis();
        Long next = SHOCKS.get(player.getId());
        if (next != null && next > now) {
            return;
        }
        SHOCKS.put(player.getId(), now + 50L + player.getRandom().nextInt(50));
        float mod = player.isCrouching() ? 0.075f : 0f;
        double sx = player.getX() - Mth.cos((player.yBodyRot + 90f) * Mth.DEG_TO_RAD) * 0.5;
        double sy = player.getY() + 1.62 - 0.45 - mod;
        double sz = player.getZ() - Mth.sin((player.yBodyRot + 90f) * Mth.DEG_TO_RAD) * 0.5;
        float yaw = player.yBodyRot - 90f - player.getRandom().nextInt(180);
        float pitch = -80f + player.getRandom().nextInt(160);
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 from = new Vec3(sx, sy, sz);
        BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(dir.scale(6.0)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Fx.thinBolt(sx, sy, sz, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 6, 0.015f);
        }
    }
}
