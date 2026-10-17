package thaumcraft.common.items.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Жезл — перенос ItemWandCasting оригинала. Стержень задаёт вместимость, набалдашники — расход.
 * Вис хранится в NBT по первичным аспектам в сотых долях: 2500 означает 25 единиц.
 */
public class WandItem extends Item {
    private static final String TAG_ROD = "rod";
    private static final String TAG_CAP = "cap";
    private static final String TAG_FOCUS = "focus";
    private static final String TAG_USE_X = "IIUX";
    private static final String TAG_USE_Y = "IIUY";
    private static final String TAG_USE_Z = "IIUZ";
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.##");
    /** Цвета первичных аспектов в тексте, как в оригинале. */
    private static final Map<Aspect, ChatFormatting> TEXT_COLORS = Map.of(
            Aspect.AIR, ChatFormatting.YELLOW, Aspect.EARTH, ChatFormatting.DARK_GREEN, Aspect.FIRE, ChatFormatting.RED,
            Aspect.WATER, ChatFormatting.DARK_AQUA, Aspect.ORDER, ChatFormatting.GRAY, Aspect.ENTROPY, ChatFormatting.DARK_GRAY);

    public static ChatFormatting textColor(Aspect aspect) {
        return TEXT_COLORS.getOrDefault(aspect, ChatFormatting.WHITE);
    }

    public WandItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    // ---------- части жезла ----------

    public static ItemStack create(Item wand, WandRod rod, WandCap cap, boolean full) {
        ItemStack stack = new ItemStack(wand);
        setRod(stack, rod);
        setCap(stack, cap);
        if (full) {
            for (Aspect aspect : Aspect.primals()) {
                storeVis(stack, aspect, getMaxVis(stack));
            }
        }
        return stack;
    }

    public static WandRod getRod(ItemStack stack) {
        WandRod rod = stack.hasTag() ? WandRod.byTag(stack.getTag().getString(TAG_ROD)) : null;
        return rod == null ? WandRod.WOOD : rod;
    }

    public static WandCap getCap(ItemStack stack) {
        WandCap cap = stack.hasTag() ? WandCap.byTag(stack.getTag().getString(TAG_CAP)) : null;
        return cap == null ? WandCap.IRON : cap;
    }

    public static void setRod(ItemStack stack, WandRod rod) {
        stack.getOrCreateTag().putString(TAG_ROD, rod.getTag());
    }

    public static void setCap(ItemStack stack, WandCap cap) {
        stack.getOrCreateTag().putString(TAG_CAP, cap.getTag());
    }

    // ---------- вис ----------

    /** Вместимость по одному аспекту в сотых долях. */
    public static int getMaxVis(ItemStack stack) {
        return getRod(stack).getCapacity() * (isSceptre(stack) ? 150 : 100);
    }

    /** Посох: длинный жезл на сердцевине посоха; бьёт больно, но в магический верстак не помещается. */
    public static boolean isStaff(ItemStack stack) {
        return getRod(stack).isStaff();
    }

    /** Скипетр: вмещает в полтора раза больше вис и тратит на десятую меньше, но не держит фокусы. */
    public static boolean isSceptre(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("sceptre");
    }

    public static void setSceptre(ItemStack stack) {
        stack.getOrCreateTag().putBoolean("sceptre", true);
    }

    private static final java.util.UUID STAFF_DAMAGE = java.util.UUID.fromString("7d0a5f2e-9f7c-4c0e-9d57-1c5a9d1d3b10");

    /** Как в оригинале: посох прибавляет 6 к урону в ближнем бою. */
    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
            net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND || !isStaff(stack)) {
            return super.getAttributeModifiers(slot, stack);
        }
        return com.google.common.collect.ImmutableMultimap.of(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(STAFF_DAMAGE, "Weapon modifier", 6.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }

    /** Запас аспекта в сотых долях. */
    public static int getVis(ItemStack stack, Aspect aspect) {
        return stack.hasTag() ? stack.getTag().getInt(aspect.getTag()) : 0;
    }

    public static void storeVis(ItemStack stack, Aspect aspect, int amount) {
        stack.getOrCreateTag().putInt(aspect.getTag(), amount);
    }

    /** Первичные аспекты, для которых в жезле ещё есть место. */
    public static List<Aspect> getAspectsWithRoom(ItemStack stack) {
        List<Aspect> out = new ArrayList<>();
        for (Aspect aspect : Aspect.primals()) {
            if (getVis(stack, aspect) < getMaxVis(stack)) {
                out.add(aspect);
            }
        }
        return out;
    }

    /**
     * Добавляет целые единицы вис.
     *
     * @return сколько единиц не поместилось
     */
    public static int addVis(ItemStack stack, Aspect aspect, int amount, boolean doIt) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int total = getVis(stack, aspect) + amount * 100;
        int leftover = Math.max(total - getMaxVis(stack), 0);
        if (doIt) {
            storeVis(stack, aspect, Math.min(total, getMaxVis(stack)));
        }
        return leftover / 100;
    }

    /** Множитель расхода: набалдашник минус скидки. Не опускается ниже 0.1. */
    public static float getConsumptionModifier(ItemStack stack, @Nullable Player player, Aspect aspect, boolean crafting) {
        float modifier = getCap(stack).getCostModifier(aspect);
        // флюсовая лихорадка и флюсовая зараза: +10% к цене за каждый уровень сильнейшей из них
        if (player != null) {
            int level = -1;
            var fever = player.getEffect(thaumcraft.common.registry.ModEffects.VIS_EXHAUSTION.get());
            var plague = player.getEffect(thaumcraft.common.registry.ModEffects.INFECTIOUS_VIS_EXHAUSTION.get());
            if (fever != null) {
                level = fever.getAmplifier();
            }
            if (plague != null) {
                level = Math.max(level, plague.getAmplifier());
            }
            modifier += (level + 1) * 0.1f;
        }
        if (isSceptre(stack)) {
            modifier -= 0.1f;
        }
        // улучшение фокуса «Бережливость»: по 10% за уровень, но не при работе в верстаке
        if (!crafting) {
            ItemStack focus = getFocusStack(stack);
            if (!focus.isEmpty()) {
                modifier -= FocusItem.getUpgradeLevel(focus, thaumcraft.api.wands.FocusUpgrade.FRUGAL) / 10f;
            }
        }
        if (player != null) {
            modifier -= getTotalVisDiscount(player, aspect);
        }
        return Math.max(modifier, 0.1f);
    }

    /** Сумма скидок надетых вещей: брони и украшений (WandManager.getTotalVisDiscount оригинала), в долях единицы. */
    public static float getTotalVisDiscount(Player player, Aspect aspect) {
        int total = 0;
        for (ItemStack bauble : thaumcraft.common.compat.Baubles.find(player, s -> s.getItem() instanceof thaumcraft.api.VisDiscountGear)) {
            total += ((thaumcraft.api.VisDiscountGear) bauble.getItem()).getVisDiscount(bauble, player, aspect);
        }
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof thaumcraft.api.VisDiscountGear gear) {
                total += gear.getVisDiscount(armor, player, aspect);
            }
        }
        return total / 100f;
    }

    /** Тратит вис одного аспекта (amount в сотых долях, до применения множителя). */
    public static boolean consumeVis(ItemStack stack, @Nullable Player player, Aspect aspect, int amount, boolean crafting) {
        int cost = (int) (amount * getConsumptionModifier(stack, player, aspect, crafting));
        if (getVis(stack, aspect) < cost) {
            return false;
        }
        storeVis(stack, aspect, getVis(stack, aspect) - cost);
        return true;
    }

    /** Тратит весь набор аспектов (в целых единицах вис) либо ничего, если хоть одного не хватает. */
    /**
     * createOculus оригинала: алтарь Элдрича с четырьмя очами и тёмным узлом над ним (при готовом лабиринте под
     * кольцом) за 100 каждого первичного виса раскрывается порталом в Запредельные земли.
     */
    private static InteractionResult createOculus(net.minecraft.world.item.context.UseOnContext context, Player player) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof thaumcraft.common.blocks.EldritchAltarBlock.Altar altar) || altar.eyes != 4 || altar.open
                || !(level.getBlockEntity(pos.above()) instanceof NodeBlockEntity node) || node.getNodeType() != thaumcraft.api.nodes.NodeType.DARK) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!altar.checkForMaze()) {
            return InteractionResult.FAIL;
        }
        thaumcraft.api.aspects.AspectList cost = new thaumcraft.api.aspects.AspectList();
        Aspect.primals().forEach(primal -> cost.add(primal, 100));
        if (!consumeAllVis(context.getItemInHand(), player, cost, true)) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, thaumcraft.common.registry.ModSounds.WAND.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
        altar.open = true;
        level.removeBlock(pos.above(), false);
        level.setBlock(pos.above(), ModBlocks.ELDRITCH_PORTAL.get().defaultBlockState(), 3);
        altar.setChanged();
        level.sendBlockUpdated(pos, context.getLevel().getBlockState(pos), context.getLevel().getBlockState(pos), 3);
        thaumcraft.common.ModAdvancements.grant(player, thaumcraft.common.ModAdvancements.OCULUS);
        return InteractionResult.CONSUME;
    }

    public static boolean consumeAllVis(ItemStack stack, @Nullable Player player, thaumcraft.api.aspects.AspectList cost,
                                        boolean crafting) {
        if (player != null && player.getAbilities().instabuild) {
            return true;
        }
        for (Aspect aspect : cost.getAspects()) {
            if (getVis(stack, aspect) < (int) (cost.getAmount(aspect) * 100 * getConsumptionModifier(stack, player, aspect, crafting))) {
                return false;
            }
        }
        for (Aspect aspect : cost.getAspects()) {
            consumeVis(stack, player, aspect, cost.getAmount(aspect) * 100, crafting);
        }
        return true;
    }

    /** То же для заклинаний фокуса: цена в сотых долях вис. При doIt = false только проверяет, хватит ли. */
    public static boolean consumeAllCentiVis(ItemStack stack, @Nullable Player player, thaumcraft.api.aspects.AspectList cost,
                                             boolean doIt) {
        if (cost == null || cost.isEmpty() || player != null && player.getAbilities().instabuild) {
            return true;
        }
        for (Aspect aspect : cost.getAspects()) {
            if (getVis(stack, aspect) < (int) (cost.getAmount(aspect) * getConsumptionModifier(stack, player, aspect, false))) {
                return false;
            }
        }
        if (doIt) {
            for (Aspect aspect : cost.getAspects()) {
                consumeVis(stack, player, aspect, cost.getAmount(aspect), false);
            }
        }
        return true;
    }

    // ---------- фокус ----------

    /** Фокус, надетый на жезл, либо пустой стек. */
    public static ItemStack getFocusStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_FOCUS)) {
            ItemStack focus = ItemStack.of(stack.getTag().getCompound(TAG_FOCUS));
            return focus.getItem() instanceof FocusItem ? focus : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    public static void setFocus(ItemStack stack, ItemStack focus) {
        if (focus.isEmpty()) {
            if (stack.hasTag()) {
                stack.getTag().remove(TAG_FOCUS);
            }
        } else {
            stack.getOrCreateTag().put(TAG_FOCUS, focus.save(new CompoundTag()));
        }
    }

    /** Сила заклинаний: уровни улучшения «Мощь» и ещё один, если по стержню бегут руны. */
    public static int getFocusPotency(ItemStack stack) {
        ItemStack focus = getFocusStack(stack);
        return focus.isEmpty() ? 0 : FocusItem.getUpgradeLevel(focus, thaumcraft.api.wands.FocusUpgrade.POTENCY)
                + (getRod(stack).hasRunes() ? 1 : 0);
    }

    /** Правая кнопка мимо «жезловых» блоков: срабатывает фокус, если прошла его пауза. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack focus = getFocusStack(stack);
        if (focus.getItem() instanceof FocusItem item && !FocusManager.isOnCooldown(player)) {
            FocusManager.setCooldown(player, item.getActivationCooldown(focus));
            if (item.onFocusRightClick(stack, level, player, hand)) {
                return net.minecraft.world.InteractionResultHolder.consume(stack);
            }
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        ItemStack focus = getFocusStack(stack);
        if (entity instanceof Player player && focus.getItem() instanceof FocusItem item) {
            item.onSwing(stack, player);
        }
        return false;
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack focus = getFocusStack(player.getMainHandItem());
        return !(focus.getItem() instanceof FocusItem item) || !item.preventsMining(focus);
    }

    // ---------- зарядка от узла ----------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof thaumcraft.api.wands.Wandable wandable) {
            return wandable.onWand(context);
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof thaumcraft.common.blocks.VisRelayBlock.Relay relay) {
            relay.cycleColor();
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.TABLE.get())) {
            return makeArcaneWorkbench(context, player);
        }
        if (context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                instanceof net.minecraft.world.level.block.AbstractCauldronBlock) {
            return makeCrucible(context);
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof thaumcraft.common.blockentity.CrucibleBlockEntity crucible) {
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (!context.getLevel().isClientSide) {
                crucible.spillRemnants();
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix) {
            if (context.getLevel().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!matrix.isActive()) {
                thaumcraft.api.aspects.AspectList cost = new thaumcraft.api.aspects.AspectList();
                Aspect.primals().forEach(primal -> cost.add(primal, 25));
                if (!matrix.validLocation() || !consumeAllVis(context.getItemInHand(), player, cost, true)) {
                    return InteractionResult.FAIL;
                }
            }
            return matrix.onWand(player) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof thaumcraft.common.blocks.TubeBlock.Sided) {
            // как в оригинале: по отводу — закрыть или открыть его, по середине — повернуть трубу;
            // у буфера Shift по отводу «придушивает» эту сторону
            Level tubeLevel = context.getLevel();
            BlockPos tubePos = context.getClickedPos();
            if (!tubeLevel.isClientSide) {
                net.minecraft.core.Direction part = thaumcraft.common.blocks.TubeBlock.hitPart(tubePos, context.getClickLocation());
                net.minecraft.world.level.block.entity.BlockEntity entity = tubeLevel.getBlockEntity(tubePos);
                boolean squeek = false;
                if (entity instanceof thaumcraft.common.blockentity.EssentiaBufferBlockEntity buffer) {
                    net.minecraft.core.Direction side = part != null ? part : context.getClickedFace();
                    if (player.isShiftKeyDown()) {
                        buffer.cycleChoke(side);
                        squeek = true;
                    } else {
                        thaumcraft.common.blocks.TubeBlock.toggleSide(tubeLevel, tubePos, side);
                    }
                } else if (part != null) {
                    thaumcraft.common.blocks.TubeBlock.toggleSide(tubeLevel, tubePos, part);
                } else if (entity instanceof thaumcraft.common.blockentity.TubeBlockEntity tube) {
                    tube.rotate();
                }
                tubeLevel.playSound(null, tubePos, squeek ? thaumcraft.common.registry.ModSounds.SQUEEK.get()
                                : thaumcraft.common.registry.ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.5f,
                        (squeek ? 1.1f : 0.9f) + tubeLevel.random.nextFloat() * 0.2f);
            }
            return InteractionResult.sidedSuccess(tubeLevel.isClientSide);
        }
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        if ((clickedState.is(Blocks.OBSIDIAN) || clickedState.is(Blocks.NETHER_BRICKS) || clickedState.is(Blocks.IRON_BARS))
                && thaumcraft.common.research.PlayerKnowledge.forSide(player).isResearchComplete("INFERNALFURNACE")) {
            InteractionResult furnace = makeInfernalFurnace(context, player);
            if (furnace != InteractionResult.PASS) {
                return furnace;
            }
        }
        if ((clickedState.is(Blocks.GLASS) || clickedState.is(net.minecraft.tags.BlockTags.WOODEN_SLABS))
                && thaumcraft.common.research.PlayerKnowledge.forSide(player).isResearchComplete("NODEJAR")) {
            BlockPos node = thaumcraft.common.blocks.NodeJarBlock.find(context.getLevel(), context.getClickedPos());
            if (node != null) {
                if (context.getLevel().isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                thaumcraft.api.aspects.AspectList cost = new thaumcraft.api.aspects.AspectList();
                Aspect.primals().forEach(primal -> cost.add(primal, 70));
                if (!consumeAllVis(context.getItemInHand(), player, cost, true)) {
                    return InteractionResult.FAIL;
                }
                thaumcraft.common.blocks.NodeJarBlock.assemble((ServerLevel) context.getLevel(), node);
                return InteractionResult.CONSUME;
            }
        }
        if (context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.BOOKSHELF)) {
            return makeThaumonomicon(context);
        }
        if (clickedState.is(ModBlocks.ELDRITCH_ALTAR.get()) && thaumcraft.common.research.PlayerKnowledge.forSide(player).isResearchComplete("OCULUS")) {
            return createOculus(context, player);
        }
        if ((clickedState.is(ModBlocks.ALCHEMICAL_FURNACE.get()) || clickedState.is(ModBlocks.ALCHEMICAL_CONSTRUCT.get())
                || clickedState.is(ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get()))
                && thaumcraft.common.research.PlayerKnowledge.forSide(player).isResearchComplete("ADVALCHEMYFURNACE")) {
            BlockPos core = thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.find(context.getLevel(), context.getClickedPos());
            if (core != null) {
                if (context.getLevel().isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                thaumcraft.api.aspects.AspectList cost = new thaumcraft.api.aspects.AspectList().add(Aspect.FIRE, 50).add(Aspect.WATER, 50)
                        .add(Aspect.ORDER, 50);
                if (!consumeAllVis(context.getItemInHand(), player, cost, true)) {
                    return InteractionResult.FAIL;
                }
                thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.assemble((ServerLevel) context.getLevel(), core);
                return InteractionResult.CONSUME;
            }
        }
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.ALCHEMICAL_CONSTRUCT.get())) {
            return makeThaumatorium(context, player);
        }
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof NodeBlockEntity)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        tag.putInt(TAG_USE_X, pos.getX());
        tag.putInt(TAG_USE_Y, pos.getY());
        tag.putInt(TAG_USE_Z, pos.getZ());
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    /**
     * Как в оригинале: две алхимические конструкции над тиглем, которых коснулся жезл, становятся тауматорием.
     * Нужно исследование «Тауматорий» и вис: 15 огня, 30 порядка и 30 воды. Лицом он встаёт к той стороне, по которой щёлкнули.
     */
    private static InteractionResult makeThaumatorium(UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos base = thaumcraft.common.blocks.ThaumatoriumBlock.findStructure(level, context.getClickedPos());
        if (base == null || !thaumcraft.common.research.PlayerKnowledge.forSide(player).isResearchComplete("THAUMATORIUM")) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        thaumcraft.api.aspects.AspectList cost = new thaumcraft.api.aspects.AspectList()
                .add(Aspect.FIRE, 15).add(Aspect.ORDER, 30).add(Aspect.WATER, 30);
        if (!consumeAllVis(context.getItemInHand(), player, cost, true)) {
            return InteractionResult.FAIL;
        }
        net.minecraft.core.Direction facing = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace()
                : player.getDirection().getOpposite();
        thaumcraft.common.blocks.ThaumatoriumBlock.assemble((ServerLevel) level, base, facing);
        return InteractionResult.CONSUME;
    }

    /**
     * Как в оригинале: куб из незерского кирпича и обсидиана с лавой внутри и решёткой в боковой грани становится
     * адской печью. Нужно исследование «Адская печь» и вис: по 50 огня и земли.
     */
    private static InteractionResult makeInfernalFurnace(UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos origin = thaumcraft.common.blocks.InfernalFurnaceBlock.find(level, context.getClickedPos());
        if (origin == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        thaumcraft.api.aspects.AspectList cost = new thaumcraft.api.aspects.AspectList().add(Aspect.FIRE, 50).add(Aspect.EARTH, 50);
        if (!consumeAllVis(context.getItemInHand(), player, cost, true)) {
            return InteractionResult.FAIL;
        }
        thaumcraft.common.blocks.InfernalFurnaceBlock.assemble((ServerLevel) level, origin,
                thaumcraft.common.blocks.InfernalFurnaceBlock.frontOf(level, origin));
        return InteractionResult.CONSUME;
    }

    /** Как в оригинале: книжная полка, которой коснулся жезл, превращается в таумономикон. */
    private static InteractionResult makeThaumonomicon(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            ItemEntity book = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    new ItemStack(ModItems.THAUMONOMICON.get()));
            level.addFreshEntity(book);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1f);
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.5);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Как в оригинале: котёл, которого коснулся жезл, становится тиглем; то, что было в котле, пропадает. */
    private static InteractionResult makeCrucible(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            level.setBlock(pos, ModBlocks.CRUCIBLE.get().defaultBlockState(), 3);
            level.playSound(null, pos, thaumcraft.common.registry.ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
            if (context.getPlayer() != null) {
                thaumcraft.common.ModAdvancements.grant(context.getPlayer(), thaumcraft.common.ModAdvancements.CRUCIBLE);
            }
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.5);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Как в оригинале: стол становится магическим верстаком, а жезл ложится в его ячейку. */
    private static InteractionResult makeArcaneWorkbench(UseOnContext context, Player player) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            level.setBlock(context.getClickedPos(), ModBlocks.ARCANE_WORKBENCH.get().defaultBlockState(), 3);
            if (level.getBlockEntity(context.getClickedPos()) instanceof ArcaneWorkbenchBlockEntity workbench) {
                workbench.setWand(context.getItemInHand().copy());
                player.setItemInHand(context.getHand(), ItemStack.EMPTY);
            }
            level.playSound(null, context.getClickedPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1f);
            thaumcraft.common.ModAdvancements.grant(player, thaumcraft.common.ModAdvancements.ARCANE_WORKBENCH);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!stack.hasTag() || !stack.getTag().contains(TAG_USE_X)) {
            // жезл не привязан к узлу — значит, кнопку держат ради фокуса
            ItemStack focus = getFocusStack(stack);
            if (focus.getItem() instanceof FocusItem item && !FocusManager.isOnCooldown(player)) {
                FocusManager.setCooldown(player, item.getActivationCooldown(focus));
                item.onUsingFocusTick(stack, player, getUseDuration(stack) - remaining);
            }
            return;
        }
        CompoundTag tag = stack.getTag();
        BlockPos pos = new BlockPos(tag.getInt(TAG_USE_X), tag.getInt(TAG_USE_Y), tag.getInt(TAG_USE_Z));
        if (!(level.getBlockEntity(pos) instanceof NodeBlockEntity node) || node.isEnergized()) {
            player.stopUsingItem();
            return;
        }
        // зарядка идёт, только пока игрок смотрит на узел
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(8)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) {
            player.stopUsingItem();
            return;
        }
        int count = getUseDuration(stack) - remaining;
        if (level.isClientSide || count % 5 != 0) {
            return;
        }

        // исследования «Отвод узла» дают по лишней единице за раз, а «Сохранение узла» не даёт осушить аспект до нуля
        // (с простейшим жезлом — деревянный стержень или железные набалдашники — и крадучись оно не действует)
        thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
        int tap = 1;
        if (knowledge.isResearchComplete("NODETAPPER1")) {
            tap++;
        }
        if (knowledge.isResearchComplete("NODETAPPER2")) {
            tap++;
        }
        boolean preserve = !player.isShiftKeyDown() && knowledge.isResearchComplete("NODEPRESERVE")
                && !getRod(stack).getTag().equals("wood") && !getCap(stack).getTag().equals("iron");
        int min = preserve ? 1 : 0;
        List<Aspect> candidates = new ArrayList<>();
        for (Aspect aspect : getAspectsWithRoom(stack)) {
            if (node.getAmount(aspect) > min) {
                candidates.add(aspect);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Aspect aspect = candidates.get(level.random.nextInt(candidates.size()));
        tap = Math.min(tap, node.getAmount(aspect));
        if (preserve && tap == node.getAmount(aspect)) {
            tap--;
        }
        if (tap <= 0) {
            return;
        }
        int leftover = addVis(stack, aspect, tap, true);
        if (leftover < tap) {
            node.takeFromContainer(aspect, tap - leftover);
            node.nodeChange();
            spawnDrainParticles((ServerLevel) level, Vec3.atCenterOf(pos), player, aspect);
            thaumcraft.common.ModAdvancements.grant(player, thaumcraft.common.ModAdvancements.NODE);
        }
    }

    /** От узла к жезлу тянется колышущийся луч в цвете аспекта (drawFloatyLine оригинала). */
    private static void spawnDrainParticles(ServerLevel level, Vec3 from, Player player, Aspect aspect) {
        Vec3 look = player.getLookAngle();
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        // кончик жезла: чуть впереди и правее глаз
        Vec3 to = player.getEyePosition().add(look.scale(0.7)).add(side.scale(0.25)).add(0, -0.15, 0);
        thaumcraft.common.network.FxPacket.send(level, thaumcraft.common.network.FxPacket.FLOATY_BEAM, from, to, aspect.getColor(), 0);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        ItemStack focus = getFocusStack(stack);
        if (entity instanceof Player player && focus.getItem() instanceof FocusItem item
                && (!stack.hasTag() || !stack.getTag().contains(TAG_USE_X))) {
            item.onStoppedUsingFocus(stack, level, player, getUseDuration(stack) - timeLeft);
        }
        if (stack.hasTag()) {
            stack.getTag().remove(TAG_USE_X);
            stack.getTag().remove(TAG_USE_Y);
            stack.getTag().remove(TAG_USE_Z);
        }
    }

    /** Если использование оборвалось само (узел пропал, вис кончились), привязка к узлу тоже снимается. */
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (stack.hasTag()) {
            stack.getTag().remove(TAG_USE_X);
            stack.getTag().remove(TAG_USE_Y);
            stack.getTag().remove(TAG_USE_Z);
        }
    }

    /** Запас вис меняется каждые несколько тиков; без этого жезл в руке дёргался бы при каждом изменении. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    // ---------- название и подсказка ----------

    /** Стержни стихий сами восполняют немного вис своей стихии, первозданный посох — любой из шести. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        WandRod rod = getRod(stack);
        int floor = getMaxVis(stack) / 10;
        if (rod.rechargesAll()) {
            if (player.tickCount % 50 == 0) {
                List<Aspect> low = new ArrayList<>();
                for (Aspect primal : Aspect.primals()) {
                    if (getVis(stack, primal) < floor) {
                        low.add(primal);
                    }
                }
                if (!low.isEmpty()) {
                    addVis(stack, low.get(level.random.nextInt(low.size())), 1, true);
                }
            }
        } else if (rod.getRechargeAspect() != null && player.tickCount % 200 == 0 && getVis(stack, rod.getRechargeAspect()) < floor) {
            addVis(stack, rod.getRechargeAspect(), 1, true);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.Wand.name",
                Component.translatable("item.Wand." + getCap(stack).getTag() + ".cap"),
                Component.translatable("item.Wand." + getRod(stack).getTag().replace("_staff", "") + ".rod"),
                Component.translatable(isStaff(stack) ? "item.Wand.staff.obj" : isSceptre(stack) ? "item.Wand.sceptre.obj"
                        : "item.Wand.wand.obj"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean detailed = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        MutableComponent summary = Component.empty();
        int totalCost = 0;
        List<Component> lines = new ArrayList<>();
        List<Aspect> primals = Aspect.primals();
        for (Aspect aspect : primals) {
            String amount = FORMAT.format(getVis(stack, aspect) / 100f);
            float modifier = getConsumptionModifier(stack, null, aspect, false);
            totalCost += (int) (modifier * 100f);
            ChatFormatting color = TEXT_COLORS.getOrDefault(aspect, ChatFormatting.WHITE);
            if (detailed) {
                lines.add(Component.literal(" ").append(aspect.getName().copy().withStyle(color))
                        .append(" x " + amount + ", ")
                        .append(Component.literal("(" + FORMAT.format(modifier * 100f) + "% ")
                                .append(Component.translatable("tc.vis.cost")).append(")").withStyle(ChatFormatting.ITALIC))
                        .append(focusCostText(stack, aspect, modifier)));
            } else {
                if (!summary.getSiblings().isEmpty()) {
                    summary.append(" | ");
                }
                summary.append(Component.literal(amount).withStyle(color));
            }
        }
        MutableComponent capacity = Component.translatable("item.capacity.text").append(" " + getMaxVis(stack) / 100)
                .withStyle(ChatFormatting.GOLD);
        if (!detailed) {
            capacity.append(Component.literal(" (" + totalCost / primals.size() + "% ")
                    .append(Component.translatable("tc.vis.costavg")).append(")").withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(capacity);
        if (detailed) {
            tooltip.addAll(lines);
        } else {
            tooltip.add(summary);
        }
        ItemStack focus = getFocusStack(stack);
        if (!focus.isEmpty()) {
            tooltip.add(focus.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
            ItemStack picked = thaumcraft.common.items.wands.foci.TradeFocus.getPickedBlock(stack);
            if (focus.getItem() instanceof thaumcraft.common.items.wands.foci.TradeFocus && !picked.isEmpty()) {
                tooltip.add(Component.literal(" ").append(picked.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
            if (detailed) {
                FocusItem.addUpgradeLines(focus, tooltip);
            }
        }
    }

    /** Сколько вис этого аспекта стоит заклинание надетого фокуса — с учётом набалдашника. */
    private static Component focusCostText(ItemStack stack, Aspect aspect, float modifier) {
        ItemStack focus = getFocusStack(stack);
        if (!(focus.getItem() instanceof FocusItem item)) {
            return Component.empty();
        }
        thaumcraft.api.aspects.AspectList cost = item.getVisCost(focus);
        int amount = cost == null ? 0 : cost.getAmount(aspect);
        if (amount <= 0) {
            return Component.empty();
        }
        return Component.literal(", " + FORMAT.format(amount * modifier / 100f) + " ")
                .append(Component.translatable(item.isVisCostPerTick(focus) ? "item.Focus.cost2" : "item.Focus.cost1"));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return thaumcraft.client.render.WandItemRenderer.INSTANCE;
            }

            /** Пока жезл в деле, рука вытянута туда, куда смотрит игрок: это видно и со стороны. */
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(LivingEntity entity,
                    net.minecraft.world.InteractionHand hand, ItemStack stack) {
                return entity.isUsingItem() && entity.getUsedItemHand() == hand
                        ? thaumcraft.client.render.WandArmPose.CASTING : null;
            }
        });
    }
}
