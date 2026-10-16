package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * AIFish оригинала: голем закидывает поплавок в воду рядом с домом и ждёт. Чем просторнее и глубже водоём и чем
 * сильнее голем, тем быстрее клюёт. Улучшения: воздух — шанс на двойной улов, огонь — рыба сразу жареная,
 * порядок — больше сокровищ, энтропия — меньше хлама.
 */
public class GolemFishGoal extends Goal {
    private final Golem golem;
    private final Level level;
    private float quality;
    private int count;
    @Nullable
    private BlockPos target;
    @Nullable
    private GolemBobber bobber;

    public GolemFishGoal(Golem golem) {
        this.golem = golem;
        this.level = golem.level();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean water(BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private boolean openWater(BlockPos pos) {
        return water(pos) && level.isEmptyBlock(pos.above());
    }

    @Override
    public boolean canUse() {
        if (target != null || count > 0 || golem.tickCount % GolemHelper.DELAY != 0 || !golem.getNavigation().isDone()) {
            return false;
        }
        if (bobber != null) {
            bobber.discard();
        }
        BlockPos found = findWater();
        if (found == null) {
            return false;
        }
        target = found;
        quality = 0f;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = found.relative(dir);
            if (openWater(side)) {
                quality += 3.0E-5f;
                if (level.canSeeSky(side.above())) {
                    quality += 3.0E-5f;
                }
                for (int depth = 1; depth <= 3; depth++) {
                    if (water(side.below(depth))) {
                        quality += 1.5E-5f;
                    }
                }
            }
        }
        golem.playSound(SoundEvents.FISHING_BOBBER_THROW, 0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        bobber = new GolemBobber(level, golem, found);
        return level.addFreshEntity(bobber);
    }

    @Override
    public boolean canContinueToUse() {
        return bobber != null && bobber.isAlive() && target != null && count-- > 0;
    }

    @Override
    public void start() {
        count = 300 + level.random.nextInt(200);
        golem.startRightArmTimer();
    }

    @Override
    public void stop() {
        if (bobber != null) {
            bobber.discard();
        }
        target = null;
        count = -1;
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
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 30f, 30f);
        float chance = quality + golem.getGolemStrength() * 1.5E-4f;
        if (level.random.nextFloat() >= chance) {
            return;
        }
        golem.startRightArmTimer();
        int catches = 1;
        int air = golem.getUpgradeAmount(Golem.UPGRADE_AIR);
        if (air > 0 && level.random.nextInt(10) < air) {
            catches++;
        }
        boolean fire = golem.getUpgradeAmount(Golem.UPGRADE_FIRE) > 0;
        for (int i = 0; i < catches; i++) {
            ItemStack fish = fishingResult();
            if (fire) {
                ItemStack source = fish;
                fish = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(source), level)
                        .map(recipe -> recipe.getResultItem(level.registryAccess()).copy()).filter(cooked -> !cooked.isEmpty()).orElse(source);
            }
            ItemEntity item = new ItemEntity(level, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, fish);
            if (fire) {
                item.setSecondsOnFire(2);
            }
            item.setPickUpDelay(20);
            // улов летит к голему, как рыба с обычной удочки
            double dx = golem.getX() + level.random.nextFloat() - level.random.nextFloat() - target.getX() + 0.5;
            double dy = golem.getY() - target.getY() + 1.0;
            double dz = golem.getZ() + level.random.nextFloat() - level.random.nextFloat() - target.getZ() + 0.5;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(distance) * 0.08, dz * 0.1);
            level.addFreshEntity(item);
        }
        if (bobber != null) {
            bobber.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.15f, 1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.4f);
            ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, bobber.getX(), bobber.getY() + 0.5, bobber.getZ(),
                    20 + level.random.nextInt(20), 0.1, 0.0, 0.1, 0.0);
            bobber.discard();
        }
        target = null;
    }

    /** Случайная открытая вода в пределах половины дальности голема от дома. */
    @Nullable
    private BlockPos findWater() {
        float distance = (float) Math.ceil(golem.getRange() / 2f);
        BlockPos home = golem.getHome();
        var random = golem.getRandom();
        for (int attempt = 0; attempt < distance * 2f; attempt++) {
            int x = (int) (home.getX() + random.nextInt((int) (1f + distance * 2f)) - distance);
            int y = (int) (home.getY() + random.nextInt((int) (1f + distance)) - distance / 2f);
            int z = (int) (home.getZ() + random.nextInt((int) (1f + distance * 2f)) - distance);
            BlockPos pos = new BlockPos(x, y, z);
            if (openWater(pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Что попалось: хлам, сокровище или рыба — с теми же шансами, что в оригинале, но сами предметы берутся
     * из нынешних таблиц рыбалки, так что добычу можно менять наборами данных.
     */
    public ItemStack catchAt(BlockPos water) {
        target = water;
        ItemStack caught = fishingResult();
        target = null;
        return caught;
    }

    private ItemStack fishingResult() {
        float roll = level.random.nextFloat();
        float junk = 0.1f - golem.getUpgradeAmount(Golem.UPGRADE_ENTROPY) * 0.025f;
        float treasure = 0.05f + golem.getUpgradeAmount(Golem.UPGRADE_ORDER) * 0.0125f;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = target.relative(dir);
            if (openWater(side)) {
                junk -= 0.005f;
                treasure += 0.00125f;
                if (level.canSeeSky(side.above())) {
                    junk -= 0.005f;
                    treasure += 0.00125f;
                }
                for (int depth = 1; depth <= 3; depth++) {
                    if (water(side.below(depth))) {
                        treasure += 0.001f;
                    }
                }
            }
        }
        junk = Mth.clamp(junk, 0f, 1f);
        treasure = Mth.clamp(treasure, 0f, 1f);
        ResourceLocation table = roll < junk ? BuiltInLootTables.FISHING_JUNK
                : roll - junk < treasure ? BuiltInLootTables.FISHING_TREASURE : BuiltInLootTables.FISHING_FISH;
        LootParams params = new LootParams.Builder((ServerLevel) level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(target))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).withParameter(LootContextParams.THIS_ENTITY, golem)
                .create(LootContextParamSets.FISHING);
        List<ItemStack> loot = level.getServer().getLootData().getLootTable(table).getRandomItems(params);
        return loot.isEmpty() ? new ItemStack(Items.COD) : loot.get(0);
    }
}
