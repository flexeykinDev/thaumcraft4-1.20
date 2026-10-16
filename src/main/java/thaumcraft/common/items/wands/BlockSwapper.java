package thaumcraft.common.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModSounds;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Очередь замен блоков для фокуса «Равноценный обмен» (VirtualSwapper и tickBlockSwap оригинала).
 * За тик в каждом мире меняется один блок: старый уходит игроку добычей, на его место встаёт блок из инвентаря.
 * Замена сама расползается по соседним таким же блокам, пока не кончится «запас хода».
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class BlockSwapper {
    private record Swap(BlockPos pos, BlockState source, ItemStack target, int life, ServerPlayer player, int wandSlot) {
    }

    private static final Map<ResourceKey<Level>, Queue<Swap>> QUEUES = new HashMap<>();

    private BlockSwapper() {
    }

    /** Открыт ли блок хоть с одной стороны: под землёй замена не расползается. */
    public static boolean isExposed(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
            BlockPos near = pos.relative(side);
            if (!level.getBlockState(near).isSolidRender(level, near)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Взмахи, ждущие конца тика. Правая кнопка тоже сопровождается взмахом, и он приходит на сервер раньше самого
     * нажатия; чтобы такой взмах не подменил замену с расползанием одиночной, он откладывается и отменяется,
     * если в том же тике пришло нажатие.
     */
    private static final Map<java.util.UUID, Swap> PENDING = new HashMap<>();

    /** Одиночная замена по взмаху (левая кнопка). */
    public static void addSingle(ServerLevel level, BlockPos pos, ItemStack target, ServerPlayer player, int wandSlot) {
        BlockState source = level.getBlockState(pos);
        if (!source.isAir() && source.getDestroySpeed(level, pos) >= 0f && !target.is(source.getBlock().asItem())) {
            PENDING.put(player.getUUID(), new Swap(pos.immutable(), source, target, 0, player, wandSlot));
        }
    }

    public static void add(ServerLevel level, BlockPos pos, ItemStack target, int life, ServerPlayer player, int wandSlot) {
        PENDING.remove(player.getUUID());
        BlockState source = level.getBlockState(pos);
        if (source.isAir() || source.getDestroySpeed(level, pos) < 0f || target.is(source.getBlock().asItem())) {
            return;
        }
        QUEUES.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>()).offer(new Swap(pos.immutable(), source, target, life, player, wandSlot));
        level.playSound(null, player, ModSounds.WAND.get(), SoundSource.PLAYERS, 0.25f, 1f);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        PENDING.values().removeIf(pending -> {
            if (pending.player().level() != level) {
                return pending.player().isRemoved();
            }
            QUEUES.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>()).offer(pending);
            level.playSound(null, pending.player(), ModSounds.WAND.get(), SoundSource.PLAYERS, 0.25f, 1f);
            return true;
        });
        Queue<Swap> queue = QUEUES.get(level.dimension());
        if (queue == null) {
            return;
        }
        // негодные заявки пропускаются сразу, чтобы за тик всё-таки случилась одна настоящая замена
        Swap swap;
        while ((swap = queue.poll()) != null) {
            if (perform(level, queue, swap)) {
                break;
            }
        }
    }

    private static boolean perform(ServerLevel level, Queue<Swap> queue, Swap swap) {
        ServerPlayer player = swap.player();
        if (player.isRemoved() || player.level() != level) {
            return false;
        }
        ItemStack wand = player.getInventory().getItem(swap.wandSlot());
        ItemStack focus = wand.getItem() instanceof WandItem ? WandItem.getFocusStack(wand) : ItemStack.EMPTY;
        if (!(focus.getItem() instanceof FocusItem focusItem)) {
            return false;
        }
        BlockPos pos = swap.pos();
        BlockState state = level.getBlockState(pos);
        Block placed = Block.byItem(swap.target().getItem());
        if (placed == Blocks.AIR || !level.mayInteract(player, pos) || swap.target().is(state.getBlock().asItem())
                || !WandItem.consumeAllCentiVis(wand, player, focusItem.getVisCost(focus), false)) {
            return false;
        }
        boolean creative = player.getAbilities().instabuild;
        int slot = creative ? 0 : player.getInventory().findSlotMatchingItem(swap.target());
        if (state != swap.source() || slot < 0) {
            return false;
        }
        if (!creative) {
            int fortune = FocusItem.getUpgradeLevel(focus, FocusUpgrade.TREASURE);
            boolean silk = FocusItem.isUpgradedWith(focus, FocusUpgrade.SILK_TOUCH);
            player.getInventory().removeItem(slot, 1);
            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            if (silk) {
                tool.enchant(Enchantments.SILK_TOUCH, 1);
            } else if (fortune > 0) {
                tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
            }
            for (ItemStack drop : Block.getDrops(state, level, pos, null, player, tool)) {
                if (!player.getInventory().add(drop)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
                }
            }
            WandItem.consumeAllCentiVis(wand, player, focusItem.getVisCost(focus), true);
        }
        BlockState newState = placed.defaultBlockState();
        level.setBlock(pos, newState, 3);
        placed.setPlacedBy(level, pos, newState, player, swap.target());
        FxPacket.blockSparkle(level, pos, 0xc0c0ff, 4);
        level.levelEvent(2001, pos, Block.getId(swap.source()));
        if (swap.life() > 0) {
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (!near.equals(pos) && level.getBlockState(near) == swap.source() && isExposed(level, near)) {
                    queue.offer(new Swap(near.immutable(), swap.source(), swap.target(), swap.life() - 1, player, swap.wandSlot()));
                }
            }
        }
        return true;
    }
}
