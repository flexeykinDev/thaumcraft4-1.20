package thaumcraft.common.items.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.GolemMarker;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

import java.util.List;
import java.util.UUID;

/**
 * Колокольчик големанта (ItemGolemBell оригинала). Щелчок по голему привязывает к нему колокольчик; после этого
 * щелчки по блокам ставят и снимают отметки — места, с которыми голем работает. Удар колокольчиком снимает голема
 * в предмет (с Shift — разбирает: ядро и часть улучшений выпадают отдельно).
 */
public class GolemBellItem extends Item {
    private static final String[] LINK_TAGS = {"golem", "markers", "golemhomex", "golemhomey", "golemhomez", "golemhomeface"};

    public GolemBellItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Nullable
    public static UUID getGolemId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("golem") ? stack.getTag().getUUID("golem") : null;
    }

    public static int getGolemHomeFace(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("golemhomeface") ? stack.getTag().getInt("golemhomeface") : -1;
    }

    @Nullable
    public static BlockPos getGolemHome(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("golemhomex")
                ? new BlockPos(tag.getInt("golemhomex"), tag.getInt("golemhomey"), tag.getInt("golemhomez")) : null;
    }

    public static List<GolemMarker> getMarkers(ItemStack stack) {
        return stack.hasTag() ? GolemMarker.load(stack.getTag(), "markers") : new java.util.ArrayList<>();
    }

    @Nullable
    private static Golem findGolem(ItemStack stack, Level level) {
        UUID id = getGolemId(stack);
        return id != null && level instanceof ServerLevel server && server.getEntity(id) instanceof Golem golem && golem.isAlive() ? golem
                : null;
    }

    private static void unlink(ItemStack stack) {
        if (stack.hasTag()) {
            for (String key : LINK_TAGS) {
                stack.getTag().remove(key);
            }
        }
    }

    /** Клавиша смены фокуса с колокольчиком в руке снимает все отметки голема. */
    public static void resetMarkers(ItemStack stack, ServerPlayer player) {
        Golem golem = findGolem(stack, player.level());
        if (golem != null) {
            stack.getOrCreateTag().put("markers", new ListTag());
            golem.setMarkers(List.of());
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f,
                    1f + player.level().random.nextFloat() * 0.1f);
        }
    }

    /**
     * Ставит отметку на сторону блока или снимает её. Голему с улучшением порядка можно задавать цвет отметки:
     * повторные щелчки перебирают шестнадцать цветов, Shift снимает отметку сразу.
     */
    public static void changeMarkers(ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        List<GolemMarker> markers = getMarkers(stack);
        Golem golem = findGolem(stack, level);
        boolean colours = golem != null && golem.getUpgradeAmount(Golem.UPGRADE_ORDER) > 0;
        String dimension = level.dimension().location().toString();
        int count = markers.size();
        int index = -1;
        int color = -1;
        for (int candidate = -1; candidate < (colours ? 16 : 0); candidate++) {
            index = markers.indexOf(new GolemMarker(pos, dimension, (byte) side.get3DDataValue(), (byte) candidate));
            color = candidate;
            if (index != -1) {
                break;
            }
        }
        if (index >= 0) {
            markers.remove(index);
            if (colours && !player.isShiftKeyDown()) {
                color++;
                if (color <= 15) {
                    markers.add(new GolemMarker(pos, dimension, (byte) side.get3DDataValue(), (byte) color));
                    count++;
                    if (player instanceof ServerPlayer serverPlayer) {
                        thaumcraft.common.network.ModNetwork.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new thaumcraft.common.network.NotificationPacket(List.of(new thaumcraft.common.network.NotificationPacket.Entry(
                                        null, Component.translatable("tc.markerchange",
                                        Component.translatable("color.minecraft." + DyeColor.byId(color).getName()))))));
                    }
                }
            }
        } else {
            markers.add(new GolemMarker(pos, dimension, (byte) side.get3DDataValue(), (byte) -1));
        }
        if (count != markers.size() || index >= 0) {
            stack.getOrCreateTag().put("markers", GolemMarker.save(markers));
            if (getGolemId(stack) != null) {
                if (golem != null) {
                    golem.setMarkers(markers);
                } else {
                    // голема больше нет: колокольчик забывает его
                    unlink(stack);
                }
            }
        }
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1f + level.random.nextFloat() * 0.1f);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            changeMarkers(stack, context.getPlayer(), context.getLevel(), context.getClickedPos(), context.getClickedFace());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** Щелчок по голему: колокольчик запоминает его, его дом и его отметки. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Golem golem)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            // в творческом режиме сюда приходит копия предмета, поэтому правим тот, что в руке
            ItemStack held = player.getItemInHand(hand);
            unlink(held);
            CompoundTag tag = held.getOrCreateTag();
            tag.put("markers", GolemMarker.save(golem.getMarkers()));
            tag.putUUID("golem", golem.getUUID());
            tag.putInt("golemhomex", golem.getHome().getX());
            tag.putInt("golemhomey", golem.getHome().getY());
            tag.putInt("golemhomez", golem.getHome().getZ());
            tag.putInt("golemhomeface", golem.homeFacing);
            golem.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7f, 1f + golem.getRandom().nextFloat() * 0.1f);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /** Удар колокольчиком снимает голема. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof thaumcraft.common.entity.golem.TravelingTrunk trunk && trunk.isAlive()) {
            if (trunk.refuses(player)) {
                return false;
            }
            if (trunk.level().isClientSide) {
                trunk.spawnAnim();
                return false;
            }
            pickUpTrunk(trunk, player.isShiftKeyDown());
            return true;
        }
        if (!(entity instanceof Golem golem) || !golem.isAlive()) {
            return false;
        }
        if (golem.level().isClientSide) {
            golem.spawnAnim();
            return false;
        }
        pickUp(golem, player.isShiftKeyDown());
        return true;
    }

    /**
     * Превращает дорожный сундук в предмет. Содержимое уезжает в предмете только с улучшением порядка, иначе высыпается.
     * dismantle — разобрать: улучшение (с шансом в половину) выпадает отдельно, а сундук остаётся пустым и простым.
     */
    public static ItemStack pickUpTrunk(thaumcraft.common.entity.golem.TravelingTrunk trunk, boolean dismantle) {
        int upgrade = trunk.getUpgrade();
        ItemStack dropped = new ItemStack(ModItems.TRAVELING_TRUNK.get());
        if (dismantle) {
            if (upgrade > -1 && trunk.getRandom().nextBoolean()) {
                trunk.spawnAtLocation(new ItemStack(ModItems.GOLEM_UPGRADES.get(upgrade).get()), 0.5f);
            }
        } else {
            if (trunk.hasCustomName()) {
                dropped.setHoverName(trunk.getCustomName());
            }
            dropped.getOrCreateTag().putByte("upgrade", (byte) upgrade);
            if (upgrade == Golem.UPGRADE_ORDER) {
                dropped.getTag().put("inventory", Golem.saveInventory(trunk.inventory));
            }
        }
        trunk.spawnAtLocation(dropped, 0.5f);
        if (upgrade != Golem.UPGRADE_ORDER || dismantle) {
            trunk.dropAllItems();
        }
        trunk.playSound(ModSounds.ZAP.get(), 0.5f, 1f);
        trunk.discard();
        return dropped;
    }

    /** Превращает голема в предмет. dismantle — разобрать: ядро и (с шансом в половину) улучшения выпадают отдельно. */
    public static ItemStack pickUp(Golem golem, boolean dismantle) {
        ItemStack dropped = new ItemStack(ModItems.GOLEMS.get(golem.getGolemType().ordinal()).get());
        CompoundTag tag = dropped.getOrCreateTag();
        if (golem.isAdvanced()) {
            tag.putByte("advanced", (byte) 1);
        }
        byte core = golem.getCore();
        byte[] upgrades = golem.getUpgrades();
        if (dismantle) {
            if (core > -1) {
                golem.spawnAtLocation(new ItemStack(ModItems.GOLEM_CORES.get(core).get()), 0.5f);
            }
            for (byte upgrade : upgrades) {
                if (upgrade > -1 && golem.getRandom().nextBoolean()) {
                    golem.spawnAtLocation(new ItemStack(ModItems.GOLEM_UPGRADES.get(upgrade).get()), 0.5f);
                }
            }
        } else {
            if (golem.hasCustomName()) {
                dropped.setHoverName(golem.getCustomName());
            }
            if (!golem.getGolemDecoration().isEmpty()) {
                tag.putString("deco", golem.getGolemDecoration());
            }
            if (core > -1) {
                tag.putByte("core", core);
            }
            tag.putByteArray("upgrades", upgrades);
            tag.put("markers", GolemMarker.save(golem.getMarkers()));
            tag.put("Inventory", Golem.saveInventory(golem.inventory));
        }
        if (tag.isEmpty()) {
            dropped.setTag(null);
        }
        golem.spawnAtLocation(dropped, 0.5f);
        if (!golem.getCarried().isEmpty()) {
            golem.spawnAtLocation(golem.getCarried(), 0.5f);
        }
        golem.playSound(ModSounds.ZAP.get(), 0.5f, 1f);
        golem.discard();
        return dropped;
    }
}
