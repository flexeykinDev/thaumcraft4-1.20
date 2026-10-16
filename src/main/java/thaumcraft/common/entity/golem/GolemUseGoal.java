package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * AIUseItem оригинала: голем подходит к отмеченному блоку и щёлкает по отмеченной стороне предметом, который несёт
 * (или пустой рукой, если дома нет хранилища). Переключатели окна: блок или пустое место, правый или левый
 * щелчок, крадучись или нет.
 */
public class GolemUseGoal extends Goal {
    private final Golem golem;
    private final Level level;
    private BlockPos pos = BlockPos.ZERO;
    private BlockState state = Blocks.AIR.defaultBlockState();
    private int count;
    private byte color = -1;
    private int nextTick;

    public GolemUseGoal(Golem golem) {
        this.golem = golem;
        this.level = golem.level();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        nextTick = golem.tickCount + golem.getRandom().nextInt(6);
    }

    /** Без домашнего хранилища голем работает пустой рукой. */
    private boolean ignoreItem() {
        return !GolemHelper.isContainer(level, GolemHelper.homeBlock(golem));
    }

    @Override
    public boolean canUse() {
        if ((golem.getCarried().isEmpty() && !ignoreItem()) || golem.tickCount < nextTick || !golem.getNavigation().isDone()) {
            return false;
        }
        nextTick = golem.tickCount + Math.max(1, 5 - golem.tickCount) * 3;
        GolemMarker marker = GolemHelper.useTarget(golem, golem.getCarried());
        if (marker == null) {
            return false;
        }
        color = marker.color();
        pos = marker.pos();
        state = level.getBlockState(pos);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return level.getBlockState(pos) == state && count-- > 0 && !golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        count = 200;
        GolemHelper.walkTo(golem, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Override
    public void stop() {
        count = 0;
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        golem.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30f, 30f);
        if (golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 4.0) {
            click();
        }
    }

    private void click() {
        List<Direction> sides = GolemHelper.markedSides(golem, pos, color);
        if (sides.isEmpty() || !(level instanceof ServerLevel server)) {
            stop();
            return;
        }
        boolean ignoreItem = ignoreItem();
        if (golem.getCarried().isEmpty() && !ignoreItem) {
            stop();
            return;
        }
        Direction side = sides.get(0);
        // у отметки на пустом месте щёлкаем по блоку за ней, с той же стороны
        BlockPos clicked = level.isEmptyBlock(pos) ? pos.relative(side.getOpposite()) : pos;
        FakePlayer player = FakePlayerFactory.getMinecraft(server);
        player.moveTo(golem.getX(), golem.getY(), golem.getZ(), golem.getYRot(), golem.getXRot());
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, golem.getCarried());
        player.setShiftKeyDown(golem.getToggles()[2]);
        try {
            if (golem.getToggles()[1]) {
                golem.startLeftArmTimer();
                player.gameMode.handleBlockBreakAction(clicked, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side,
                        level.getMaxBuildHeight(), 0);
            } else {
                InteractionResult result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(clicked).add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.5)), side, clicked, false));
                if (result.consumesAction()) {
                    golem.startRightArmTimer();
                }
            }
            ItemStack left = player.getMainHandItem();
            // всё лишнее, что оказалось у «руки» после щелчка (пустое ведро, бутылка), голем берёт себе или роняет
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack extra = player.getInventory().getItem(slot);
                if (extra.isEmpty() || extra == left) {
                    continue;
                }
                if (left.isEmpty()) {
                    left = extra.copy();
                } else {
                    golem.spawnAtLocation(extra.copy(), 0.5f);
                }
            }
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            golem.setCarried(left.isEmpty() ? ItemStack.EMPTY : left);
        } catch (RuntimeException failed) {
            player.getInventory().clearContent();
        }
        stop();
    }
}
