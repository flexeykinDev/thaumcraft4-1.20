package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Стержень жезла: определяет, сколько вис каждого первичного аспекта жезл вмещает. */
public final class WandRod {
    private static final Map<String, WandRod> RODS = new LinkedHashMap<>();

    // значения из ConfigItems оригинала
    public static final WandRod WOOD = new WandRod("wood", 25, 1, "minecraft:stick");
    public static final WandRod GREATWOOD = new WandRod("greatwood", 50, 3, "thaumcraft:wand_rod_greatwood");
    public static final WandRod SILVERWOOD = new WandRod("silverwood", 100, 9, "thaumcraft:wand_rod_silverwood");
    // стержни стихий: вмещают по 75 и сами понемногу восполняют вис своей стихии
    public static final WandRod OBSIDIAN = new WandRod("obsidian", 75, 6, "thaumcraft:wand_rod_obsidian").recharges(Aspect.EARTH);
    public static final WandRod ICE = new WandRod("ice", 75, 6, "thaumcraft:wand_rod_ice").recharges(Aspect.WATER);
    public static final WandRod QUARTZ = new WandRod("quartz", 75, 6, "thaumcraft:wand_rod_quartz").recharges(Aspect.ORDER);
    public static final WandRod REED = new WandRod("reed", 75, 6, "thaumcraft:wand_rod_reed").recharges(Aspect.AIR);
    public static final WandRod BLAZE = new WandRod("blaze", 75, 6, "thaumcraft:wand_rod_blaze").recharges(Aspect.FIRE).glowing();
    public static final WandRod BONE = new WandRod("bone", 75, 6, "thaumcraft:wand_rod_bone").recharges(Aspect.ENTROPY);
    // сердцевины посохов (StaffRod оригинала): вмещают в два с половиной раза больше своих стержней
    public static final WandRod GREATWOOD_STAFF = staff("greatwood", 125, 8);
    public static final WandRod OBSIDIAN_STAFF = staff("obsidian", 175, 14).recharges(Aspect.EARTH);
    public static final WandRod SILVERWOOD_STAFF = staff("silverwood", 250, 24);
    public static final WandRod ICE_STAFF = staff("ice", 175, 14).recharges(Aspect.WATER);
    public static final WandRod QUARTZ_STAFF = staff("quartz", 175, 14).recharges(Aspect.ORDER);
    public static final WandRod REED_STAFF = staff("reed", 175, 14).recharges(Aspect.AIR);
    public static final WandRod BLAZE_STAFF = staff("blaze", 175, 14).recharges(Aspect.FIRE).glowing();
    public static final WandRod BONE_STAFF = staff("bone", 175, 14).recharges(Aspect.ENTROPY);
    /** Первозданный посох: восполняет любой первичный аспект и покрыт бегущими рунами. */
    public static final WandRod PRIMAL_STAFF = staff("primal", 250, 32).allPrimals().runes();

    private final String tag;
    private final int capacity;
    private final int craftCost;
    private ResourceLocation texture;
    private final ResourceLocation itemId;
    @Nullable
    private Aspect rechargeAspect;
    private boolean glowing;
    private boolean staff;
    private boolean rechargeAll;
    private boolean runes;

    public WandRod(String tag, int capacity, int craftCost, String itemId) {
        this.itemId = thaumcraft.common.Res.parse(itemId);
        this.tag = tag;
        this.capacity = capacity;
        this.craftCost = craftCost;
        // текстура лежит в пространстве имён мода, который добавил стержень (деревянный — из обычной палки, его текстура у Thaumcraft)
        String namespace = this.itemId.getNamespace().equals("minecraft") ? "thaumcraft" : this.itemId.getNamespace();
        this.texture = thaumcraft.common.Res.of(namespace, "textures/models/wand_rod_" + tag + ".png");
        RODS.put(tag, this);
    }

    /** Сердцевина посоха: имя с суффиксом _staff, а текстура — как у обычного стержня из того же материала. */
    private static WandRod staff(String material, int capacity, int craftCost) {
        WandRod rod = new WandRod(material + "_staff", capacity, craftCost, "thaumcraft:wand_rod_" + material + "_staff");
        rod.staff = true;
        rod.texture = thaumcraft.common.Res.of("thaumcraft", "textures/models/wand_rod_" + material + ".png");
        return rod;
    }

    private WandRod allPrimals() {
        rechargeAll = true;
        return this;
    }

    private WandRod runes() {
        runes = true;
        return this;
    }

    public boolean isStaff() {
        return staff;
    }

    /** Восполняет ли стержень любой из первичных аспектов (первозданный посох): раз в 50 тиков. */
    public boolean rechargesAll() {
        return rechargeAll;
    }

    /** Бегут ли по стержню руны; такой жезл усиливает фокусы на уровень. */
    public boolean hasRunes() {
        return runes;
    }

    private WandRod recharges(Aspect aspect) {
        rechargeAspect = aspect;
        return this;
    }

    /** Стержень светится в руке (огненный, ихоровый). */
    public WandRod glowing() {
        glowing = true;
        return this;
    }

    /**
     * Стихия, вис которой стержень восполняет сам (WandRodPrimalOnUpdate оригинала): раз в 200 тиков по единице,
     * пока этого вис меньше десятой части вместимости. null — стержень сам не заряжается.
     */
    @Nullable
    public Aspect getRechargeAspect() {
        return rechargeAspect;
    }

    /** Светится ли стержень в темноте (пламенный). */
    public boolean isGlowing() {
        return glowing;
    }

    public String getTag() {
        return tag;
    }

    /** Вместимость по каждому первичному аспекту, в целых единицах вис. */
    public int getCapacity() {
        return capacity;
    }

    public int getCraftCost() {
        return craftCost;
    }

    /** Предмет-стержень, из которого собирается жезл. */
    public ResourceLocation getItemId() {
        return itemId;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Nullable
    public static WandRod byTag(String tag) {
        return RODS.get(tag);
    }

    public static Collection<WandRod> all() {
        return Collections.unmodifiableCollection(RODS.values());
    }
}
