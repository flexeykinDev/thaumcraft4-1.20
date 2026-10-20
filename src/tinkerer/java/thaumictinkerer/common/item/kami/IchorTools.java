package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeTier;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.tools.ToolHelper;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ихорные инструменты (ItemIchorPick/Shovel/Axe/Sword) и их пробуждённые версии (…Adv + ToolHandler). Ихорий не изнашивается.
 * Пробуждённые меняют режим по Shift+ПКМ: у кирки — блок, квадрат 5×5, линия в 10 блоков; у лопаты — блок, квадрат, столб
 * на 8 вверх и вниз; у топора — блок, квадрат, всё дерево целиком; у меча — один враг, все такие же враги в 3 блоках, удар
 * вполсилы, зато каждый приносит сердце души.
 */
public final class IchorTools {
    /** toolMaterialIchor: уровень 4, без износа, скорость 10, урон +5, зачаровываемость 25. */
    public static final Tier TIER = new ForgeTier(4, 0, 10f, 5f, 25, net.minecraftforge.common.Tags.Blocks.NEEDS_NETHERITE_TOOL,
            () -> Ingredient.EMPTY);
    private static final String TAG_MODE = "mode";

    private IchorTools() {
    }

    public static Item.Properties properties() {
        return new Item.Properties().rarity(Kami.RARITY).fireResistant();
    }

    public static int mode(ItemStack stack) {
        return stack.hasTag() ? Math.floorMod(stack.getTag().getInt(TAG_MODE), 3) : 0;
    }

    /** Shift+ПКМ: следующий режим, название — над хотбаром. */
    static boolean cycle(Level level, Player player, ItemStack stack, String type) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        if (!level.isClientSide) {
            stack.getOrCreateTag().putInt(TAG_MODE, (mode(stack) + 1) % 3);
            player.displayClientMessage(modeName(type, stack), true);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
        }
        return true;
    }

    static Component modeName(String type, ItemStack stack) {
        return Component.translatable("ttmisc.mode." + type + "." + mode(stack));
    }

    static void tooltip(String type, ItemStack stack, List<Component> tooltip) {
        tooltip.add(modeName(type, stack).copy().withStyle(ChatFormatting.GRAY));
    }

    /** Грань, по которой бьёт игрок (raytraceFromEntity оригинала). */
    @Nullable
    private static Direction face(Player player) {
        double reach = player.getBlockReach();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : null;
    }

    /** removeBlocksInIteration: ломает блоки в коробке [from, to) вокруг pos, если их берёт этот инструмент и они не намного твёрже. */
    private static void breakBox(Player player, BlockPos pos, int x0, int y0, int z0, int x1, int y1, int z1, TagKey<Block> mineable) {
        Level level = player.level();
        float origin = level.getBlockState(pos).getDestroyProgress(player, level, pos);
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        breakExtra(player, pos.offset(x, y, z), origin, mineable);
                    }
                }
            }
        }
    }

    private static void breakExtra(Player player, BlockPos at, float origin, TagKey<Block> mineable) {
        Level level = player.level();
        BlockState state = level.getBlockState(at);
        if (state.isAir() || state.getDestroySpeed(level, at) < 0 || !(state.is(mineable) || !state.requiresCorrectToolForDrops())) {
            return;
        }
        float progress = state.getDestroyProgress(player, level, at);
        if (progress <= 0 || origin / progress > 10f) {
            return;
        }
        ToolHelper.harvestBlock(level, player, at, true, 0xFFC0FF);
    }

    /** Квадрат 5×5 поперёк грани; если бить сбоку, он сдвинут вверх на блок, чтобы удобно прокладывать ход. */
    private static void square(Player player, BlockPos pos, Direction face, TagKey<Block> mineable) {
        boolean x = face.getStepX() == 0;
        boolean y = face.getStepY() == 0;
        boolean z = face.getStepZ() == 0;
        breakBox(player, pos, x ? -2 : 0, y ? -1 : 0, z ? -2 : 0, x ? 3 : 1, y ? 4 : 1, z ? 3 : 1, mineable);
    }

    public static class Pick extends PickaxeItem {
        public Pick() {
            super(TIER, 1, -2.8f, properties());
        }
    }

    public static class Shovel extends ShovelItem {
        public Shovel() {
            super(TIER, 1.5f, -3f, properties());
        }
    }

    public static class Axe extends AxeItem {
        public Axe() {
            super(TIER, 5f, -3f, properties());
        }
    }

    public static class Sword extends SwordItem {
        public Sword() {
            super(TIER, 3, -2.4f, properties());
        }
    }

    public static class AdvPick extends Pick {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            return cycle(level, player, stack, "pick") ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : super.use(level, player, hand);
        }

        @Override
        public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
            Direction face = face(player);
            if (!player.level().isClientSide && face != null) {
                if (mode(stack) == 1) {
                    square(player, pos, face, BlockTags.MINEABLE_WITH_PICKAXE);
                } else if (mode(stack) == 2) {
                    // линия в 10 блоков вглубь, начиная с разбиваемого
                    int dx = -face.getStepX();
                    int dy = -face.getStepY();
                    int dz = -face.getStepZ();
                    breakBox(player, pos, dx < 0 ? -10 : 0, dy < 0 ? -10 : 0, dz < 0 ? -10 : 0, dx > 0 ? 10 : 1, dy > 0 ? 10 : 1,
                            dz > 0 ? 10 : 1, BlockTags.MINEABLE_WITH_PICKAXE);
                }
            }
            return false;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip("pick", stack, tooltip);
        }
    }

    public static class AdvShovel extends Shovel {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            return cycle(level, player, stack, "shovel") ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : super.use(level, player, hand);
        }

        @Override
        public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
            if (!player.level().isClientSide) {
                Direction face = face(player);
                if (mode(stack) == 1 && face != null) {
                    square(player, pos, face, BlockTags.MINEABLE_WITH_SHOVEL);
                } else if (mode(stack) == 2) {
                    breakBox(player, pos, 0, -8, 0, 1, 8, 1, BlockTags.MINEABLE_WITH_SHOVEL);
                }
            }
            return false;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip("shovel", stack, tooltip);
        }
    }

    public static class AdvAxe extends Axe {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            return cycle(level, player, stack, "axe") ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : super.use(level, player, hand);
        }

        @Override
        public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
            Level level = player.level();
            if (!level.isClientSide) {
                Direction face = face(player);
                if (mode(stack) == 1 && face != null) {
                    square(player, pos, face, BlockTags.MINEABLE_WITH_AXE);
                } else if (mode(stack) == 2 && level.getBlockState(pos).is(BlockTags.LOGS)) {
                    fellTree(player, pos);
                }
            }
            return false;
        }

        /** Все брёвна того же дерева, связанные с этим (до 256), идут в руки игроку. */
        private static void fellTree(Player player, BlockPos start) {
            Level level = player.level();
            Block log = level.getBlockState(start).getBlock();
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            while (!queue.isEmpty() && seen.size() < 256) {
                BlockPos at = queue.poll();
                for (BlockPos near : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                    if (!seen.contains(near) && level.getBlockState(near).is(log)) {
                        BlockPos copy = near.immutable();
                        seen.add(copy);
                        queue.add(copy);
                    }
                }
            }
            seen.remove(start);
            seen.forEach(at -> ToolHelper.harvestBlock(level, player, at, true, 10));
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip("axe", stack, tooltip);
        }
    }

    public static class AdvSword extends Sword {
        /** Пока бьём соседей по площади, их удары не должны снова бить по площади. */
        private static boolean sweeping;

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            return cycle(level, player, stack, "sword") ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : super.use(level, player, hand);
        }

        @Override
        public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
            if (sweeping || player.level().isClientSide || !(entity instanceof LivingEntity target) || target.hurtTime != 0 || !target.isAlive()) {
                return false;
            }
            if (mode(stack) == 1) {
                sweeping = true;
                try {
                    for (Entity other : player.level().getEntitiesOfClass(target.getClass(), new AABB(target.position(), target.position()).inflate(3))) {
                        // прямой урон, а не player.attack: тот сбросил бы заряд удара по главной цели
                        if (other != target && other != player) {
                            other.hurt(player.damageSources().playerAttack(player),
                                    (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                        }
                    }
                } finally {
                    sweeping = false;
                }
            } else if (mode(stack) == 2) {
                // на миг сопротивление II у цели: удар вполсилы
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1, 1));
                SoulHearts.add(player, 1);
            }
            return false;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip("sword", stack, tooltip);
        }
    }
}
