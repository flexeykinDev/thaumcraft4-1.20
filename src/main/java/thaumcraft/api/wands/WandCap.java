package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Набалдашники жезла: определяют, во сколько раз дороже или дешевле жезл тратит вис. */
public final class WandCap {
    private static final Map<String, WandCap> CAPS = new LinkedHashMap<>();

    // значения из ConfigItems оригинала
    public static final WandCap IRON = new WandCap("iron", 1.1f, List.of(), 1f, 1, "thaumcraft:wand_cap_iron");
    public static final WandCap GOLD = new WandCap("gold", 1.0f, List.of(), 1f, 3, "thaumcraft:wand_cap_gold");
    public static final WandCap THAUMIUM = new WandCap("thaumium", 0.9f, List.of(), 1f, 6, "thaumcraft:wand_cap_thaumium");
    /** Медный: как железный, но порядок и энтропия обходятся без наценки. */
    public static final WandCap COPPER = new WandCap("copper", 1.1f, List.of(Aspect.ORDER, Aspect.ENTROPY), 1f, 2,
            "thaumcraft:wand_cap_copper");
    public static final WandCap VOID = new WandCap("void", 0.8f, List.of(), 1f, 9, "thaumcraft:wand_cap_void");
    /** Серебряный: стихии — дешевле на 5%. Как в оригинале, рецепт есть, только если серебро даёт другой мод. */
    public static final WandCap SILVER = new WandCap("silver", 1.0f, List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER), 0.95f, 4,
            "thaumcraft:wand_cap_silver");

    private final String tag;
    private final float baseCostModifier;
    private final List<Aspect> specialAspects;
    private final float specialCostModifier;
    private final int craftCost;
    private final ResourceLocation texture;
    private final ResourceLocation itemId;

    public WandCap(String tag, float baseCostModifier, List<Aspect> specialAspects, float specialCostModifier, int craftCost,
                   String itemId) {
        this.itemId = thaumcraft.common.Res.parse(itemId);
        this.tag = tag;
        this.baseCostModifier = baseCostModifier;
        this.specialAspects = specialAspects;
        this.specialCostModifier = specialCostModifier;
        this.craftCost = craftCost;
        // текстура лежит в пространстве имён мода, который добавил набалдашник
        this.texture = thaumcraft.common.Res.of(this.itemId.getNamespace(), "textures/models/wand_cap_" + tag + ".png");
        CAPS.put(tag, this);
    }

    public String getTag() {
        return tag;
    }

    /** Множитель расхода вис для аспекта: у некоторых набалдашников отдельные аспекты обходятся дешевле. */
    public float getCostModifier(Aspect aspect) {
        return specialAspects.contains(aspect) ? specialCostModifier : baseCostModifier;
    }

    /** Множитель расхода без учёта особых аспектов (getBaseCostModifier оригинала). */
    public float getBaseCostModifier() {
        return baseCostModifier;
    }

    public int getCraftCost() {
        return craftCost;
    }

    /** Предмет-набалдашник, из пары которых собирается жезл. */
    public ResourceLocation getItemId() {
        return itemId;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Nullable
    public static WandCap byTag(String tag) {
        return CAPS.get(tag);
    }

    public static Collection<WandCap> all() {
        return Collections.unmodifiableCollection(CAPS.values());
    }
}
