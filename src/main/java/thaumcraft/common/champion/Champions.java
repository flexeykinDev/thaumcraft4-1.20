package thaumcraft.common.champion;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.Warp;

import java.util.UUID;

/**
 * Особые мобы («чемпионы» оригинала: ChampionModifier, EntityUtils.makeChampion, EventHandlerEntity). Изредка обычный
 * монстр появляется усиленным: +30 здоровья, втрое больший урон и одно из тринадцати свойств, вынесенное в имя.
 * Убитый игроком, он оставляет опыт и мешок с добычей. Номер свойства хранит атрибут, видимый и клиенту:
 * по нему рисуются частицы вокруг моба.
 */
public final class Champions {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Thaumcraft.MODID);
    /** -2 — судьба моба ещё не решена, -1 — обычный, от 0 — номер свойства. */
    public static final RegistryObject<Attribute> CHAMPION_MOD = ATTRIBUTES.register("champion_mod",
            () -> new RangedAttribute("attribute.name.thaumcraft.champion_mod", -2.0, -2.0, 100.0).setSyncable(true));

    /** Монстры, которые могут стать особыми (championWhiteList оригинала). */
    public static final TagKey<EntityType<?>> WHITELIST = TagKey.create(Registries.ENTITY_TYPE, Thaumcraft.id("champion_whitelist"));

    /** Когда срабатывает свойство. */
    public enum Trigger { PASSIVE, EVERY_TICK, ON_ATTACK, ON_HURT }

    public record Kind(int id, String name, Trigger trigger, AttributeModifier marker) {
        public Component title() {
            return Component.translatable("champion.mod." + name);
        }
    }

    public static final int BOLD = 0;
    public static final int SPINED = 1;
    public static final int ARMORED = 2;
    public static final int MIGHTY = 3;
    public static final int GRIM = 4;
    public static final int WARDED = 5;
    public static final int WARP = 6;
    public static final int UNDYING = 7;
    public static final int FIERY = 8;
    public static final int SICKLY = 9;
    public static final int VENOMOUS = 10;
    public static final int VAMPIRIC = 11;
    public static final int INFESTED = 12;

    public static final Kind[] MODS = {
            mod(BOLD, "bold", Trigger.PASSIVE, "40289aa1-907f-4ac6-ad79-e6681efe2cbc"),
            mod(SPINED, "spine", Trigger.ON_HURT, "365eead5-3f15-42a8-9e68-36100faef945"),
            mod(ARMORED, "armor", Trigger.ON_HURT, "4e23758d-348e-42a8-8de6-08ae0a59033c"),
            mod(MIGHTY, "mighty", Trigger.PASSIVE, "6d2ffe79-f034-4a06-b288-e1916c21e385"),
            mod(GRIM, "grim", Trigger.ON_ATTACK, "0f23321e-f921-4246-90b8-21ef202de224"),
            mod(WARDED, "warded", Trigger.EVERY_TICK, "b622c4d8-abc6-4db7-b3ee-5cf71b8e5286"),
            mod(WARP, "warp", Trigger.ON_ATTACK, "107da049-af7a-4409-989a-6de23c8fe036"),
            mod(UNDYING, "undying", Trigger.EVERY_TICK, "cb9484d3-6255-4893-a4f2-3ecc375692ee"),
            mod(FIERY, "fiery", Trigger.ON_ATTACK, "6b567fdf-9245-48f5-8314-f93fe5db1427"),
            mod(SICKLY, "sickly", Trigger.ON_ATTACK, "b5718868-9ab0-424c-af1f-8b35e836b46e"),
            mod(VENOMOUS, "venomous", Trigger.ON_ATTACK, "ab9a132e-c619-4c0a-a103-10cbbcfba1a2"),
            mod(VAMPIRIC, "vampiric", Trigger.ON_ATTACK, "3412251e-af81-4c3c-93ba-2e1c33b049ea"),
            mod(INFESTED, "infested", Trigger.ON_HURT, "9c577fbe-ddbc-4ea2-a661-770ea775f43b")};

    private static final AttributeModifier NONE = new AttributeModifier(UUID.fromString("1e645a3d-9115-4807-a61c-705172839f87"),
            "normal", 1.0, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier HEALTH = new AttributeModifier(UUID.fromString("a62bef38-48cc-42a6-ac5e-ef913841c4fd"),
            "Champion health buff", 30.0, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier DAMAGE = new AttributeModifier(UUID.fromString("a340d2db-d881-4c25-ac62-f0ad14cd63b0"),
            "Champion damage buff", 2.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier BOLD_SPEED = new AttributeModifier(UUID.fromString("4b1edd33-caa9-47ae-a702-d86c05701037"),
            "Bold speed boost", 0.3, AttributeModifier.Operation.MULTIPLY_BASE);
    private static final AttributeModifier MIGHTY_DAMAGE = new AttributeModifier(UUID.fromString("7163897f-07f5-49b3-9ce4-b74beb83d2d3"),
            "Mighty damage boost", 3.0, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private Champions() {
    }

    private static Kind mod(int id, String name, Trigger trigger, String uuid) {
        return new Kind(id, name, trigger, new AttributeModifier(UUID.fromString(uuid), name, id + 2, AttributeModifier.Operation.ADDITION));
    }

    /** Номер свойства особого моба; отрицательное число — моб обычный. */
    public static int get(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(CHAMPION_MOD.get());
        return instance == null ? -1 : (int) instance.getValue();
    }

    private static void apply(LivingEntity entity, Attribute attribute, AttributeModifier modifier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(modifier.getId());
            instance.addPermanentModifier(modifier);
        }
    }

    /** Делает монстра особым со случайным свойством. */
    public static void makeChampion(Monster mob, boolean persist) {
        makeChampion(mob, mob instanceof Creeper ? BOLD : mob.getRandom().nextInt(MODS.length), persist);
    }

    public static void makeChampion(Monster mob, int type, boolean persist) {
        AttributeInstance marker = mob.getAttribute(CHAMPION_MOD.get());
        if (marker == null) {
            return;
        }
        for (Kind other : MODS) {
            marker.removeModifier(other.marker().getId());
        }
        marker.removeModifier(NONE.getId());
        marker.addPermanentModifier(MODS[type].marker());
        if (mob instanceof thaumcraft.common.entity.ThaumcraftBoss boss) {
            // у хранителей свойство только в имени, рядом с титулом; здоровья и урона оно не прибавляет
            boss.generateName();
        } else {
            apply(mob, Attributes.MAX_HEALTH, HEALTH);
            apply(mob, Attributes.ATTACK_DAMAGE, DAMAGE);
            mob.heal(25f);
            mob.setCustomName(MODS[type].title().copy().append(" ").append(mob.getType().getDescription()));
        }
        if (persist) {
            mob.setPersistenceRequired();
        }
        switch (type) {
            case BOLD -> apply(mob, Attributes.MOVEMENT_SPEED, BOLD_SPEED);
            case MIGHTY -> apply(mob, Attributes.ATTACK_DAMAGE, MIGHTY_DAMAGE);
            case WARDED -> mob.setAbsorptionAmount(mob.getAbsorptionAmount() + (int) mob.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2);
            default -> {
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        /** Атрибут получают все монстры, как в оригинале все потомки EntityMob. */
        @SubscribeEvent
        public static void addAttribute(EntityAttributeModificationEvent event) {
            for (EntityType<? extends LivingEntity> type : event.getTypes()) {
                if (type.getCategory() == MobCategory.MONSTER && !event.has(type, CHAMPION_MOD.get())) {
                    event.add(type, CHAMPION_MOD.get());
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class Events {
        /**
         * Судьба монстра решается один раз, при первом появлении в мире: шанс 1 из 100, на лёгкой сложности меньше,
         * на трудной и в жутких биомах, Нижнем мире и Крае — больше.
         */
        @SubscribeEvent
        public static void join(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster mob)) {
                return;
            }
            AttributeInstance marker = mob.getAttribute(CHAMPION_MOD.get());
            if (marker == null || marker.getValue() >= -1.0) {
                return;
            }
            RandomSource random = mob.getRandom();
            int c = random.nextInt(100);
            Difficulty difficulty = event.getLevel().getDifficulty();
            if (difficulty == Difficulty.EASY) {
                c += 2;
            }
            if (difficulty == Difficulty.HARD) {
                c -= 2;
            }
            var biome = event.getLevel().getBiome(mob.blockPosition());
            if (biome.is(Tags.Biomes.IS_SPOOKY) || biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_END)) {
                c -= 2;
            }
            net.minecraft.world.level.Level level = event.getLevel();
            if (thaumcraft.common.world.outer.OuterLands.is(level)) {
                // в Запредельных землях особых больше, а в залах скрижали и в библиотеках — почти все (isDangerousLocation)
                c -= 3;
                if (level.getServer() != null) {
                    thaumcraft.common.world.outer.Cell cell = thaumcraft.common.world.outer.Labyrinth.get(level.getServer())
                            .get(mob.getBlockX() >> 4, mob.getBlockZ() >> 4);
                    if (cell != null && (cell.feature == 6 || cell.feature == 8)) {
                        c -= 10;
                    }
                }
            }
            if (mob instanceof thaumcraft.common.entity.ThaumcraftBoss) {
                // вес хранителей в championModWhitelist — 200: особыми они становятся всегда
                c -= 199;
            }
            if (mob.getType().is(WHITELIST) && c <= 0 && mob.getAttributeBaseValue(Attributes.MAX_HEALTH) >= 10.0) {
                makeChampion(mob, false);
            } else {
                marker.removeModifier(NONE.getId());
                marker.addPermanentModifier(NONE);
            }
        }

        /** «Неуязвимый» понемногу наращивает щит, «бессмертный» лечится. */
        @SubscribeEvent
        public static void tick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Monster mob) || mob.level().isClientSide || !mob.isAlive()) {
                return;
            }
            int type = get(mob);
            if (type == WARDED) {
                if (mob.invulnerableTime <= 0 && mob.tickCount % 25 == 0) {
                    int limit = (int) mob.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2;
                    if (mob.getAbsorptionAmount() < limit) {
                        mob.setAbsorptionAmount(mob.getAbsorptionAmount() + 1f);
                    }
                }
            } else if (type == UNDYING && mob.tickCount % 20 == 0) {
                mob.heal(1f);
            }
        }

        @SubscribeEvent
        public static void hurt(LivingHurtEvent event) {
            DamageSource source = event.getSource();
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide) {
                return;
            }
            if (victim instanceof Monster warded && get(warded) == WARDED && warded.getAbsorptionAmount() > 0f) {
                thaumcraft.common.runic.RunicShield.showShield(warded, source);
                warded.level().playSound(null, warded.getX(), warded.getY(), warded.getZ(), ModSounds.RUNIC_SHIELD_EFFECT.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.66f, 1.1f + warded.getRandom().nextFloat() * 0.1f);
            }
            // свойства, срабатывающие, когда бьют самого особого моба
            if (victim instanceof Monster mob && source.getDirectEntity() instanceof LivingEntity attacker) {
                int type = get(mob);
                if (type >= 0 && MODS[type].trigger() == Trigger.ON_HURT) {
                    event.setAmount(whenHurt(type, mob, attacker, source, event.getAmount()));
                }
            }
            // свойства, срабатывающие, когда бьёт он
            if (event.getAmount() > 0f && source.getDirectEntity() instanceof Monster mob) {
                int type = get(mob);
                if (type >= 0 && MODS[type].trigger() == Trigger.ON_ATTACK) {
                    whenAttacking(type, mob, victim, event.getAmount());
                }
            }
        }

        private static float whenHurt(int type, Monster mob, LivingEntity attacker, DamageSource source, float amount) {
            RandomSource random = mob.getRandom();
            switch (type) {
                case SPINED -> {
                    if (!source.is(DamageTypes.THORNS)) {
                        attacker.hurt(mob.damageSources().thorns(mob), 1 + random.nextInt(3));
                        attacker.playSound(SoundEvents.THORNS_HIT, 0.5f, 1f);
                    }
                }
                case ARMORED -> {
                    if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                        return amount * 19f / 25f;
                    }
                }
                case INFESTED -> {
                    if (random.nextFloat() < 0.4f) {
                        thaumcraft.common.entity.TaintSpider spiderling = ModEntities.TAINT_SPIDER.get().create(mob.level());
                        if (spiderling != null) {
                            spiderling.moveTo(mob.getX(), mob.getY() + mob.getBbHeight() / 2f, mob.getZ(), random.nextFloat() * 360f, 0f);
                            mob.level().addFreshEntity(spiderling);
                            mob.playSound(ModSounds.GORE.get(), 0.5f, 1f);
                        }
                    }
                }
                default -> {
                }
            }
            return amount;
        }

        private static void whenAttacking(int type, Monster mob, LivingEntity target, float amount) {
            RandomSource random = mob.getRandom();
            switch (type) {
                case GRIM -> {
                    if (random.nextFloat() < 0.4f) {
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
                    }
                }
                case WARP -> {
                    if (random.nextFloat() < 0.33f && target instanceof ServerPlayer player) {
                        Warp.add(player, 1 + random.nextInt(3), true);
                    }
                }
                case FIERY -> {
                    if (random.nextFloat() < 0.4f) {
                        target.setSecondsOnFire(4);
                    }
                }
                case SICKLY -> {
                    if (random.nextFloat() < 0.4f) {
                        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 500));
                    }
                }
                case VENOMOUS -> {
                    if (random.nextFloat() < 0.4f) {
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                    }
                }
                case VAMPIRIC -> mob.heal(Math.max(2f, amount / 2f));
                default -> {
                }
            }
        }

        /** Особый моб, убитый игроком, оставляет 5–7 очков опыта и мешок с добычей; «Добыча» на оружии повышает его редкость. */
        @SubscribeEvent
        public static void drops(LivingDropsEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide || !event.isRecentlyHit() || event.getSource().getEntity() instanceof FakePlayer
                    || !(entity instanceof Monster) || get(entity) < 0 || !(entity.level() instanceof ServerLevel level)) {
                return;
            }
            RandomSource random = entity.getRandom();
            ExperienceOrb.award(level, entity.position(), 5 + random.nextInt(3));
            int tier = Math.min(2, Mth.floor((random.nextInt(9) + event.getLootingLevel()) / 5f));
            ItemStack bag = new ItemStack((tier == 2 ? ModItems.LOOT_BAG_RARE : tier == 1 ? ModItems.LOOT_BAG_UNCOMMON : ModItems.LOOT_BAG_COMMON).get());
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), bag));
        }
    }
}
