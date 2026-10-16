package thaumcraft.common.items.wands.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import thaumcraft.common.blocks.WardedBlock;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;

/**
 * Фокус «Защита» (ItemFocusWarding оригинала): правая кнопка по цельному блоку делает его неразрушимым
 * для всех, повторное нажатие хозяином снимает защиту. Пока фокус на жезле в руке, защищённые блоки
 * подсвечены рунной рамкой.
 */
public class WardingFocus extends FocusItem implements thaumcraft.common.items.wands.Architect {
    private static final AspectList COST = new AspectList().add(Aspect.EARTH, 25).add(Aspect.ORDER, 25).add(Aspect.WATER, 10);
    private static final ResourceLocation DEPTH = Thaumcraft.id("textures/unstitched/item/focus_warding_depth.png");
    private static final ResourceLocation ORNAMENT = Thaumcraft.id("textures/item/focus_warding_orn.png");
    /** Полсекунды паузы на каждый блок, чтобы зажатая кнопка не ставила и тут же не снимала защиту. */
    private static final Map<String, Long> DELAY = new HashMap<>();

    @Override
    protected String sortingPrefix() {
        return "BWA";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0xffe9cf;
    }

    @Override
    public ResourceLocation getDepthTexture(ItemStack focus) {
        return DEPTH;
    }

    @Override
    public int getDepthFrames(ItemStack focus) {
        return 9;
    }

    @Override
    public ResourceLocation getOrnament(ItemStack focus) {
        return ORNAMENT;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return COST.copy();
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        player.swing(hand);
        if (!(level instanceof ServerLevel server)) {
            return true;
        }
        Vec3 eye = player.getEyePosition();
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return true;
        }
        BlockPos pos = hit.getBlockPos();
        String key = pos.toShortString() + ":" + level.dimension().location();
        long now = System.currentTimeMillis();
        if (DELAY.getOrDefault(key, 0L) > now) {
            return true;
        }
        DELAY.values().removeIf(until -> until < now);
        DELAY.put(key, now + 500L);
        ItemStack focus = WandItem.getFocusStack(wand);
        boolean done = false;
        // без «Архитектора» область — один блок; с ним защита ставится или снимается сразу со всей отмеченной области
        java.util.List<BlockPos> area = getArchitectBlocks(wand, level, pos, hit.getDirection(), player);
        if (WardedBlock.canWard(level, pos)) {
            for (BlockPos each : area) {
                if (!WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), true)) {
                    break;
                }
                done |= WardedBlock.ward(server, each, player);
            }
        } else {
            for (BlockPos each : area) {
                done |= WardedBlock.unward(server, each, player);
            }
        }
        if (done) {
            level.playSound(null, pos, ModSounds.ZAP.get(), SoundSource.PLAYERS, 0.25f, 1f);
        }
        return true;
    }

    @Override
    public int getMaxAreaSize(ItemStack focus) {
        return 3 + getUpgradeLevel(focus, FocusUpgrade.ENLARGE);
    }

    /**
     * Объёмная область вокруг блока: если он ещё не защищён — все цельные блоки, которые можно защитить,
     * если защищён — все защищённые блоки того же хозяина.
     */
    @Override
    public java.util.List<BlockPos> getArchitectBlocks(ItemStack wand, Level level, BlockPos origin, net.minecraft.core.Direction side,
                                                       Player player) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        boolean unward = level.getBlockEntity(origin) instanceof WardedBlock.Warded;
        int ax = 0;
        int ay = 0;
        int az = 0;
        if (isUpgradedWith(WandItem.getFocusStack(wand), FocusUpgrade.ARCHITECT)) {
            ax = thaumcraft.common.items.wands.Architect.getAreaX(wand);
            ay = thaumcraft.common.items.wands.Architect.getAreaY(wand);
            az = thaumcraft.common.items.wands.Architect.getAreaZ(wand);
        }
        // «ширина» и «высота» считаются по грани, на которую смотрит игрок, «глубина» — вглубь от неё
        int limitX;
        int limitY;
        int limitZ;
        switch (side.getAxis()) {
            case Y -> {
                limitX = ax;
                limitY = ay;
                limitZ = az;
            }
            case Z -> {
                limitX = az;
                limitY = ax;
                limitZ = ay;
            }
            default -> {
                limitX = ay;
                limitY = ax;
                limitZ = az;
            }
        }
        java.util.Set<BlockPos> checked = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!checked.add(pos) || Math.abs(pos.getX() - origin.getX()) > limitX || Math.abs(pos.getY() - origin.getY()) > limitY
                    || Math.abs(pos.getZ() - origin.getZ()) > limitZ) {
                continue;
            }
            boolean fits = unward
                    ? level.getBlockEntity(pos) instanceof WardedBlock.Warded warded && player.getUUID().equals(warded.getOwner())
                    : WardedBlock.canWard(level, pos);
            if (!fits) {
                continue;
            }
            out.add(pos);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                queue.add(pos.relative(dir));
            }
        }
        return out;
    }

    @Override
    public boolean showAxis(ItemStack wand, net.minecraft.core.Direction side, net.minecraft.core.Direction.Axis axis) {
        int dim = thaumcraft.common.items.wands.Architect.getAreaDim(wand);
        if (dim == 0) {
            return true;
        }
        net.minecraft.core.Direction.Axis x = net.minecraft.core.Direction.Axis.X;
        net.minecraft.core.Direction.Axis y = net.minecraft.core.Direction.Axis.Y;
        net.minecraft.core.Direction.Axis z = net.minecraft.core.Direction.Axis.Z;
        return switch (side.getAxis()) {
            case Y -> axis == x && dim == 1 || axis == z && dim == 2 || axis == y && dim == 3;
            case Z -> axis == y && dim == 1 || axis == x && dim == 2 || axis == z && dim == 3;
            case X -> axis == y && dim == 1 || axis == z && dim == 2 || axis == x && dim == 3;
        };
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL};
            case 2 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.ARCHITECT};
            case 3, 4, 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE};
            default -> new FocusUpgrade[0];
        };
    }

    @Override
    public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade type, int rank) {
        return type != FocusUpgrade.ENLARGE || isUpgradedWith(focus, FocusUpgrade.ARCHITECT);
    }
}
