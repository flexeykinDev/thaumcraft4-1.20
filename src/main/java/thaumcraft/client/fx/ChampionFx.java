package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.champion.Champions;

/** Частицы вокруг особых мобов — перенос showFX всех тринадцати ChampionMod оригинала: у каждого свойства свои. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ChampionFx {
    private ChampionFx() {
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Monster mob && mob.level().isClientSide && mob.isAlive()) {
            int type = Champions.get(mob);
            if (type >= 0 && !Minecraft.getInstance().isPaused()) {
                show(mob, type);
            }
        }
    }

    /** Частица в случайном месте внутри моба. */
    private static void generic(LivingEntity mob, float heightPart, double dy, float r, float g, float b, float alpha, boolean loop,
                                int start, int count, int age, float scale) {
        RandomSource random = mob.getRandom();
        AABB box = mob.getBoundingBox();
        Fx.generic(box.minX + random.nextFloat() * mob.getBbWidth(), box.minY + random.nextFloat() * mob.getBbHeight() * heightPart,
                box.minZ + random.nextFloat() * mob.getBbWidth(), 0, dy, 0, r, g, b, alpha, loop, start, count, 1, age, 0, scale);
    }

    private static void show(LivingEntity mob, int type) {
        RandomSource random = mob.getRandom();
        switch (type) {
            case Champions.BOLD -> {
                if (random.nextBoolean()) {
                    AABB box = mob.getBoundingBox();
                    Fx.spark(box.minX + random.nextFloat() * mob.getBbWidth(), box.minY + random.nextFloat() * mob.getBbHeight() / 3f,
                            box.minZ + random.nextFloat() * mob.getBbWidth(), 0.2f, 0.3f - random.nextFloat() * 0.1f, 0f,
                            0.8f + random.nextFloat() * 0.2f, 1f);
                }
            }
            case Champions.SPINED -> {
                if (random.nextBoolean()) {
                    generic(mob, 1f, 0, 0.5f + random.nextFloat() * 0.2f, 0.1f + random.nextFloat() * 0.2f, 0.1f + random.nextFloat() * 0.2f,
                            0.7f, false, 176 + random.nextInt(4) * 3, 3, 3, 1.2f + random.nextFloat() * 0.3f);
                }
            }
            case Champions.ARMORED -> {
                if (random.nextInt(4) == 0) {
                    generic(mob, 1f, 0, 0.9f, 0.9f, 0.9f + random.nextFloat() * 0.1f, 0.7f, false, 112, 9, 5 + random.nextInt(4),
                            0.6f + random.nextFloat() * 0.2f);
                }
            }
            case Champions.MIGHTY -> {
                if (random.nextFloat() <= 0.3f) {
                    generic(mob, 1f, 0, 0.8f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f,
                            0.7f, false, 176 + random.nextInt(4) * 3, 3, 4 + random.nextInt(3), 1f + random.nextFloat() * 0.3f);
                }
            }
            case Champions.GRIM -> {
                if (random.nextBoolean()) {
                    generic(mob, 1f, -0.02, random.nextFloat() * 0.2f, random.nextFloat() * 0.2f, random.nextFloat() * 0.2f, 0.8f, false,
                            160, 10, 8 + random.nextInt(4), 0.6f + random.nextFloat() * 0.4f);
                }
            }
            case Champions.WARDED -> {
                if (random.nextBoolean()) {
                    generic(mob, 1f, 0, 0.5f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.1f,
                            0.6f, true, 21, 4, 4 + random.nextInt(4), 0.8f + random.nextFloat() * 0.3f);
                }
            }
            case Champions.WARP -> {
                if (random.nextBoolean()) {
                    generic(mob, 1f, 0, 0.8f + random.nextFloat() * 0.2f, 0f, 0.9f + random.nextFloat() * 0.1f, 0.7f, true, 72, 8,
                            10 + random.nextInt(4), 0.6f + random.nextFloat() * 0.4f);
                }
            }
            case Champions.UNDYING -> {
                if (random.nextBoolean()) {
                    generic(mob, 1f, 0.03, 0.1f + random.nextFloat() * 0.1f, 0.8f + random.nextFloat() * 0.2f, 0.1f + random.nextFloat() * 0.1f,
                            0.9f, true, 21, 4, 4 + random.nextInt(4), 0.5f + random.nextFloat() * 0.2f);
                }
            }
            case Champions.FIERY -> generic(mob, 1f, 0.03, 0.9f + random.nextFloat() * 0.1f, 1f, 1f, 0.7f, false, 160, 10,
                    8 + random.nextInt(4), 0.7f + random.nextFloat() * 0.2f);
            case Champions.SICKLY -> {
                if (random.nextBoolean()) {
                    RandomSource r = random;
                    AABB box = mob.getBoundingBox();
                    Fx.generic(box.minX + r.nextFloat() * mob.getBbWidth(), box.minY + r.nextFloat() * mob.getBbHeight(),
                            box.minZ + r.nextFloat() * mob.getBbWidth(), 0, -0.02, 0, 0.2f, 0.6f + r.nextFloat() * 0.1f,
                            0.2f + r.nextFloat() * 0.1f, 0.5f, false, 1, 4, 2, 5 + r.nextInt(4), 0, 0.9f + r.nextFloat() * 0.3f);
                }
            }
            case Champions.VENOMOUS -> {
                if (random.nextBoolean()) {
                    generic(mob, 1f, 0.02, 0.2f, 0.6f + random.nextFloat() * 0.1f, 0.2f + random.nextFloat() * 0.1f, 0.7f, false, 147, 4,
                            8 + random.nextInt(4), 0.5f + random.nextFloat() * 0.2f);
                }
            }
            case Champions.VAMPIRIC -> {
                if (random.nextFloat() <= 0.2f) {
                    generic(mob, 1f, 0, 0.9f + random.nextFloat() * 0.1f, 0f, 0f, 0.9f, false, 147, 4, 8 + random.nextInt(4),
                            0.5f + random.nextFloat() * 0.2f);
                }
            }
            case Champions.INFESTED -> {
                if (random.nextBoolean()) {
                    // капли слизи, срывающиеся с моба
                    mob.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)), mob.getX(),
                            mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(), (random.nextFloat() - 0.5f) * 0.1, 0.05,
                            (random.nextFloat() - 0.5f) * 0.1);
                }
            }
            default -> {
            }
        }
    }
}
