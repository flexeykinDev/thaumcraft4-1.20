package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModSounds;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Зеркало миротворца (ItemPlacementMirror): Shift+ПКМ по блоку запоминает его, Shift+ПКМ в воздух меняет размер (3×3 … 11×11),
 * ПКМ ставит сразу всю плоскость таких блоков перед гранью, если их хватает в инвентаре и в кольцах «Чёрная дыра».
 */
public class PlacementMirrorItem extends Item {
    private static final String TAG_STATE = "blockState";
    private static final String TAG_SIZE = "size";

    public PlacementMirrorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Kami.RARITY));
    }

    public static BlockState state(Level level, ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_STATE)
                ? NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), stack.getTag().getCompound(TAG_STATE))
                : Blocks.AIR.defaultBlockState();
    }

    public static int size(ItemStack stack) {
        return (stack.hasTag() && stack.getTag().contains(TAG_SIZE) ? stack.getTag().getInt(TAG_SIZE) : 3) | 1;
    }

    /** getBlocksToPlace: клетки плоскости перед гранью, на которую смотрит игрок (до 5 блоков). */
    public static List<BlockPos> targets(ItemStack stack, Player player) {
        List<BlockPos> out = new ArrayList<>();
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(5)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return out;
        }
        BlockPos base = hit.getBlockPos();
        if (level.getBlockState(base).canBeReplaced()) {
            base = base.below();
        }
        Direction face = hit.getDirection();
        int quarter = Mth.floor(player.getYRot() * 4f / 360f + 0.5) & 3;
        int radius = (size(stack) ^ 1) / 2;
        boolean vertical = face.getAxis() == Direction.Axis.Y;
        boolean steep = player.getXRot() > 75f;
        int rx = face.getAxis() == Direction.Axis.X ? 0 : vertical ? (!steep && (quarter & 1) != 0 ? 0 : radius) : radius;
        int ry = vertical ? (steep ? 0 : radius) : radius;
        int rz = face.getAxis() == Direction.Axis.Z ? 0 : vertical ? (!steep && (quarter & 1) != 1 ? 0 : radius) : radius;
        for (int x = -rx; x < rx + 1; x++) {
            for (int y = 0; y < ry * 2 + 1; y++) {
                for (int z = -rz; z < rz + 1; z++) {
                    BlockPos at = base.offset(x + face.getStepX(), y + face.getStepY(), z + face.getStepZ());
                    BlockState there = level.getBlockState(at);
                    if (there.isAir() || there.canBeReplaced()) {
                        out.add(at);
                    }
                }
            }
        }
        return out;
    }

    /** Кольца «Чёрная дыра» игрока: в инвентаре и на пальцах. */
    private static List<ItemStack> rings(Player player) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BlockTalismanItem) {
                out.add(stack);
            }
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.findCurios(stack -> stack.getItem() instanceof BlockTalismanItem)
                .forEach(result -> out.add(result.stack())));
        return out;
    }

    private static boolean hasBlocks(Player player, Item item, int need) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && !stack.hasTag()) {
                have += stack.getCount();
            }
        }
        for (ItemStack ring : rings(player)) {
            if (BlockTalismanItem.block(ring) == item) {
                have += BlockTalismanItem.count(ring);
            }
        }
        return have >= need;
    }

    private static void consume(Player player, Item item) {
        if (player.getAbilities().instabuild) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && !stack.hasTag()) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack ring : rings(player)) {
            if (BlockTalismanItem.block(ring) == item && BlockTalismanItem.remove(ring, 1) > 0) {
                return;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int size = size(stack) == 11 ? 3 : size(stack) + 2;
                stack.getOrCreateTag().putInt(TAG_SIZE, size);
                player.displayClientMessage(Component.literal(size + " x " + size), true);
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            BlockState clicked = level.getBlockState(context.getClickedPos());
            if (clicked.getRenderShape() == RenderShape.MODEL && !clicked.hasBlockEntity() && clicked.getBlock().asItem() != Items.AIR) {
                if (!level.isClientSide) {
                    stack.getOrCreateTag().put(TAG_STATE, NbtUtils.writeBlockState(clicked));
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            place(level, player, stack);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void place(Level level, Player player, ItemStack stack) {
        BlockState state = state(level, stack);
        Item item = state.getBlock().asItem();
        if (state.isAir() || item == Items.AIR) {
            return;
        }
        List<BlockPos> targets = targets(stack, player);
        if (targets.isEmpty() || !hasBlocks(player, item, targets.size())) {
            return;
        }
        for (BlockPos at : targets) {
            level.setBlock(at, state, Block.UPDATE_ALL);
            consume(player, item);
        }
        level.playSound(null, player.blockPosition(), ModSounds.WAND.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int size = size(stack);
        tooltip.add(Component.literal(size + " x " + size).withStyle(ChatFormatting.GRAY));
        if (level != null && !state(level, stack).isAir()) {
            tooltip.add(state(level, stack).getBlock().getName().copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
