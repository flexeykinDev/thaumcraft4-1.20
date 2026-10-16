package thaumcraft.common.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Thaumcraft.MODID);

    /**
     * Флюсовая лихорадка (PotionVisExhaust оригинала): каждый уровень удорожает любую трату вис жезлом на 10%.
     * Сам эффект ничего не делает — его учитывает WandItem.getConsumptionModifier.
     */
    public static final RegistryObject<MobEffect> VIS_EXHAUSTION = EFFECTS.register("vis_exhaustion",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x664477) {
            });

    /**
     * Порченый яд (PotionFluxTaint оригинала): раз в 40 тиков (вдвое чаще с каждым уровнем) отнимает единицу
     * здоровья. В отличие от обычного яда может убить; нежить к нему невосприимчива.
     */
    public static final RegistryObject<MobEffect> TAINT_POISON = EFFECTS.register("taint_poison",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x663377) {
                @Override
                public void applyEffectTick(net.minecraft.world.entity.LivingEntity target, int amplifier) {
                    // порождения порчи этот яд лечит
                    if (target instanceof thaumcraft.api.entities.TaintedMob) {
                        target.heal(1f);
                    } else if (!target.isInvertedHealAndHarm()) {
                        target.hurt(target.damageSources().magic(), 1f);
                    }
                }

                @Override
                public boolean isDurationEffectTick(int duration, int amplifier) {
                    int interval = 40 >> amplifier;
                    return interval <= 0 || duration % interval == 0;
                }
            });

    /** Таумархия (PotionThaumarhia): раз в секунду с шансом 1 из 15 под ногами появляется флюсовая жижа. */
    public static final RegistryObject<MobEffect> THAUMARHIA = EFFECTS.register("thaumarhia",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x664477) {
                @Override
                public void applyEffectTick(net.minecraft.world.entity.LivingEntity target, int amplifier) {
                    net.minecraft.world.level.Level level = target.level();
                    if (!level.isClientSide && level.random.nextInt(15) == 0 && level.isEmptyBlock(target.blockPosition())) {
                        level.setBlock(target.blockPosition(), ModBlocks.FLUX_GOO.get().defaultBlockState(), 3);
                    }
                }

                @Override
                public boolean isDurationEffectTick(int duration, int amplifier) {
                    return duration % 20 == 0;
                }
            });

    /** Неестественный голод (PotionUnnaturalHunger): сытость тает на глазах, обычная еда не помогает. */
    public static final RegistryObject<MobEffect> UNNATURAL_HUNGER = EFFECTS.register("unnatural_hunger",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x446633) {
                @Override
                public void applyEffectTick(net.minecraft.world.entity.LivingEntity target, int amplifier) {
                    if (!target.level().isClientSide && target instanceof net.minecraft.world.entity.player.Player player) {
                        player.causeFoodExhaustion(0.025f * (amplifier + 1));
                    }
                }

                @Override
                public boolean isDurationEffectTick(int duration, int amplifier) {
                    return true;
                }
            });

    /** Помутнение зрения (PotionBlurredVision): мир размыт; рисует ClientWarp. */
    public static final RegistryObject<MobEffect> BLURRED_VISION = EFFECTS.register("blurred_vision",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x808080) {
            });

    /** Отвержение солнцем (PotionSunScorned): на свету под открытым небом загораешься, в темноте лечишься. */
    public static final RegistryObject<MobEffect> SUN_SCORNED = EFFECTS.register("sun_scorned",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xf8d86a) {
                @Override
                public void applyEffectTick(net.minecraft.world.entity.LivingEntity target, int amplifier) {
                    net.minecraft.world.level.Level level = target.level();
                    if (level.isClientSide) {
                        return;
                    }
                    float light = target.getLightLevelDependentMagicValue();
                    if (light > 0.5f && level.random.nextFloat() * 30f < (light - 0.4f) * 2f && level.canSeeSky(target.blockPosition())) {
                        target.setSecondsOnFire(4);
                    } else if (light < 0.25f && level.random.nextFloat() > light * 2f) {
                        target.heal(1f);
                    }
                }

                @Override
                public boolean isDurationEffectTick(int duration, int amplifier) {
                    return duration % 40 == 0;
                }
            });

    /** Флюсовая зараза (PotionInfectiousVisExhaust): та же лихорадка, но перекидывается на всех в четырёх блоках, слабея на уровень. */
    public static final RegistryObject<MobEffect> INFECTIOUS_VIS_EXHAUSTION = EFFECTS.register("infectious_vis_exhaustion",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x665577) {
                @Override
                public void applyEffectTick(net.minecraft.world.entity.LivingEntity target, int amplifier) {
                    if (target.level().isClientSide) {
                        return;
                    }
                    for (net.minecraft.world.entity.LivingEntity near : target.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class, target.getBoundingBox().inflate(4))) {
                        if (!near.hasEffect(this)) {
                            near.addEffect(amplifier > 0
                                    ? new net.minecraft.world.effect.MobEffectInstance(this, 6000, amplifier - 1)
                                    : new net.minecraft.world.effect.MobEffectInstance(VIS_EXHAUSTION.get(), 6000, 0));
                        }
                    }
                }

                @Override
                public boolean isDurationEffectTick(int duration, int amplifier) {
                    return duration % 40 == 0;
                }
            });

    /** Смертельный взгляд (PotionDeathGaze): всё, на что смотришь, иссыхает и нападает; считает WarpEvents.checkDeathGaze. */
    public static final RegistryObject<MobEffect> DEATH_GAZE = EFFECTS.register("death_gaze",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x664433) {
            });

    /** Защита от искажения (PotionWarpWard): пока действует, приступов безумия не бывает. */
    public static final RegistryObject<MobEffect> WARP_WARD = EFFECTS.register("warp_ward",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xe0f2f7) {
            });

    private ModEffects() {
    }
}
