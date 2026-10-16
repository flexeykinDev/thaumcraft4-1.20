package thaumcraft.common.items.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.TravelingTrunk;
import thaumcraft.common.registry.ModEntities;

import java.util.List;

/** Дорожный сундук в виде предмета (ItemTrunkSpawner оригинала): помнит улучшение, а с улучшением порядка — и содержимое. */
public class TrunkSpawnerItem extends Item {
    public TrunkSpawnerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        if (tag.contains("upgrade")) {
            byte upgrade = tag.getByte("upgrade");
            if (upgrade > -1 && upgrade < GolemUpgradeItem.NAMES.length) {
                tooltip.add(Component.translatable("item.thaumcraft.golem_upgrade_" + GolemUpgradeItem.NAMES[upgrade]).withStyle(ChatFormatting.BLUE));
            }
        }
        if (tag.contains("inventory")) {
            tooltip.add(Component.translatable("item.TrunkSpawner.text.1"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (spawn((ServerLevel) level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getItemInHand(), player) != null
                && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    public static TravelingTrunk spawn(ServerLevel level, double x, double y, double z, ItemStack stack, Player player) {
        TravelingTrunk trunk = ModEntities.TRAVELING_TRUNK.get().create(level);
        if (trunk == null) {
            return null;
        }
        float yaw = Mth.wrapDegrees(level.random.nextFloat() * 360f);
        trunk.moveTo(x, y, z, yaw, 0f);
        trunk.yHeadRot = yaw;
        trunk.yBodyRot = yaw;
        trunk.setOwner(player);
        if (stack.hasCustomHoverName()) {
            trunk.setCustomName(stack.getHoverName());
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("upgrade")) {
            trunk.setUpgrade(tag.getByte("upgrade"));
        }
        if (tag != null && tag.contains("inventory")) {
            Golem.loadInventory(trunk.inventory, tag.getList("inventory", Tag.TAG_COMPOUND));
        }
        level.addFreshEntity(trunk);
        return trunk;
    }
}
