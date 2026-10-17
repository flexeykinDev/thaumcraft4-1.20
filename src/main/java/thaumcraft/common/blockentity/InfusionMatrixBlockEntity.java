package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.FluxBlock;
import thaumcraft.common.crafting.InfusionRecipe;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Руническая матрица — сердце алтаря инфузии, перенос TileInfusionMatrix оригинала.
 * Алтарь: матрица, под ней через блок центральный пьедестал, по четырём углам от него столбы
 * (кирпичи из магического камня, на них магический камень). Жезл сначала пробуждает матрицу,
 * следующим касанием запускает рецепт: эссенция тянется из банок в 12 блоках, затем по одному
 * поглощаются предметы с пьедесталов вокруг. Несимметричная расстановка и сложные рецепты дают
 * нестабильность — шанс неприятностей на каждом шаге.
 */
public class InfusionMatrixBlockEntity extends BlockEntity implements thaumcraft.api.aspects.AspectContainer {
    private static final int MAX_INSTABILITY = 25;
    private static final int JAR_RANGE = 12;

    private boolean active;
    private boolean crafting;
    private int symmetry;
    private int instability;
    private AspectList recipeEssentia = new AspectList();
    @Nullable
    private ResourceLocation recipeId;
    /** Какие из компонентов рецепта ещё не поглощены (номера в списке компонентов рецепта). */
    private final List<Integer> remaining = new ArrayList<>();
    private int recipeInstability;
    private final List<BlockPos> pedestals = new ArrayList<>();
    private final List<BlockPos> jars = new ArrayList<>();
    private boolean checkSurroundings = true;
    private int count;
    private int itemCount;

    public InfusionMatrixBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSION_MATRIX.get(), pos, state);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCrafting() {
        return crafting;
    }

    public int getInstability() {
        return instability;
    }

    @Override
    public AspectList getContainedAspects() {
        return recipeEssentia;
    }

    /** Эссенция, которую ещё предстоит вытянуть из банок. */
    public AspectList getRecipeEssentia() {
        return recipeEssentia;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            if (state.hasProperty(thaumcraft.common.blocks.InfusionMatrixBlock.ACTIVE)
                    && state.getValue(thaumcraft.common.blocks.InfusionMatrixBlock.ACTIVE) != active) {
                level.setBlock(worldPosition, state.setValue(thaumcraft.common.blocks.InfusionMatrixBlock.ACTIVE, active), 3);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------- строение алтаря ----------

    public boolean validLocation() {
        if (level == null || !(level.getBlockEntity(worldPosition.below(2)) instanceof PedestalBlockEntity)) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                BlockState top = level.getBlockState(worldPosition.offset(dx, -1, dz));
                BlockState bottom = level.getBlockState(worldPosition.offset(dx, -2, dz));
                // столб — либо ещё не пробуждённый (камень на кирпичах), либо уже ставший столпом инфузии
                boolean raw = top.is(ModBlocks.ARCANE_STONE.get()) && bottom.is(ModBlocks.ARCANE_STONE_BRICKS.get());
                boolean formed = top.is(ModBlocks.INFUSION_PILLAR.get()) && bottom.is(ModBlocks.INFUSION_PILLAR.get());
                if (!raw && !formed) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isStabiliser(BlockState state) {
        // В оригинале стабилизаторами были черепа, свечи и кристаллы Thaumcraft; ванильных свечей тогда не было.
        return state.getBlock() instanceof AbstractSkullBlock || state.getBlock() instanceof AbstractCandleBlock
                || state.getBlock() instanceof thaumcraft.common.blocks.CrystalClusterBlock
                || state.getBlock() instanceof thaumcraft.common.blocks.TallowCandleBlock;
    }

    /** Ищет пьедесталы, банки и стабилизаторы вокруг и считает, насколько расстановка несимметрична. */
    private void getSurroundings() {
        pedestals.clear();
        jars.clear();
        if (level == null) {
            return;
        }
        List<BlockPos> stabilisers = new ArrayList<>();
        for (int dx = -JAR_RANGE; dx <= JAR_RANGE; dx++) {
            for (int dz = -JAR_RANGE; dz <= JAR_RANGE; dz++) {
                boolean found = false;
                for (int down = -JAR_RANGE; down <= JAR_RANGE; down++) {
                    BlockPos pos = worldPosition.offset(dx, -down, dz);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    BlockEntity entity = level.getBlockEntity(pos);
                    if (entity instanceof EssentiaBlockEntity jar && jar.getCapacity() == EssentiaBlockEntity.JAR_CAPACITY
                            || entity instanceof thaumcraft.common.blocks.MirrorBlock.Mirror mirror && mirror.essentia()
                            || entity instanceof thaumcraft.common.blocks.EssentiaReservoirBlock.Reservoir) {
                        jars.add(pos);
                    }
                    if (dx == 0 && dz == 0 || down < -5 || down > 10) {
                        continue;
                    }
                    // как в оригинале: в каждом столбце считается только верхний пьедестал ниже матрицы, не дальше 8 блоков
                    if (!found && down > 0 && Math.abs(dx) <= 8 && Math.abs(dz) <= 8 && entity instanceof PedestalBlockEntity) {
                        pedestals.add(pos);
                        found = true;
                    } else if (isStabiliser(level.getBlockState(pos))) {
                        stabilisers.add(pos);
                    }
                }
            }
        }
        symmetry = 0;
        for (BlockPos pos : pedestals) {
            boolean hasItem = false;
            if (level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
                symmetry += 2;
                if (!pedestal.getItem().isEmpty()) {
                    symmetry++;
                    hasItem = true;
                }
            }
            // пьедестал напротив гасит нестабильность
            BlockPos mirror = new BlockPos(2 * worldPosition.getX() - pos.getX(), pos.getY(), 2 * worldPosition.getZ() - pos.getZ());
            if (level.getBlockEntity(mirror) instanceof PedestalBlockEntity opposite) {
                symmetry -= 2;
                if (!opposite.getItem().isEmpty() && hasItem) {
                    symmetry--;
                }
            }
        }
        float bonus = 0f;
        for (BlockPos pos : stabilisers) {
            bonus += 0.1f;
            BlockPos mirror = new BlockPos(2 * worldPosition.getX() - pos.getX(), pos.getY(), 2 * worldPosition.getZ() - pos.getZ());
            if (isStabiliser(level.getBlockState(mirror))) {
                bonus -= 0.2f;
            }
        }
        symmetry = (int) (symmetry + bonus);
    }

    // ---------- жезл ----------

    /** @return true, если касание жезла что-то сделало */
    /** Сколько уровней опыта ещё нужно забрать (только у зачарования наполнением). */
    private int recipeXp;
    /** Пауза в один шаг после каждого взятого уровня. */
    private boolean xpPause;

    /** Кто запустил текущее вливание. */
    @org.jetbrains.annotations.Nullable
    private java.util.UUID crafter;

    public boolean onWand(Player player) {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (active && !crafting) {
            crafter = player.getUUID();
            craftingStart(PlayerKnowledge.get(player));
            return true;
        }
        return wake();
    }

    /** Пробуждает матрицу, если алтарь собран правильно. */
    public boolean wake() {
        if (level == null || active || !validLocation()) {
            return false;
        }
        active = true;
        // replaceInfusionAltar оригинала: четыре столба становятся столпами инфузии
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                BlockPos bricks = worldPosition.offset(dx, -2, dz);
                if (level.getBlockState(bricks).is(ModBlocks.ARCANE_STONE_BRICKS.get())) {
                    thaumcraft.common.blocks.InfusionPillarBlock.form(level, bricks, (dx > 0 ? 2 : 0) + (dz > 0 ? 1 : 0));
                    if (level instanceof ServerLevel server) {
                        thaumcraft.common.network.FxPacket.blockSparkle(server, bricks, -9999, 4);
                        thaumcraft.common.network.FxPacket.blockSparkle(server, bricks.above(), -9999, 4);
                    }
                }
            }
        }
        level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
        sync();
        return true;
    }

    private List<ItemStack> itemsAround() {
        List<ItemStack> items = new ArrayList<>();
        for (BlockPos pos : pedestals) {
            if (level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal && !pedestal.getItem().isEmpty()) {
                items.add(pedestal.getItem());
            }
        }
        return items;
    }

    /** Ищет рецепт по тому, что лежит на пьедесталах, среди изученных этим игроком, и начинает инфузию. */
    public void craftingStart(PlayerKnowledge knowledge) {
        if (!validLocation()) {
            active = false;
            sync();
            return;
        }
        getSurroundings();
        if (!(level.getBlockEntity(worldPosition.below(2)) instanceof PedestalBlockEntity centre) || centre.getItem().isEmpty()) {
            return;
        }
        List<ItemStack> around = itemsAround();
        if (around.isEmpty()) {
            return;
        }
        InfusionRecipe recipe = InfusionRecipe.find(level, knowledge, centre.getItem(), around);
        if (recipe == null) {
            return;
        }
        recipeId = recipe.getId();
        remaining.clear();
        for (int i = 0; i < recipe.getComponents(centre.getItem()).size(); i++) {
            remaining.add(i);
        }
        recipeInstability = recipe.getInstability(centre.getItem());
        recipeEssentia = recipe.getAspects(centre.getItem());
        recipeXp = recipe.getXpCost(centre.getItem());
        xpPause = false;
        instability = symmetry + recipeInstability;
        crafting = true;
        level.playSound(null, worldPosition, ModSounds.CRAFT_START.get(), SoundSource.BLOCKS, 0.5f, 1f);
        level.playSound(null, worldPosition, ModSounds.INFUSER_START.get(), SoundSource.BLOCKS, 0.5f, 1f);
        sync();
    }

    @Nullable
    private InfusionRecipe recipe() {
        return recipeId != null && level != null
                && level.getRecipeManager().byKey(recipeId).orElse(null) instanceof InfusionRecipe recipe ? recipe : null;
    }

    // ---------- работа ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfusionMatrixBlockEntity matrix) {
        matrix.count++;
        if (matrix.checkSurroundings) {
            matrix.checkSurroundings = false;
            matrix.getSurroundings();
        }
        if (matrix.active && matrix.count % (matrix.crafting ? 20 : 100) == 0 && !matrix.validLocation()) {
            matrix.active = false;
            matrix.sync();
            return;
        }
        if (matrix.active && matrix.crafting) {
            if (matrix.count % 65 == 0) {
                level.playSound(null, pos, ModSounds.INFUSER.get(), SoundSource.BLOCKS, 0.5f, 1f);
            }
            if (matrix.count % 10 == 0) {
                matrix.craftCycle((ServerLevel) level);
                matrix.setChanged();
            }
        }
    }

    // только на клиенте: плавное пробуждение и счётчик времени инфузии для рендерера
    private float startUp;
    private float startUpOld;
    private int craftCount;

    /** Насколько матрица «раскрутилась»: 0 — спит, 1 — полностью пробуждена. */
    public float getStartUp(float partialTick) {
        return startUpOld + (startUp - startUpOld) * partialTick;
    }

    public int getCraftCount() {
        return craftCount;
    }

    /** doEffects оригинала: руны на центральном пьедестале, разгон и остановка вращения. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, InfusionMatrixBlockEntity matrix) {
        if (matrix.crafting) {
            RandomSource random = level.random;
            matrix.craftCount++;
            thaumcraft.client.fx.Fx.blockRunes(pos.getX(), pos.getY() - 2, pos.getZ(), 0.5f + random.nextFloat() * 0.2f, 0.1f,
                    0.7f + random.nextFloat() * 0.3f, 25, -0.03f);
        } else if (matrix.craftCount > 0) {
            matrix.craftCount = Math.min(50, Math.max(0, matrix.craftCount - 2));
        }
        matrix.startUpOld = matrix.startUp;
        if (matrix.active && matrix.startUp != 1f) {
            matrix.startUp += Math.max(matrix.startUp / 10f, 0.001f);
            if (matrix.startUp > 0.999f) {
                matrix.startUp = 1f;
            }
        }
        if (!matrix.active && matrix.startUp > 0f) {
            matrix.startUp -= matrix.startUp / 10f;
            if (matrix.startUp < 0.001f) {
                matrix.startUp = 0f;
            }
        }
    }

    /** Лучи во время инфузии выходят далеко за пределы блока. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }

    /** Один шаг инфузии, раз в полсекунды. */
    private void craftCycle(ServerLevel server) {
        RandomSource random = server.random;
        InfusionRecipe recipe = recipe();
        PedestalBlockEntity centre = server.getBlockEntity(worldPosition.below(2)) instanceof PedestalBlockEntity p ? p : null;
        boolean valid = recipe != null && centre != null && recipe.centreMatches(centre.getItem());

        if (!valid || instability > 0 && random.nextInt(500) <= instability) {
            instabilityEvent(server, random.nextInt(21));
            if (valid) {
                return;
            }
        }
        if (!valid) {
            // центральный предмет забрали: всё насмарку
            instability = 0;
            crafting = false;
            recipeEssentia = new AspectList();
            recipeId = null;
            server.playSound(null, worldPosition, ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 1f, 0.6f);
            sync();
            return;
        }

        if (recipeXp > 0) {
            takeExperience(server);
            return;
        }

        if (recipeEssentia.visSize() > 0) {
            for (Aspect aspect : recipeEssentia.getAspects()) {
                if (drainEssentia(server, aspect)) {
                    recipeEssentia.reduce(aspect, 1);
                    sync();
                    return;
                }
                // нужной эссенции рядом нет: матрица ждёт и понемногу теряет устойчивость
                if (random.nextInt(Math.max(1, 100 - recipeInstability * 3)) == 0) {
                    instability = Math.min(MAX_INSTABILITY, instability + 1);
                }
                sync();
            }
            checkSurroundings = true;
            return;
        }
        if (remaining.isEmpty()) {
            instability = 0;
            crafting = false;
            centre.setItem(recipe.getResult(centre.getItem()));
            // создателем считается тот, кто запустил алтарь, — если он ещё в игре
            thaumcraft.common.research.Warp.onCrafted(crafter == null ? null : server.getPlayerByUUID(crafter), centre.getItem());
            if (crafter != null && server.getPlayerByUUID(crafter) != null) {
                thaumcraft.common.ModAdvancements.grant(server.getPlayerByUUID(crafter), thaumcraft.common.ModAdvancements.INFUSION);
            }
            recipeId = null;
            server.playSound(null, worldPosition.below(2), ModSounds.WAND.get(), SoundSource.BLOCKS, 0.5f, 1f);
            // блок-событие 12 оригинала: центральный пьедестал вспыхивает искрами
            thaumcraft.common.network.FxPacket.blockSparkle(server, worldPosition.below(2), -9999, 12);
            sync();
            return;
        }
        for (int a = 0; a < remaining.size(); a++) {
            Ingredient wanted = recipe.getComponents(centre.getItem()).get(remaining.get(a));
            for (BlockPos pos : pedestals) {
                if (server.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal && !pedestal.getItem().isEmpty()
                        && wanted.test(pedestal.getItem())) {
                    if (itemCount == 0) {
                        itemCount = 5;
                        beam(server, pos);
                    } else if (itemCount-- <= 1) {
                        pedestal.setItem(pedestal.getItem().getCraftingRemainingItem());
                        remaining.remove(a);
                        thaumcraft.common.network.FxPacket.blockSparkle(server, pos, -9999, 5);
                    } else {
                        beam(server, pos);
                    }
                    return;
                }
            }
            // нужного предмета на пьедесталах нет: эссенции требуется больше, а устойчивость падает
            List<Aspect> all = recipe.getAspects(centre.getItem()).getAspects();
            if (!all.isEmpty() && random.nextInt(1 + a) == 0) {
                recipeEssentia.add(all.get(random.nextInt(all.size())), 1);
                if (random.nextInt(Math.max(1, 50 - recipeInstability * 2)) == 0) {
                    instability = Math.min(MAX_INSTABILITY, instability + 1);
                }
                sync();
            }
        }
    }

    private net.minecraft.world.phys.Vec3 focus() {
        return new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5, worldPosition.getY() - 0.5, worldPosition.getZ() + 0.5);
    }

    /** Предмет на пьедестале крошится и летит в матрицу (PacketFXInfusionSource оригинала). */
    private void beam(ServerLevel server, BlockPos from) {
        thaumcraft.common.network.FxPacket.send(server, thaumcraft.common.network.FxPacket.ITEM_STREAM,
                net.minecraft.world.phys.Vec3.atLowerCornerOf(from), focus(), 0, 0);
    }

    /** Молния от матрицы (PacketFXBlockZap оригинала) с её звуком. */
    private void bolt(ServerLevel server, net.minecraft.world.phys.Vec3 target) {
        thaumcraft.common.network.FxPacket.bolt(server, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition), target);
        server.playSound(null, worldPosition, ModSounds.ZAP.get(), SoundSource.BLOCKS, 0.1f, 1f + server.random.nextFloat() * 0.2f);
    }

    /**
     * Зачарование наполнением сперва берёт плату опытом: раз в секунду — уровень у кого-нибудь из игроков в десяти
     * блоках, слегка раня его. Если рядом есть люди, но опыта ни у кого нет, алтарь ждёт, а цена в эссенции
     * и нестабильность тем временем растут.
     */
    private void takeExperience(ServerLevel server) {
        if (xpPause) {
            xpPause = false;
            return;
        }
        RandomSource random = server.random;
        List<net.minecraft.world.entity.player.Player> players = server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new AABB(worldPosition).inflate(10.0), p -> !p.isSpectator());
        for (net.minecraft.world.entity.player.Player target : players) {
            if (target.experienceLevel > 0) {
                target.giveExperienceLevels(-1);
                recipeXp--;
                target.hurt(server.damageSources().magic(), random.nextInt(2));
                thaumcraft.common.network.FxPacket.send(server, thaumcraft.common.network.FxPacket.ESSENTIA,
                        target.getEyePosition(), focus(), 0x80ff20, 0);
                server.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                        1f, 2f + random.nextFloat() * 0.4f);
                xpPause = true;
                sync();
                return;
            }
        }
        List<Aspect> wanted = recipeEssentia.getAspects();
        if (!players.isEmpty() && !wanted.isEmpty() && random.nextInt(3) == 0) {
            recipeEssentia.add(wanted.get(random.nextInt(wanted.size())), 1);
            if (random.nextInt(Math.max(1, 50 - recipeInstability * 2)) == 0) {
                instability = Math.min(MAX_INSTABILITY, instability + 1);
            }
            sync();
        }
    }

    /** Берёт единицу эссенции из любой банки поблизости (EssentiaHandler.drainEssentia оригинала). */
    private boolean drainEssentia(ServerLevel server, Aspect aspect) {
        for (BlockPos pos : jars) {
            BlockEntity source = server.getBlockEntity(pos);
            // зеркало эссенции берёт её из банок перед своей парой
            if (source instanceof thaumcraft.common.blocks.MirrorBlock.Mirror mirror && mirror.takeEssentia(aspect)
                    || source instanceof EssentiaBlockEntity jar && jar.take(aspect, 1)
                    || source instanceof thaumcraft.common.blocks.EssentiaReservoirBlock.Reservoir reservoir && reservoir.take(aspect, 1)) {
                thaumcraft.common.network.FxPacket.send(server, thaumcraft.common.network.FxPacket.ESSENTIA,
                        new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), focus(),
                        aspect.getColor(), 0);
                return true;
            }
        }
        return false;
    }

    // ---------- неприятности ----------

    /** Те же 21 исход и те же шансы, что в оригинале. */
    private void instabilityEvent(ServerLevel server, int roll) {
        switch (roll) {
            case 0, 2, 10, 13 -> ejectItem(server, 0);
            case 1, 11 -> ejectItem(server, 2);
            case 3, 8, 14 -> zap(server, false);
            case 4, 15 -> ejectItem(server, 5);
            case 5, 16 -> harm(server, false);
            case 6, 17 -> ejectItem(server, 1);
            case 7 -> ejectItem(server, 4);
            case 9 -> server.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    1.5f + server.random.nextFloat(), Level.ExplosionInteraction.NONE);
            case 12 -> zap(server, true);
            case 18 -> harm(server, true);
            case 19 -> ejectItem(server, 3);
            default -> warp(server);
        }
    }

    /** inEvWarp оригинала: случайный игрок рядом получает искажение — в четверти случаев липкое, иначе временное 1-5. */
    private void warp(ServerLevel server) {
        List<net.minecraft.server.level.ServerPlayer> players = server.getEntitiesOfClass(
                net.minecraft.server.level.ServerPlayer.class, new AABB(worldPosition).inflate(10));
        if (players.isEmpty()) {
            return;
        }
        net.minecraft.server.level.ServerPlayer target = players.get(server.random.nextInt(players.size()));
        if (server.random.nextFloat() < 0.25f) {
            thaumcraft.common.research.Warp.addSticky(target, 1);
        } else {
            thaumcraft.common.research.Warp.add(target, 1 + server.random.nextInt(5), true);
        }
    }

    private List<LivingEntity> nearby(ServerLevel server) {
        return server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(10));
    }

    private void zap(ServerLevel server, boolean all) {
        for (LivingEntity target : nearby(server)) {
            target.hurt(server.damageSources().magic(), 4 + server.random.nextInt(4));
            bolt(server, new net.minecraft.world.phys.Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ()));
            if (!all) {
                break;
            }
        }
    }

    private void harm(ServerLevel server, boolean all) {
        for (LivingEntity target : nearby(server)) {
            if (server.random.nextBoolean()) {
                target.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 120, 0));
            } else {
                MobEffectInstance effect = new MobEffectInstance(ModEffects.VIS_EXHAUSTION.get(), 2400, 0, true, true);
                effect.setCurativeItems(new ArrayList<>());
                target.addEffect(effect);
            }
            if (!all) {
                break;
            }
        }
    }

    /**
     * Матрица сбрасывает предмет со случайного пьедестала. Виды: 0 — просто роняет, 1 — роняет и оставляет жижу,
     * 2 — роняет и оставляет газ, 3 — уничтожает и оставляет жижу, 4 — уничтожает и оставляет газ, 5 — роняет со взрывом.
     */
    private void ejectItem(ServerLevel server, int type) {
        for (int attempt = 0; attempt < 50 && !pedestals.isEmpty(); attempt++) {
            BlockPos pos = pedestals.get(server.random.nextInt(pedestals.size()));
            if (!(server.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || pedestal.getItem().isEmpty()) {
                continue;
            }
            if (type < 3 || type == 5) {
                Containers.dropItemStack(server, pos.getX(), pos.getY() + 1, pos.getZ(), pedestal.getItem());
            }
            pedestal.setItem(ItemStack.EMPTY);
            if (type == 1 || type == 3) {
                server.setBlock(pos.above(), ModBlocks.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.LEVEL, 7), 3);
                server.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 0.3f, 1f);
            } else if (type == 2 || type == 4) {
                server.setBlock(pos.above(), ModBlocks.FLUX_GAS.get().defaultBlockState().setValue(FluxBlock.LEVEL, 7), 3);
                server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1f);
            } else if (type == 5) {
                server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1f, Level.ExplosionInteraction.NONE);
            }
            bolt(server, new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5));
            return;
        }
    }

    // ---------- сохранение ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Active", active);
        tag.putBoolean("Crafting", crafting);
        tag.putInt("RecipeXp", recipeXp);
        if (crafter != null) {
            tag.putUUID("Crafter", crafter);
        }
        tag.putShort("Instability", (short) instability);
        tag.put("Essentia", recipeEssentia.save(new CompoundTag()));
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
            tag.putIntArray("Remaining", remaining);
            tag.putInt("RecipeInstability", recipeInstability);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean("Active");
        crafting = tag.getBoolean("Crafting");
        recipeXp = tag.getInt("RecipeXp");
        crafter = tag.hasUUID("Crafter") ? tag.getUUID("Crafter") : null;
        instability = tag.getShort("Instability");
        recipeEssentia = AspectList.load(tag.getCompound("Essentia"));
        recipeId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        remaining.clear();
        for (int index : tag.getIntArray("Remaining")) {
            remaining.add(index);
        }
        recipeInstability = tag.getInt("RecipeInstability");
        checkSurroundings = true;
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
