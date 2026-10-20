package thaumictinkerer.common.potion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.common.network.FxPacket;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.registry.TTBlocks;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Силы наполненных зелий (ModPotions + PotionEffectHandler): с аером удар подбрасывает врага, с игнисом поджигает его на пять
 * минут, с террой между игроком и врагом встаёт силовая стена, с аквой лава вокруг застывает обсидианом.
 */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class TTEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ThaumicTinkerer.MODID);

    public static final RegistryObject<MobEffect> AIR = EFFECTS.register("air_imbued", () -> new Imbued(0xffff7e));
    public static final RegistryObject<MobEffect> FIRE = EFFECTS.register("fire_imbued", () -> new Imbued(0xff5a01));
    public static final RegistryObject<MobEffect> EARTH = EFFECTS.register("earth_imbued", () -> new Imbued(0x56c000));
    public static final RegistryObject<MobEffect> WATER = EFFECTS.register("water_imbued", () -> new Imbued(0x3cd4fc));

    /** Кого и когда ударили: подброс длится секунду, поджог — пять минут. */
    private static final Map<Entity, Long> AIR_HIT = new WeakHashMap<>();
    private static final Map<Entity, Long> FIRE_HIT = new WeakHashMap<>();

    private TTEffects() {
    }

    private static class Imbued extends MobEffect {
        Imbued(int color) {
            super(MobEffectCategory.BENEFICIAL, color);
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Entity target = event.getEntity();
        long now = target.level().getGameTime();
        if (player.hasEffect(AIR.get())) {
            AIR_HIT.put(target, now);
        }
        if (player.hasEffect(FIRE.get())) {
            FIRE_HIT.put(target, now);
        }
        if (player.hasEffect(EARTH.get())) {
            wall(player, target);
        }
    }

    /** Стена 5×5 из силового поля посередине между игроком и врагом, поперёк линии удара. */
    private static void wall(Player player, Entity target) {
        Level level = player.level();
        boolean alongX = Math.abs(target.getZ() - player.getZ()) < Math.abs(target.getX() - player.getX());
        int x = Mth.floor((target.getX() + player.getX()) / 2);
        int y = Mth.floor(player.getY() + 2);
        int z = Mth.floor((target.getZ() + player.getZ()) / 2);
        for (int dy = -2; dy < 3; dy++) {
            for (int side = -2; side < 3; side++) {
                BlockPos pos = alongX ? new BlockPos(x, y + dy, z + side) : new BlockPos(x + side, y + dy, z);
                if (level.isEmptyBlock(pos)) {
                    level.setBlock(pos, TTBlocks.FORCEFIELD.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide || !player.hasEffect(WATER.get())) {
            return;
        }
        BlockPos from = BlockPos.containing(player.getX() - 2, player.getY() - 2, player.getZ() - 2);
        for (BlockPos pos : BlockPos.betweenClosed(from, from.offset(3, 3, 3))) {
            if (player.level().getBlockState(pos).is(Blocks.LAVA)) {
                player.level().setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
                FxPacket.send((ServerLevel) player.level(), FxPacket.BURST, Vec3.atCenterOf(pos), Vec3.atCenterOf(pos), 120, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Iterator<Map.Entry<Entity, Long>> it = AIR_HIT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Entity, Long> hit = it.next();
            Entity entity = hit.getKey();
            long now = entity.level().getGameTime();
            if (entity.isAlive() && now % 5 == 0) {
                RandomSource random = entity.level().random;
                entity.setDeltaMovement(random.nextFloat() - 0.5, random.nextFloat(), random.nextFloat() - 0.5);
                entity.hurtMarked = true;
                FxPacket.send((ServerLevel) entity.level(), FxPacket.BURST, entity.position(), entity.position(), 50, 0);
            }
            if (!entity.isAlive() || now > hit.getValue() + 20) {
                it.remove();
            }
        }
        for (Iterator<Map.Entry<Entity, Long>> it = FIRE_HIT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Entity, Long> hit = it.next();
            Entity entity = hit.getKey();
            long now = entity.level().getGameTime();
            if (entity.isAlive() && now % 5 == 0) {
                entity.setSecondsOnFire(6);
                ((ServerLevel) entity.level()).sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1, entity.getZ(), 30,
                        1.2, 1.2, 1.2, 0);
            }
            if (!entity.isAlive() || now > hit.getValue() + 6000) {
                it.remove();
            }
        }
    }
}
