package thaumcraft.common.items.wands.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.blocks.HoleBlock;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;

/**
 * Фокус «Портативная дыра» (ItemFocusPortableHole оригинала): пробивает в стене временный тоннель 3x3
 * глубиной до 33 блоков. Через шесть секунд блоки возвращаются на место. Цена — 0.1 воздуха и энтропии
 * за каждый блок глубины.
 */
public class PortableHoleFocus extends FocusItem {
    private static final AspectList COST = new AspectList().add(Aspect.ENTROPY, 10).add(Aspect.AIR, 10);
    private static final ResourceLocation DEPTH = Thaumcraft.id("textures/unstitched/item/focus_portablehole_depth.png");

    @Override
    protected String sortingPrefix() {
        return "BPH";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0x091429;
    }

    @Override
    public ResourceLocation getDepthTexture(ItemStack focus) {
        return DEPTH;
    }

    @Override
    public int getDepthFrames(ItemStack focus) {
        return 16;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return COST.copy();
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        Vec3 eye = player.getEyePosition();
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        // TODO: во Внешних землях фокус не работает (звук wandfail) — когда появится это измерение
        ItemStack focus = WandItem.getFocusStack(wand);
        Direction inward = hit.getDirection().getOpposite();
        int max = 33 + getUpgradeLevel(focus, FocusUpgrade.ENLARGE) * 8;
        int distance = 0;
        BlockPos.MutableBlockPos cursor = hit.getBlockPos().mutable();
        while (distance < max && HoleBlock.canPierce(level, cursor)) {
            cursor.move(inward);
            distance++;
        }
        // как в оригинале: цена растёт с глубиной тоннеля
        AspectList cost = getVisCost(focus);
        for (Aspect aspect : cost.getAspects()) {
            cost.merge(aspect, cost.getAmount(aspect) * distance);
        }
        if (!level.isClientSide) {
            if (WandItem.consumeAllCentiVis(wand, player, cost, true)) {
                int duration = 120 + 60 * getUpgradeLevel(focus, FocusUpgrade.EXTEND);
                HoleBlock.createHole(level, hit.getBlockPos(), inward, distance + 1, duration);
            }
            level.playSound(null, hit.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        }
        player.swing(hand);
        return true;
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE, FocusUpgrade.EXTEND}
                : new FocusUpgrade[0];
    }
}
