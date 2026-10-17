package thaumcraft.common.items.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.GolemMarker;
import thaumcraft.common.entity.golem.GolemType;
import thaumcraft.common.registry.ModEntities;

import java.util.List;

/**
 * Голем в виде предмета (ItemGolemPlacer оригинала). Поставленный у блока, голем считает этот блок домом.
 * Снятый колокольчиком голем уносит в предмете ядро, улучшения, украшения, отметки и образцы.
 */
public class GolemPlacerItem extends Item {
    private final GolemType type;

    public GolemPlacerItem(GolemType type) {
        super(new Item.Properties().stacksTo(1));
        this.type = type;
    }

    public GolemType getGolemType() {
        return type;
    }

    public static boolean isAdvanced(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("advanced");
    }

    public static boolean hasCore(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("core");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        if (tag.contains("core")) {
            tooltip.add(Component.translatable("item.ItemGolemCore.name").append(": ")
                    .append(GolemCoreItem.coreName(tag.getByte("core")).copy().withStyle(ChatFormatting.GOLD)));
        }
        if (tag.contains("advanced")) {
            tooltip.add(Component.translatable("tc.adv"));
        }
        if (tag.contains("upgrades")) {
            MutableComponent text = Component.empty();
            boolean any = false;
            for (byte upgrade : tag.getByteArray("upgrades")) {
                if (upgrade > -1 && upgrade < GolemUpgradeItem.NAMES.length) {
                    text.append(Component.translatable("item.thaumcraft.golem_upgrade_" + GolemUpgradeItem.NAMES[upgrade])).append(" ");
                    any = true;
                }
            }
            if (any) {
                tooltip.add(text.withStyle(ChatFormatting.BLUE));
            }
        }
        if (tag.contains("markers")) {
            tooltip.add(Component.literal(tag.getList("markers", Tag.TAG_COMPOUND).size() + " ")
                    .append(Component.translatable("tc.markedloc")).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (tag.contains("deco")) {
            String deco = tag.getString("deco");
            MutableComponent text = Component.empty();
            String letters = "HGBFRVP";
            for (int i = 0; i < letters.length(); i++) {
                if (deco.indexOf(letters.charAt(i)) >= 0) {
                    text.append(Component.translatable("item.ItemGolemDecoration." + i + ".name")).append(" ");
                }
            }
            tooltip.add(text.withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    /** Как в оригинале: с зажатым Shift голема не ставят, чтобы можно было класть предмет в сундуки и на пьедесталы. */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Direction side = context.getClickedFace();
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = clicked.relative(side);
        // на заборе голем встаёт на полблока выше
        double lift = side == Direction.UP && level.getBlockState(clicked).is(BlockTags.FENCES) ? 0.5 : 0.0;
        if (spawn((ServerLevel) level, pos.getX() + 0.5, pos.getY() + lift, pos.getZ() + 0.5, side, stack, player) != null
                && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    public Golem spawn(ServerLevel level, double x, double y, double z, Direction side, ItemStack stack, Player player) {
        Golem golem = ModEntities.GOLEM.get().create(level);
        if (golem == null) {
            return null;
        }
        CompoundTag tag = stack.getOrCreateTag();
        golem.configure(type, tag.contains("advanced"));
        golem.moveTo(x, y, z, level.random.nextFloat() * 360f, 0f);
        golem.setHome(BlockPos.containing(x, y, z));
        if (tag.contains("core")) {
            golem.setCore(tag.getByte("core"));
        }
        if (tag.contains("upgrades")) {
            golem.loadUpgrades(tag.getByteArray("upgrades"));
        }
        if (tag.contains("deco")) {
            golem.setGolemDecoration(tag.getString("deco"));
        }
        golem.setup(side.get3DDataValue());
        golem.setHealth(golem.getMaxHealth());
        golem.setOwner(player);
        if (tag.contains("core")) {
            thaumcraft.common.ModAdvancements.grant(player, thaumcraft.common.ModAdvancements.GOLEM);
        }
        if (tag.contains("markers")) {
            golem.setMarkers(GolemMarker.load(tag, "markers"));
        }
        if (stack.hasCustomHoverName()) {
            golem.setCustomName(stack.getHoverName());
        }
        if (tag.contains("Inventory")) {
            Golem.loadInventory(golem.inventory, tag.getList("Inventory", Tag.TAG_COMPOUND));
        }
        if (stack.getTag() != null && stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
        level.addFreshEntity(golem);
        golem.playAmbientSound();
        return golem;
    }
}
