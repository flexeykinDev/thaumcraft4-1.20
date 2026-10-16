package thaumcraft.common.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.AlumentumEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Thaumcraft.MODID);

    public static final RegistryObject<EntityType<AlumentumEntity>> ALUMENTUM = ENTITIES.register("alumentum",
            () -> EntityType.Builder.<AlumentumEntity>of(AlumentumEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("alumentum"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.MindSpider>> MIND_SPIDER = ENTITIES.register("mind_spider",
            () -> EntityType.Builder.<thaumcraft.common.entity.MindSpider>of(thaumcraft.common.entity.MindSpider::new, MobCategory.MONSTER)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).build("mind_spider"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.EmberEntity>> EMBER = ENTITIES.register("ember",
            () -> EntityType.Builder.<thaumcraft.common.entity.EmberEntity>of(thaumcraft.common.entity.EmberEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("ember"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.FrostShardEntity>> FROST_SHARD = ENTITIES.register("frost_shard",
            () -> EntityType.Builder.<thaumcraft.common.entity.FrostShardEntity>of(thaumcraft.common.entity.FrostShardEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f).clientTrackingRange(4).updateInterval(2).build("frost_shard"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.PechBlastEntity>> PECH_BLAST = ENTITIES.register("pech_blast",
            () -> EntityType.Builder.<thaumcraft.common.entity.PechBlastEntity>of(thaumcraft.common.entity.PechBlastEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("pech_blast"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.GolemOrb>> GOLEM_ORB = ENTITIES.register("golem_orb",
            () -> EntityType.Builder.<thaumcraft.common.entity.GolemOrb>of(thaumcraft.common.entity.GolemOrb::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(2).build("golem_orb"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.EldritchCrab>> ELDRITCH_CRAB = ENTITIES.register("eldritch_crab",
            () -> EntityType.Builder.<thaumcraft.common.entity.EldritchCrab>of(thaumcraft.common.entity.EldritchCrab::new, MobCategory.MONSTER)
                    .sized(0.8f, 0.6f).clientTrackingRange(8).build("eldritch_crab"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.InhabitedZombie>> INHABITED_ZOMBIE = ENTITIES.register("inhabited_zombie",
            () -> EntityType.Builder.<thaumcraft.common.entity.InhabitedZombie>of(thaumcraft.common.entity.InhabitedZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("inhabited_zombie"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.CultistKnight>> CULTIST_KNIGHT = ENTITIES.register("cultist_knight",
            () -> EntityType.Builder.<thaumcraft.common.entity.CultistKnight>of(thaumcraft.common.entity.CultistKnight::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(8).build("cultist_knight"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.CultistCleric>> CULTIST_CLERIC = ENTITIES.register("cultist_cleric",
            () -> EntityType.Builder.<thaumcraft.common.entity.CultistCleric>of(thaumcraft.common.entity.CultistCleric::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(8).build("cultist_cleric"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.CultistLeader>> CULTIST_LEADER = ENTITIES.register("cultist_leader",
            () -> EntityType.Builder.<thaumcraft.common.entity.CultistLeader>of(thaumcraft.common.entity.CultistLeader::new, MobCategory.MONSTER)
                    .sized(0.75f, 2.25f).clientTrackingRange(10).build("cultist_leader"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.EldritchGuardian>> ELDRITCH_GUARDIAN = ENTITIES.register("eldritch_guardian",
            () -> EntityType.Builder.<thaumcraft.common.entity.EldritchGuardian>of(thaumcraft.common.entity.EldritchGuardian::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.25f).clientTrackingRange(10).build("eldritch_guardian"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.EldritchOrb>> ELDRITCH_ORB = ENTITIES.register("eldritch_orb",
            () -> EntityType.Builder.<thaumcraft.common.entity.EldritchOrb>of(thaumcraft.common.entity.EldritchOrb::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(2).build("eldritch_orb"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.EldritchWarden>> ELDRITCH_WARDEN = ENTITIES.register("eldritch_warden",
            () -> EntityType.Builder.<thaumcraft.common.entity.EldritchWarden>of(thaumcraft.common.entity.EldritchWarden::new, MobCategory.MONSTER)
                    .sized(1.5f, 3.5f).clientTrackingRange(12).build("eldritch_warden"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.EldritchGolem>> ELDRITCH_GOLEM = ENTITIES.register("eldritch_golem",
            () -> EntityType.Builder.<thaumcraft.common.entity.EldritchGolem>of(thaumcraft.common.entity.EldritchGolem::new, MobCategory.MONSTER)
                    .sized(1.75f, 3.5f).clientTrackingRange(12).fireImmune().build("eldritch_golem"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.CultistPortal>> CULTIST_PORTAL = ENTITIES.register("cultist_portal",
            () -> EntityType.Builder.<thaumcraft.common.entity.CultistPortal>of(thaumcraft.common.entity.CultistPortal::new, MobCategory.MONSTER)
                    .sized(1.5f, 3.0f).clientTrackingRange(12).fireImmune().build("cultist_portal"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintacleGiant>> TAINTACLE_GIANT = ENTITIES.register("taintacle_giant",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintacleGiant>of(thaumcraft.common.entity.TaintacleGiant::new, MobCategory.MONSTER)
                    .sized(1.1f, 6.0f).clientTrackingRange(12).build("taintacle_giant"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.PrimalOrbEntity>> PRIMAL_ORB = ENTITIES.register("primal_orb",
            () -> EntityType.Builder.<thaumcraft.common.entity.PrimalOrbEntity>of(thaumcraft.common.entity.PrimalOrbEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(2).build("primal_orb"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.FireBat>> FIRE_BAT = ENTITIES.register("fire_bat",
            () -> EntityType.Builder.<thaumcraft.common.entity.FireBat>of(thaumcraft.common.entity.FireBat::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.9f).fireImmune().clientTrackingRange(8).build("fire_bat"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.FocusOrbEntity>> FOCUS_ORB = ENTITIES.register("focus_orb",
            () -> EntityType.Builder.<thaumcraft.common.entity.FocusOrbEntity>of(thaumcraft.common.entity.FocusOrbEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(2).build("focus_orb"));

    // порождения порчи
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintSpider>> TAINT_SPIDER = ENTITIES.register("taint_spider",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintSpider>of(thaumcraft.common.entity.TaintSpider::new, MobCategory.MONSTER)
                    .sized(0.4f, 0.3f).clientTrackingRange(8).build("taint_spider"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintSpore>> TAINT_SPORE = ENTITIES.register("taint_spore",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintSpore>of(thaumcraft.common.entity.TaintSpore::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).build("taint_spore"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintSporeSwarmer>> TAINT_SPORE_SWARMER =
            ENTITIES.register("taint_spore_swarmer", () -> EntityType.Builder.<thaumcraft.common.entity.TaintSporeSwarmer>of(
                    thaumcraft.common.entity.TaintSporeSwarmer::new, MobCategory.MONSTER).sized(1f, 1f).clientTrackingRange(8)
                    .build("taint_spore_swarmer"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintSwarm>> TAINT_SWARM = ENTITIES.register("taint_swarm",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintSwarm>of(thaumcraft.common.entity.TaintSwarm::new, MobCategory.MONSTER)
                    .sized(2f, 2f).clientTrackingRange(8).build("taint_swarm"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintedBeasts.Cow>> TAINTED_COW = ENTITIES.register("tainted_cow",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintedBeasts.Cow>of(thaumcraft.common.entity.TaintedBeasts.Cow::new, MobCategory.MONSTER)
                    .sized(0.9f, 1.3f).clientTrackingRange(8).build("tainted_cow"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintedBeasts.Pig>> TAINTED_PIG = ENTITIES.register("tainted_pig",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintedBeasts.Pig>of(thaumcraft.common.entity.TaintedBeasts.Pig::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.9f).clientTrackingRange(8).build("tainted_pig"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintedBeasts.Sheep>> TAINTED_SHEEP = ENTITIES.register("tainted_sheep",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintedBeasts.Sheep>of(thaumcraft.common.entity.TaintedBeasts.Sheep::new, MobCategory.MONSTER)
                    .sized(0.9f, 1.3f).clientTrackingRange(8).build("tainted_sheep"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintedBeasts.Chicken>> TAINTED_CHICKEN = ENTITIES.register("tainted_chicken",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintedBeasts.Chicken>of(thaumcraft.common.entity.TaintedBeasts.Chicken::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.8f).clientTrackingRange(8).build("tainted_chicken"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintedBeasts.Villager>> TAINTED_VILLAGER = ENTITIES.register("tainted_villager",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintedBeasts.Villager>of(thaumcraft.common.entity.TaintedBeasts.Villager::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(8).build("tainted_villager"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintCreeper>> TAINTED_CREEPER = ENTITIES.register("tainted_creeper",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintCreeper>of(thaumcraft.common.entity.TaintCreeper::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(8).build("tainted_creeper"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.Taintacle>> TAINTACLE = ENTITIES.register("taintacle",
            () -> EntityType.Builder.<thaumcraft.common.entity.Taintacle>of(thaumcraft.common.entity.Taintacle::new, MobCategory.MONSTER)
                    .sized(0.66f, 3.0f).clientTrackingRange(8).build("taintacle"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.TaintacleSmall>> TAINTACLE_SMALL = ENTITIES.register("taintacle_small",
            () -> EntityType.Builder.<thaumcraft.common.entity.TaintacleSmall>of(thaumcraft.common.entity.TaintacleSmall::new, MobCategory.MONSTER)
                    .sized(0.22f, 1.0f).clientTrackingRange(8).build("taintacle_small"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.ThaumicSlime>> THAUMIC_SLIME = ENTITIES.register("thaumic_slime",
            () -> EntityType.Builder.<thaumcraft.common.entity.ThaumicSlime>of(thaumcraft.common.entity.ThaumicSlime::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).build("thaumic_slime"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.AspectOrbEntity>> ASPECT_ORB = ENTITIES.register("aspect_orb",
            () -> EntityType.Builder.<thaumcraft.common.entity.AspectOrbEntity>of(thaumcraft.common.entity.AspectOrbEntity::new, MobCategory.MISC)
                    .sized(0.125f, 0.125f).clientTrackingRange(8).updateInterval(20).build("aspect_orb"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.BottleTaintEntity>> BOTTLE_TAINT = ENTITIES.register("bottle_taint",
            () -> EntityType.Builder.<thaumcraft.common.entity.BottleTaintEntity>of(thaumcraft.common.entity.BottleTaintEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("bottle_taint"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.BrainyZombie>> BRAINY_ZOMBIE = ENTITIES.register("brainy_zombie",
            () -> EntityType.Builder.<thaumcraft.common.entity.BrainyZombie>of(thaumcraft.common.entity.BrainyZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("brainy_zombie"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.BrainyZombie.Giant>> GIANT_BRAINY_ZOMBIE = ENTITIES.register("giant_brainy_zombie",
            () -> EntityType.Builder.<thaumcraft.common.entity.BrainyZombie.Giant>of(thaumcraft.common.entity.BrainyZombie.Giant::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("giant_brainy_zombie"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.Wisp>> WISP = ENTITIES.register("wisp",
            () -> EntityType.Builder.<thaumcraft.common.entity.Wisp>of(thaumcraft.common.entity.Wisp::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.9f).clientTrackingRange(8).build("wisp"));
    public static final RegistryObject<EntityType<thaumcraft.common.entity.Pech>> PECH = ENTITIES.register("pech",
            () -> EntityType.Builder.<thaumcraft.common.entity.Pech>of(thaumcraft.common.entity.Pech::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(8).build("pech"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.golem.Golem>> GOLEM = ENTITIES.register("golem",
            () -> EntityType.Builder.<thaumcraft.common.entity.golem.Golem>of(thaumcraft.common.entity.golem.Golem::new, MobCategory.MISC)
                    .sized(0.4f, 0.95f).clientTrackingRange(8).build("golem"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.golem.GolemBobber>> GOLEM_BOBBER = ENTITIES.register("golem_bobber",
            () -> EntityType.Builder.<thaumcraft.common.entity.golem.GolemBobber>of(thaumcraft.common.entity.golem.GolemBobber::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(5).build("golem_bobber"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.PrimalArrow>> PRIMAL_ARROW = ENTITIES.register("primal_arrow",
            () -> EntityType.Builder.<thaumcraft.common.entity.PrimalArrow>of(thaumcraft.common.entity.PrimalArrow::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(20).build("primal_arrow"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.FollowingItem>> FOLLOWING_ITEM = ENTITIES.register("following_item",
            () -> EntityType.Builder.<thaumcraft.common.entity.FollowingItem>of(thaumcraft.common.entity.FollowingItem::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(20).build("following_item"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.golem.GolemDart>> GOLEM_DART = ENTITIES.register("golem_dart",
            () -> EntityType.Builder.<thaumcraft.common.entity.golem.GolemDart>of(thaumcraft.common.entity.golem.GolemDart::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(20).build("golem_dart"));

    public static final RegistryObject<EntityType<thaumcraft.common.entity.golem.TravelingTrunk>> TRAVELING_TRUNK =
            ENTITIES.register("traveling_trunk", () -> EntityType.Builder.<thaumcraft.common.entity.golem.TravelingTrunk>of(
                    thaumcraft.common.entity.golem.TravelingTrunk::new, MobCategory.MISC).sized(0.8f, 0.8f).fireImmune()
                    .clientTrackingRange(8).build("traveling_trunk"));

    private ModEntities() {
    }
}
