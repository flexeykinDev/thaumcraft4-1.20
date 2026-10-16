package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ToolActions;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModMaterials;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Лопата землекопа — перенос ItemElementalShovel оригинала. Копает сразу девять блоков (три на три в плоскости
 * грани, по которой бьют), а щелчком ставит девять копий блока, взятых из сумки игрока. Как лягут эти девять
 * блоков — плашмя к грани, стеной или полом — переключает клавиша «Переключатель жезла»; крадучись, игрок видит
 * разметку будущей кладки.
 */
public class ElementalShovelItem extends ShovelItem {
    /** Грань, по которой игрок начал копать: от неё зависит плоскость, в которой ломаются соседние блоки. */
    private static final Map<UUID, Direction> SIDES = new ConcurrentHashMap<>();

    public ElementalShovelItem() {
        super(ModMaterials.ELEMENTAL_TOOL, 1.5f, -3f, new Item.Properties().rarity(Rarity.RARE));
    }

    public static byte getOrientation(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("or") ? stack.getTag().getByte("or") : 0;
    }

    public static void setOrientation(ItemStack stack, byte orientation) {
        stack.getOrCreateTag().putByte("or", (byte) (orientation % 3));
    }

    /** Девять мест перед гранью side блока pos, куда лопата поставит блоки. */
    private static List<BlockPos> area(ItemStack stack, BlockPos pos, Direction side, Player player) {
        List<BlockPos> result = new ArrayList<>(9);
        byte orientation = getOrientation(stack);
        boolean vertical = side.getAxis() == Direction.Axis.Y;
        boolean facingZ = (Mth.floor(player.getYRot() * 4f / 360f + 0.5) & 1) == 0;
        for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
                int xx = 0;
                int yy = 0;
                int zz = 0;
                if (orientation == 1) {
                    yy = bb;
                    if (vertical ? !facingZ : side.getAxis() == Direction.Axis.Z) {
                        zz = aa;
                    } else {
                        xx = aa;
                    }
                } else if (orientation == 2) {
                    if (vertical) {
                        yy = bb;
                        if (facingZ) {
                            xx = aa;
                        } else {
                            zz = aa;
                        }
                    } else {
                        zz = bb;
                        xx = aa;
                    }
                } else if (vertical) {
                    xx = aa;
                    zz = bb;
                } else if (side.getAxis() == Direction.Axis.Z) {
                    xx = aa;
                    yy = bb;
                } else {
                    zz = aa;
                    yy = bb;
                }
                result.add(pos.offset(xx, yy, zz).relative(side));
            }
        }
        return result;
    }

    /** Разметка кладки (IArchitect оригинала): видна, только пока игрок крадётся. */
    public List<BlockPos> getArchitectBlocks(ItemStack stack, Level level, BlockPos pos, Direction side, Player player) {
        List<BlockPos> result = new ArrayList<>();
        if (!player.isShiftKeyDown()) {
            return result;
        }
        for (BlockPos target : area(stack, pos, side, player)) {
            if (level.getBlockState(target).canBeReplaced()) {
                result.add(target);
            }
        }
        return result;
    }

    private static boolean consume(Player player, Item item) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item) && !stack.hasTag()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        // крадучись лопатой можно, как обычной, протоптать тропинку или потушить костёр
        if (player.isShiftKeyDown() && (state.getToolModifiedState(context, ToolActions.SHOVEL_FLATTEN, true) != null
                || state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)) {
            return super.useOn(context);
        }
        if (level.getBlockEntity(pos) != null || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.BED_PART) || state.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState copy = state;
        if (copy.hasProperty(BlockStateProperties.WATERLOGGED)) {
            copy = copy.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        if (copy.hasProperty(SlabBlock.TYPE) && copy.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            copy = copy.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        ItemStack shovel = context.getItemInHand();
        boolean placed = false;
        for (BlockPos target : area(shovel, pos, context.getClickedFace(), player)) {
            if (shovel.isEmpty() || !level.getBlockState(target).canBeReplaced() || !level.mayInteract(player, target)
                    || !player.mayUseItemAt(target, context.getClickedFace(), shovel)) {
                continue;
            }
            BlockState put;
            int color;
            if (consume(player, state.getBlock().asItem())) {
                put = copy;
                color = 8401408;
            } else if (state.is(Blocks.GRASS_BLOCK) && consume(player, net.minecraft.world.item.Items.DIRT)) {
                // дёрна в сумке нет — вместо него ложится простая земля
                put = Blocks.DIRT.defaultBlockState();
                color = 3;
            } else {
                continue;
            }
            level.setBlock(target, put, 3);
            level.playSound(null, target, state.getSoundType(level, pos, player).getPlaceSound(), SoundSource.BLOCKS, 0.6f,
                    0.9f + level.random.nextFloat() * 0.2f);
            FxPacket.blockSparkle((ServerLevel) level, target, color, 4);
            shovel.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(context.getHand()));
            placed = true;
        }
        if (placed) {
            player.swing(context.getHand(), true);
        }
        return placed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        HitResult hit = getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            SIDES.put(player.getUUID(), block.getDirection());
        }
        return super.onBlockStartBreak(stack, pos, player);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (miner.isShiftKeyDown() || level.isClientSide || !(miner instanceof Player player) || !isCorrectToolForDrops(stack, state)) {
            return super.mineBlock(stack, level, state, pos, miner);
        }
        Direction side = SIDES.getOrDefault(player.getUUID(), Direction.UP);
        for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
                BlockPos near = switch (side.getAxis()) {
                    case Y -> pos.offset(aa, 0, bb);
                    case Z -> pos.offset(aa, bb, 0);
                    case X -> pos.offset(0, bb, aa);
                };
                if (near.equals(pos) || stack.isEmpty() || !level.mayInteract(player, near)) {
                    continue;
                }
                BlockState there = level.getBlockState(near);
                if (there.getDestroySpeed(level, near) >= 0f && isCorrectToolForDrops(stack, there)
                        && ToolHelper.harvestBlock(level, player, near, true, 3)) {
                    stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                }
            }
        }
        return true;
    }
}
