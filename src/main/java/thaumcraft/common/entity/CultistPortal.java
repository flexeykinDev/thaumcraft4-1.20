package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.ThaumBannerBlock;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

/**
 * Багровый портал — перенос EntityCultistPortal оригинала, босс одного из залов лабиринта: висящий в воздухе
 * алый разрыв (500 здоровья). Раскрывшись, расставляет по сторонам знамёна культа и разбрасывает ящики с добычей,
 * затем, пока рядом есть игрок, всё чаще выпускает рыцарей и клириков Багрового культа (тем реже, чем их больше
 * вокруг), на тринадцатой волне — претора культа, а дальше каждая новая волна ранит сам портал. Подошедших
 * вплотную бьёт разрядом. Погибая, взрывается и оставляет первозданную жемчужину.
 */
public class CultistPortal extends Monster {
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int stage;
    private int stagecounter = 200;
    public int pulse;

    public CultistPortal(EntityType<? extends CultistPortal> type, Level level) {
        super(type, level);
        xpReward = 30;
        setPersistenceRequired();
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 5.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1f;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            bossEvent.setName(getDisplayName());
            bossEvent.setProgress(getHealth() / getMaxHealth());
            if (stagecounter <= 0) {
                if (level().getNearestPlayer(this, 48.0) != null) {
                    level().broadcastEntityEvent(this, (byte) 16);
                    if (stage < 5) {
                        stagecounter = 15 + random.nextInt(10 - stage) - stage;
                        spawnMinions();
                    } else if (stage == 12) {
                        stagecounter = 50 + getTiming() * 2 + random.nextInt(50);
                        spawnBoss();
                    } else {
                        int t = getTiming();
                        stagecounter = t + random.nextInt(5 + t / 3);
                        spawnMinions();
                    }
                    stage++;
                } else {
                    stagecounter = 30 + random.nextInt(30);
                }
            } else {
                stagecounter--;
                if (stagecounter == 160 && stage == 0) {
                    placeBanners();
                }
                if (stagecounter > 20 && stagecounter < 150 && stage == 0 && stagecounter % 13 == 0) {
                    int a = (int) getX() + random.nextInt(5) - random.nextInt(5);
                    int b = (int) getZ() + random.nextInt(5) - random.nextInt(5);
                    BlockPos at = new BlockPos(a, (int) getY(), b);
                    if (a != (int) getX() && b != (int) getZ() && level().isEmptyBlock(at)) {
                        level().broadcastEntityEvent(this, (byte) 16);
                        float rr = random.nextFloat();
                        level().setBlock(at, (rr < 0.05f ? ModBlocks.LOOT_CRATE_RARE : rr < 0.2f ? ModBlocks.LOOT_CRATE_UNCOMMON : ModBlocks.LOOT_CRATE_COMMON)
                                .get().defaultBlockState(), 3);
                        FxPacket.send((ServerLevel) level(), FxPacket.BLOCK_ARC, Vec3.atLowerCornerOf(at), Vec3.ZERO, getId(), 1);
                        playSound(ModSounds.WAND_FAIL.get(), 1f, 1f);
                    }
                }
            }
            if (stage < 12) {
                heal(1f);
            }
        }
        if (pulse > 0) {
            pulse--;
        }
    }

    /** Четыре знамени культа в шести блоках по сторонам света. */
    private void placeBanners() {
        level().broadcastEntityEvent(this, (byte) 16);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos at = new BlockPos((int) getX() - dir.getStepX() * 6, (int) getY(), (int) getZ() + dir.getStepZ() * 6);
            int face = switch (dir) {
                case NORTH -> 8;
                case WEST -> 12;
                case EAST -> 4;
                default -> 0;
            };
            level().setBlock(at, ModBlocks.THAUM_BANNER.get().defaultBlockState().setValue(ThaumBannerBlock.ROTATION, face), 3);
            FxPacket.send((ServerLevel) level(), FxPacket.BLOCK_ARC, Vec3.atLowerCornerOf(at), Vec3.ZERO, getId(), 1);
            playSound(ModSounds.WAND_FAIL.get(), 1f, 1f);
        }
    }

    /** Чем больше культистов вокруг, тем дольше портал медлит со следующей волной. */
    private int getTiming() {
        return level().getEntitiesOfClass(Cultist.class, new AABB(blockPosition()).inflate(32.0)).size() * 20;
    }

    private void spawnMinions() {
        Cultist cultist = (random.nextFloat() > 0.33f ? ModEntities.CULTIST_KNIGHT : ModEntities.CULTIST_CLERIC).get().create(level());
        if (cultist != null) {
            cultist.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
            cultist.finalizeSpawn((ServerLevel) level(), level().getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            cultist.restrictTo(blockPosition(), 32);
            level().addFreshEntity(cultist);
            cultist.spawnAnim();
            cultist.playSound(ModSounds.WAND_FAIL.get(), 1f, 1f);
        }
        if (stage > 12) {
            hurt(damageSources().fellOutOfWorld(), 5 + random.nextInt(5));
        }
    }

    private void spawnBoss() {
        CultistLeader leader = ModEntities.CULTIST_LEADER.get().create(level());
        if (leader == null) {
            return;
        }
        leader.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
        leader.finalizeSpawn((ServerLevel) level(), level().getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        leader.restrictTo(blockPosition(), 32);
        level().addFreshEntity(leader);
        leader.spawnAnim();
        leader.playSound(ModSounds.WAND_FAIL.get(), 1f, 1f);
    }

    /** onCollideWithPlayer оригинала: вплотную подошедших бьёт разрядом. */
    @Override
    public void playerTouch(Player player) {
        if (distanceToSqr(player) < 3.0 && player.hurt(damageSources().indirectMagic(this, this), 8f)) {
            playSound(ModSounds.ZAP.get(), 1f, (random.nextFloat() - random.nextFloat()) * 0.1f + 1f);
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.75f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 540;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MONOLITH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ZAP.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHOCK.get();
    }

    /** entityDropSpecialItem оригинала: первозданная жемчужина висит в воздухе и не пропадает. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        ItemEntity pearl = spawnAtLocation(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), getBbHeight() / 2f);
        if (pearl != null) {
            pearl.setUnlimitedLifetime();
            pearl.setNoGravity(true);
            pearl.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), 2f, Level.ExplosionInteraction.NONE);
        }
        super.die(source);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            pulse = 10;
            spawnAnim();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("stage", stage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stage = tag.getInt("stage");
    }
}
