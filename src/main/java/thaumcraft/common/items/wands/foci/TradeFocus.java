package thaumcraft.common.items.wands.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.BlockSwapper;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;

/**
 * Фокус «Равноценный обмен» (ItemFocusTrade оригинала). Приседание и правая кнопка запоминают блок;
 * правая кнопка по другому блоку заменяет его и соседние такие же запомненным блоком из инвентаря,
 * левая — только один блок. Заменённые блоки достаются игроку.
 */
public class TradeFocus extends FocusItem implements thaumcraft.common.items.wands.Architect {
    private static final AspectList COST = new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.EARTH, 5).add(Aspect.ORDER, 5);
    private static final AspectList COST_SILK = new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 1).add(Aspect.EARTH, 6)
            .add(Aspect.WATER, 1).add(Aspect.ORDER, 6).add(Aspect.ENTROPY, 6);
    private static final ResourceLocation ORNAMENT = Thaumcraft.id("textures/item/focus_trade_orn.png");
    private static final String TAG_PICKED = "picked";

    @Override
    protected String sortingPrefix() {
        return "BT";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0x857b93;
    }

    @Override
    public ResourceLocation getOrnament(ItemStack focus) {
        return ORNAMENT;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.SILK_TOUCH) ? COST_SILK : COST;
    }

    /** С этим фокусом жезл блоки не ломает: левая кнопка меняет блок. */
    @Override
    public boolean preventsMining(ItemStack focus) {
        return true;
    }

    /** Блок, запомненный жезлом, либо пустой стек. */
    public static ItemStack getPickedBlock(ItemStack wand) {
        return wand.hasTag() && wand.getTag().contains(TAG_PICKED) ? ItemStack.of(wand.getTag().getCompound(TAG_PICKED)) : ItemStack.EMPTY;
    }

    @Nullable
    private static BlockHitResult target(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = target(level, player);
        if (hit == null) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) == null) {
                BlockState state = level.getBlockState(pos);
                ItemStack picked = state.getCloneItemStack(hit, level, pos, player);
                if (picked.isEmpty()) {
                    picked = new ItemStack(state.getBlock().asItem());
                }
                if (!picked.isEmpty()) {
                    picked.setCount(1);
                    wand.getOrCreateTag().put(TAG_PICKED, picked.save(new CompoundTag()));
                }
            } else {
                player.swing(hand);
            }
            return true;
        }
        ItemStack picked = getPickedBlock(wand);
        if (picked.isEmpty()) {
            return false;
        }
        if (level.isClientSide) {
            player.swing(hand);
        } else if (level.getBlockEntity(pos) == null) {
            if (isUpgradedWith(WandItem.getFocusStack(wand), FocusUpgrade.ARCHITECT)) {
                // «Архитектор»: меняется ровно отмеченная область, без расползания
                for (BlockPos each : getArchitectBlocks(wand, level, pos, hit.getDirection(), player)) {
                    BlockSwapper.add((ServerLevel) level, each, picked, 0, (ServerPlayer) player, player.getInventory().selected);
                }
            } else {
                int life = 3 + getUpgradeLevel(WandItem.getFocusStack(wand), FocusUpgrade.ENLARGE);
                BlockSwapper.add((ServerLevel) level, pos, picked, life, (ServerPlayer) player, player.getInventory().selected);
            }
        }
        return true;
    }

    /** Взмах (левая кнопка): меняется только тот блок, на который смотрит игрок. */
    @Override
    public void onSwing(ItemStack wand, Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack picked = getPickedBlock(wand);
        BlockHitResult hit = target(level, player);
        if (hit != null && !picked.isEmpty() && level.getBlockEntity(hit.getBlockPos()) == null) {
            BlockSwapper.addSingle(level, hit.getBlockPos(), picked, (ServerPlayer) player, player.getInventory().selected);
        }
    }

    @Override
    public int getMaxAreaSize(ItemStack focus) {
        return 3 + getUpgradeLevel(focus, FocusUpgrade.ENLARGE) * 2;
    }

    @Override
    public int getAreaDimensions() {
        return 2;
    }

    /** Плоская область на той грани, куда смотрит игрок: такие же блоки, открытые хотя бы с одной стороны. */
    @Override
    public java.util.List<BlockPos> getArchitectBlocks(ItemStack wand, Level level, BlockPos origin, net.minecraft.core.Direction side,
                                                       Player player) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        BlockState wanted = level.getBlockState(origin);
        boolean sideZ = side.getAxis() == net.minecraft.core.Direction.Axis.Z;
        int sizeX = sideZ ? thaumcraft.common.items.wands.Architect.getAreaZ(wand) : thaumcraft.common.items.wands.Architect.getAreaX(wand);
        int sizeZ = sideZ ? thaumcraft.common.items.wands.Architect.getAreaX(wand) : thaumcraft.common.items.wands.Architect.getAreaZ(wand);
        java.util.Set<BlockPos> checked = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!checked.add(pos)) {
                continue;
            }
            int dx = Math.abs(pos.getX() - origin.getX());
            int dy = Math.abs(pos.getY() - origin.getY());
            int dz = Math.abs(pos.getZ() - origin.getZ());
            boolean inside = switch (side.getAxis()) {
                case Y -> dx <= sizeX && dz <= sizeZ;
                case Z -> dx <= sizeX && dy <= sizeZ;
                case X -> dy <= sizeX && dz <= sizeZ;
            };
            BlockState state = level.getBlockState(pos);
            if (!inside || state != wanted || state.isAir() || state.getDestroySpeed(level, pos) < 0f
                    || !BlockSwapper.isExposed(level, pos) || !level.mayInteract(player, pos)) {
                continue;
            }
            out.add(pos);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                if (dir.getAxis() != side.getAxis()) {
                    queue.add(pos.relative(dir));
                }
            }
        }
        return out;
    }

    @Override
    public boolean showAxis(ItemStack wand, net.minecraft.core.Direction side, net.minecraft.core.Direction.Axis axis) {
        int dim = thaumcraft.common.items.wands.Architect.getAreaDim(wand);
        net.minecraft.core.Direction.Axis first = side.getAxis() == net.minecraft.core.Direction.Axis.Y
                ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Y;
        net.minecraft.core.Direction.Axis second = side.getAxis() == net.minecraft.core.Direction.Axis.Z
                ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z;
        return axis == first && (dim == 0 || dim == 1) || axis == second && (dim == 0 || dim == 2);
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE, FocusUpgrade.TREASURE, FocusUpgrade.ARCHITECT};
            case 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE, FocusUpgrade.SILK_TOUCH};
            default -> new FocusUpgrade[0];
        };
    }
}
