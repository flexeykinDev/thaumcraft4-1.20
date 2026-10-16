package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.golem.GolemCoreItem;
import thaumcraft.common.items.golem.GolemUpgradeItem;
import thaumcraft.common.registry.ModSounds;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Голем — перенос EntityGolemBase оригинала. Сам по себе голем ничего не делает: поведение ему задаёт ядро,
 * способности — улучшения, а место работы — дом (блок, у которого его поставили) и отметки колокольчика.
 */
public class Golem extends AbstractGolem implements OwnableEntity {
    public static final int UPGRADE_AIR = 0;
    public static final int UPGRADE_EARTH = 1;
    public static final int UPGRADE_FIRE = 2;
    public static final int UPGRADE_WATER = 3;
    public static final int UPGRADE_ORDER = 4;
    public static final int UPGRADE_ENTROPY = 5;

    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> TOGGLES = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> DECORATION = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> CORE = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.BYTE);
    /** Цвета ячеек: по знаку на ячейку, h — без цвета, иначе шестнадцатеричная цифра. */
    private static final EntityDataAccessor<String> COLORS = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.STRING);
    /** Улучшения: по шестнадцатеричной цифре на ячейку, f — пусто. */
    private static final EntityDataAccessor<String> UPGRADES = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> ADVANCED = SynchedEntityData.defineId(Golem.class, EntityDataSerializers.BOOLEAN);

    /** Образцы предметов, с которыми голем работает (окно голема). */
    public SimpleContainer inventory = new SimpleContainer(1);
    private ItemStack itemCarried = ItemStack.EMPTY;
    /** За чем голем с ядром наполнения сейчас идёт. */
    public ItemStack itemWatched = ItemStack.EMPTY;
    /** За какой жидкостью сейчас идёт голем с ядром переливания. */
    @Nullable
    public net.minecraft.world.level.material.Fluid fluidWatched;
    public net.minecraftforge.fluids.FluidStack fluidCarried = net.minecraftforge.fluids.FluidStack.EMPTY;
    @Nullable
    public Aspect essentia;
    public int essentiaAmount;
    public int homeFacing;
    public boolean paused;
    public boolean inactive;
    private byte[] colors = {-1};
    private byte[] upgrades = {-1};
    private int regenTimer;
    private List<GolemMarker> markers = new ArrayList<>();

    /** Клиентская анимация: голова поднимается после установки ядра. */
    public float bootup = -1f;
    public int action;
    public int leftArm;
    public int rightArm;
    public int healing;

    public Golem(EntityType<? extends Golem> type, Level level) {
        super(type, level);
        setMaxUpStep(1f);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
            ground.setCanFloat(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CARRIED, ItemStack.EMPTY);
        entityData.define(OWNER, "");
        entityData.define(TOGGLES, (byte) 0);
        entityData.define(TYPE, (byte) GolemType.WOOD.ordinal());
        entityData.define(DECORATION, "");
        entityData.define(CORE, (byte) -1);
        entityData.define(COLORS, "h");
        entityData.define(UPGRADES, "f");
        entityData.define(ADVANCED, false);
    }

    /** Материал и «продвинутость» задаются один раз, при создании голема из предмета. */
    public void configure(GolemType type, boolean advanced) {
        entityData.set(TYPE, (byte) type.ordinal());
        entityData.set(ADVANCED, advanced);
        upgrades = new byte[type.upgrades + (advanced ? 1 : 0)];
        java.util.Arrays.fill(upgrades, (byte) -1);
        syncUpgrades();
    }

    // ---------- настройка по ядру и улучшениям ----------

    /** Ставит голема у блока: homeFacing — сторона блока, по которой щёлкнули. */
    public void setup(int side) {
        homeFacing = side;
        setupGolem();
        setupGolemInventory();
    }

    public boolean setupGolemInventory() {
        int core = getCore();
        if (!GolemCoreItem.hasInventory(core)) {
            return false;
        }
        int size = core == GolemCoreItem.LIQUID ? 1 + getUpgradeAmount(UPGRADE_FIRE) : 6 + getUpgradeAmount(UPGRADE_FIRE) * 6;
        SimpleContainer resized = new SimpleContainer(size);
        for (int i = 0; i < Math.min(size, inventory.getContainerSize()); i++) {
            resized.setItem(i, inventory.getItem(i));
        }
        inventory = resized;
        byte[] old = colors;
        colors = new byte[size];
        for (int i = 0; i < size; i++) {
            colors[i] = i < old.length ? old[i] : -1;
        }
        syncColors();
        return true;
    }

    public void setupGolem() {
        GolemType type = getGolemType();
        setPathfindingMalus(BlockPathTypes.WATER, type.isHeavy() ? 0f : -1f);
        String deco = getGolemDecoration();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.health + (deco.contains("H") ? 5 : 0));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2 + getGolemStrength() + getUpgradeAmount(UPGRADE_EARTH) + (deco.contains("M") ? 2 : 0));
        if (level().isClientSide) {
            return;
        }
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        int core = getCore();
        if (core < 0) {
            return;
        }
        switch (core) {
            case GolemCoreItem.GUARD -> {
                if (deco.contains("R")) {
                    goalSelector.addGoal(2, new GolemWorkGoals.DartAttack(this));
                }
                goalSelector.addGoal(3, new GolemWorkGoals.AttackOnCollide(this));
                targetSelector.addGoal(1, new HurtByTargetGoal(this));
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, this::isValidTarget));
            }
            case GolemCoreItem.BUTCHER -> {
                if (deco.contains("R")) {
                    goalSelector.addGoal(2, new GolemWorkGoals.DartAttack(this));
                }
                goalSelector.addGoal(3, new GolemWorkGoals.AttackOnCollide(this));
                targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, this::isValidTarget));
            }
            default -> GolemTasks.addCoreGoals(this, core);
        }
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new ReturnHomeGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---------- данные ----------

    public GolemType getGolemType() {
        return GolemType.byId(entityData.get(TYPE));
    }

    public boolean isAdvanced() {
        return entityData.get(ADVANCED);
    }

    public byte getCore() {
        return entityData.get(CORE);
    }

    public void setCore(byte core) {
        entityData.set(CORE, core);
    }

    public String getGolemDecoration() {
        return entityData.get(DECORATION);
    }

    public void setGolemDecoration(String decoration) {
        entityData.set(DECORATION, decoration);
    }

    public int getGolemStrength() {
        return getGolemType().strength + getUpgradeAmount(UPGRADE_EARTH);
    }

    public int getUpgradeSlots() {
        return entityData.get(UPGRADES).length();
    }

    /** Улучшение в ячейке; -1 — пусто. Работает и на клиенте. */
    public byte getUpgrade(int slot) {
        String text = entityData.get(UPGRADES);
        if (slot < 0 || slot >= text.length()) {
            return -1;
        }
        int value = Character.digit(text.charAt(slot), 16);
        return value == 15 || value < 0 ? -1 : (byte) value;
    }

    public int getUpgradeAmount(int type) {
        int amount = 0;
        for (int slot = 0; slot < getUpgradeSlots(); slot++) {
            if (getUpgrade(slot) == type) {
                amount++;
            }
        }
        return amount;
    }

    public void setUpgrade(int slot, byte upgrade) {
        upgrades[slot] = upgrade;
        syncUpgrades();
    }

    public byte[] getUpgrades() {
        return upgrades.clone();
    }

    /** Улучшения из предмета-голема: лишние отбрасываются, недостающие ячейки пусты. */
    public void loadUpgrades(byte[] saved) {
        for (int i = 0; i < upgrades.length; i++) {
            upgrades[i] = i < saved.length ? saved[i] : -1;
        }
        syncUpgrades();
    }

    private void syncUpgrades() {
        StringBuilder text = new StringBuilder();
        for (byte upgrade : upgrades) {
            text.append(upgrade < 0 ? 'f' : Character.forDigit(upgrade, 16));
        }
        entityData.set(UPGRADES, text.toString());
    }

    /** Цвет ячейки образца; -1 — без цвета. */
    public int getColors(int slot) {
        String text = entityData.get(COLORS);
        if (slot < 0 || slot >= text.length() || text.charAt(slot) == 'h') {
            return -1;
        }
        return Character.digit(text.charAt(slot), 16);
    }

    public void setColors(int slot, int color) {
        if (slot >= 0 && slot < colors.length) {
            colors[slot] = (byte) color;
            syncColors();
        }
    }

    private void syncColors() {
        StringBuilder text = new StringBuilder();
        for (byte color : colors) {
            text.append(color < 0 ? 'h' : Character.forDigit(color, 16));
        }
        entityData.set(COLORS, text.toString());
    }

    public boolean[] getToggles() {
        byte value = entityData.get(TOGGLES);
        boolean[] toggles = new boolean[8];
        for (int i = 0; i < 8; i++) {
            toggles[i] = (value >> i & 1) != 0;
        }
        return toggles;
    }

    public void setToggle(int index, boolean on) {
        byte value = entityData.get(TOGGLES);
        entityData.set(TOGGLES, (byte) (on ? value | 1 << index : value & ~(1 << index)));
    }

    public boolean canAttackHostiles() {
        return !getToggles()[1];
    }

    public boolean canAttackAnimals() {
        return !getToggles()[2];
    }

    public boolean canAttackPlayers() {
        return !getToggles()[3];
    }

    public boolean canAttackCreepers() {
        return !getToggles()[4];
    }

    /** Сравнивать предметы по тегам (словарю руд оригинала). */
    public boolean checkOreDict() {
        return getToggles()[5];
    }

    public boolean ignoreDamage() {
        return getToggles()[6];
    }

    public boolean ignoreNBT() {
        return getToggles()[7];
    }

    // ---------- хозяин, дом, отметки ----------

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        try {
            return entityData.get(OWNER).isEmpty() ? null : UUID.fromString(entityData.get(OWNER));
        } catch (IllegalArgumentException broken) {
            return null;
        }
    }

    public void setOwner(Player player) {
        entityData.set(OWNER, player.getUUID().toString());
    }

    public BlockPos getHome() {
        return getRestrictCenter();
    }

    public void setHome(BlockPos pos) {
        restrictTo(pos, 32);
    }

    public float getRange() {
        float range = 16 + getUpgradeAmount(UPGRADE_WATER) * 4;
        if (getGolemDecoration().contains("G")) {
            range += Math.max(range * 0.1f, 1f);
        }
        if (isAdvanced()) {
            range += Math.max(range * 0.2f, 2f);
        }
        return range;
    }

    public boolean isWithinHomeDistance(BlockPos pos) {
        float range = getRange();
        return getHome().distSqr(pos) < range * range;
    }

    public List<GolemMarker> getMarkers() {
        String here = level().dimension().location().toString();
        markers.removeIf(marker -> !marker.dimension().equals(here));
        return markers;
    }

    public void setMarkers(List<GolemMarker> markers) {
        this.markers = new ArrayList<>(markers);
    }

    // ---------- ноша ----------

    public int getCarryLimit() {
        int base = getGolemType().carry;
        return base + Math.min(16, Math.max(4, base)) * getUpgradeAmount(UPGRADE_EARTH);
    }

    public int getFluidCarryLimit() {
        return (int) Math.floor(Math.sqrt(getCarryLimit())) * 1000;
    }

    public ItemStack getCarried() {
        return itemCarried;
    }

    public void setCarried(ItemStack stack) {
        itemCarried = stack;
        updateCarried();
    }

    public int getCarrySpace() {
        return itemCarried.isEmpty() ? getCarryLimit()
                : Math.min(getCarryLimit() - itemCarried.getCount(), itemCarried.getMaxStackSize() - itemCarried.getCount());
    }

    /** То, что видно в руках у голема; на клиенте — единственный источник. */
    public ItemStack getCarriedForDisplay() {
        return entityData.get(CARRIED);
    }

    public void updateCarried() {
        if (itemCarried.isEmpty() && getCore() == GolemCoreItem.LIQUID && !fluidCarried.isEmpty()) {
            // жидкость показывается ведром с ней: по нему клиент узнаёт и саму жидкость, и её количество
            ItemStack bucket = new ItemStack(fluidCarried.getFluid().getBucket());
            bucket.getOrCreateTag().putInt(FLUID_AMOUNT, fluidCarried.getAmount());
            entityData.set(CARRIED, bucket);
            return;
        }
        if (itemCarried.isEmpty() && getCore() == GolemCoreItem.ALCHEMY) {
            // голем-алхимик всегда держит банку; эссенция в ней показана в долях его вместимости
            ItemStack jar = new ItemStack(thaumcraft.common.registry.ModItems.WARDED_JAR.get());
            if (essentia != null && essentiaAmount > 0) {
                CompoundTag contents = new CompoundTag();
                contents.putString("Aspect", essentia.getTag());
                contents.putShort("Amount", (short) Math.max(1, (int) (64f * essentiaAmount / getCarryLimit())));
                jar.getOrCreateTag().put("BlockEntityTag", contents);
            }
            entityData.set(CARRIED, jar);
            return;
        }
        entityData.set(CARRIED, itemCarried.copy());
    }

    /** Метка количества жидкости на предмете-образе ноши. */
    public static final String FLUID_AMOUNT = "GolemFluid";

    // ---------- жизнь ----------

    /** Скорость задаёт материал, а не атрибут: так голем не ускоряется и не замедляется зельями пути. */
    @Override
    public float getSpeed() {
        if (paused || inactive) {
            return 0f;
        }
        String deco = getGolemDecoration();
        GolemType type = getGolemType();
        float speed = type.speed * (deco.contains("B") ? 1.1f : 1f);
        if (deco.contains("P")) {
            speed *= 0.88f;
        }
        speed *= 1f + getUpgradeAmount(UPGRADE_AIR) * 0.15f;
        if (isAdvanced()) {
            speed *= 1.1f;
        }
        if (isInWater() && type.isHeavy()) {
            speed *= 2f;
        }
        return speed;
    }

    @Override
    protected void customServerAiStep() {
        if (paused || inactive) {
            getNavigation().stop();
        }
        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (action > 0) {
            action--;
        }
        if (leftArm > 0) {
            leftArm--;
        }
        if (rightArm > 0) {
            rightArm--;
        }
        if (healing > 0) {
            healing--;
        }
        BlockState below = level().getBlockState(blockPosition().below());
        inactive = below.getBlock() instanceof thaumcraft.common.blocks.GolemFetterBlock
                && below.getValue(thaumcraft.common.blocks.GolemFetterBlock.POWERED);
        if (level().isClientSide) {
            if (bootup > 0f && getCore() > -1) {
                bootup *= bootup / 33.1f;
                level().playLocalSound(getX(), getY(), getZ(), ModSounds.CAMERA_TICKS.get(), getSoundSource(), bootup * 0.2f, bootup, false);
            }
            return;
        }
        if (regenTimer > 0) {
            regenTimer--;
        } else {
            regenTimer = getGolemType().regenDelay;
            if (getGolemDecoration().contains("F")) {
                regenTimer = (int) (regenTimer * 0.66f);
            }
            if (getHealth() < getMaxHealth()) {
                level().broadcastEntityEvent(this, (byte) 5);
                heal(1f);
            }
        }
        // голем, ушедший дальше 48 блоков от дома или застрявший в стене, возвращается домой
        BlockPos home = getHome();
        if (hasRestriction() && (distanceToSqr(home.getX(), home.getY(), home.getZ()) >= 2304.0 || isInWall())) {
            returnHome();
        }
    }

    private void returnHome() {
        BlockPos home = getHome();
        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos spot = home.offset(dx, dy, dz);
                    BlockPos floor = spot.below();
                    if (level().getBlockState(floor).isFaceSturdy(level(), floor, Direction.UP)
                            && !level().getBlockState(spot).isRedstoneConductor(level(), spot)) {
                        moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, getYRot(), getXRot());
                        getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Големы не дышат. */
    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public void setSecondsOnFire(int seconds) {
        if (!getGolemType().fireResist) {
            super.setSecondsOnFire(seconds);
        }
    }

    @Override
    public int getArmorValue() {
        int armor = super.getArmorValue() + getGolemType().armor;
        String deco = getGolemDecoration();
        if (deco.contains("V")) {
            armor++;
        }
        if (deco.contains("P")) {
            armor += 4;
        }
        return Math.min(20, armor);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        paused = false;
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypeTags.IS_FIRE) && getGolemType().fireResist) {
            return false;
        }
        if (getGolemType() == GolemType.THAUMIUM && source.is(DamageTypes.MAGIC)) {
            amount *= 0.5f;
        }
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            BlockPos home = getHome();
            moveTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 0f, 0f);
        }
        // улучшение энтропии: шипы
        int thorns = getUpgradeAmount(UPGRADE_ENTROPY);
        Entity attacker = source.getDirectEntity();
        if (attacker != null && thorns > 0 && attacker != this && !level().isClientSide) {
            attacker.hurt(damageSources().thorns(this), thorns * 2 + random.nextInt(2 * thorns));
            attacker.playSound(SoundEvents.THORNS_HIT, 0.5f, 1f);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            // визор: добыча выпадает так, будто существо убил хозяин
            if (getGolemDecoration().contains("V") && target instanceof LivingEntity living && getOwner() instanceof Player owner) {
                living.setLastHurtByPlayer(owner);
            }
            int fire = getUpgradeAmount(UPGRADE_FIRE);
            if (fire > 0) {
                target.setSecondsOnFire(fire * 4);
            }
        }
        return hit;
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != EntityType.VILLAGER && type != getType() && type != EntityType.BAT;
    }

    /** Кого голем со стражьим или мясницким ядром считает целью (isValidTarget оригинала). */
    public boolean isValidTarget(LivingEntity target) {
        if (!target.isAlive() || target instanceof Bat || target instanceof AbstractVillager || target instanceof Golem
                || target instanceof Player player && player.getUUID().equals(getOwnerUUID())
                || !isWithinHomeDistance(target.blockPosition())) {
            return false;
        }
        boolean animal = target instanceof Animal && !(target instanceof Enemy)
                && !(target instanceof TamableAnimal tamable && tamable.isTame());
        if (getCore() == GolemCoreItem.BUTCHER) {
            return animal && !target.isBaby();
        }
        boolean order = getUpgradeAmount(UPGRADE_ORDER) > 0;
        if (target instanceof Creeper) {
            return canAttackCreepers() && order;
        }
        if (target instanceof Enemy) {
            return canAttackHostiles();
        }
        if (animal) {
            return canAttackAnimals() && order;
        }
        return target instanceof Player && canAttackPlayers() && order;
    }

    /** Надевает украшение, если такого ещё нет и оно не спорит с уже надетым (addDecoration оригинала). */
    public boolean addDecoration(String type) {
        String deco = getGolemDecoration();
        if (deco.contains(type)) {
            return false;
        }
        for (String pair : new String[]{"FH", "GV", "BP"}) {
            if (pair.contains(type) && (deco.contains(pair.substring(0, 1)) || deco.contains(pair.substring(1)))) {
                return false;
            }
        }
        setGolemDecoration(deco + type);
        playSound(ModSounds.CAMERA_CLACK.get(), 1f, 1f);
        float health = getHealth();
        setupGolem();
        setHealth(Math.min(health, getMaxHealth()));
        return true;
    }

    /** Выстрел из дротикомёта: чем больше улучшений воды, тем точнее. */
    public void performRangedAttack(LivingEntity target) {
        GolemDart dart = new GolemDart(level(), this, target, 1.6f, 7f - getUpgradeAmount(UPGRADE_WATER) * 1.75f);
        dart.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4);
        playSound(ModSounds.GOLEM_IRON_SHOOT.get(), 0.5f, 1f / (random.nextFloat() * 0.4f + 0.6f));
        level().addFreshEntity(dart);
        startLeftArmTimer();
    }

    /** Продвинутые големы бьют чуть чаще. */
    public int getAttackSpeed() {
        return 20 - (isAdvanced() ? 2 : 0);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        if (!itemCarried.isEmpty()) {
            spawnAtLocation(itemCarried, 0.5f);
        }
    }

    // ---------- взаимодействие ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof GolemCoreItem core && getCore() == -1 && core.getCore() >= 0) {
            if (!level().isClientSide) {
                setCore((byte) core.getCore());
                setupGolem();
                setupGolemInventory();
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(ModSounds.UPGRADE.get(), 0.5f, 1f);
                level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.getItem() instanceof GolemUpgradeItem upgrade) {
            for (int slot = 0; slot < getUpgradeSlots(); slot++) {
                if (getUpgrade(slot) == -1 && getUpgradeAmount(upgrade.getUpgrade()) < 2) {
                    if (!level().isClientSide) {
                        setUpgrade(slot, (byte) upgrade.getUpgrade());
                        setupGolem();
                        setupGolemInventory();
                        if (!player.getAbilities().instabuild) {
                            held.shrink(1);
                        }
                        playSound(ModSounds.UPGRADE.get(), 0.5f, 1f);
                    }
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
            }
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof thaumcraft.common.items.golem.GolemDecorationItem decoration) {
            if (!level().isClientSide && addDecoration(decoration.getLetter()) && !player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(Items.COOKIE)) {
            // печенье лечит голема и ненадолго его подгоняет
            if (level().isClientSide) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(ParticleTypes.HEART, getX() + random.nextFloat() * getBbWidth() * 2f - getBbWidth(),
                            getY() + 0.5 + random.nextFloat() * getBbHeight(), getZ() + random.nextFloat() * getBbWidth() * 2f - getBbWidth(),
                            random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
                }
            } else {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(SoundEvents.GENERIC_EAT, 0.3f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1f);
                MobEffectInstance speed = getEffect(MobEffects.MOVEMENT_SPEED);
                int duration = 600 + (speed != null && speed.getDuration() < 2400 ? speed.getDuration() : 0);
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
                heal(5f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return GolemTasks.interact(this, player, hand);
    }

    // ---------- анимация ----------

    /** Положение рук при работе: 0..3..0 за шесть тиков. */
    public int getActionTimer() {
        return 3 - Math.abs(action - 3);
    }

    public void startActionTimer() {
        if (action == 0) {
            action = 6;
            level().broadcastEntityEvent(this, (byte) 4);
        }
    }

    public void startLeftArmTimer() {
        if (leftArm == 0) {
            leftArm = 5;
            level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    public void startRightArmTimer() {
        if (rightArm == 0) {
            rightArm = 5;
            level().broadcastEntityEvent(this, (byte) 8);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 4 -> action = 6;
            case 5 -> healing = 5;
            case 6 -> leftArm = 5;
            case 8 -> rightArm = 5;
            case 7 -> bootup = 33f;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide) {
            // голем, которого мы впервые видим уже с ядром, стоит с поднятой головой
            if (CORE.equals(key) && getCore() >= 0 && bootup < 0f) {
                bootup = 0f;
            }
            // число ячеек-образцов клиент узнаёт по строке их цветов
            if (COLORS.equals(key) && inventory.getContainerSize() != entityData.get(COLORS).length()) {
                inventory = new SimpleContainer(entityData.get(COLORS).length());
            }
            if (TYPE.equals(key) || DECORATION.equals(key) || UPGRADES.equals(key)) {
                setupGolem();
            }
        }
    }

    public float getHealthPercentage() {
        return getHealth() / getMaxHealth();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CAMERA_CLACK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CAMERA_CLACK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CAMERA_CLACK.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("item.thaumcraft.golem_" + getGolemType().id());
    }

    // ---------- сохранение ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BlockPos home = getHome();
        tag.putInt("HomeX", home.getX());
        tag.putInt("HomeY", home.getY());
        tag.putInt("HomeZ", home.getZ());
        tag.putByte("HomeFacing", (byte) homeFacing);
        tag.putByte("GolemType", (byte) getGolemType().ordinal());
        tag.putByte("Core", getCore());
        tag.putString("Decoration", getGolemDecoration());
        tag.putByte("toggles", entityData.get(TOGGLES));
        tag.putBoolean("advanced", isAdvanced());
        tag.putByteArray("colors", colors);
        tag.putByteArray("upgrades", upgrades);
        if (!fluidCarried.isEmpty()) {
            tag.put("Fluid", fluidCarried.writeToNBT(new CompoundTag()));
        }
        if (essentia != null && essentiaAmount > 0) {
            tag.putString("essentia", essentia.getTag());
            tag.putByte("essentiaAmount", (byte) essentiaAmount);
        }
        if (!itemCarried.isEmpty()) {
            tag.put("ItemCarried", itemCarried.save(new CompoundTag()));
        }
        tag.putString("Owner", entityData.get(OWNER));
        tag.put("Markers", GolemMarker.save(markers));
        tag.put("Inventory", saveInventory(inventory));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")));
        homeFacing = tag.getByte("HomeFacing");
        configure(GolemType.byId(tag.getByte("GolemType")), tag.getBoolean("advanced"));
        setCore(tag.getByte("Core"));
        fluidCarried = tag.contains("Fluid") ? net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"))
                : net.minecraftforge.fluids.FluidStack.EMPTY;
        essentia = Aspect.byTag(tag.getString("essentia"));
        essentiaAmount = essentia == null ? 0 : tag.getByte("essentiaAmount");
        entityData.set(TOGGLES, tag.getByte("toggles"));
        itemCarried = tag.contains("ItemCarried") ? ItemStack.of(tag.getCompound("ItemCarried")) : ItemStack.EMPTY;
        setGolemDecoration(tag.getString("Decoration"));
        entityData.set(OWNER, tag.getString("Owner"));
        markers = GolemMarker.load(tag, "Markers");
        loadUpgrades(tag.getByteArray("upgrades"));
        colors = tag.getByteArray("colors");
        setupGolem();
        setupGolemInventory();
        loadInventory(inventory, tag.getList("Inventory", Tag.TAG_COMPOUND));
        updateCarried();
    }

    public static ListTag saveInventory(SimpleContainer container) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag item = stack.save(new CompoundTag());
                item.putByte("Slot", (byte) slot);
                list.add(item);
            }
        }
        return list;
    }

    public static void loadInventory(SimpleContainer container, ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            int slot = item.getByte("Slot") & 255;
            if (slot < container.getContainerSize()) {
                container.setItem(slot, ItemStack.of(item));
            }
        }
    }

    /** AIReturnHome оригинала: голем без дела идёт к дому, а упёршись — пробует обойти. */
    private static class ReturnHomeGoal extends Goal {
        private final Golem golem;
        private int pathingDelay;
        private int pathingDelayInc = 5;
        private int count;
        private BlockPos previous = BlockPos.ZERO;

        ReturnHomeGoal(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean atHome() {
            BlockPos home = golem.getHome();
            return golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) < 3.0;
        }

        @Override
        public boolean canUse() {
            if (pathingDelay > 0) {
                pathingDelay--;
            }
            return pathingDelay <= 0 && golem.hasRestriction() && !atHome();
        }

        @Override
        public boolean canContinueToUse() {
            return pathingDelay <= 0 && count > 0 && !golem.getNavigation().isDone() && !atHome();
        }

        @Override
        public void start() {
            count = 20;
            previous = golem.blockPosition();
            BlockPos home = golem.getHome();
            golem.getNavigation().moveTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            count--;
            if (count == 0 && previous.equals(golem.blockPosition())) {
                Vec3 around = DefaultRandomPos.getPos(golem, 2, 1);
                if (around != null) {
                    count = 20;
                    if (!golem.getNavigation().moveTo(around.x + 0.5, around.y + 0.5, around.z + 0.5, 1.0)) {
                        pathingDelay = pathingDelayInc;
                        if (pathingDelayInc < 50) {
                            pathingDelayInc += 5;
                        }
                    } else {
                        pathingDelayInc = 5;
                    }
                }
            }
        }
    }
}
