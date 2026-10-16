package thaumcraft.api.aspects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Аспект — базовая единица магии. Шесть первичных, остальные составные из двух компонентов.
 * Аддоны добавляют свои аспекты, просто создавая новые экземпляры.
 */
public final class Aspect {
    private static final Map<String, Aspect> REGISTRY = new LinkedHashMap<>();

    // Первичные
    public static final Aspect AIR = new Aspect("aer", 0xffff7e);
    public static final Aspect EARTH = new Aspect("terra", 0x56c000);
    public static final Aspect FIRE = new Aspect("ignis", 0xff5a01);
    public static final Aspect WATER = new Aspect("aqua", 0x3cd4fc);
    public static final Aspect ORDER = new Aspect("ordo", 0xd5d4ec);
    public static final Aspect ENTROPY = new Aspect("perditio", 0x404040).normalBlend();

    // Составные
    public static final Aspect VOID = new Aspect("vacuos", 0x888888, AIR, ENTROPY).normalBlend();
    public static final Aspect LIGHT = new Aspect("lux", 0xfff663, AIR, FIRE);
    public static final Aspect WEATHER = new Aspect("tempestas", 0xffffff, AIR, WATER);
    public static final Aspect MOTION = new Aspect("motus", 0xcdccf4, AIR, ORDER);
    public static final Aspect COLD = new Aspect("gelum", 0xe1ffff, FIRE, ENTROPY);
    public static final Aspect CRYSTAL = new Aspect("vitreus", 0x80ffff, EARTH, ORDER);
    public static final Aspect LIFE = new Aspect("victus", 0xde0005, WATER, EARTH);
    public static final Aspect POISON = new Aspect("venenum", 0x89f000, WATER, ENTROPY);
    public static final Aspect ENERGY = new Aspect("potentia", 0xc0ffff, ORDER, FIRE);
    public static final Aspect EXCHANGE = new Aspect("permutatio", 0x578357, ENTROPY, ORDER);
    public static final Aspect METAL = new Aspect("metallum", 0xb5b5cd, EARTH, CRYSTAL);
    public static final Aspect DEATH = new Aspect("mortuus", 0x887788, LIFE, ENTROPY);
    public static final Aspect FLIGHT = new Aspect("volatus", 0xe7e7d7, AIR, MOTION);
    public static final Aspect DARKNESS = new Aspect("tenebrae", 0x222222, VOID, LIGHT);
    public static final Aspect SOUL = new Aspect("spiritus", 0xebebfb, LIFE, DEATH);
    public static final Aspect HEAL = new Aspect("sano", 0xff2f34, LIFE, ORDER);
    public static final Aspect TRAVEL = new Aspect("iter", 0xe0585b, MOTION, EARTH);
    public static final Aspect ELDRITCH = new Aspect("alienis", 0x805080, VOID, DARKNESS);
    public static final Aspect MAGIC = new Aspect("praecantatio", 0x9700c0, VOID, ENERGY);
    public static final Aspect AURA = new Aspect("auram", 0xffc0ff, MAGIC, AIR);
    public static final Aspect TAINT = new Aspect("vitium", 0x800080, MAGIC, ENTROPY);
    public static final Aspect SLIME = new Aspect("limus", 0x01f800, LIFE, WATER);
    public static final Aspect PLANT = new Aspect("herba", 0x01ac00, LIFE, EARTH);
    public static final Aspect TREE = new Aspect("arbor", 0x876531, AIR, PLANT);
    public static final Aspect BEAST = new Aspect("bestia", 0x9f6409, MOTION, LIFE);
    public static final Aspect FLESH = new Aspect("corpus", 0xee478d, DEATH, BEAST);
    public static final Aspect UNDEAD = new Aspect("exanimis", 0x3a4000, MOTION, DEATH);
    public static final Aspect MIND = new Aspect("cognitio", 0xffc2b3, FIRE, SOUL);
    public static final Aspect SENSES = new Aspect("sensus", 0x0fd9ff, AIR, SOUL);
    public static final Aspect MAN = new Aspect("humanus", 0xffd7c0, BEAST, MIND);
    public static final Aspect CROP = new Aspect("messis", 0xe1b371, PLANT, MAN);
    public static final Aspect MINE = new Aspect("perfodio", 0xdcd2d8, MAN, EARTH);
    public static final Aspect TOOL = new Aspect("instrumentum", 0x4040ee, MAN, ORDER);
    public static final Aspect HARVEST = new Aspect("meto", 0xeead82, CROP, TOOL);
    public static final Aspect WEAPON = new Aspect("telum", 0xc05050, TOOL, FIRE);
    public static final Aspect ARMOR = new Aspect("tutamen", 0x00c0c0, TOOL, EARTH);
    public static final Aspect HUNGER = new Aspect("fames", 0x9a0305, LIFE, VOID);
    public static final Aspect GREED = new Aspect("lucrum", 0xe6be44, MAN, HUNGER);
    public static final Aspect CRAFT = new Aspect("fabrico", 0x809d80, MAN, TOOL);
    public static final Aspect CLOTH = new Aspect("pannus", 0xeaeac2, TOOL, BEAST);
    public static final Aspect MECHANISM = new Aspect("machina", 0x8080a0, MOTION, TOOL);
    public static final Aspect TRAP = new Aspect("vinculum", 0x9a8080, MOTION, ENTROPY);

    private final String tag;
    private final int color;
    @Nullable
    private final Aspect first;
    @Nullable
    private final Aspect second;
    private final ResourceLocation image;
    /** Светящиеся эффекты аспекта складываются с фоном; у тёмных аспектов (энтропия, пустота) — обычная прозрачность. */
    private boolean additiveBlend = true;

    /** Первичный аспект. */
    public Aspect(String tag, int color) {
        this(tag, color, null, null);
    }

    /** Составной аспект из двух компонентов. */
    public Aspect(String tag, int color, @Nullable Aspect first, @Nullable Aspect second) {
        this(tag, color, first, second, thaumcraft.common.Res.of("thaumcraft", "textures/aspects/" + tag + ".png"));
    }

    public Aspect(String tag, int color, @Nullable Aspect first, @Nullable Aspect second, ResourceLocation image) {
        if (REGISTRY.containsKey(tag)) {
            throw new IllegalArgumentException("Аспект '" + tag + "' уже зарегистрирован");
        }
        this.tag = tag;
        this.color = color;
        this.first = first;
        this.second = second;
        this.image = image;
        REGISTRY.put(tag, this);
    }

    public Aspect normalBlend() {
        additiveBlend = false;
        return this;
    }

    public boolean isAdditiveBlend() {
        return additiveBlend;
    }

    public String getTag() {
        return tag;
    }

    public int getColor() {
        return color;
    }

    public ResourceLocation getImage() {
        return image;
    }

    public boolean isPrimal() {
        return first == null || second == null;
    }

    @Nullable
    public Aspect getFirstComponent() {
        return first;
    }

    @Nullable
    public Aspect getSecondComponent() {
        return second;
    }

    /** Латинское название с заглавной буквы, как в оригинале. */
    public Component getName() {
        return Component.literal(Character.toUpperCase(tag.charAt(0)) + tag.substring(1));
    }

    public Component getDescription() {
        return Component.translatable("tc.aspect." + tag);
    }

    @Nullable
    public static Aspect byTag(String tag) {
        return REGISTRY.get(tag);
    }

    public static Collection<Aspect> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static List<Aspect> primals() {
        List<Aspect> out = new ArrayList<>();
        for (Aspect aspect : REGISTRY.values()) {
            if (aspect.isPrimal()) {
                out.add(aspect);
            }
        }
        return out;
    }

    public static List<Aspect> compounds() {
        List<Aspect> out = new ArrayList<>();
        for (Aspect aspect : REGISTRY.values()) {
            if (!aspect.isPrimal()) {
                out.add(aspect);
            }
        }
        return out;
    }

    /** Результат соединения двух аспектов, либо null, если такой комбинации нет. */
    @Nullable
    public static Aspect combine(Aspect a, Aspect b) {
        for (Aspect aspect : REGISTRY.values()) {
            if (aspect.isPrimal()) {
                continue;
            }
            if ((aspect.first == a && aspect.second == b) || (aspect.first == b && aspect.second == a)) {
                return aspect;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return tag;
    }
}
