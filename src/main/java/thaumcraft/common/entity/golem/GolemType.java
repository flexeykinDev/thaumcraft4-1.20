package thaumcraft.common.entity.golem;

/** Материал голема — перенос EnumGolemType оригинала со всеми его числами. */
public enum GolemType {
    STRAW(10, 0, 0.38f, false, 1, 1, 75, 0),
    WOOD(20, 6, 0.35f, false, 1, 4, 75, 1),
    TALLOW(20, 9, 0.33f, false, 2, 8, 75, 2),
    CLAY(25, 9, 0.33f, true, 1, 8, 100, 2),
    FLESH(15, 6, 0.35f, false, 2, 4, 40, 1),
    STONE(30, 12, 0.32f, true, 1, 16, 100, 3),
    IRON(35, 15, 0.31f, true, 1, 32, 125, 4),
    THAUMIUM(40, 15, 0.32f, true, 2, 32, 100, 4);

    public final int health;
    public final int armor;
    public final float speed;
    public final boolean fireResist;
    /** Сколько улучшений помещается в голема. */
    public final int upgrades;
    /** Сколько предметов голем несёт за раз. */
    public final int carry;
    /** Через сколько тиков голем восстанавливает единицу здоровья. */
    public final int regenDelay;
    public final int strength;

    GolemType(int health, int armor, float speed, boolean fireResist, int upgrades, int carry, int regenDelay, int strength) {
        this.health = health;
        this.armor = armor;
        this.speed = speed;
        this.fireResist = fireResist;
        this.upgrades = upgrades;
        this.carry = carry;
        this.regenDelay = regenDelay;
        this.strength = strength;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Каменные, железные и тауминиевые големы не боятся воды и ходят по дну. */
    public boolean isHeavy() {
        return this == STONE || this == IRON || this == THAUMIUM;
    }

    public static GolemType byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : WOOD;
    }
}
