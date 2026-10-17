package thaumcraft.common.research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entity.MindSpider;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Приступы безумия — перенос WarpEvents оригинала. Раз в 2000 тиков игра бросает кости: чем больше «счётчик
 * искажения», тем вероятнее приступ, а чем больше самого искажения, тем он тяжелее — от шёпота, дающего знания,
 * до полчищ пауков, которых видит только сам безумец, и тумана, в котором кто-то есть.
 */
public final class WarpEvents {


    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
    }

    /** Эффект, который не снимается молоком; cures — чем его всё же можно вылечить. */
    private static void afflict(ServerPlayer player, MobEffect effect, int duration, int amplifier, ItemStack... cures) {
        MobEffectInstance instance = new MobEffectInstance(effect, duration, amplifier, true, true);
        instance.setCurativeItems(new ArrayList<>(List.of(cures)));
        player.addEffect(instance);
    }

    public static void check(ServerPlayer player) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        RandomSource random = player.getRandom();
        int warp = knowledge.getWarpTotal();
        int actualWarp = knowledge.getWarpPerm() + knowledge.getWarpSticky();
        warp += getWarpFromGear(player);
        int counter = knowledge.getWarpCounter();
        int roll = random.nextInt(100);
        if (counter > 0 && warp > 0 && roll <= Math.sqrt(counter)) {
            warp = Math.min(100, (warp + warp + counter) / 3);
            counter = (int) (counter - Math.max(5.0, Math.sqrt(counter) * 2.0));
            knowledge.setWarpCounter(counter);
            int eff = random.nextInt(warp);
            // маска «Ухмыляющийся бес» на шлеме крепостной брони смягчает приступ на 2-5
            net.minecraft.world.item.ItemStack helm = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (helm.getItem() instanceof thaumcraft.common.items.armor.FortressArmorItem
                    && thaumcraft.common.items.armor.FortressArmorItem.getMask(helm) == 0) {
                eff -= 2 + random.nextInt(4);
            }
            FxPacket.sendTo(player, FxPacket.WARP_VIGNETTE, Vec3.ZERO, Vec3.ZERO, 0, 0);
            int level = Math.min(3, warp / 15);
            if (eff > 0) {
                if (eff <= 4) {
                    grantResearch(player, 1);
                    say(player, "warp.text.3");
                } else if (eff > 8) {
                    if (eff <= 12) {
                        say(player, "warp.text.11");
                    } else if (eff <= 16) {
                        afflict(player, ModEffects.VIS_EXHAUSTION.get(), 5000, level);
                        say(player, "warp.text.1");
                    } else if (eff <= 20) {
                        afflict(player, ModEffects.THAUMARHIA.get(), Math.min(32000, 10 * warp), 0);
                        say(player, "warp.text.15");
                    } else if (eff <= 24) {
                        afflict(player, ModEffects.UNNATURAL_HUNGER.get(), 5000, level, new ItemStack(Items.ROTTEN_FLESH),
                                new ItemStack(thaumcraft.common.registry.ModItems.ZOMBIE_BRAIN.get()));
                        say(player, "warp.text.2");
                    } else if (eff <= 28) {
                        say(player, "warp.text.12");
                    } else if (eff <= 32) {
                        spawnMist(player, 1);
                    } else if (eff <= 36) {
                        player.addEffect(new MobEffectInstance(ModEffects.BLURRED_VISION.get(), Math.min(32000, 10 * warp), 0, true, true));
                    } else if (eff <= 40) {
                        afflict(player, ModEffects.SUN_SCORNED.get(), 5000, level);
                        say(player, "warp.text.5");
                    } else if (eff <= 44) {
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, level, true, true));
                        say(player, "warp.text.9");
                    } else if (eff <= 48) {
                        afflict(player, ModEffects.INFECTIOUS_VIS_EXHAUSTION.get(), 6000, level);
                        say(player, "warp.text.1");
                    } else if (eff <= 52) {
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, true));
                        say(player, "warp.text.10");
                    } else if (eff <= 56) {
                        afflict(player, ModEffects.DEATH_GAZE.get(), 6000, level);
                        say(player, "warp.text.4");
                    } else if (eff <= 60) {
                        suddenlySpiders(player, warp, false);
                    } else if (eff <= 64) {
                        say(player, "warp.text.13");
                    } else if (eff <= 68) {
                        spawnMist(player, warp / 30);
                    } else if (eff <= 72) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, true));
                    } else if (eff == 76) {
                        // минута ясности: липкое искажение чуть отступает
                        if (knowledge.getWarpSticky() > 0) {
                            Warp.addSticky(player, -1);
                        }
                        say(player, "warp.text.14");
                    } else if (eff <= 80) {
                        afflict(player, ModEffects.UNNATURAL_HUNGER.get(), 6000, level, new ItemStack(Items.ROTTEN_FLESH));
                        say(player, "warp.text.2");
                    } else if (eff <= 84) {
                        grantResearch(player, warp / 10);
                        say(player, "warp.text.3");
                    } else if (eff > 88) {
                        if (eff <= 92) {
                            suddenlySpiders(player, warp, true);
                        } else {
                            spawnMist(player, warp / 15);
                        }
                    }
                }
            }
            // безумие открывает запретные знания
            if (actualWarp > 10 && !knowledge.isResearchComplete("BATHSALTS") && !knowledge.isResearchComplete("@BATHSALTS")) {
                say(player, "warp.text.8");
                knowledge.completeResearch("@BATHSALTS");
            }
            if (actualWarp > 25 && !knowledge.isResearchComplete("ELDRITCHMINOR")) {
                grantResearch(player, 10);
                ResearchHelper.complete(player, "ELDRITCHMINOR");
            }
            if (actualWarp > 50 && !knowledge.isResearchComplete("ELDRITCHMAJOR")) {
                grantResearch(player, 20);
                ResearchHelper.complete(player, "ELDRITCHMAJOR");
            }
        }
        // временное искажение понемногу рассеивается
        knowledge.addWarpTemp(-1);
        SyncKnowledgePacket.send(player);
    }

    /** Туман на две минуты, а в нём — до восьми стражей Элдрича, уже идущих на игрока. */
    private static void spawnMist(ServerPlayer player, int guardians) {
        FxPacket.sendTo(player, FxPacket.MIST, Vec3.ZERO, Vec3.ZERO, 2400, 0);
        for (int a = 0; a < Math.min(8, guardians); a++) {
            spawnGuardian(player);
        }
        say(player, "warp.text.6");
    }

    /** spawnGuardian оригинала: страж встаёт на твёрдую землю в 7–24 блоках от игрока и сразу нападает. */
    private static void spawnGuardian(ServerPlayer player) {
        var level = player.serverLevel();
        var random = player.getRandom();
        thaumcraft.common.entity.EldritchGuardian guardian = thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get().create(level);
        if (guardian == null) {
            return;
        }
        int i = net.minecraft.util.Mth.floor(player.getX());
        int j = net.minecraft.util.Mth.floor(player.getY());
        int k = net.minecraft.util.Mth.floor(player.getZ());
        for (int l = 0; l < 50; l++) {
            int x = i + net.minecraft.util.Mth.randomBetweenInclusive(random, 7, 24) * net.minecraft.util.Mth.randomBetweenInclusive(random, -1, 1);
            int y = j + net.minecraft.util.Mth.randomBetweenInclusive(random, 7, 24) * net.minecraft.util.Mth.randomBetweenInclusive(random, -1, 1);
            int z = k + net.minecraft.util.Mth.randomBetweenInclusive(random, 7, 24) * net.minecraft.util.Mth.randomBetweenInclusive(random, -1, 1);
            net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(x, y - 1, z);
            if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                guardian.setPos(x, y, z);
                if (level.noCollision(guardian) && !level.containsAnyLiquid(guardian.getBoundingBox())) {
                    guardian.setTarget(player);
                    level.addFreshEntity(guardian);
                    return;
                }
            }
        }
    }

    /** Шёпот раскрывает тайны: несколько очков случайных первичных аспектов. */
    private static void grantResearch(ServerPlayer player, int times) {
        List<Aspect> primals = Aspect.primals();
        int amount = 1 + player.getRandom().nextInt(Math.max(1, times));
        thaumcraft.api.aspects.AspectList gained = new thaumcraft.api.aspects.AspectList();
        for (int i = 0; i < amount; i++) {
            gained.add(primals.get(player.getRandom().nextInt(primals.size())), 1);
        }
        ResearchHelper.giveAspects(player, gained);
    }

    /**
     * «Они повсюду!» — вокруг появляются пауки разума, по одному на единицу искажения (до 50).
     *
     * @param real false — видения: видит их только сам игрок, укусить они не могут и через минуту исчезают
     */
    public static void suddenlySpiders(ServerPlayer player, int warp, boolean real) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        int spawns = Math.min(50, warp);
        for (int a = 0; a < spawns; a++) {
            MindSpider spider = ModEntities.MIND_SPIDER.get().create(level);
            if (spider == null) {
                break;
            }
            boolean success = false;
            for (int attempt = 0; attempt < 50; attempt++) {
                BlockPos pos = player.blockPosition().offset(Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1),
                        Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1), Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1));
                BlockState below = level.getBlockState(pos.below());
                if (below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                    spider.setPos(pos.getX(), pos.getY(), pos.getZ());
                    if (level.noCollision(spider) && !level.containsAnyLiquid(spider.getBoundingBox())) {
                        success = true;
                        break;
                    }
                }
            }
            if (success) {
                spider.setTarget(player);
                if (!real) {
                    spider.setViewer(player.getGameProfile().getName());
                    spider.setHarmless(true);
                }
                level.addFreshEntity(spider);
            }
        }
        say(player, "warp.text.7");
    }

    /** Смертельный взгляд: всё живое, на что смотрит игрок, иссыхает и бросается на него. */
    public static void checkDeathGaze(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(ModEffects.DEATH_GAZE.get());
        if (effect == null) {
            return;
        }
        int range = Math.min(8 + effect.getAmplifier() * 3, 24);
        Vec3 look = player.getLookAngle();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || !entity.isPickable() || !player.hasLineOfSight(entity)) {
                continue;
            }
            // в поле зрения: угол между взглядом и направлением на цель меньше 0.75 радиана
            Vec3 to = entity.getEyePosition().subtract(player.getEyePosition());
            if (to.length() > range || Math.acos(Mth.clamp(look.dot(to.normalize()), -1.0, 1.0)) > 0.75) {
                continue;
            }
            living.setLastHurtByMob(player);
            if (living instanceof Mob mob) {
                mob.setTarget(player);
            }
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
        }
    }

    private WarpEvents() {
    }

    /** getWarpFromGear оригинала: искажение от вещи в руке, надетой брони и амулетов. */
    public static int getWarpFromGear(net.minecraft.world.entity.player.Player player) {
        int warp = gearWarp(player.getMainHandItem(), player);
        for (net.minecraft.world.item.ItemStack armor : player.getArmorSlots()) {
            warp += gearWarp(armor, player);
        }
        for (net.minecraft.world.item.ItemStack bauble : thaumcraft.common.compat.Baubles.find(player, stack -> stack.getItem()
                instanceof thaumcraft.api.WarpingGear)) {
            warp += gearWarp(bauble, player);
        }
        return warp;
    }

    public static int gearWarp(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        return stack.getItem() instanceof thaumcraft.api.WarpingGear gear ? gear.getWarp(stack, player) : 0;
    }
}
