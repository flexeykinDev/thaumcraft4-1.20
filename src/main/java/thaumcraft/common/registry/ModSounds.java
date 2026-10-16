package thaumcraft.common.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Thaumcraft.MODID);

    public static final RegistryObject<SoundEvent> CAMERA_TICKS = register("cameraticks");
    public static final RegistryObject<SoundEvent> LEARN = register("learn");
    public static final RegistryObject<SoundEvent> PAGE = register("page");
    public static final RegistryObject<SoundEvent> CAMERA_CLACK = register("cameraclack");

    public static final RegistryObject<SoundEvent> ALEMBIC_KNOCK = register("alembicknock");
    public static final RegistryObject<SoundEvent> BUBBLE = register("bubble");
    public static final RegistryObject<SoundEvent> CRAFT_FAIL = register("craftfail");
    public static final RegistryObject<SoundEvent> URN_BREAK = register("urnbreak");
    public static final RegistryObject<SoundEvent> CRAB_CLAW = register("crabclaw");
    public static final RegistryObject<SoundEvent> CRAB_DEATH = register("crabdeath");
    public static final RegistryObject<SoundEvent> CRAB_TALK = register("crabtalk");
    public static final RegistryObject<SoundEvent> CHANT = register("chant");
    public static final RegistryObject<SoundEvent> EG_IDLE = register("egidle");
    public static final RegistryObject<SoundEvent> EG_ATTACK = register("egattack");
    public static final RegistryObject<SoundEvent> EG_DEATH = register("egdeath");
    public static final RegistryObject<SoundEvent> EG_SCREECH = register("egscreech");
    public static final RegistryObject<SoundEvent> EVIL_PORTAL = register("evilportal");
    public static final RegistryObject<SoundEvent> MONOLITH = register("monolith");
    public static final RegistryObject<SoundEvent> WHISPERS = register("whispers");
    public static final RegistryObject<SoundEvent> CREAK = register("creak");
    public static final RegistryObject<SoundEvent> PUMP = register("pump");
    public static final RegistryObject<SoundEvent> SQUEEK = register("squeek");
    public static final RegistryObject<SoundEvent> TOOL = register("tool");
    public static final RegistryObject<SoundEvent> CRAFT_START = register("craftstart");
    public static final RegistryObject<SoundEvent> INFUSER = register("infuser");
    public static final RegistryObject<SoundEvent> INFUSER_START = register("infuserstart");
    public static final RegistryObject<SoundEvent> SPILL = register("spill");
    public static final RegistryObject<SoundEvent> WAND = register("wand");
    public static final RegistryObject<SoundEvent> HEARTBEAT = register("heartbeat");
    public static final RegistryObject<SoundEvent> ZAP = register("zap");
    public static final RegistryObject<SoundEvent> FIRE_LOOP = register("fireloop");
    public static final RegistryObject<SoundEvent> SHOCK = register("shock");
    public static final RegistryObject<SoundEvent> ICE = register("ice");
    public static final RegistryObject<SoundEvent> RUMBLE = register("rumble");
    public static final RegistryObject<SoundEvent> JACOBS = register("jacobs");
    public static final RegistryObject<SoundEvent> CRYSTAL = register("crystal");
    public static final RegistryObject<SoundEvent> GORE = register("gore");
    public static final RegistryObject<SoundEvent> ROOTS = register("roots");
    public static final RegistryObject<SoundEvent> PECH_IDLE = register("pech_idle");
    public static final RegistryObject<SoundEvent> PECH_TRADE = register("pech_trade");
    public static final RegistryObject<SoundEvent> PECH_DICE = register("pech_dice");
    public static final RegistryObject<SoundEvent> PECH_HIT = register("pech_hit");
    public static final RegistryObject<SoundEvent> PECH_DEATH = register("pech_death");
    public static final RegistryObject<SoundEvent> PECH_CHARGE = register("pech_charge");
    public static final RegistryObject<SoundEvent> WISP_LIVE = register("wisplive");
    public static final RegistryObject<SoundEvent> WISP_DEAD = register("wispdead");
    public static final RegistryObject<SoundEvent> TENTACLE = register("tentacle");
    public static final RegistryObject<SoundEvent> SWARM = register("swarm");
    public static final RegistryObject<SoundEvent> SWARM_ATTACK = register("swarmattack");
    public static final RegistryObject<SoundEvent> FLY = register("fly");
    public static final RegistryObject<SoundEvent> RUNIC_SHIELD_EFFECT = register("runicshieldeffect");
    public static final RegistryObject<SoundEvent> RUNIC_SHIELD_CHARGE = register("runicshieldcharge");
    public static final RegistryObject<SoundEvent> COINS = register("coins");
    public static final RegistryObject<SoundEvent> WAND_FAIL = register("wandfail");
    public static final RegistryObject<SoundEvent> JAR = register("jar");
    public static final RegistryObject<SoundEvent> WRITE = register("write");
    public static final RegistryObject<SoundEvent> ERASE = register("erase");
    public static final RegistryObject<SoundEvent> KEY = register("key");
    public static final RegistryObject<SoundEvent> COMBINE = register("hhon");
    public static final RegistryObject<SoundEvent> UPGRADE = register("upgrade");
    public static final RegistryObject<SoundEvent> GOLEM_IRON_SHOOT = register("golemironshoot");
    public static final RegistryObject<SoundEvent> HHOFF = register("hhoff");
    public static final RegistryObject<SoundEvent> WIND = register("wind");
    public static final RegistryObject<SoundEvent> BRAIN = register("brain");
    public static final RegistryObject<SoundEvent> DOOR_FAIL = register("doorfail");
    public static final RegistryObject<SoundEvent> SWING = register("swing");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Thaumcraft.id(name)));
    }

    private ModSounds() {
    }
}
