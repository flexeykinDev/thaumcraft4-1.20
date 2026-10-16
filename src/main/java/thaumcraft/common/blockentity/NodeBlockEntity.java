package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Узел ауры — перенос TileNode оригинала. У каждого аспекта есть текущий запас вис и предел, до которого
 * узел сам восстанавливается. Числа и порядок действий повторяют оригинал.
 */
public class NodeBlockEntity extends BlockEntity implements thaumcraft.api.visnet.VisNode, thaumcraft.api.aspects.AspectContainer {
    /** Запас по аспектам. Аспект с нулём остаётся в списке: узел его ещё может восстановить. */
    private final Map<Aspect, Integer> vis = new LinkedHashMap<>();
    /** Предел восстановления по аспектам. */
    private final Map<Aspect, Integer> base = new LinkedHashMap<>();
    private NodeType type = NodeType.NORMAL;
    @Nullable
    private NodeModifier modifier;

    private int count;
    private int regeneration = -1;
    private int wait;
    /**
     * Реальное время последней подзарядки в миллисекундах. Как в оригинале, по нему узел навёрстывает
     * упущенное, пока чанк был выгружен — в том числе пока игра или сервер были выключены.
     */
    private long lastActive;
    private boolean catchUp;
    /** Сила стабилизатора под узлом: 0 — его нет, 1 — обычный, 2 — улучшенный. */
    private int lock;
    /** Заряженный узел — источник сети вис: сам не восстанавливается и не отдаёт вис жезлу. */
    private boolean energized;
    /** Сколько вис каждого первичного аспекта заряженный узел даёт сети за тик, в сотых долях. */
    private final thaumcraft.api.aspects.AspectList visOut = new thaumcraft.api.aspects.AspectList();
    /** Остаток на текущий тик. */
    private thaumcraft.api.aspects.AspectList visLeft = new thaumcraft.api.aspects.AspectList();
    private final thaumcraft.api.visnet.VisNodeLink visLink = new thaumcraft.api.visnet.VisNodeLink();

    public NodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NODE.get(), pos, state);
    }

    // ---------- доступ ----------

    public NodeType getNodeType() {
        return type;
    }

    public void setNodeType(NodeType type) {
        this.type = type;
    }

    @Nullable
    public NodeModifier getNodeModifier() {
        return modifier;
    }

    public void setNodeModifier(@Nullable NodeModifier modifier) {
        this.modifier = modifier;
    }

    /** Задаёт узел с нуля: запас равен пределу. */
    public void setAspects(AspectList aspects) {
        vis.clear();
        base.clear();
        for (Aspect aspect : aspects.getAspects()) {
            vis.put(aspect, aspects.getAmount(aspect));
            base.put(aspect, aspects.getAmount(aspect));
        }
        nodeChange();
    }

    /** Аспекты узла в порядке появления, включая исчерпанные. */
    public List<Aspect> getAspects() {
        return new ArrayList<>(vis.keySet());
    }

    public int getAmount(Aspect aspect) {
        return vis.getOrDefault(aspect, 0);
    }

    public int getBase(Aspect aspect) {
        return base.getOrDefault(aspect, 0);
    }

    public int visSize() {
        return vis.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int baseSize() {
        return base.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public AspectList getContainedAspects() {
        return toAspectList();
    }

    /** Текущий запас без исчерпанных аспектов: для показа игроку. */
    public AspectList toAspectList() {
        AspectList list = new AspectList();
        vis.forEach(list::add);
        return list;
    }

    /** @return сколько не поместилось: запас не поднимается выше предела */
    public int addToContainer(Aspect aspect, int amount) {
        int left = Math.max(0, amount + getAmount(aspect) - getBase(aspect));
        vis.merge(aspect, amount - left, Integer::sum);
        return left;
    }

    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (energized || !vis.containsKey(aspect) || getAmount(aspect) < amount) {
            return false;
        }
        vis.put(aspect, getAmount(aspect) - amount);
        return true;
    }

    public void setBase(Aspect aspect, int value) {
        base.put(aspect, Math.max(0, value));
    }

    private boolean isFreeNode() {
        return getBlockState().is(ModBlocks.AURA_NODE.get());
    }

    /** Свободный узел ауры, а не узел в стволе серебряного дерева. */
    public boolean isAuraNode() {
        return isFreeNode();
    }

    public int getLock() {
        return lock;
    }

    public boolean isEnergized() {
        return energized;
    }

    // ---------- заряженный узел: источник сети вис ----------

    private static void addPrimals(Aspect aspect, int amount, thaumcraft.api.aspects.AspectList out) {
        if (aspect.isPrimal()) {
            out.add(aspect, amount);
        } else {
            // составной аспект делится между своими половинами, как в ResearchManager.reduceToPrimals
            addPrimals(aspect.getFirstComponent(), amount, out);
            addPrimals(aspect.getSecondComponent(), amount, out);
        }
    }

    /** setupNode оригинала: выход по каждому первичному аспекту — квадратный корень из его предела. */
    private void setupOutput() {
        for (Aspect old : new java.util.ArrayList<>(visOut.getAspects())) {
            visOut.remove(old);
        }
        thaumcraft.api.aspects.AspectList primals = new thaumcraft.api.aspects.AspectList();
        for (Map.Entry<Aspect, Integer> entry : base.entrySet()) {
            addPrimals(entry.getKey(), entry.getValue(), primals);
        }
        for (Aspect aspect : primals.getAspects()) {
            float amount = primals.getAmount(aspect);
            if (modifier == NodeModifier.BRIGHT) {
                amount = (int) (amount * 1.2f);
            } else if (modifier == NodeModifier.PALE) {
                amount = (int) (amount * 0.8f);
            } else if (modifier == NodeModifier.FADING) {
                amount = (int) (amount * 0.5f);
            }
            int out = (int) Math.floor(Math.sqrt(amount));
            if (type == NodeType.UNSTABLE && level != null) {
                out += level.random.nextInt(5) - 2;
            }
            if (out >= 1) {
                visOut.merge(aspect, out);
            }
        }
    }

    /** Преобразователь зарядил узел: с этого мига он источник сети вис. */
    public void energize() {
        energized = true;
        // на вид заряженный узел полон; забрать из него вис жезлом уже нельзя
        vis.putAll(base);
        setupOutput();
        sync();
    }

    /** Заряд снят: узел снова обычный, но совершенно пустой. */
    public void deenergize() {
        energized = false;
        vis.replaceAll((aspect, amount) -> 0);
        for (Aspect old : new java.util.ArrayList<>(visOut.getAspects())) {
            visOut.remove(old);
        }
        visLink.remove(this);
        regeneration = -1;
        sync();
    }

    /** Сколько вис в сотых долях узел даёт сети за тик. */
    public thaumcraft.api.aspects.AspectList getVisOutput() {
        return visOut;
    }

    @Override
    public thaumcraft.api.visnet.VisNodeLink visLink() {
        return visLink;
    }

    @Override
    public BlockPos visPos() {
        return worldPosition;
    }

    @Override
    public Level visLevel() {
        return level;
    }

    @Override
    public boolean isVisNodeRemoved() {
        return isRemoved() || !energized;
    }

    @Override
    public int getVisRange() {
        return 8;
    }

    @Override
    public boolean isVisSource() {
        return energized;
    }

    @Override
    public int consumeVis(Aspect aspect, int amount) {
        int drain = Math.min(visLeft.getAmount(aspect), amount);
        if (drain > 0) {
            visLeft.reduce(aspect, drain);
        }
        return drain;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && energized) {
            visLink.remove(this);
        }
        super.setRemoved();
    }

    /** Раз в две с половиной секунды узел смотрит, стоит ли под ним стабилизатор. */
    private void checkLock(Level level) {
        if (count > 1 && count % 50 != 0 || !isFreeNode()) {
            return;
        }
        int found = thaumcraft.common.blocks.NodeStabilizerBlock.lockAt(level, worldPosition.below());
        if (found != lock) {
            lock = found;
            regeneration = -1;
        }
    }

    public void nodeChange() {
        regeneration = -1;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------- работа ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, NodeBlockEntity node) {
        node.count++;
        node.checkLock(level);
        if (node.energized) {
            // нестабильный заряженный узел время от времени пересчитывает свой выход
            if (node.type == NodeType.UNSTABLE && level.random.nextInt(500) == 1 || node.visOut.isEmpty()) {
                node.setupOutput();
            }
            node.visLeft = node.visOut.copy();
            node.visLink.serverTick(node);
            return;
        }
        node.pullEntities(level);
        boolean change = node.handleDischarge(level);
        change |= node.handleRecharge(level);
        change |= node.handleTaint(level);
        node.handleDark(level);
        node.handlePure(level);
        change |= node.handleStability(level);
        node.handleHungryBreak(level);
        if (change && !node.isRemoved()) {
            node.sync();
        }
    }

    /** На клиенте узел только притягивает самого игрока: его движение считает клиент. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, NodeBlockEntity node) {
        node.count++;
        node.pullEntities(level);
        // тёмный узел даёт о себе знать зловещим камням
        if (node.type == NodeType.DARK && node.count % 50 == 0) {
            thaumcraft.common.items.SinisterStoneItem.mark(level, pos);
        }
    }

    /** Голодный узел тянет к себе всё вокруг и ранит тех, кто подошёл вплотную. */
    private void pullEntities(Level level) {
        if (type != NodeType.HUNGRY) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(worldPosition);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(worldPosition).inflate(15))) {
            if (entity instanceof Player player && (player.getAbilities().invulnerable || player.isSpectator())) {
                continue;
            }
            boolean isPlayer = entity instanceof Player;
            if (!level.isClientSide && damageIfClose(level, entity, center)) {
                sync();
            }
            // движение игроков считает их клиент, всех остальных — сервер
            if (level.isClientSide != isPlayer) {
                continue;
            }
            double dx = (center.x - entity.getX()) / 15.0;
            double dy = (center.y - entity.getY()) / 15.0;
            double dz = (center.z - entity.getZ()) / 15.0;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double pull = 1.0 - distance;
            if (pull > 0 && distance > 0) {
                pull *= pull;
                entity.setDeltaMovement(entity.getDeltaMovement().add(
                        dx / distance * pull * 0.15, dy / distance * pull * 0.25, dz / distance * pull * 0.15));
            }
        }
    }

    /**
     * Голодный узел ранит тех, кто подошёл вплотную, а убив — поглощает: один из первичных аспектов жертвы пополняет
     * узел, а если узел и так полон, то изредка навсегда увеличивает его предел.
     *
     * @return изменился ли узел
     */
    private boolean damageIfClose(Level level, Entity entity, Vec3 center) {
        if (!entity.isAlive() || entity.isInvulnerable() || entity.distanceToSqr(center) >= 2.0) {
            return false;
        }
        entity.hurt(level.damageSources().fellOutOfWorld(), 1f);
        if (entity.isAlive()) {
            return false;
        }
        AspectList primals = thaumcraft.common.research.ResearchHelper.reduceToPrimals(
                thaumcraft.common.research.ScanManager.targetForEntity(entity).aspects());
        if (primals.size() == 0) {
            return false;
        }
        Aspect aspect = primals.getAspects().get(level.random.nextInt(primals.size()));
        if (getAmount(aspect) < getBase(aspect)) {
            addToContainer(aspect, 1);
            return true;
        }
        if (level.random.nextInt(1 + getBase(aspect) * 2) < primals.getAmount(aspect)) {
            setBase(aspect, getBase(aspect) + 1);
            return true;
        }
        return false;
    }

    /**
     * handleNodeStability оригинала: нестабильный узел раз в пять секунд с шансом один к двум выбрасывает сферу
     * одного из своих первичных аспектов. Закреплённый стабилизатором, он вместо этого может со временем стать
     * обычным, а угасающий — бледным.
     */
    private boolean handleStability(Level level) {
        if (count % 100 != 0) {
            return false;
        }
        boolean change = false;
        RandomSource random = level.random;
        if (type == NodeType.UNSTABLE && random.nextBoolean()) {
            if (lock == 0) {
                List<Aspect> primals = new ArrayList<>();
                for (Aspect aspect : getAspects()) {
                    if (aspect.isPrimal()) {
                        primals.add(aspect);
                    }
                }
                if (!primals.isEmpty()) {
                    Aspect aspect = primals.get(random.nextInt(primals.size()));
                    if (takeFromContainer(aspect, 1)) {
                        thaumcraft.common.entity.AspectOrbEntity.spawn(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                                worldPosition.getZ() + 0.5, aspect, 1);
                        change = true;
                    }
                }
            } else if (random.nextInt(10000 / lock) == 42) {
                type = NodeType.NORMAL;
                change = true;
            }
        }
        if (modifier == NodeModifier.FADING && lock > 0 && random.nextInt(12500 / lock) == 69) {
            modifier = NodeModifier.PALE;
            change = true;
        }
        return change;
    }

    /** Раз в 2,5 секунды голодный узел ломает случайный непрочный блок в 16 блоках от себя. */
    /**
     * handleTaintNode оригинала: заражённый узел раз в две с половиной секунды заражает землю в восьми блоках вокруг
     * и растит волокна порчи; обычный узел в порченой земле со временем сам становится заражённым.
     */
    private boolean handleTaint(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return false;
        }
        RandomSource random = level.random;
        if (type == NodeType.TAINTED && count % 50 == 0) {
            BlockPos target = worldPosition.offset(random.nextInt(8) - random.nextInt(8), 0, random.nextInt(8) - random.nextInt(8));
            // заражается сразу столбец 4x4, поэтому вчетверо реже, чем в оригинале
            if (!thaumcraft.common.taint.Taint.isTainted(level, target) && random.nextInt(4) == 0) {
                thaumcraft.common.taint.Taint.setTainted(server, target);
            }
            if (random.nextBoolean()) {
                thaumcraft.common.taint.Taint.spreadFibres(server, worldPosition.offset(random.nextInt(5) - random.nextInt(5),
                        random.nextInt(5) - random.nextInt(5), random.nextInt(5) - random.nextInt(5)));
            }
        } else if (type != NodeType.PURE && type != NodeType.TAINTED && count % 100 == 0
                && thaumcraft.common.taint.Taint.isTainted(level, worldPosition) && random.nextInt(500) == 0) {
            type = NodeType.TAINTED;
            return true;
        }
        return false;
    }

    private static boolean ordinaryWorld(Level level) {
        return level.dimension() != Level.NETHER && level.dimension() != Level.END;
    }

    /**
     * handleDarkNode оригинала: тёмный узел раз в две с половиной секунды превращает землю в двенадцати блоках вокруг
     * в жуткие земли, а когда рядом игрок — призывает яростных зомби, не больше четырёх разом.
     */
    private void handleDark(Level level) {
        if (type != NodeType.DARK || count % 50 != 0 || !(level instanceof net.minecraft.server.level.ServerLevel server) || !ordinaryWorld(level)) {
            return;
        }
        RandomSource random = level.random;
        BlockPos target = worldPosition.offset(random.nextInt(12) - random.nextInt(12), 0, random.nextInt(12) - random.nextInt(12));
        // меняется сразу столбец 4x4, поэтому вчетверо реже, чем в оригинале
        if (!thaumcraft.common.world.ModBiomes.is(level, target, thaumcraft.common.world.ModBiomes.EERIE) && random.nextInt(4) == 0) {
            thaumcraft.common.world.ModBiomes.set(server, target, thaumcraft.common.world.ModBiomes.EERIE);
        }
        if (!random.nextBoolean() || level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 24.0, false) == null) {
            return;
        }
        var zombieType = thaumcraft.common.registry.ModEntities.GIANT_BRAINY_ZOMBIE.get();
        if (level.getEntitiesOfClass(thaumcraft.common.entity.BrainyZombie.Giant.class, new AABB(worldPosition).inflate(10, 6, 10)).size() > 3) {
            return;
        }
        thaumcraft.common.entity.BrainyZombie.Giant zombie = zombieType.create(level);
        if (zombie == null) {
            return;
        }
        zombie.moveTo(worldPosition.getX() + (random.nextDouble() - random.nextDouble()) * 5.0, worldPosition.getY() + random.nextInt(3) - 1,
                worldPosition.getZ() + (random.nextDouble() - random.nextDouble()) * 5.0, random.nextFloat() * 360f, 0f);
        if (net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(zombieType, server, net.minecraft.world.entity.MobSpawnType.SPAWNER,
                zombie.blockPosition(), random) && zombie.checkSpawnObstruction(level) && level.noCollision(zombie)) {
            level.addFreshEntity(zombie);
            level.levelEvent(2004, worldPosition, 0);
            zombie.spawnAnim();
        }
    }

    /**
     * handlePureNode оригинала: чистый узел очищает порченую землю в восьми блоках вокруг, превращая её в волшебный лес,
     * а узел в стволе серебряного дерева превращает в волшебный лес вообще любую землю.
     */
    private void handlePure(Level level) {
        if (type != NodeType.PURE || count % 50 != 0 || !(level instanceof net.minecraft.server.level.ServerLevel server) || !ordinaryWorld(level)) {
            return;
        }
        RandomSource random = level.random;
        BlockPos target = worldPosition.offset(random.nextInt(8) - random.nextInt(8), 0, random.nextInt(8) - random.nextInt(8));
        if (thaumcraft.common.world.ModBiomes.is(level, target, thaumcraft.common.world.ModBiomes.MAGICAL_FOREST)) {
            return;
        }
        if ((thaumcraft.common.taint.Taint.isTainted(level, target) || !isFreeNode()) && random.nextInt(4) == 0) {
            thaumcraft.common.world.ModBiomes.set(server, target, thaumcraft.common.world.ModBiomes.MAGICAL_FOREST);
        }
    }

    private void handleHungryBreak(Level level) {
        if (type != NodeType.HUNGRY || count % 50 != 0) {
            return;
        }
        RandomSource random = level.random;
        int x = worldPosition.getX() + random.nextInt(16) - random.nextInt(16);
        int y = worldPosition.getY() + random.nextInt(16) - random.nextInt(16);
        int z = worldPosition.getZ() + random.nextInt(16) - random.nextInt(16);
        y = Math.min(y, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        BlockHitResult hit = level.clip(new ClipContext(Vec3.atCenterOf(worldPosition), new Vec3(x + 0.5, y + 0.5, z + 0.5),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(worldPosition)
                || hit.getBlockPos().distSqr(worldPosition) >= 256) {
            return;
        }
        float hardness = level.getBlockState(hit.getBlockPos()).getDestroySpeed(level, hit.getBlockPos());
        if (hardness >= 0f && hardness < 5f) {
            level.destroyBlock(hit.getBlockPos(), true);
        }
    }

    /** Сильный узел вытягивает вис из более слабого соседа в радиусе 4 блоков и иногда растёт за его счёт. */
    private boolean handleDischarge(Level level) {
        // стабилизированный узел соседей не трогает
        if (!isFreeNode() || modifier == NodeModifier.FADING || lock == 1) {
            return false;
        }
        boolean shiny = type == NodeType.HUNGRY || modifier == NodeModifier.BRIGHT;
        int interval = modifier == null ? 2 : shiny ? 1 : modifier == NodeModifier.PALE ? 3 : 2;
        if (count % interval != 0) {
            return false;
        }
        RandomSource random = level.random;
        int dx = random.nextInt(5) - random.nextInt(5);
        int dy = random.nextInt(5) - random.nextInt(5);
        int dz = random.nextInt(5) - random.nextInt(5);
        if (modifier == NodeModifier.PALE && random.nextBoolean() || dx == 0 && dy == 0 && dz == 0) {
            return false;
        }
        BlockPos otherPos = worldPosition.offset(dx, dy, dz);
        if (!level.isLoaded(otherPos) || !(level.getBlockEntity(otherPos) instanceof NodeBlockEntity other)
                || !other.isFreeNode() || other.vis.isEmpty() || other.lock > 0 || other.energized) {
            return false;
        }
        int otherAverage = (other.visSize() + other.baseSize()) / 2;
        int thisAverage = (visSize() + baseSize()) / 2;
        if (otherAverage >= thisAverage) {
            return false;
        }
        List<Aspect> otherAspects = other.getAspects();
        Aspect aspect = otherAspects.get(random.nextInt(otherAspects.size()));
        boolean taken = false;
        if (getAmount(aspect) < getBase(aspect) && other.takeFromContainer(aspect, 1)) {
            addToContainer(aspect, 1);
            taken = true;
        } else if (other.takeFromContainer(aspect, 1)) {
            if (random.nextInt(1 + (int) (getBase(aspect) / (shiny ? 1.5 : 1.0))) == 0) {
                base.merge(aspect, 1, Integer::sum);
                if (modifier == NodeModifier.PALE && random.nextInt(100) == 0) {
                    modifier = null;
                    regeneration = -1;
                }
                if (random.nextInt(3) == 0) {
                    other.setBase(aspect, other.getBase(aspect) - 1);
                }
            }
            taken = true;
        }
        if (taken) {
            other.wait = other.regeneration / 2;
            other.sync();
            // между узлами проскакивает молния (PacketFXBlockZap оригинала)
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                thaumcraft.common.network.FxPacket.bolt(server, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition),
                        net.minecraft.world.phys.Vec3.atCenterOf(other.getBlockPos()));
                server.playSound(null, worldPosition, thaumcraft.common.registry.ModSounds.ZAP.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.1f, 1f + server.random.nextFloat() * 0.2f);
            }
        }
        return taken;
    }

    private boolean handleRecharge(Level level) {
        boolean change = false;
        if (regeneration < 0) {
            regeneration = modifier == null ? 600 : switch (modifier) {
                case BRIGHT -> 400;
                case PALE -> 900;
                case FADING -> 0;
            };
            // под стабилизатором узел восстанавливается вдвое, под улучшенным — в двадцать раз медленнее
            if (lock == 1) {
                regeneration *= 2;
            } else if (lock == 2) {
                regeneration *= 20;
            }
        }

        RandomSource random = level.random;
        if (catchUp) {
            catchUp = false;
            // 75 мс на тик вместо обычных 50: без присмотра узел восстанавливается в полтора раза медленнее
            long interval = regeneration * 75L;
            long missed = interval > 0 ? (System.currentTimeMillis() - lastActive) / interval : 0;
            for (long i = 0; i < Math.min(missed, baseSize()); i++) {
                if (rechargeOne(random)) {
                    change = true;
                }
            }
        }

        // исчерпанный аспект постепенно теряет предел и может исчезнуть совсем, а узел — потускнеть
        if (count % 1200 == 0) {
            for (Aspect aspect : getAspects()) {
                if (getAmount(aspect) > 0) {
                    continue;
                }
                setBase(aspect, getBase(aspect) - 1);
                if (random.nextInt(20) == 0 || getBase(aspect) <= 0) {
                    vis.remove(aspect);
                    if (random.nextInt(5) == 0) {
                        if (modifier == NodeModifier.BRIGHT) {
                            modifier = null;
                        } else if (modifier == null) {
                            modifier = NodeModifier.PALE;
                        }
                        if (modifier == NodeModifier.PALE && random.nextInt(5) == 0) {
                            modifier = NodeModifier.FADING;
                        }
                    }
                    nodeChange();
                    break;
                }
                nodeChange();
            }
            if (vis.isEmpty()) {
                destroyNode(level);
                return false;
            }
        }

        if (wait > 0) {
            wait--;
        }
        if (regeneration > 0 && wait == 0 && count % regeneration == 0) {
            lastActive = System.currentTimeMillis();
            change |= rechargeOne(random);
        }
        return change;
    }

    private boolean rechargeOne(RandomSource random) {
        List<Aspect> depleted = new ArrayList<>();
        for (Aspect aspect : vis.keySet()) {
            if (getAmount(aspect) < getBase(aspect)) {
                depleted.add(aspect);
            }
        }
        if (depleted.isEmpty()) {
            return false;
        }
        addToContainer(depleted.get(random.nextInt(depleted.size())), 1);
        return true;
    }

    /** Узел, потерявший все аспекты, исчезает; сучок серебряного дерева становится обычным бревном. */
    private void destroyNode(Level level) {
        if (isFreeNode()) {
            level.removeBlock(worldPosition, false);
        } else {
            level.setBlock(worldPosition, ModBlocks.SILVERWOOD_LOG.get().defaultBlockState().setValue(
                    RotatedPillarBlock.AXIS, getBlockState().getValue(RotatedPillarBlock.AXIS)), 3);
        }
    }

    // ---------- сохранение и синхронизация ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByte("Type", (byte) type.ordinal());
        tag.putByte("Modifier", (byte) (modifier == null ? -1 : modifier.ordinal()));
        tag.put("Vis", writeMap(vis));
        tag.put("Base", writeMap(base));
        tag.putLong("LastActive", lastActive);
        tag.putBoolean("Energized", energized);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NodeType[] types = NodeType.values();
        type = types[Math.floorMod(tag.getByte("Type"), types.length)];
        byte mod = tag.getByte("Modifier");
        modifier = mod >= 0 && mod < NodeModifier.values().length ? NodeModifier.values()[mod] : null;
        readMap(tag.getList("Vis", Tag.TAG_COMPOUND), vis);
        readMap(tag.getList("Base", Tag.TAG_COMPOUND), base);
        lastActive = tag.getLong("LastActive");
        energized = tag.getBoolean("Energized");
        catchUp = lastActive > 0;
    }

    private static ListTag writeMap(Map<Aspect, Integer> map) {
        ListTag list = new ListTag();
        map.forEach((aspect, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", amount);
            list.add(entry);
        });
        return list;
    }

    private static void readMap(ListTag list, Map<Aspect, Integer> map) {
        map.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Aspect aspect = Aspect.byTag(entry.getString("key"));
            if (aspect != null) {
                map.put(aspect, entry.getInt("amount"));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Свечение узла заметно больше самого блока. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }
}
