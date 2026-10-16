package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.entity.FollowingItem;
import thaumcraft.common.entity.golem.GolemWorkGoals;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModMaterials;
import thaumcraft.common.registry.ModSounds;

/**
 * Топор потока — перенос ItemElementalAxe оригинала. Срубая ствол, он снимает дерево с макушки: каждый раз ломается
 * самое дальнее бревно, а добыча сама плывёт к игроку. Если топор зажать, он притягивает лежащие вокруг предметы.
 */
public class ElementalAxeItem extends AxeItem {
    public ElementalAxeItem() {
        super(ModMaterials.ELEMENTAL_TOOL, 5f, -3f, new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, user.getBoundingBox().inflate(10.0))) {
            if (!item.isAlive() || item.distanceToSqr(user) > 100.0 || item instanceof FollowingItem following && following.isFollowing()) {
                continue;
            }
            double dx = item.getX() - user.getX();
            double dy = item.getY() - user.getY() + user.getBbHeight() / 2f;
            double dz = item.getZ() - user.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1.0e-4) {
                continue;
            }
            Vec3 motion = item.getDeltaMovement().subtract(dx / distance * 0.3, dy / distance * 0.3, dz / distance * 0.3);
            item.setDeltaMovement(Mth.clamp(motion.x, -0.35, 0.35), Mth.clamp(motion.y, -0.35, 0.35), Mth.clamp(motion.z, -0.35, 0.35));
            if (level.isClientSide) {
                Fx.crucibleBubble(item.getX() + (level.random.nextFloat() - level.random.nextFloat()) * 0.125f,
                        item.getY() + (level.random.nextFloat() - level.random.nextFloat()) * 0.125f,
                        item.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * 0.125f, 0.33f, 0.33f, 1f);
            }
        }
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (player.isShiftKeyDown() || !state.is(BlockTags.LOGS)) {
            return super.onBlockStartBreak(stack, pos, player);
        }
        if (level instanceof ServerLevel server) {
            ToolHelper.harvestBlock(level, player, GolemWorkGoals.furthestLog(level, pos, state.getBlock()), true, 10);
            FxPacket.send(server, FxPacket.BLOCK_BUBBLE, Vec3.atLowerCornerOf(pos), Vec3.ZERO, 0x5454FF, 0);
            level.playSound(null, pos, ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.15f, 1f);
            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }
}
