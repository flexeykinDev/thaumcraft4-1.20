package thaumcraft.api.wands;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Улучшение фокуса (FocusUpgradeType оригинала). Номера те же, что в оригинале: они хранятся в предмете.
 * Улучшения ставятся в фокальном манипуляторе, по одному на каждый из пяти рангов.
 */
public final class FocusUpgrade {
    private static FocusUpgrade[] types = new FocusUpgrade[20];

    public static final FocusUpgrade POTENCY = new FocusUpgrade(0, "potency", Aspect.WEAPON);
    public static final FocusUpgrade FRUGAL = new FocusUpgrade(1, "frugal", Aspect.HUNGER);
    public static final FocusUpgrade TREASURE = new FocusUpgrade(2, "treasure", Aspect.GREED);
    public static final FocusUpgrade ENLARGE = new FocusUpgrade(3, "enlarge", Aspect.TRAVEL);
    public static final FocusUpgrade ALCHEMISTS_FIRE = new FocusUpgrade(4, "alchemistsfire", Aspect.ENERGY, Aspect.SLIME);
    public static final FocusUpgrade ALCHEMISTS_FROST = new FocusUpgrade(5, "alchemistsfrost", Aspect.COLD, Aspect.TRAP);
    public static final FocusUpgrade ARCHITECT = new FocusUpgrade(6, "architect", Aspect.CRAFT);
    public static final FocusUpgrade EXTEND = new FocusUpgrade(7, "extend", Aspect.EXCHANGE);
    public static final FocusUpgrade SILK_TOUCH = new FocusUpgrade(8, "silktouch", Aspect.GREED);
    // особые улучшения отдельных фокусов
    public static final FocusUpgrade FIREBALL = new FocusUpgrade(9, "fireball", Aspect.DARKNESS);
    public static final FocusUpgrade FIREBEAM = new FocusUpgrade(10, "firebeam", Aspect.ENERGY, Aspect.AIR);
    public static final FocusUpgrade SCATTERSHOT = new FocusUpgrade(11, "scattershot", Aspect.COLD, Aspect.WEAPON);
    public static final FocusUpgrade ICEBOULDER = new FocusUpgrade(12, "iceboulder", Aspect.COLD, Aspect.CRYSTAL);
    public static final FocusUpgrade BAT_BOMBS = new FocusUpgrade(13, "batbombs", Aspect.ENERGY, Aspect.TRAP);
    public static final FocusUpgrade DEVIL_BATS = new FocusUpgrade(14, "devilbats", Aspect.ARMOR);
    public static final FocusUpgrade VAMPIRE_BATS = new FocusUpgrade(19, "vampirebats", Aspect.HUNGER, Aspect.LIFE);
    public static final FocusUpgrade NIGHTSHADE = new FocusUpgrade(15, "nightshade", Aspect.LIFE, Aspect.POISON, Aspect.MAGIC);
    public static final FocusUpgrade SEEKER = new FocusUpgrade(16, "seeker", Aspect.SENSES, Aspect.MIND);
    public static final FocusUpgrade CHAIN_LIGHTNING = new FocusUpgrade(17, "chainlightning", Aspect.WEATHER);
    public static final FocusUpgrade EARTH_SHOCK = new FocusUpgrade(18, "earthshock", Aspect.WEATHER);

    /** «Лозоходство»: сырая руда иногда выпадает самородной гроздью, которая даёт вдвое больше металла. */
    public static final FocusUpgrade DOWSING = new FocusUpgrade(20, "dowsing", Aspect.MINE);

    public final short id;
    private final String name;
    private final ResourceLocation icon;
    /** Аспекты, которыми платят за улучшение. */
    private final Aspect[] aspects;

    private FocusUpgrade(int id, String name, Aspect... aspects) {
        this.id = (short) id;
        this.name = name;
        this.aspects = aspects;
        this.icon = thaumcraft.common.Res.of("thaumcraft", "textures/foci/" + name + ".png");
        if (id >= types.length) {
            FocusUpgrade[] bigger = new FocusUpgrade[id + 1];
            System.arraycopy(types, 0, bigger, 0, types.length);
            types = bigger;
        }
        types[id] = this;
    }

    @Nullable
    public static FocusUpgrade byId(int id) {
        return id >= 0 && id < types.length ? types[id] : null;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public Component getName() {
        return Component.translatable("focus.upgrade." + name + ".name");
    }

    public Component getText() {
        return Component.translatable("focus.upgrade." + name + ".text");
    }

    private static void addPrimals(Aspect aspect, int amount, AspectList out) {
        if (aspect.isPrimal()) {
            out.add(aspect, amount);
        } else {
            addPrimals(aspect.getFirstComponent(), amount, out);
            addPrimals(aspect.getSecondComponent(), amount, out);
        }
    }

    /**
     * Цена улучшения на данном ранге в сотых долях вис: по 2 вис каждого аспекта улучшения на первом ранге
     * и вдвое больше на каждом следующем; составные аспекты раскладываются на первичные.
     */
    public AspectList getVisCost(int rank) {
        int amount = 200;
        for (int i = 1; i < rank; i++) {
            amount *= 2;
        }
        AspectList out = new AspectList();
        for (Aspect aspect : aspects) {
            addPrimals(aspect, amount, out);
        }
        return out;
    }
}
