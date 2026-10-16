package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blocks.NodeJarBlock;
import thaumcraft.common.registry.ModBlocks;

/**
 * Узел в банке — TileJarRenderer (для TileJarNode) и ItemJarNodeRenderer оригинала: в мире узел рисуется так же,
 * как свободный, чуть ниже середины банки; в руке и в интерфейсе — три скрещённых слоя узла, видимые всегда.
 */
public class NodeJarRenderer implements BlockEntityRenderer<NodeJarBlock.Jar> {
    private final NodeRenderer nodes;

    public NodeJarRenderer(BlockEntityRendererProvider.Context context) {
        nodes = new NodeRenderer(context);
    }

    @Override
    public void render(NodeJarBlock.Jar jar, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.0, -0.1, 0.0);
        nodes.render(jar.ghost(), partialTick, pose, buffer, light, overlay);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(NodeJarBlock.Jar jar) {
        return true;
    }

    /** ItemNodeRenderer.renderItemNode оригинала: слои узла без поворота к камере. */
    static void itemNode(PoseStack pose, MultiBufferSource buffer, AspectList aspects, NodeType type, NodeModifier modifier) {
        if (aspects.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float ticks = mc.player == null ? 0 : mc.player.tickCount;
        float alpha = 0.5f;
        if (modifier == NodeModifier.BRIGHT) {
            alpha *= 1.5f;
        } else if (modifier == NodeModifier.PALE) {
            alpha *= 0.66f;
        } else if (modifier == NodeModifier.FADING) {
            alpha *= Mth.sin(ticks / 3f) * 0.25f + 0.33f;
        }
        long nt = System.nanoTime();
        int frame = (int) ((nt / 40_000_000L + 1L) % NodeRenderer.FRAMES);
        int count = 0;
        float average = 0f;
        for (Aspect aspect : aspects.getAspects()) {
            float layerAlpha = aspect.isAdditiveBlend() ? alpha : alpha * 1.5f;
            average += aspects.getAmount(aspect);
            float scale = Mth.sin(ticks / (14f - count)) * 0.25f + 0.5f;
            scale = 0.2f + scale * (aspects.getAmount(aspect) / 50f);
            NodeRenderer.drawLayer(pose, buffer.getBuffer(aspect.isAdditiveBlend() ? ModRenderTypes.NODE_GLOW : ModRenderTypes.NODE_DARK), 0f, scale,
                    layerAlpha / aspects.size(), 0, frame, aspect.getColor());
            count++;
        }
        average /= aspects.size();
        float scale = 0.1f + average / 150f;
        int strip = 1;
        boolean additive = true;
        switch (type) {
            case UNSTABLE -> strip = 6;
            case DARK -> {
                strip = 2;
                additive = false;
            }
            case TAINTED -> {
                strip = 5;
                additive = false;
            }
            case PURE -> strip = 4;
            case HUNGRY -> {
                scale *= 0.75f;
                strip = 3;
            }
            default -> {
            }
        }
        NodeRenderer.drawLayer(pose, buffer.getBuffer(additive ? ModRenderTypes.NODE_GLOW : ModRenderTypes.NODE_DARK), 0f, scale, alpha, strip, frame,
                0xffffff);
    }

    /** ItemNodeRenderer оригинала: узел ауры в руке и в инвентаре — обычный узел четырёх стихий, втрое крест-накрест. */
    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer NODE_ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                private final AspectList aspects = new AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 40)
                        .add(thaumcraft.api.aspects.Aspect.FIRE, 40).add(thaumcraft.api.aspects.Aspect.EARTH, 40)
                        .add(thaumcraft.api.aspects.Aspect.WATER, 40);

                @Override
                public void renderByItem(ItemStack stack, net.minecraft.world.item.ItemDisplayContext context, PoseStack pose,
                                         MultiBufferSource buffer, int light, int overlay) {
                    pose.pushPose();
                    pose.translate(0.5, 0.5, 0.5);
                    pose.scale(2f, 2f, 2f);
                    itemNode(pose, buffer, aspects, NodeType.NORMAL, null);
                    pose.mulPose(Axis.YP.rotationDegrees(90f));
                    itemNode(pose, buffer, aspects, NodeType.NORMAL, null);
                    pose.mulPose(Axis.XP.rotationDegrees(90f));
                    itemNode(pose, buffer, aspects, NodeType.NORMAL, null);
                    pose.popPose();
                }
            };

    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                @Override
                public void renderByItem(ItemStack stack, net.minecraft.world.item.ItemDisplayContext context, PoseStack pose,
                                         MultiBufferSource buffer, int light, int overlay) {
                    Minecraft mc = Minecraft.getInstance();
                    if (stack.hasTag()) {
                        var tag = stack.getTag();
                        AspectList aspects = AspectList.load(tag.getCompound("Aspects"));
                        NodeType type = NodeType.values()[Math.max(0, Math.min(NodeType.values().length - 1, tag.getByte("type")))];
                        int mod = tag.getByte("modifier");
                        NodeModifier modifier = mod >= 0 && mod < NodeModifier.values().length ? NodeModifier.values()[mod] : null;
                        pose.pushPose();
                        pose.translate(0.5, 0.4, 0.5);
                        itemNode(pose, buffer, aspects, type, modifier);
                        pose.mulPose(Axis.YP.rotationDegrees(90f));
                        itemNode(pose, buffer, aspects, type, modifier);
                        pose.mulPose(Axis.XP.rotationDegrees(90f));
                        itemNode(pose, buffer, aspects, type, modifier);
                        pose.popPose();
                    }
                    var model = mc.getBlockRenderer().getBlockModel(ModBlocks.NODE_JAR.get().defaultBlockState());
                    mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),
                            null, model, 1f, 1f, 1f, light, overlay);
                }
            };
}
