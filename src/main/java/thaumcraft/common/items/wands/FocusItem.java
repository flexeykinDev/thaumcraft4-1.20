package thaumcraft.common.items.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Фокус жезла (ItemFocusBasic оригинала). Сам по себе ничего не делает: его надевают на жезл,
 * и тогда правая кнопка мыши с жезлом в руке творит заклинание фокуса за счёт вис жезла.
 * Цены указаны в сотых долях вис, как в оригинале.
 */
public abstract class FocusItem extends Item {
    private static final String TAG_UPGRADES = "upgrade";
    private static final DecimalFormat FORMAT = new DecimalFormat("#####.##");

    protected FocusItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    /** Как движется жезл, пока фокус работает: плавно покачивается или мелко дрожит от напряжения. */
    public enum Animation {
        WAVE, CHARGE
    }

    public Animation getAnimation(ItemStack focus) {
        return Animation.WAVE;
    }

    /**
     * «Глубина» кристалла: анимированная сердцевина, которая видна сквозь полупрозрачный фокус.
     * Текстура — вертикальная лента кадров; null, если сердцевины нет.
     */
    @Nullable
    public net.minecraft.resources.ResourceLocation getDepthTexture(ItemStack focus) {
        return null;
    }

    public int getDepthFrames(ItemStack focus) {
        return 1;
    }

    /** Украшение вокруг кристалла: плоский значок, поставленный крест-накрест; null, если его нет. */
    @Nullable
    public net.minecraft.resources.ResourceLocation getOrnament(ItemStack focus) {
        return null;
    }

    /** Цвет кристалла на вершине жезла. */
    public abstract int getFocusColor(ItemStack focus);

    /** Цена заклинания в сотых долях вис. */
    public abstract AspectList getVisCost(ItemStack focus);

    /** Берётся ли цена каждый тик, пока держат кнопку, или один раз за выстрел. */
    public boolean isVisCostPerTick(ItemStack focus) {
        return false;
    }

    /** Пауза между срабатываниями, в миллисекундах. */
    public int getActivationCooldown(ItemStack focus) {
        return 0;
    }

    /** Ключ, по которому фокусы упорядочены в круговом меню: буквы фокуса и номера его улучшений. */
    public String getSortingHelper(ItemStack focus) {
        StringBuilder out = new StringBuilder(sortingPrefix());
        for (short id : getAppliedUpgrades(focus)) {
            out.append(id);
        }
        return out.toString();
    }

    protected abstract String sortingPrefix();

    /** Какие улучшения предлагает фокальный манипулятор на ранге от 1 до 5. */
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgrade[0];
    }

    public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade type, int rank) {
        return true;
    }

    /** Нажатие правой кнопки. Возвращает true, если фокус что-то сделал или начал делать. */
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        return false;
    }

    /** Каждый тик, пока кнопка зажата. */
    public void onUsingFocusTick(ItemStack wand, Player player, int count) {
    }

    public void onStoppedUsingFocus(ItemStack wand, Level level, Player player, int count) {
    }

    /** Взмах жезлом (левая кнопка мыши). */
    public void onSwing(ItemStack wand, Player player) {
    }

    /** Запрещает ли фокус ломать блоки жезлом: нужно тем, у кого левая кнопка занята своим действием. */
    public boolean preventsMining(ItemStack focus) {
        return false;
    }

    // ---------- улучшения ----------

    public static short[] getAppliedUpgrades(ItemStack focus) {
        short[] out = {-1, -1, -1, -1, -1};
        if (focus.hasTag() && focus.getTag().contains(TAG_UPGRADES, Tag.TAG_LIST)) {
            ListTag list = focus.getTag().getList(TAG_UPGRADES, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < 5; i++) {
                out[i] = list.getCompound(i).getShort("id");
            }
        }
        return out;
    }

    public static boolean applyUpgrade(ItemStack focus, FocusUpgrade type, int rank) {
        short[] upgrades = getAppliedUpgrades(focus);
        if (rank < 1 || rank > 5 || upgrades[rank - 1] != -1) {
            return false;
        }
        upgrades[rank - 1] = type.id;
        ListTag list = new ListTag();
        for (short id : upgrades) {
            CompoundTag entry = new CompoundTag();
            entry.putShort("id", id);
            list.add(entry);
        }
        focus.getOrCreateTag().put(TAG_UPGRADES, list);
        return true;
    }

    public static int getUpgradeLevel(ItemStack focus, FocusUpgrade type) {
        int level = 0;
        for (short id : getAppliedUpgrades(focus)) {
            if (id == type.id) {
                level++;
            }
        }
        return level;
    }

    public static boolean isUpgradedWith(ItemStack focus, FocusUpgrade type) {
        return getUpgradeLevel(focus, type) > 0;
    }

    // ---------- подсказка ----------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        AspectList cost = getVisCost(stack);
        if (cost != null && !cost.isEmpty()) {
            tooltip.add(Component.translatable(isVisCostPerTick(stack) ? "item.Focus.cost2" : "item.Focus.cost1"));
            for (Aspect aspect : cost.getAspectsSortedByName()) {
                tooltip.add(Component.literal(" ").append(aspect.getName().copy().withStyle(WandItem.textColor(aspect)))
                        .append(" x " + FORMAT.format(cost.getAmount(aspect) / 100f)));
            }
        }
        addUpgradeLines(stack, tooltip);
    }

    /** Список улучшений: одинаковые сворачиваются в одну строку с римской цифрой. */
    public static void addUpgradeLines(ItemStack focus, List<Component> tooltip) {
        Map<Short, Integer> counts = new LinkedHashMap<>();
        for (short id : getAppliedUpgrades(focus)) {
            if (id >= 0) {
                counts.merge(id, 1, Integer::sum);
            }
        }
        for (Map.Entry<Short, Integer> entry : counts.entrySet()) {
            FocusUpgrade type = FocusUpgrade.byId(entry.getKey());
            if (type == null) {
                continue;
            }
            MutableComponent line = type.getName().copy();
            if (entry.getValue() > 1) {
                line.append(" ").append(Component.translatable("enchantment.level." + entry.getValue()));
            }
            tooltip.add(line.withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
