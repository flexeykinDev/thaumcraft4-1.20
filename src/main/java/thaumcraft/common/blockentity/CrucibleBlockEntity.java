package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.blocks.FluxBlock;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.List;

/**
 * Тигель — перенос TileCrucible оригинала. Вода греется от огня или лавы под тиглем; в кипящей воде брошенные
 * предметы растворяются в аспекты либо, если аспектов хватает на рецепт, превращаются в его результат.
 */
public class CrucibleBlockEntity extends BlockEntity implements thaumcraft.api.aspects.AspectContainer {
    public static final int CAPACITY = 1000;
    /** Сколько аспектов помещается без переливания. */
    public static final int MAX_ASPECTS = 100;
    /** Вода кипит, когда нагрев выше этого значения. */
    public static final int BOILING = 150;
    private static final int MAX_HEAT = 200;
    /** Метка на выброшенном результате, чтобы тигель не растворил его обратно. */
    public static final String OUTPUT_TAG = "thaumcraft:crucible_output";

    private int heat;
    private int water;
    private AspectList aspects = new AspectList();
    private long counter = -100L;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    public int getWater() {
        return water;
    }

    @Override
    public AspectList getContainedAspects() {
        return getAspects();
    }

    public AspectList getAspects() {
        return aspects;
    }

    public boolean isBoiling() {
        return heat > BOILING && water > 0;
    }

    /** Сумма всех растворённых аспектов. */
    public int aspectTotal() {
        return aspects.visSize();
    }

    /** Высота поверхности жидкости от дна блока: растёт и от воды, и от растворённых аспектов. */
    public float getFluidHeight() {
        float base = 0.3f + 0.5f * water / CAPACITY;
        float out = base + aspectTotal() / (float) MAX_ASPECTS * (1f - base);
        return Math.min(out, 1.001f);
    }

    /** @return сколько воды удалось долить */
    public int fill(int amount) {
        int added = Math.min(amount, CAPACITY - water);
        if (added > 0) {
            water += added;
            sync();
        }
        return added;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // ---------- работа ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        crucible.counter++;
        int previous = crucible.heat;
        if (crucible.water <= 0) {
            if (crucible.heat > 0) {
                crucible.heat--;
            }
        } else if (isHeatSource(level.getBlockState(pos.below()))) {
            if (crucible.heat < MAX_HEAT) {
                // каждые мехи сбоку добавляют ещё по 2 к нагреву за тик
                crucible.heat = Math.min(MAX_HEAT, crucible.heat + 1
                        + thaumcraft.common.blocks.BellowsBlock.count(level, pos, net.minecraft.core.Direction.Plane.HORIZONTAL) * 2);
            }
        } else if (crucible.heat > 0) {
            crucible.heat--;
        }
        if (previous > BOILING != crucible.heat > BOILING) {
            crucible.sync();
        }

        RandomSource random = level.random;
        // перелив: лишнее по одному аспекту выплёскивается наружу
        if (crucible.aspectTotal() > MAX_ASPECTS && crucible.counter % 5L == 0L) {
            List<Aspect> present = crucible.aspects.getAspects();
            crucible.aspects.reduce(present.get(random.nextInt(present.size())), 1);
            crucible.spill();
            crucible.sync();
        }

        // выкипание: раз в пять секунд уходит немного воды, а один аспект распадается на составную часть
        if (crucible.counter > 100L && crucible.heat > BOILING) {
            crucible.counter = 0L;
            if (crucible.aspectTotal() > 0) {
                List<Aspect> present = crucible.aspects.getAspects();
                Aspect aspect = present.get(random.nextInt(present.size()));
                if (aspect.isPrimal()) {
                    // первичные выбираются реже: им даётся вторая попытка уступить место составному
                    aspect = present.get(random.nextInt(present.size()));
                }
                crucible.water = Math.max(0, crucible.water - 2);
                crucible.aspects.reduce(aspect, 1);
                if (!aspect.isPrimal()) {
                    crucible.aspects.add(random.nextBoolean() ? aspect.getFirstComponent() : aspect.getSecondComponent(), 1);
                } else {
                    crucible.spill();
                }
            }
            crucible.sync();
        }
    }

    /** Как в оригинале: греют огонь, лава и нитор под тиглем. */
    public static boolean isHeatSource(BlockState below) {
        return below.is(BlockTags.FIRE) || below.getFluidState().is(FluidTags.LAVA) || below.is(ModBlocks.NITOR.get());
    }

    /** drawEffects оригинала: пена на кипящей воде, пена через край при переливе и цветные пузыри раствора. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        if (crucible.water <= 0) {
            return;
        }
        RandomSource random = level.random;
        float surface = pos.getY() + crucible.getFluidHeight();
        if (crucible.heat > BOILING) {
            thaumcraft.client.fx.Fx.crucibleFroth(pos.getX() + 0.2f + random.nextFloat() * 0.6f, surface,
                    pos.getZ() + 0.2f + random.nextFloat() * 0.6f);
            if (crucible.aspectTotal() > MAX_ASPECTS) {
                for (int i = 0; i < 2; i++) {
                    thaumcraft.client.fx.Fx.crucibleFrothDown(pos.getX(), pos.getY() + 1, pos.getZ() + random.nextFloat());
                    thaumcraft.client.fx.Fx.crucibleFrothDown(pos.getX() + 1, pos.getY() + 1, pos.getZ() + random.nextFloat());
                    thaumcraft.client.fx.Fx.crucibleFrothDown(pos.getX() + random.nextFloat(), pos.getY() + 1, pos.getZ());
                    thaumcraft.client.fx.Fx.crucibleFrothDown(pos.getX() + random.nextFloat(), pos.getY() + 1, pos.getZ() + 1);
                }
            }
        }
        if (random.nextInt(6) == 0 && !crucible.aspects.isEmpty()) {
            List<Aspect> present = crucible.aspects.getAspects();
            int color = present.get(random.nextInt(present.size())).getColor();
            thaumcraft.client.fx.Fx.crucibleBubble(pos.getX() + (5 + random.nextInt(22)) / 32f + 0.015625f, surface + 0.05f,
                    pos.getZ() + (5 + random.nextInt(22)) / 32f + 0.015625f,
                    (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
        }
    }

    /**
     * Выплеск (TileCrucible.spill оригинала): с шансом 1 из 4 над тиглем появляется флюсовая жижа или газ.
     * Если место над тиглем занято флюсом, он густеет; если чем-то другим — флюс появляется рядом.
     */
    private void spill() {
        if (!(level instanceof ServerLevel server) || server.random.nextInt(4) != 0) {
            return;
        }
        RandomSource random = server.random;
        BlockPos above = worldPosition.above();
        BlockState there = server.getBlockState(above);
        if (there.isAir()) {
            placeFlux(server, above);
        } else if (there.getBlock() instanceof FluxBlock && there.getValue(FluxBlock.LEVEL) < 7) {
            server.setBlock(above, there.setValue(FluxBlock.LEVEL, there.getValue(FluxBlock.LEVEL) + 1), 3);
        } else {
            BlockPos near = worldPosition.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (server.getBlockState(near).isAir()) {
                placeFlux(server, near);
            }
        }
    }

    private static void placeFlux(ServerLevel server, BlockPos pos) {
        server.setBlock(pos, (server.random.nextBoolean() ? ModBlocks.FLUX_GAS : ModBlocks.FLUX_GOO).get().defaultBlockState(), 3);
    }

    /** Жезл с зажатым Shift или разрушение тигля: всё содержимое выливается. */
    public void spillRemnants() {
        if (water <= 0 && aspects.isEmpty()) {
            return;
        }
        int spills = aspectTotal() / 2;
        water = 0;
        aspects = new AspectList();
        for (int i = 0; i < spills; i++) {
            spill();
        }
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, ModSounds.SPILL.get(), SoundSource.BLOCKS, 0.2f, 1f);
            boilBurst(server);
        }
        sync();
    }

    /** Всплеск пузырей в цвет раствора (crucibleBoil оригинала, сила 5). */
    private void boilBurst(ServerLevel server) {
        List<Aspect> present = aspects.getAspects();
        int color = present.isEmpty() ? 0 : present.get(server.random.nextInt(present.size())).getColor();
        thaumcraft.common.network.FxPacket.send(server, thaumcraft.common.network.FxPacket.CRUCIBLE_BOIL,
                new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + Math.min(0.99, getFluidHeight()),
                        worldPosition.getZ() + 0.5), net.minecraft.world.phys.Vec3.ZERO, color, 5);
    }

    /** Предмет попал в кипящий тигель: по одному из стопки он идёт либо в рецепт, либо в раствор. */
    public void attemptSmelt(ItemEntity entity) {
        attemptSmelt(entity, entity.getOwner() instanceof Player thrower ? PlayerKnowledge.get(thrower) : null);
    }

    /** @param knowledge знания бросившего; null — предмет никто не бросал, он может только раствориться */
    public void attemptSmelt(ItemEntity entity, @org.jetbrains.annotations.Nullable PlayerKnowledge knowledge) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ItemStack stack = entity.getItem();
        int remaining = stack.getCount();
        boolean dissolved = false;
        boolean crafted = false;
        boolean rejected = false;

        for (int i = stack.getCount(); i > 0; i--) {
            CrucibleRecipe recipe = CrucibleRecipe.find(server, knowledge, aspects, stack);
            if (recipe != null && water > 0) {
                for (Aspect aspect : recipe.getAspects().getAspects()) {
                    aspects.reduce(aspect, recipe.getAspects().getAmount(aspect));
                }
                water = Math.max(0, water - 50);
                eject(server, recipe.getResult());
                thaumcraft.common.research.Warp.onCrafted(entity.getOwner() instanceof Player thrower ? thrower : null, recipe.getResult());
                crafted = true;
                remaining--;
                counter = -250L;
            } else {
                AspectList contained = ItemAspectManager.INSTANCE.getAspects(stack);
                if (contained.isEmpty()) {
                    rejected = true;
                    break;
                }
                aspects.add(contained);
                dissolved = true;
                remaining--;
                counter = -150L;
            }
        }

        if (remaining <= 0) {
            entity.discard();
        } else {
            ItemStack rest = stack.copy();
            rest.setCount(remaining);
            entity.setItem(rest);
        }
        if (rejected) {
            // то, в чём нет аспектов, тигель выталкивает обратно
            RandomSource random = server.random;
            entity.setDeltaMovement((random.nextFloat() - random.nextFloat()) * 0.2f, 0.35,
                    (random.nextFloat() - random.nextFloat()) * 0.2f);
            entity.hurtMarked = true;
            server.playSound(null, entity, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f,
                    (random.nextFloat() - random.nextFloat()) * 0.7f + 1f);
        }
        if (dissolved) {
            server.playSound(null, worldPosition, ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2f,
                    1f + server.random.nextFloat() * 0.4f);
            thaumcraft.common.network.FxPacket.blockSparkle(server, worldPosition, -9999, 5);
        }
        if (crafted) {
            server.playSound(null, worldPosition, ModSounds.SPILL.get(), SoundSource.BLOCKS, 0.2f, 1f);
            boilBurst(server);
        }
        if (dissolved || crafted) {
            sync();
        }
    }

    /**
     * Результат медленно всплывает над тиглем и зависает там, как EntitySpecialItem оригинала:
     * без тяжести скорость гасится сопротивлением воздуха, и предмет поднимается примерно на блок.
     */
    private void eject(ServerLevel server, ItemStack result) {
        boolean first = true;
        while (!result.isEmpty()) {
            ItemStack part = result.split(result.getMaxStackSize());
            ItemEntity item = new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.71,
                    worldPosition.getZ() + 0.5, part);
            RandomSource random = server.random;
            item.setDeltaMovement(first ? 0 : (random.nextFloat() - random.nextFloat()) * 0.01f, 0.017,
                    first ? 0 : (random.nextFloat() - random.nextFloat()) * 0.01f);
            item.setNoGravity(true);
            item.getPersistentData().putBoolean(OUTPUT_TAG, true);
            item.setDefaultPickUpDelay();
            server.addFreshEntity(item);
            first = false;
        }
    }

    // ---------- сохранение и синхронизация ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putShort("Heat", (short) heat);
        tag.putShort("Water", (short) water);
        tag.put("Aspects", aspects.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.getShort("Heat");
        water = tag.getShort("Water");
        aspects = AspectList.load(tag.getCompound("Aspects"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
