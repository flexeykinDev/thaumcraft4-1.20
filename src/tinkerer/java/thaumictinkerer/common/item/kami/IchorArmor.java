package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.Revealer;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.focus.TTFoci;
import thaumictinkerer.common.registry.TTBlocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ихоровая мантия (ItemIchorclothArmor): защита как у алмазной, не изнашивается, скидка на вис 4% (сапоги — 3%).
 * Пробуждённые части (ItemGem…) дают силы, которые выключаются по отдельности Shift+ПКМ, а все сразу — клавишей
 * (KamiArmorHandler): капюшон — откровение, дыхание и зрение под водой, лечение на неполной сытости; мантия — полёт и
 * защита от снарядов; поножи — огнестойкость, свет вокруг и впереди, огонь лечит; сапоги — спешка, шаг на блок, разгон,
 * высокий прыжок, без урона от падения, земля под ногами зарастает травой.
 */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class IchorArmor {
    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 3;
                case CHESTPLATE -> 8;
                case LEGGINGS -> 6;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 20;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return ThaumicTinkerer.MODID + ":ichor";
        }

        @Override
        public float getToughness() {
            return 2f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    };

    private static final String COMPOUND = "ThaumicTinkerer";
    private static final String TAG_STATUS = "GemArmor";
    private static final String TAG_DISABLED = "disabled";
    private static final UUID STEP = UUID.fromString("5b0a5b3e-6a3e-4b6c-9a4f-1e2d3c4b5a69");
    /** На ком сейчас включён полёт мантии (playersWithFlight оригинала). */
    private static final Set<UUID> FLYING = new HashSet<>();
    private static final Set<UUID> FLYING_CLIENT = new HashSet<>();
    /** Состояние клавиши на этом клиенте. */
    public static boolean clientEnabled = true;

    private IchorArmor() {
    }

    /** getArmorStatus: силы пробуждённой брони у игрока включены (по умолчанию да). */
    public static boolean enabled(Player player) {
        if (player.level().isClientSide) {
            return clientEnabled;
        }
        CompoundTag data = player.getPersistentData().getCompound(COMPOUND);
        return !data.contains(TAG_STATUS) || data.getBoolean(TAG_STATUS);
    }

    public static void setEnabled(Player player, boolean status) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(COMPOUND)) {
            root.put(COMPOUND, new CompoundTag());
        }
        root.getCompound(COMPOUND).putBoolean(TAG_STATUS, status);
    }

    private static boolean active(Player player, EquipmentSlot slot, Class<? extends Gem> type) {
        ItemStack stack = player.getItemBySlot(slot);
        return type.isInstance(stack.getItem()) && !Gem.disabled(stack) && enabled(player);
    }

    public static class Cloth extends ArmorItem implements VisDiscountGear {
        public Cloth(Type type) {
            super(MATERIAL, type, new Item.Properties().rarity(Kami.RARITY).fireResistant());
        }

        @Override
        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return ThaumicTinkerer.MODID + ":textures/models/armor/" + layer() + (slot == EquipmentSlot.LEGS ? "_layer_2" : "_layer_1") + ".png";
        }

        String layer() {
            return "ichor";
        }

        @Override
        public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
            return getType() == Type.BOOTS ? 3 : 4;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** ItemIchorclothArmorAdv: Shift+ПКМ выключает и включает силы этой части. */
    public static class Gem extends Cloth {
        public Gem(Type type) {
            super(type);
        }

        public static boolean disabled(ItemStack stack) {
            return stack.hasTag() && stack.getTag().getBoolean(TAG_DISABLED);
        }

        @Override
        String layer() {
            return "ichor_gem";
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!player.isShiftKeyDown()) {
                return super.use(level, player, hand);
            }
            if (!level.isClientSide) {
                stack.getOrCreateTag().putBoolean(TAG_DISABLED, !disabled(stack));
                player.displayClientMessage(Component.translatable("ttmisc.awakenedArmor" + (disabled(stack) ? 1 : 0)), true);
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            if (disabled(stack)) {
                tooltip.add(Component.translatable("ttmisc.awakenedArmor1").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static class GemHelm extends Gem implements Revealer, thaumcraft.api.Goggles {
        public GemHelm() {
            super(Type.HELMET);
        }

        @Override
        public boolean showNodes(ItemStack stack, LivingEntity wearer) {
            return true;
        }

        @Override
        public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
            return true;
        }
    }

    public static class GemChest extends Gem {
        public GemChest() {
            super(Type.CHESTPLATE);
        }
    }

    public static class GemLegs extends Gem {
        public GemLegs() {
            super(Type.LEGGINGS);
        }
    }

    public static class GemBoots extends Gem {
        public GemBoots() {
            super(Type.BOOTS);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        boolean server = !player.level().isClientSide;
        if (active(player, EquipmentSlot.HEAD, GemHelm.class) && server) {
            if (player.isUnderWater()) {
                player.setAirSupply(player.getMaxAirSupply());
                refresh(player, new MobEffectInstance(MobEffects.NIGHT_VISION, 202, 0, true, false));
            }
            int food = player.getFoodData().getFoodLevel();
            if (food > 0 && food < 18 && player.isHurt() && player.tickCount % 80 == 0) {
                player.heal(1f);
            }
        }
        // полёт мантии: включается, пока она надета и не выключена, и снимается, когда её сняли
        boolean flight = active(player, EquipmentSlot.CHEST, GemChest.class);
        Set<UUID> flying = server ? FLYING : FLYING_CLIENT;
        if (flight) {
            if (flying.add(player.getUUID()) || !player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            if (server) {
                TTFoci.Deflect.protect(player);
            }
        } else if (flying.remove(player.getUUID()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (active(player, EquipmentSlot.LEGS, GemLegs.class) && server) {
            refresh(player, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 202, 10, true, false));
            light(player);
        }
        AttributeInstance step = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        boolean boots = active(player, EquipmentSlot.FEET, GemBoots.class);
        if (step != null) {
            boolean want = boots && !player.isShiftKeyDown();
            if (want && step.getModifier(STEP) == null) {
                step.addTransientModifier(new AttributeModifier(STEP, "ichor boots step", 0.5, AttributeModifier.Operation.ADDITION));
            } else if (!want && step.getModifier(STEP) != null) {
                step.removeModifier(STEP);
            }
        }
        if (boots) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2, 1, true, false));
            if ((player.onGround() || player.getAbilities().flying) && player.zza > 0) {
                player.moveRelative(player.getAbilities().flying ? 0.075f : 0.15f, new Vec3(0, 0, 1));
            }
            player.fallDistance = 0;
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 1, player.getZ());
            if (server && player.level().getBlockState(below).is(Blocks.DIRT)) {
                player.level().setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void refresh(Player player, MobEffectInstance effect) {
        MobEffectInstance current = player.getEffect(effect.getEffect());
        if (current == null || current.getDuration() <= 202) {
            player.addEffect(effect);
        }
    }

    /** Свет поножей: нитор над головой и ещё пять огоньков в ту сторону, куда смотрит игрок. */
    private static void light(Player player) {
        Level level = player.level();
        int x = Mth.floor(player.getX());
        int y = (int) player.getY() + 1;
        int z = Mth.floor(player.getZ());
        place(level, new BlockPos(x, y, z));
        float angle = Mth.wrapDegrees(player.getYRot() + 90f) * Mth.DEG_TO_RAD;
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        for (int i = 1; i <= 5; i++) {
            place(level, new BlockPos(x + (int) (dx * (i + 1)), y, z + (int) (dz * (i + 1))));
        }
    }

    private static void place(Level level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(TTBlocks.NITOR_GAS.get())) {
            level.setBlock(pos, TTBlocks.NITOR_GAS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Поножи: урон огнём не ранит, а лечит. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide
                && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && active(player, EquipmentSlot.LEGS, GemLegs.class)) {
            event.setCanceled(true);
            player.heal(event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && active(player, EquipmentSlot.FEET, GemBoots.class)) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.3, 0));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof GemBoots) {
            event.setDistance(0);
        }
    }
}
