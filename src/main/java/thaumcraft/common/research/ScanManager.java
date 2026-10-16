package thaumcraft.common.research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import thaumcraft.common.blockentity.NodeBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.ItemAspectManager;
import net.minecraftforge.network.PacketDistributor;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.NotificationPacket;
import thaumcraft.common.network.SyncKnowledgePacket;

import java.util.ArrayList;
import java.util.List;

/** Сканирование таумометром: поиск цели, проверка и начисление знаний. */
public final class ScanManager {
    private static final double ENTITY_RANGE = 10.0;
    private static final double BLOCK_RANGE = 5.0;
    /** Config.aspectTotalCap оригинала: после этого запаса очки аспекта копятся намного медленнее. */
    private static final int ASPECT_POOL_CAP = 100;

    /**
     * Цель сканирования.
     *
     * @param key     под этим ключом цель запоминается как изученная
     * @param aspects аспекты цели; пустые, если из неё нечего узнать
     * @param name    название для показа игроку
     * @param pos     точка в мире, где рисуются частицы сканирования
     * @param display что показывать в линзе таумометра, когда цель изучена (у узла — его текущий запас)
     */
    public record Target(String key, AspectList aspects, Component name, Vec3 pos, AspectList display) {
        public Target(String key, AspectList aspects, Component name, Vec3 pos) {
            this(key, aspects, name, pos, aspects);
        }
    }

    /** То, на что смотрит игрок: сначала существо в 10 блоках, затем блок или жидкость. */
    @Nullable
    public static Target findTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);

        Vec3 blockEnd = eye.add(look.scale(BLOCK_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, blockEnd,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;

        Vec3 entityEnd = eye.add(look.scale(ENTITY_RANGE));
        // существо за стеной не считается: луч обрывается на первом блоке
        BlockHitResult wall = player.level().clip(new ClipContext(eye, entityEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = wall.getType() == HitResult.Type.BLOCK ? wall.getLocation() : entityEnd;
        AABB box = player.getBoundingBox().expandTowards(look.scale(ENTITY_RANGE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, limit, box,
                e -> !e.isSpectator() && e.isPickable() || e instanceof ItemEntity, eye.distanceToSqr(limit));

        if (entityHit != null) {
            return targetForEntity(entityHit.getEntity());
        }
        if (hitBlock) {
            return forBlock(player.level(), blockHit.getBlockPos());
        }
        return null;
    }

    public static Target targetForEntity(Entity entity) {
        if (entity instanceof ItemEntity item) {
            return targetForItem(item.getItem(), item.position().add(0, 0.25, 0));
        }
        Vec3 center = entity.position().add(0, entity.getBbHeight() / 2, 0);
        if (entity instanceof Player other) {
            return new Target(playerKey(other), playerAspects(other),
                    other.getDisplayName(), center);
        }
        StringBuilder key = new StringBuilder("entity:").append(EntityType.getKey(entity.getType()));
        if (entity instanceof LivingEntity living && living.isBaby()) {
            key.append("|child");
        }
        if (entity instanceof Creeper creeper && creeper.isPowered()) {
            key.append("|powered");
        }
        return new Target(key.toString(), ItemAspectManager.INSTANCE.getEntityAspects(entity),
                entity.getDisplayName(), center);
    }

    /** Как в оригинале: у каждого игрока свой постоянный набор из трёх случайных аспектов. */
    private static AspectList playerAspects(Player player) {
        AspectList aspects = new AspectList().add(Aspect.MAN, 4);
        java.util.Random random = new java.util.Random(("player_" + player.getGameProfile().getName()).hashCode());
        Aspect[] all = Aspect.all().toArray(new Aspect[0]);
        for (int i = 0; i < 3; i++) {
            aspects.add(all[random.nextInt(all.length)], 4);
        }
        return aspects;
    }

    public static Target targetForItem(ItemStack stack, Vec3 pos) {
        return new Target(itemKey(stack), ItemAspectManager.INSTANCE.getAspects(stack), stack.getHoverName(), pos);
    }

    private static Target forBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof NodeBlockEntity node) {
            return forNode(level, pos, node);
        }
        ItemStack stack = state.getBlock().getCloneItemStack(level, pos, state);
        Vec3 center = Vec3.atCenterOf(pos);
        if (!stack.isEmpty()) {
            Target target = targetForItem(stack, center);
            if (!target.aspects().isEmpty()) {
                return target;
            }
        }
        return new Target("block:" + ForgeRegistries.BLOCKS.getKey(state.getBlock()),
                ItemAspectManager.INSTANCE.getBlockAspects(state.getBlock()), state.getBlock().getName(), center);
    }

    /** generateNodeAspects оригинала: знания дают десятую часть запаса узла, но не меньше 4 за аспект, плюс добавка за тип. */
    private static Target forNode(Level level, BlockPos pos, NodeBlockEntity node) {
        AspectList aspects = new AspectList();
        for (Aspect aspect : node.getAspects()) {
            aspects.merge(aspect, Math.max(4, node.getAmount(aspect) / 10));
        }
        switch (node.getNodeType()) {
            case UNSTABLE -> aspects.merge(Aspect.ENTROPY, 4);
            case HUNGRY -> aspects.merge(Aspect.HUNGER, 4);
            case TAINTED -> aspects.merge(Aspect.TAINT, 4);
            case PURE -> aspects.merge(Aspect.HEAL, 2).add(Aspect.ORDER, 2);
            case DARK -> aspects.merge(Aspect.DEATH, 2).add(Aspect.DARKNESS, 2);
            default -> {
            }
        }
        MutableComponent name = node.getNodeType().getName().copy();
        if (node.getNodeModifier() != null) {
            name.append(", ").append(node.getNodeModifier().getName());
        }
        String key = "node:" + level.dimension().location() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
        return new Target(key, aspects, name, Vec3.atCenterOf(pos), node.toAspectList());
    }

    public static String playerKey(Player player) {
        return "entity:player_" + player.getGameProfile().getName();
    }

    /** Ключ, под которым предмет запоминается как изученный. Нужен и клиенту — для подсказок. */
    public static String itemKey(ItemStack stack) {
        return "item:" + ForgeRegistries.ITEMS.getKey(stack.getItem());
    }

    /**
     * Завершает сканирование на сервере: проверяет, что игрок способен понять цель, запоминает её
     * и начисляет очки исследований по каждому аспекту.
     */
    public static void completeScan(ServerPlayer player, Target target) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        if (knowledge.hasScanned(target.key())) {
            return;
        }
        AspectList aspects = target.aspects();
        if (aspects.isEmpty()) {
            notify(player, Component.translatable("tc.unknownobject"));
            return;
        }
        for (Aspect aspect : aspects.getAspects()) {
            if (!knowledge.hasDiscoveredParents(aspect)) {
                Aspect missing = knowledge.hasDiscovered(aspect.getFirstComponent())
                        ? aspect.getSecondComponent() : aspect.getFirstComponent();
                notify(player, Component.translatable("tc.discoveryerror",
                        Component.translatable("tc.aspect.help." + missing.getTag())));
                return;
            }
        }

        knowledge.addScanned(target.key());
        List<NotificationPacket.Entry> notifications = new ArrayList<>();
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (knowledge.discover(aspect)) {
                amount += 2;
                notifications.add(new NotificationPacket.Entry(aspect,
                        Component.translatable("tc.addaspectdiscovery", aspect.getName()).withStyle(ChatFormatting.GOLD)));
            }
            int pool = knowledge.getPool(aspect);
            if (pool >= ASPECT_POOL_CAP) {
                amount = (int) Math.sqrt(amount);
            }
            if (amount > 1 && pool >= ASPECT_POOL_CAP * 1.25f) {
                amount = 1;
            }
            knowledge.addPool(aspect, amount);
            notifications.add(new NotificationPacket.Entry(aspect,
                    Component.translatable("tc.addaspectpool", amount, aspect.getName())));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new NotificationPacket(notifications));
        ResearchHelper.createClue(player, target.key(), aspects);

        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.2f, 0.5f + player.getRandom().nextFloat() * 0.2f);
        SyncKnowledgePacket.send(player);
    }

    private static void notify(ServerPlayer player, Component text) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new NotificationPacket(List.of(new NotificationPacket.Entry(null, text))));
    }

    private ScanManager() {
    }
}
