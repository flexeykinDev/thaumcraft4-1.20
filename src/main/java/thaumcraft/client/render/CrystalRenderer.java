package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.CrystalClusterBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Кристаллическая гроздь — перенос TileCrystalRenderer оригинала: один крупный кристалл и пять поменьше вокруг,
 * у каждой грозди свои наклоны и размеры (они зависят от места), кристаллы слегка мерцают.
 */
public class CrystalRenderer implements BlockEntityRenderer<CrystalClusterBlock.Crystal> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/crystal.png");
    /** ModelCrystal оригинала: куб, повёрнутый углом вверх. */
    private static final ModelPart CRYSTAL = new ModelPart(List.of(new ModelPart.Cube(0, 0, -16f, -16f, 0f, 16f, 16f, 16f, 0f, 0f, 0f,
            true, 64f, 32f, EnumSet.allOf(Direction.class))), Map.of());

    static {
        CRYSTAL.setPos(0f, 32f, 0f);
        CRYSTAL.xRot = 0.7071f;
        CRYSTAL.zRot = 0.7071f;
    }

    public CrystalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystalClusterBlock.Crystal tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (!(tile.getBlockState().getBlock() instanceof CrystalClusterBlock block)) {
            return;
        }
        BlockPos pos = tile.getBlockPos();
        draw(pose, buffer, block.kind, tile.getBlockState().getValue(CrystalClusterBlock.FACING),
                block.kind + pos.getX() + (long) pos.getY() * pos.getZ());
    }

    /** Гроздь в кубе от 0 до 1. */
    public static void draw(PoseStack pose, MultiBufferSource buffer, int kind, Direction facing, long seed) {
        Random rand = new Random(seed);
        // у смешанной грозди большой кристалл цвета порядка, малые — остальных стихий
        int color = CrystalClusterBlock.COLORS[kind < 6 ? kind : 4];
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        crystal(pose, consumer, facing, (rand.nextFloat() - rand.nextFloat()) * 5f, (rand.nextFloat() - rand.nextFloat()) * 5f,
                rand, color, 1.1f);
        for (int a = 1; a < 6; a++) {
            if (kind >= 6) {
                color = CrystalClusterBlock.COLORS[a == 5 ? 5 : a - 1];
            }
            int angle1 = rand.nextInt(36) + 72 * a;
            int angle2 = 15 + rand.nextInt(15);
            crystal(pose, consumer, facing, angle1, angle2, rand, color, 0.8f);
        }
    }

    private static void crystal(PoseStack pose, VertexConsumer consumer, Direction facing, float a1, float a2, Random rand, int color,
                                float size) {
        Player player = Minecraft.getInstance().player;
        int ticks = player == null ? 0 : player.tickCount;
        float shade = Mth.sin((ticks + rand.nextInt(10)) / (5f + rand.nextFloat())) * 0.075f + 0.925f;
        float r = Math.min(1f, (color >> 16 & 255) / 220f);
        float g = Math.min(1f, (color >> 8 & 255) / 220f);
        float b = Math.min(1f, (color & 255) / 220f);
        pose.pushPose();
        switch (facing) {
            case DOWN -> {
                pose.translate(0.5f, 1.3f, 0.5f);
                pose.mulPose(Axis.XP.rotationDegrees(180f));
            }
            case UP -> pose.translate(0.5f, -0.3f, 0.5f);
            case NORTH -> {
                pose.translate(0.5f, 0.5f, 1.3f);
                pose.mulPose(Axis.XP.rotationDegrees(-90f));
            }
            case SOUTH -> {
                pose.translate(0.5f, 0.5f, -0.3f);
                pose.mulPose(Axis.XP.rotationDegrees(90f));
            }
            case WEST -> {
                pose.translate(1.3f, 0.5f, 0.5f);
                pose.mulPose(Axis.ZP.rotationDegrees(90f));
            }
            case EAST -> {
                pose.translate(-0.3f, 0.5f, 0.5f);
                pose.mulPose(Axis.ZP.rotationDegrees(-90f));
            }
        }
        pose.mulPose(Axis.YP.rotationDegrees(a1));
        pose.mulPose(Axis.XP.rotationDegrees(a2));
        pose.scale((0.15f + rand.nextFloat() * 0.075f) * size, (0.5f + rand.nextFloat() * 0.1f) * size,
                (0.15f + rand.nextFloat() * 0.05f) * size);
        // кристаллы светятся сами: яркость не зависит от освещения вокруг и слегка дрожит
        int glow = (int) (210f * shade);
        CRYSTAL.render(pose, consumer, glow, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
        pose.popPose();
    }

    /** Гроздь как предмет: та же отрисовка, кристаллы растут вверх. */
    public static final BlockEntityWithoutLevelRenderer ITEM = new BlockEntityWithoutLevelRenderer(
            Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light,
                                 int overlay) {
            if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CrystalClusterBlock block) {
                draw(pose, buffer, block.kind, Direction.UP, block.kind);
            }
        }
    };
}
