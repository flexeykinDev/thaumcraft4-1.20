package thaumictinkerer.common.item.focus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.FocusItem;
import thaumictinkerer.ThaumicTinkerer;

/**
 * Общее у фокусов Thaumic Tinkerer (ItemModFocus оригинала): цвет, цена, украшение или сердцевина из текстур аддона;
 * по умолчанию доступно только улучшение «Сокровище».
 */
public abstract class TTFocus extends FocusItem {
    private final String name;
    private final int color;
    private final AspectList cost;
    private final boolean perTick;
    @Nullable
    private final ResourceLocation ornament;
    @Nullable
    private final ResourceLocation depth;
    private final int depthFrames;

    /** @param decor "orn" — украшение, "depth:N" — сердцевина из N кадров, null — ничего */
    protected TTFocus(String name, int color, AspectList cost, boolean perTick, @Nullable String decor) {
        this.name = name;
        this.color = color;
        this.cost = cost;
        this.perTick = perTick;
        this.ornament = "orn".equals(decor) ? ThaumicTinkerer.id("textures/item/" + name + "_orn.png") : null;
        this.depth = decor != null && decor.startsWith("depth") ? ThaumicTinkerer.id("textures/unstitched/item/" + name + "_depth.png") : null;
        this.depthFrames = decor != null && decor.startsWith("depth:") ? Integer.parseInt(decor.substring(6)) : 1;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    protected String sortingPrefix() {
        return name.toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return color;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return cost;
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focus) {
        return perTick;
    }

    @Nullable
    @Override
    public ResourceLocation getOrnament(ItemStack focus) {
        return ornament;
    }

    @Nullable
    @Override
    public ResourceLocation getDepthTexture(ItemStack focus) {
        return depth;
    }

    @Override
    public int getDepthFrames(ItemStack focus) {
        return depthFrames;
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgrade[]{FocusUpgrade.TREASURE};
    }
}
