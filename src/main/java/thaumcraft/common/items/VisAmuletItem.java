package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.VisRelayBlock;
import thaumcraft.common.items.wands.WandItem;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Камень вис и амулет хранения вис (ItemAmuletVis оригинала, виды 0 и 1): украшение на шею с собственным запасом
 * вис всех шести стихий. Надетый, понемногу подзаряжает жезл в руке, а сам пополняется от реле сети вис
 * поблизости и на пьедестале подзарядки. Запас, как у жезла, хранится в сотых долях.
 */
public class VisAmuletItem extends Item implements ICurioItem {
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.##");
    private final int capacity;
    private final boolean lesser;

    /** @param capacity вместимость по каждой стихии в сотых долях вис */
    public VisAmuletItem(int capacity, boolean lesser) {
        super(new Item.Properties().stacksTo(1).rarity(lesser ? Rarity.UNCOMMON : Rarity.RARE));
        this.capacity = capacity;
        this.lesser = lesser;
    }

    public int getMaxVis(ItemStack stack) {
        return capacity;
    }

    public static int getVis(ItemStack stack, Aspect aspect) {
        return stack.hasTag() ? stack.getTag().getInt(aspect.getTag()) : 0;
    }

    public static void storeVis(ItemStack stack, Aspect aspect, int amount) {
        stack.getOrCreateTag().putInt(aspect.getTag(), amount);
    }

    /** Стихии, для которых ещё есть место. */
    public List<Aspect> getAspectsWithRoom(ItemStack stack) {
        List<Aspect> out = new ArrayList<>();
        for (Aspect aspect : Aspect.primals()) {
            if (getVis(stack, aspect) < getMaxVis(stack)) {
                out.add(aspect);
            }
        }
        return out;
    }

    /** Добавляет целые единицы вис. @return сколько единиц не поместилось */
    public int addVis(ItemStack stack, Aspect aspect, int amount, boolean doIt) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int total = getVis(stack, aspect) + amount * 100;
        int leftover = Math.max(total - getMaxVis(stack), 0);
        if (doIt) {
            storeVis(stack, aspect, Math.min(total, getMaxVis(stack)));
        }
        return leftover / 100;
    }

    /** Добавляет сотые доли вис. @return сколько сотых не поместилось */
    public int addRealVis(ItemStack stack, Aspect aspect, int amount, boolean doIt) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int total = getVis(stack, aspect) + amount;
        int leftover = Math.max(total - getMaxVis(stack), 0);
        if (doIt) {
            storeVis(stack, aspect, Math.min(total, getMaxVis(stack)));
        }
        return leftover;
    }

    /** Списывает набор сотых долей вис целиком или не списывает ничего. */
    /** Полный амулет вис надевается только после его изучения (canEquip ItemAmuletVis оригинала). */
    @Override
    public boolean canEquip(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) {
        return lesser || !(context.entity() instanceof net.minecraft.world.entity.player.Player player)
                || thaumcraft.common.research.PlayerKnowledge.forSide(player).isResearchComplete("VISAMULET");
    }

    public boolean consumeAllVis(ItemStack stack, AspectList cost, boolean doIt) {
        if (cost == null || cost.isEmpty()) {
            return false;
        }
        for (Aspect aspect : cost.getAspects()) {
            if (getVis(stack, aspect) < cost.getAmount(aspect)) {
                return false;
            }
        }
        if (doIt) {
            for (Aspect aspect : cost.getAspects()) {
                storeVis(stack, aspect, getVis(stack, aspect) - cost.getAmount(aspect));
            }
        }
        return true;
    }

    /** Четыре раза в секунду: до 0.05 вис каждой стихии в жезл в руке и столько же себе от реле рядом. */
    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (wearer.level().isClientSide || wearer.tickCount % 5 != 0) {
            return;
        }
        ItemStack held = wearer.getMainHandItem();
        if (held.getItem() instanceof WandItem) {
            for (Aspect aspect : WandItem.getAspectsWithRoom(held)) {
                int stored = getVis(stack, aspect);
                if (stored > 0) {
                    int amount = Math.min(Math.min(5, WandItem.getMaxVis(held) - WandItem.getVis(held, aspect)), stored);
                    storeVis(stack, aspect, stored - amount);
                    WandItem.storeVis(held, aspect, WandItem.getVis(held, aspect) + amount);
                }
            }
        }
        VisRelayBlock.Relay relay = VisRelayBlock.Relay.nearestTo(wearer);
        if (relay != null) {
            for (Aspect aspect : getAspectsWithRoom(stack)) {
                int amount = relay.consumeVis(aspect, Math.min(5, getMaxVis(stack) - getVis(stack, aspect)));
                if (amount > 0) {
                    addRealVis(stack, aspect, amount, true);
                    relay.triggerConsumeEffect(aspect);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (lesser) {
            tooltip.add(Component.translatable("item.ItemAmuletVis.text").withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("item.capacity.text").append(" " + getMaxVis(stack) / 100).withStyle(ChatFormatting.GOLD));
        if (stack.hasTag()) {
            for (Aspect aspect : Aspect.primals()) {
                if (stack.getTag().contains(aspect.getTag())) {
                    tooltip.add(Component.literal(" ").append(aspect.getName().copy().withStyle(WandItem.textColor(aspect)))
                            .append(" x " + FORMAT.format(getVis(stack, aspect) / 100f)));
                }
            }
        }
    }
}
