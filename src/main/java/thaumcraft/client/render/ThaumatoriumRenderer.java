package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.ThaumatoriumBlock;
import thaumcraft.common.crafting.CrucibleRecipe;

/**
 * Тауматорий: сам корпус — обычная модель блока, а здесь, как в TileThaumatoriumRenderer оригинала, на лицевой
 * стороне показывается то, что он делает; если рецептов несколько, они сменяются раз в две секунды.
 */
public class ThaumatoriumRenderer implements BlockEntityRenderer<ThaumatoriumBlock.Core> {
    private final ItemRenderer items;

    public ThaumatoriumRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(ThaumatoriumBlock.Core core, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (core.recipes.isEmpty() || core.getLevel() == null || Minecraft.getInstance().player == null) {
            return;
        }
        int shown = Minecraft.getInstance().player.tickCount / 40 % core.recipes.size();
        CrucibleRecipe recipe = core.recipe(core.recipes.get(shown));
        if (recipe == null) {
            return;
        }
        Direction facing = core.getFacing();
        ItemStack stack = recipe.getResult().copyWithCount(1);
        int front = LevelRenderer.getLightColor(core.getLevel(), core.getBlockPos().above().relative(facing));
        pose.pushPose();
        pose.translate(0.5 + facing.getStepX() / 1.99, 1.5, 0.5 + facing.getStepZ() / 1.99);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        pose.scale(0.375f, 0.375f, 0.375f);
        items.renderStatic(stack, ItemDisplayContext.FIXED, front, OverlayTexture.NO_OVERLAY, pose, buffer, core.getLevel(), 0);
        pose.popPose();
    }
}
