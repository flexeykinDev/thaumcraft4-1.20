package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.items.ManaBeanItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.taint.Taint;
import thaumcraft.common.world.ModBiomes;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Печ (EntityPech оригинала): невысокий собиратель с огромным тюком, житель волшебных биомов. Сам не нападает и
 * сторонится игроков, но стоит ударить одного — злятся все печи в округе. Подбирает с земли всё подряд к себе в тюк,
 * а ценное (золото, алмазы, изумруды, зёрна маны — всё, в чём есть аспект жадности) принимает как подарок: получив
 * достаточно, перестаёт бояться игрока и соглашается торговать. Три вида: добытчик с инструментом, маг с жезлом
 * и искатель с луком — у каждого свой товар.
 */
public class Pech extends Monster implements RangedAttackMob {
    public static final int FORAGER = 0;
    public static final int MAGE = 1;
    public static final int STALKER = 2;

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(Pech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(Pech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(Pech.class, EntityDataSerializers.BOOLEAN);

    /** Во сколько печ ценит подарок: шанс расположить его к себе — эта цена из десяти. */
    private static final Map<Item, Integer> VALUED = Map.of(Items.GOLD_INGOT, 2, Items.GOLDEN_APPLE, 2, Items.ENDER_PEARL, 3,
            Items.DIAMOND, 4, Items.EMERALD, 5);

    /** Тюк: то, что печ насобирал. */
    public final SimpleContainer loot = new SimpleContainer(9);
    /** Идёт торг: печ стоит на месте. */
    public boolean trading;
    private boolean updateAiNextTick = true;
    private int chargeCount;
    /** Только на клиенте: шевеление челюстью, когда печ бормочет. */
    public float mumble;

    private final Goal bowAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15f);
    private final Goal blastAttack = new RangedAttackGoal(this, 0.6, 20, 30, 15f);
    private final Goal meleeAttack = new MeleeAttackGoal(this, 0.6, false);
    private final Goal avoidPlayer = new AvoidEntityGoal<>(this, Player.class, 8f, 0.5, 0.6);

    public Pech(EntityType<? extends Pech> type, Level level) {
        super(type, level);
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        setDropChance(EquipmentSlot.MAINHAND, 0.2f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.ARMOR, 2.0);
    }

    /**
     * getCanSpawnHere оригинала: в темноте, в волшебном биоме (кроме порченых земель), не больше четырёх печей рядом;
     * вне обычного мира — только в волшебном лесу и жутких землях.
     */
    public static boolean canSpawn(EntityType<Pech> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        boolean magical = level.getBiome(pos).is(Tags.Biomes.IS_MAGICAL) && !Taint.isTainted(level, pos);
        if (level.getLevel().dimension() != Level.OVERWORLD && !ModBiomes.is(level, pos, ModBiomes.MAGICAL_FOREST)
                && !ModBiomes.is(level, pos, ModBiomes.EERIE)) {
            magical = false;
        }
        return magical && level.getEntitiesOfClass(Pech.class, new AABB(pos).inflate(16.0)).size() < 4
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TYPE, FORAGER);
        entityData.define(ANGER, 0);
        entityData.define(TAMED, false);
    }

    public int getPechType() {
        return entityData.get(TYPE);
    }

    public void setPechType(int type) {
        entityData.set(TYPE, type);
    }

    public int getAnger() {
        return entityData.get(ANGER);
    }

    public void setAnger(int anger) {
        entityData.set(ANGER, anger);
    }

    public boolean isTamed() {
        return entityData.get(TAMED);
    }

    public void setTamed(boolean tamed) {
        entityData.set(TAMED, tamed);
        updateAiNextTick = true;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(switch (getPechType()) {
            case MAGE -> "entity.thaumcraft.pech.mage";
            case STALKER -> "entity.thaumcraft.pech.stalker";
            default -> "entity.thaumcraft.pech";
        });
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.66f;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeGoal(this));
        goalSelector.addGoal(3, new ItemGotoGoal(this));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> true));
        goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3f, 1f));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8f));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Чем драться, зависит от того, что в руке: лук, жезл или что-нибудь тяжёлое. Прирученный печ игроков не сторонится. */
    private void setCombatTask() {
        goalSelector.removeGoal(meleeAttack);
        goalSelector.removeGoal(bowAttack);
        goalSelector.removeGoal(blastAttack);
        goalSelector.removeGoal(avoidPlayer);
        ItemStack held = getMainHandItem();
        if (held.is(Items.BOW)) {
            goalSelector.addGoal(2, bowAttack);
        } else if (held.getItem() instanceof WandItem) {
            goalSelector.addGoal(2, blastAttack);
        } else {
            goalSelector.addGoal(2, meleeAttack);
        }
        if (!isTamed()) {
            goalSelector.addGoal(4, avoidPlayer);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!level().isClientSide && slot == EquipmentSlot.MAINHAND) {
            updateAiNextTick = true;
        }
    }

    /** addRandomArmor оригинала: что печ держит в руке; от этого зависит и его вид. */
    private void giveRandomTool() {
        ItemStack held = switch (random.nextInt(20)) {
            case 0, 12 -> {
                ItemStack wand = WandItem.create(ModItems.WAND.get(), WandRod.WOOD, WandCap.IRON, false);
                WandItem.setFocus(wand, new ItemStack(ModItems.FOCUS_PECH.get()));
                WandItem.addVis(wand, Aspect.EARTH, 2 + random.nextInt(6), true);
                WandItem.addVis(wand, Aspect.ENTROPY, 2 + random.nextInt(6), true);
                WandItem.addVis(wand, Aspect.WATER, 2 + random.nextInt(6), true);
                WandItem.addVis(wand, Aspect.AIR, random.nextInt(4), true);
                WandItem.addVis(wand, Aspect.FIRE, random.nextInt(4), true);
                WandItem.addVis(wand, Aspect.ORDER, random.nextInt(4), true);
                yield wand;
            }
            case 1 -> new ItemStack(Items.STONE_SWORD);
            case 2, 4, 10, 11, 13 -> new ItemStack(Items.BOW);
            case 3 -> new ItemStack(Items.STONE_AXE);
            case 5 -> new ItemStack(Items.IRON_SWORD);
            case 6 -> new ItemStack(Items.IRON_AXE);
            case 7 -> new ItemStack(Items.FISHING_ROD);
            case 8 -> new ItemStack(Items.STONE_PICKAXE);
            case 9 -> new ItemStack(Items.IRON_PICKAXE);
            default -> ItemStack.EMPTY;
        };
        if (!held.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, held);
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(level, difficulty, reason, data, tag);
        giveRandomTool();
        ItemStack held = getMainHandItem();
        if (held.getItem() instanceof WandItem) {
            setPechType(MAGE);
            setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        } else if (!held.isEmpty()) {
            if (held.is(Items.BOW)) {
                setPechType(STALKER);
            }
            populateDefaultEquipmentEnchantments(random, difficulty);
        }
        setCanPickUpLoot(random.nextFloat() < 0.75f * difficulty.getSpecialMultiplier());
        return data;
    }

    // ---------- настроение ----------

    private void becomeAngryAt(LivingEntity enemy) {
        if (getAnger() <= 0) {
            level().broadcastEntityEvent(this, (byte) 19);
            playSound(ModSounds.PECH_CHARGE.get(), getSoundVolume(), getVoicePitch());
        }
        setTarget(enemy);
        setAnger(400 + random.nextInt(400));
        setTamed(false);
    }

    /** Ударивший печа игрок ссорится со всеми печами в тридцати двух блоках. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && source.getEntity() instanceof Player player) {
            for (Pech other : level().getEntitiesOfClass(Pech.class, getBoundingBox().inflate(32, 16, 32), p -> p != this)) {
                other.becomeAngryAt(player);
            }
            becomeAngryAt(player);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        if (mumble > 0f) {
            mumble *= 0.75f;
        }
        if (!level().isClientSide) {
            if (getAnger() > 0) {
                setAnger(getAnger() - 1);
            }
            // пока печ зол, он сам ищет, на кого броситься
            if (getAnger() > 0 && getTarget() == null) {
                Player nearest = level().getNearestPlayer(getX(), getY(), getZ(), 16.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
                if (nearest != null && hasLineOfSight(nearest)) {
                    setTarget(nearest);
                    if (chargeCount > 0) {
                        chargeCount--;
                    }
                    if (chargeCount == 0) {
                        chargeCount = 100;
                        playSound(ModSounds.PECH_CHARGE.get(), getSoundVolume(), getVoicePitch());
                    }
                    level().broadcastEntityEvent(this, (byte) 17);
                }
            }
        } else if (random.nextInt(15) == 0 && getAnger() > 0) {
            mood(ParticleTypes.ANGRY_VILLAGER, 1);
        } else if (random.nextInt(25) == 0 && isTamed()) {
            mood(ParticleTypes.HAPPY_VILLAGER, 1);
        }
        super.tick();
    }

    private void mood(net.minecraft.core.particles.ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            level().addParticle(particle, getX() + random.nextFloat() * getBbWidth() * 2f - getBbWidth(),
                    getY() + 0.5 + random.nextFloat() * getBbHeight(), getZ() + random.nextFloat() * getBbWidth() * 2f - getBbWidth(),
                    random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 16 -> mumble = (float) Math.PI;
            case 17 -> mumble = (float) Math.PI * 2f;
            case 18 -> mood(ParticleTypes.HAPPY_VILLAGER, 5);
            case 19 -> {
                mood(ParticleTypes.ANGRY_VILLAGER, 5);
                mumble = (float) Math.PI * 2f;
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void customServerAiStep() {
        if (updateAiNextTick) {
            updateAiNextTick = false;
            setCombatTask();
        }
        super.customServerAiStep();
        if (tickCount % 40 == 0) {
            heal(1f);
        }
    }

    // ---------- подбор и подарки ----------

    public static boolean isValued(ItemStack stack) {
        return getValue(stack) > 0;
    }

    /** Цена предмета для печа: из короткого списка любимых вещей либо по количеству аспекта жадности в нём, но не больше 32. */
    public static int getValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof ManaBeanItem) {
            return 1;
        }
        Integer listed = VALUED.get(stack.getItem());
        return listed != null ? listed : Math.min(32, ItemAspectManager.INSTANCE.getAspects(stack).getAmount(Aspect.GREED));
    }

    public boolean canPickup(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!isTamed() && isValued(stack)) {
            return true;
        }
        return loot.canAddItem(stack);
    }

    /**
     * Подбирает предмет. Неприрученный печ ценную вещь не прячет, а «съедает» по одной: каждая может расположить его
     * к игроку. Всё прочее идёт в тюк.
     *
     * @return то, что в тюк не влезло
     */
    public ItemStack pickupItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (!isTamed() && isValued(stack)) {
            if (random.nextInt(10) < getValue(stack)) {
                setTamed(true);
                level().broadcastEntityEvent(this, (byte) 18);
            }
            stack.shrink(1);
            return stack;
        }
        return loot.addItem(stack);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getItemInHand(hand).is(Items.NAME_TAG)) {
            return InteractionResult.PASS;
        }
        if (isTamed()) {
            if (player instanceof ServerPlayer server) {
                NetworkHooks.openScreen(server, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Pech.this.getDisplayName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                        return new PechMenu(id, inventory, Pech.this);
                    }
                }, buffer -> buffer.writeVarInt(getId()));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // ---------- бой ----------

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (getPechType() == STALKER) {
            ItemStack bow = getMainHandItem();
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), power);
            double dx = target.getX() - getX();
            double dy = target.getY(0.3333333333333333) - arrow.getY();
            double dz = target.getZ() - getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            int difficulty = level().getDifficulty().getId();
            arrow.shoot(dx, dy + flat * 0.2, dz, 1.6f, 14 - difficulty * 4);
            arrow.setBaseDamage(power * 2f + random.nextGaussian() * 0.25 + difficulty * 0.11f);
            int strength = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
            if (strength > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + strength * 0.5 + 0.5);
            }
            int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
            if (punch > 0) {
                arrow.setKnockback(punch);
            }
            playSound(SoundEvents.ARROW_SHOOT, 1f, 1f / (random.nextFloat() * 0.4f + 0.8f));
            level().addFreshEntity(arrow);
        } else if (getPechType() == MAGE) {
            PechBlastEntity blast = new PechBlastEntity(level(), this, 1, 0, random.nextFloat() < 0.1f);
            double dx = target.getX() + target.getDeltaMovement().x - getX();
            double dy = target.getY() + target.getEyeHeight() - 1.5 - getY();
            double dz = target.getZ() + target.getDeltaMovement().z - getZ();
            float flat = Mth.sqrt((float) (dx * dx + dz * dz));
            blast.shoot(dx, dy + flat * 0.1f, dz, 1.5f, 4f);
            playSound(ModSounds.ICE.get(), 0.4f, 1f + random.nextFloat() * 0.1f);
            level().addFreshEntity(blast);
        }
        swing(InteractionHand.MAIN_HAND);
    }

    // ---------- добыча и сохранение ----------

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        for (int i = 0; i < loot.getContainerSize(); i++) {
            if (!loot.getItem(i).isEmpty() && random.nextFloat() < 0.88f) {
                spawnAtLocation(loot.getItem(i).copy(), 1.5f);
            }
        }
        List<Aspect> primals = Aspect.primals();
        for (int i = 0; i < 1 + looting; i++) {
            if (random.nextBoolean()) {
                spawnAtLocation(ManaBeanItem.of(ModItems.MANA_BEAN.get(), primals.get(random.nextInt(primals.size()))), 1.5f);
            }
        }
        if (random.nextInt(10) < 1 + looting) {
            spawnAtLocation(new ItemStack(ModItems.COIN.get()), 1.5f);
        }
        // редкая добыча, только с убитых игроком: фрагмент знаний
        if (recentlyHit && random.nextFloat() < 0.025f + looting * 0.01f) {
            spawnAtLocation(new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), 1.5f);
        }
    }

    /** Печ с полупустым тюком может исчезнуть, набравший добра — остаётся. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        int stacks = 0;
        for (int i = 0; i < loot.getContainerSize(); i++) {
            if (!loot.getItem(i).isEmpty()) {
                stacks++;
            }
        }
        return stacks < 5;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("PechType", (byte) getPechType());
        tag.putShort("Anger", (short) getAnger());
        tag.putBoolean("Tamed", isTamed());
        tag.put("Loot", loot.createTag());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPechType(tag.getByte("PechType"));
        setAnger(tag.getShort("Anger"));
        setTamed(tag.getBoolean("Tamed"));
        loot.fromTag(tag.getList("Loot", 10));
        updateAiNextTick = true;
    }

    // ---------- голос ----------

    /** Рядом с сородичем печ не ворчит себе под нос, а «торгуется». */
    @Override
    public void playAmbientSound() {
        if (!level().isClientSide) {
            if (random.nextInt(3) == 0 && !level().getEntitiesOfClass(Pech.class, getBoundingBox().inflate(4, 2, 4), p -> p != this).isEmpty()) {
                level().broadcastEntityEvent(this, (byte) 17);
                playSound(ModSounds.PECH_TRADE.get(), getSoundVolume(), getVoicePitch());
                return;
            }
            level().broadcastEntityEvent(this, (byte) 16);
        }
        super.playAmbientSound();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PECH_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PECH_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PECH_DEATH.get();
    }

    // ---------- цели ----------

    /** AIPechTradePlayer оригинала: пока открыто окно торга, прирученный печ стоит на месте. */
    private static class TradeGoal extends Goal {
        private final Pech pech;

        TradeGoal(Pech pech) {
            this.pech = pech;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return pech.isAlive() && !pech.isInWater() && pech.isTamed() && pech.onGround() && !pech.hurtMarked && pech.trading;
        }

        @Override
        public void start() {
            pech.getNavigation().stop();
        }

        @Override
        public void stop() {
            pech.trading = false;
        }
    }

    /** AIPechItemEntityGoto оригинала: печ бежит к лежащему в шестнадцати блоках предмету и подбирает его. */
    private static class ItemGotoGoal extends Goal {
        private static final float RANGE = 16f;
        private final Pech pech;
        private ItemEntity target;
        private int count;
        private int failedPathPenalty;

        ItemGotoGoal(Pech pech) {
            this.pech = pech;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (pech.tickCount % 5 > 0 || --count > 0) {
                return false;
            }
            double best = Double.MAX_VALUE;
            target = null;
            for (ItemEntity item : pech.level().getEntitiesOfClass(ItemEntity.class, pech.getBoundingBox().inflate(RANGE))) {
                // то, что игрок получил от печа и выбросил при закрытии окна, печ обратно не тащит
                if (!pech.canPickup(item.getItem()) || item.getPersistentData().getBoolean(PechMenu.DROP_TAG)) {
                    continue;
                }
                double distance = item.distanceToSqr(pech);
                if (distance < best && distance <= RANGE * RANGE) {
                    best = distance;
                    target = item;
                }
            }
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !pech.getNavigation().isDone() && target.distanceToSqr(pech) < RANGE * RANGE;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void start() {
            pech.getNavigation().moveTo(target, 0.75);
            count = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            pech.getLookControl().setLookAt(target, 30f, 30f);
            if (pech.getSensing().hasLineOfSight(target) && --count <= 0) {
                count = failedPathPenalty + 4 + pech.getRandom().nextInt(4);
                pech.getNavigation().moveTo(target, 0.75);
                var path = pech.getNavigation().getPath();
                if (path != null && path.getEndNode() != null
                        && target.distanceToSqr(path.getEndNode().x, path.getEndNode().y, path.getEndNode().z) < 1.0) {
                    failedPathPenalty = 0;
                } else {
                    failedPathPenalty += 10;
                }
            }
            if (pech.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) <= 1.5) {
                count = 0;
                int before = target.getItem().getCount();
                ItemStack rest = pech.pickupItem(target.getItem().copy());
                if (rest.isEmpty()) {
                    target.discard();
                } else {
                    target.setItem(rest);
                }
                if (rest.getCount() != before) {
                    target.playSound(SoundEvents.ITEM_PICKUP, 0.2f, ((pech.getRandom().nextFloat() - pech.getRandom().nextFloat()) * 0.7f + 1f) * 2f);
                }
            }
        }
    }
}
