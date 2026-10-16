package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.JarBlockItem;
import thaumcraft.common.items.tools.ToolHelper;
import thaumcraft.common.menu.HoverHarnessMenu;
import thaumcraft.common.registry.ModEnchantments;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;
import java.util.function.Consumer;

/**
 * Летательная упряжь — перенос ItemHoverHarness и Hover оригинала. В упряжь (щелчком в руке) вставляется банка с
 * Potentia; клавиша H включает парение, и игрок летает, как в творческом режиме, только медленнее. Одна единица
 * Potentia держит в воздухе 18 секунд (с поясом парения — 14, зато полёт быстрее). Кончилась эссенция — упряжь гаснет.
 */
public class HoverHarnessItem extends ArmorItem implements VisDiscountGear {
    /** Сколько тактов парения даёт одна единица Potentia (EFFICIENCY оригинала). */
    public static final int EFFICIENCY = 360;

    public HoverHarnessItem() {
        super(ModMaterials.SPECIAL_ARMOR, Type.CHESTPLATE, new Item.Properties().durability(400).rarity(Rarity.EPIC));
    }

    public static ItemStack getJar(ItemStack harness) {
        return harness.hasTag() && harness.getTag().contains("jar") ? ItemStack.of(harness.getTag().getCompound("jar")) : ItemStack.EMPTY;
    }

    public static void setJar(ItemStack harness, ItemStack jar) {
        if (jar.isEmpty()) {
            if (harness.hasTag()) {
                harness.getTag().remove("jar");
            }
        } else {
            harness.getOrCreateTag().put("jar", jar.save(new CompoundTag()));
        }
    }

    /** Сколько Potentia в банке упряжи. */
    public static int getFuel(ItemStack harness) {
        ItemStack jar = getJar(harness);
        return JarBlockItem.getAspect(jar) == Aspect.ENERGY ? JarBlockItem.getAmount(jar) : 0;
    }

    public static boolean isHovering(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof HoverHarnessItem && chest.hasTag() && chest.getTag().getBoolean("hover");
    }

    public static void setHovering(ItemStack harness, boolean hover) {
        harness.getOrCreateTag().putBoolean("hover", hover);
    }

    /** Пояс парения на поясе (слот Curios «belt»). */
    public static boolean hasGirdle(Player player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof HoverGirdleItem).isPresent()).orElse(false);
    }

    /** expendCharge оригинала: такт парения; раз в EFFICIENCY тактов банка теряет единицу Potentia. */
    private static boolean expendCharge(Player player, ItemStack harness) {
        int fuel = getFuel(harness);
        if (fuel <= 0) {
            return false;
        }
        float limit = EFFICIENCY * (hasGirdle(player) ? 0.8f : 1f);
        CompoundTag tag = harness.getOrCreateTag();
        int charge = tag.getShort("charge");
        if (charge < limit) {
            tag.putShort("charge", (short) (charge + 1));
            return true;
        }
        tag.putShort("charge", (short) 0);
        fuel--;
        ItemStack jar = getJar(harness);
        CompoundTag data = jar.getOrCreateTagElement("BlockEntityTag");
        if (fuel > 0) {
            data.putShort("Amount", (short) fuel);
        } else {
            jar.removeTagKey("BlockEntityTag");
        }
        setJar(harness, jar);
        return fuel > 0;
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean hover = stack.hasTag() && stack.getTag().getBoolean("hover");
        boolean powered = hover && (level.isClientSide ? getFuel(stack) > 0 : expendCharge(player, stack));
        if (hover && !powered && !level.isClientSide) {
            setHovering(stack, false);
            hover = false;
        }
        if (hover && powered) {
            // полёт дала упряжь: только его она потом и отнимет, чужой (от других модов) не трогает
            player.getPersistentData().putBoolean("thaumcraftHover", true);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.fallDistance = 0f;
            if (player instanceof ServerPlayer server) {
                ToolHelper.resetFloatCounter(server);
            }
            if (level.isClientSide) {
                // парение медленнее творческого полёта; «Спешка» и пояс парения его ускоряют
                int haste = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.HASTE.get(), stack);
                float mod = Math.min(1f, 0.7f + 0.075f * haste + (hasGirdle(player) ? 0.21f : 0f));
                player.setDeltaMovement(player.getDeltaMovement().multiply(mod, 1.0, mod));
                HoverClient.tick(player);
            }
        } else {
            player.fallDistance *= 0.75f;
        }
    }

    /** Упряжь снята или погасла: игрок перестаёт летать. */
    static void land(Player player) {
        if (!player.getPersistentData().getBoolean("thaumcraftHover")) {
            return;
        }
        player.getPersistentData().remove("thaumcraftHover");
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            NetworkHooks.openScreen(server, new SimpleMenuProvider((id, inventory, p) -> new HoverHarnessMenu(id, inventory, slot),
                    stack.getHoverName()), buf -> buf.writeInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.GOLD_INGOT) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return aspect == Aspect.AIR ? 5 : 2;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumcraft:textures/models/hoverharness.png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int fuel = getFuel(stack);
        if (fuel > 0) {
            tooltip.add(Component.literal(Aspect.ENERGY.getName() + " x " + fuel));
        }
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tc.visdiscount").append(" (Aer): " + getVisDiscount(stack, null, Aspect.AIR) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                                    HumanoidModel<?> original) {
                return thaumcraft.client.render.HoverHarnessLayer.bodyModel();
            }
        });
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class Events {
        private Events() {
        }

        /** В воздухе на упряжи копается так же быстро, как на земле. */
        @SubscribeEvent
        public static void breakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            if (!player.onGround() && isHovering(player)) {
                event.setNewSpeed(event.getOriginalSpeed() * 5f);
            }
        }

        /** Сняли упряжь или погасили её в полёте — полёт кончился. */
        @SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !isHovering(event.player)) {
                land(event.player);
            }
        }
    }
}
