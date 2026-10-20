package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.Res;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Кольцо «Чёрная дыра» (ItemBlockTalisman): хранит сколько угодно блоков одного вида. ПКМ по блоку запоминает его вид,
 * потом ставит блоки из кольца или раскладывает их в сундук; Shift+ПКМ включает и выключает. Включённое кольцо на пальце
 * собирает такие блоки из инвентаря, оставляя одну стопку, и пополняет её.
 */
public class BlockTalismanItem extends Item implements ICurioItem {
    private static final String TAG_ITEM = "blockItem";
    private static final String TAG_COUNT = "blockCount";
    private static final String TAG_ACTIVE = "active";

    public BlockTalismanItem() {
        super(new Item.Properties().stacksTo(1).rarity(Kami.RARITY));
    }

    public static Item block(ItemStack stack) {
        return stack.hasTag() ? BuiltInRegistries.ITEM.get(Res.parse(stack.getTag().getString(TAG_ITEM).isEmpty()
                ? "minecraft:air" : stack.getTag().getString(TAG_ITEM))) : Items.AIR;
    }

    public static int count(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(TAG_COUNT) : 0;
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_ACTIVE);
    }

    private static void setCount(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(TAG_COUNT, Math.max(0, count));
    }

    /** Забирает из кольца до count блоков; возвращает, сколько забрано. */
    public static int remove(ItemStack stack, int count) {
        int have = count(stack);
        setCount(stack, have - count);
        return Math.min(have, count);
    }

    public static void add(ItemStack stack, int count) {
        setCount(stack, count(stack) + count);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && block(stack) != Items.AIR) {
            stack.getOrCreateTag().putBoolean(TAG_ACTIVE, !isActive(stack));
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Item clicked = level.getBlockState(pos).getBlock().asItem();
        // пустое кольцо запоминает блок, по которому щёлкнули
        if (block(stack) == Items.AIR || count(stack) == 0) {
            if (clicked == Items.AIR) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                stack.getOrCreateTag().putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(clicked).toString());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        Item block = block(stack);
        BlockEntity entity = level.getBlockEntity(pos);
        IItemHandler handler = entity == null ? null
                : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, context.getClickedFace()).orElse(null);
        if (handler != null) {
            if (!level.isClientSide) {
                for (int slot = 0; slot < handler.getSlots() && count(stack) > 0; slot++) {
                    int room = Math.min(block.getMaxStackSize(), handler.getSlotLimit(slot)) - handler.getStackInSlot(slot).getCount();
                    if (room <= 0) {
                        continue;
                    }
                    ItemStack offer = new ItemStack(block, Math.min(room, count(stack)));
                    ItemStack rest = handler.insertItem(slot, offer, false);
                    remove(stack, offer.getCount() - rest.getCount());
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // иначе ставит один блок из кольца, как обычный предмет
        ItemStack one = new ItemStack(block);
        InteractionResult result = one.useOn(new UseOnContext(level, context.getPlayer(), context.getHand(), one,
                new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside())) {
        });
        if (one.isEmpty() && !level.isClientSide) {
            remove(stack, 1);
        }
        // useOn предмета мог подменить то, что в руке, — возвращаем кольцо
        if (context.getPlayer() != null) {
            context.getPlayer().setItemInHand(context.getHand(), stack);
        }
        return result;
    }

    /** onWornTick: блоки этого вида из инвентаря уходят в кольцо, остаётся одна полная стопка. */
    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (context.entity().level().isClientSide || !isActive(stack) || !(context.entity() instanceof Player player)) {
            return;
        }
        Item block = block(stack);
        if (block == Items.AIR) {
            return;
        }
        Inventory inventory = player.getInventory();
        int keep = -1;
        boolean free = false;
        int[] amounts = new int[inventory.items.size()];
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack inSlot = inventory.items.get(slot);
            if (inSlot.isEmpty()) {
                free = true;
            } else if (inSlot.is(block) && !inSlot.hasTag()) {
                amounts[slot] = inSlot.getCount();
                keep = keep == -1 ? slot : amounts[slot] > amounts[keep] && keep > 8 ? slot : keep;
            }
        }
        if (keep == -1) {
            if (free && count(stack) > 0) {
                inventory.add(new ItemStack(block, remove(stack, block.getMaxStackSize())));
            }
            return;
        }
        for (int slot = 0; slot < amounts.length; slot++) {
            if (slot != keep && amounts[slot] != 0) {
                add(stack, amounts[slot]);
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        ItemStack kept = inventory.items.get(keep);
        if (kept.getCount() < kept.getMaxStackSize()) {
            kept.grow(remove(stack, kept.getMaxStackSize() - kept.getCount()));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (block(stack) != Items.AIR) {
            tooltip.add(new ItemStack(block(stack)).getHoverName().copy().append(" (x" + count(stack) + ")").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(isActive(stack) ? "ttmisc.active" : "ttmisc.inactive").withStyle(ChatFormatting.GRAY));
    }
}
