package thaumictinkerer.common.item.focus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModSounds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Фокусы Thaumic Tinkerer: ItemFocusFlight, Smelt, Telekinesis, Deflect, Heal, Dislocation и EnderChest оригинала. */
public final class TTFoci {
    private TTFoci() {
    }

    private static boolean pay(ItemStack wand, Player player, AspectList cost, boolean doIt) {
        return WandItem.consumeAllCentiVis(wand, player, cost, doIt && !player.level().isClientSide);
    }

    private static void sparkles(Level level, Vec3 at, int type) {
        if (level instanceof ServerLevel server) {
            FxPacket.send(server, FxPacket.SPARKLES, at, Vec3.ZERO, type, 30);
        }
    }

    @Nullable
    private static BlockHitResult target(Player player, double reach) {
        HitResult hit = player.pick(reach, 1f, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block : null;
    }

    /** Полёт: рывок по направлению взгляда, падение не засчитывается. */
    public static class Flight extends TTFocus {
        public Flight() {
            super("focus_flight", 0x9EF1FF, new AspectList().add(Aspect.AIR, 15), false, "orn");
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            if (pay(wand, player, getVisCost(WandItem.getFocusStack(wand)), true)) {
                double speed = 2 / 3.0 * (1 + getUpgradeLevel(WandItem.getFocusStack(wand), FocusUpgrade.POTENCY) * 0.2);
                player.setDeltaMovement(player.getLookAngle().scale(speed));
                player.fallDistance = 0;
                player.hurtMarked = true;
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WIND.get(), SoundSource.PLAYERS, 0.4f, 1f);
                sparkles(level, player.position(), 1);
            }
            player.swing(hand);
            return true;
        }

        @Override
        public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return new FocusUpgrade[]{FocusUpgrade.TREASURE, FocusUpgrade.POTENCY};
        }
    }

    /** Переплавка: пока луч держат на блоке, он плавится в печной результат, если тот — блок. */
    public static class Smelt extends TTFocus {
        private static final Map<UUID, Object[]> PROGRESS = new HashMap<>();

        public Smelt() {
            super("focus_smelt", 0xFF0000, new AspectList().add(Aspect.FIRE, 45).add(Aspect.ENTROPY, 12), false, "orn");
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            Level level = player.level();
            ItemStack focus = WandItem.getFocusStack(wand);
            BlockHitResult hit = target(player, 6);
            if (level.isClientSide || hit == null || !pay(wand, player, getVisCost(focus), false)) {
                return;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            SimpleContainer input = new SimpleContainer(new ItemStack(state.getBlock()));
            ItemStack result = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level)
                    .map(recipe -> recipe.getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
            if (!(result.getItem() instanceof BlockItem block)) {
                PROGRESS.remove(player.getUUID());
                return;
            }
            Object[] data = PROGRESS.get(player.getUUID());
            if (data == null || !pos.equals(data[0])) {
                int potency = Math.min(3, getUpgradeLevel(focus, FocusUpgrade.POTENCY));
                PROGRESS.put(player.getUUID(), new Object[]{pos, 20 - potency * 5});
                return;
            }
            int left = (int) data[1] - 1;
            data[1] = left;
            if (count % 3 == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.2f, 1f);
            }
            sparkles(level, Vec3.atCenterOf(pos), 4);
            if (left <= 0) {
                level.setBlock(pos, block.getBlock().defaultBlockState(), Block.UPDATE_ALL);
                pay(wand, player, getVisCost(focus), true);
                PROGRESS.remove(player.getUUID());
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.6f, 1f);
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1f, 1f);
            }
        }

        @Override
        public void onStoppedUsingFocus(ItemStack wand, Level level, Player player, int count) {
            PROGRESS.remove(player.getUUID());
        }

        @Override
        public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return new FocusUpgrade[]{FocusUpgrade.TREASURE, FocusUpgrade.POTENCY};
        }
    }

    /** Телекинез: притягивает выпавшие предметы к точке в пяти блоках перед собой (с Shift — к себе). */
    public static class Telekinesis extends TTFocus {
        public Telekinesis() {
            super("focus_telekinesis", 0x9C00BE, new AspectList().add(Aspect.AIR, 5).add(Aspect.ENTROPY, 5), true, "orn");
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            if (player.level().isClientSide) {
                return;
            }
            Vec3 point = player.getBoundingBox().getCenter();
            if (!player.isShiftKeyDown()) {
                point = point.add(player.getLookAngle().scale(5));
            }
            point = point.add(0, 0.5, 0);
            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(point, point).inflate(6));
            if (items.isEmpty() || !pay(wand, player, getVisCost(WandItem.getFocusStack(wand)), true)) {
                return;
            }
            for (ItemEntity item : items) {
                Vec3 pull = point.subtract(item.position());
                item.setDeltaMovement(pull.length() > 1 ? pull.normalize().scale(1 / 3.0) : pull.scale(1 / 3.0));
                if (count % 5 == 0) {
                    sparkles(player.level(), item.position(), 0);
                }
            }
        }

        @Override
        public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return new FocusUpgrade[0];
        }
    }

    /** Отражение: снаряды, летящие в держащего, разворачиваются от него. */
    public static class Deflect extends TTFocus {
        public Deflect() {
            super("focus_deflect", 0xFFFFFF, new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 4), true, null);
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            if (!player.level().isClientSide && pay(wand, player, getVisCost(WandItem.getFocusStack(wand)), true)) {
                protect(player);
            }
        }

        public static void protect(Player player) {
            AABB box = new AABB(player.getX() - 4, player.getY() - 4, player.getZ() - 4, player.getX() + 3, player.getY() + 3,
                    player.getZ() + 3);
            for (Entity entity : player.level().getEntitiesOfClass(Entity.class, box, e -> e instanceof Projectile)) {
                Projectile projectile = (Projectile) entity;
                if (projectile.getOwner() == player || projectile instanceof ThrownExperienceBottle) {
                    continue;
                }
                Vec3 away = projectile.position().subtract(player.getBoundingBox().getCenter()).normalize();
                double speed = Math.max(0.3, projectile.getDeltaMovement().length());
                projectile.setDeltaMovement(away.scale(speed));
                projectile.setPos(projectile.position().add(away));
                projectile.hurtMarked = true;
                sparkles(player.level(), projectile.position(), 6);
            }
        }
    }

    /** Лечение: пока держат, раз в полторы секунды (быстрее с «Мощью») возвращает полсердца. */
    public static class Heal extends TTFocus {
        private static final Map<UUID, Integer> PROGRESS = new HashMap<>();

        public Heal() {
            super("focus_heal", 0xFD00D6, new AspectList().add(Aspect.EARTH, 45).add(Aspect.WATER, 45), false, "depth:9");
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            if (player.level().isClientSide || !player.isHurt() || !pay(wand, player, getVisCost(WandItem.getFocusStack(wand)), false)) {
                return;
            }
            int potency = getUpgradeLevel(WandItem.getFocusStack(wand), FocusUpgrade.POTENCY);
            int progress = PROGRESS.merge(player.getUUID(), 1, Integer::sum);
            if (progress >= 30 - potency * 10 / 3) {
                PROGRESS.put(player.getUUID(), 0);
                pay(wand, player, getVisCost(WandItem.getFocusStack(wand)), true);
                player.heal(1f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WAND.get(), SoundSource.PLAYERS, 0.5f, 1f);
                sparkles(player.level(), player.position().add(0, 1, 0), 2);
            }
        }

        @Override
        public void onStoppedUsingFocus(ItemStack wand, Level level, Player player, int count) {
            PROGRESS.remove(player.getUUID());
        }

        @Override
        public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return new FocusUpgrade[]{FocusUpgrade.TREASURE, FocusUpgrade.POTENCY};
        }
    }

    /** Эндер-сундук: открывает эндер-сундук игрока где угодно. */
    public static class EnderChest extends TTFocus {
        public EnderChest() {
            super("focus_ender_chest", 0x132223, new AspectList().add(Aspect.ENTROPY, 100).add(Aspect.ORDER, 100), false, "depth:1");
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer && pay(wand, player, getVisCost(WandItem.getFocusStack(wand)), true)) {
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
                        Component.translatable("container.enderchest")));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
            }
            return true;
        }
    }

    /**
     * Перемещение: первый щелчок забирает блок вместе с содержимым (сундук с вещами, печь) в фокус, второй — ставит
     * его на новое место. Блок с содержимым стоит в пять раз дороже, рассадник — в двадцать.
     */
    public static class Dislocation extends TTFocus {
        private static final AspectList COST = new AspectList().add(Aspect.ENTROPY, 500).add(Aspect.ORDER, 500).add(Aspect.EARTH, 100);
        private static final AspectList COST_TILE = new AspectList().add(Aspect.ENTROPY, 2500).add(Aspect.ORDER, 2500).add(Aspect.EARTH, 500);
        private static final AspectList COST_SPAWNER = new AspectList().add(Aspect.ENTROPY, 10000).add(Aspect.ORDER, 10000)
                .add(Aspect.EARTH, 5000);

        public Dislocation() {
            super("focus_dislocation", 0xFFB400, COST, false, "orn");
        }

        @Nullable
        public static BlockState picked(ItemStack focus) {
            return focus.hasTag() && focus.getTag().contains("Picked")
                    ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), focus.getTag().getCompound("Picked")) : null;
        }

        @Override
        public String getSortingHelper(ItemStack focus) {
            BlockState state = picked(focus);
            return super.getSortingHelper(focus) + (state == null ? "" : BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            BlockHitResult hit = target(player, 5);
            if (hit == null) {
                return false;
            }
            player.swing(hand);
            if (level.isClientSide) {
                return true;
            }
            ItemStack focus = WandItem.getFocusStack(wand);
            BlockState held = picked(focus);
            BlockPos pos = hit.getBlockPos();
            if (held != null) {
                BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, hand, hit));
                BlockPos place = context.getClickedPos();
                if (!context.canPlace() || !held.canSurvive(level, place) || !player.mayUseItemAt(place, hit.getDirection(), wand)) {
                    return true;
                }
                level.setBlock(place, held, Block.UPDATE_ALL);
                CompoundTag data = focus.getTag().getCompound("Tile");
                BlockEntity tile = level.getBlockEntity(place);
                if (tile != null && !data.isEmpty()) {
                    tile.load(data);
                    tile.setChanged();
                }
                focus.getTag().remove("Picked");
                focus.getTag().remove("Tile");
                WandItem.setFocus(wand, focus);
                FxPacket.send((ServerLevel) level, FxPacket.BURST, Vec3.atCenterOf(place), Vec3.ZERO, 0, 0);
                level.playSound(null, place, ModSounds.WAND.get(), SoundSource.PLAYERS, 0.5f, 1f);
                return true;
            }
            BlockState state = level.getBlockState(pos);
            BlockEntity tile = level.getBlockEntity(pos);
            AspectList cost = tile == null ? COST : tile instanceof SpawnerBlockEntity ? COST_SPAWNER : COST_TILE;
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || state.is(Blocks.PISTON_HEAD) || state.is(Blocks.MOVING_PISTON)
                    || !player.mayUseItemAt(pos, hit.getDirection(), wand) || !pay(wand, player, cost, true)) {
                return true;
            }
            focus.getOrCreateTag().put("Picked", NbtUtils.writeBlockState(state));
            focus.getTag().put("Tile", tile == null ? new CompoundTag() : tile.saveWithoutMetadata());
            WandItem.setFocus(wand, focus);
            if (tile != null) {
                // содержимое уехало в фокус — блок не должен высыпать вещи
                level.removeBlockEntity(pos);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            FxPacket.send((ServerLevel) level, FxPacket.BURST, Vec3.atCenterOf(pos), Vec3.ZERO, 0, 0);
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
            level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.PLAYERS, 0.5f, 1f);
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            BlockState state = picked(stack);
            if (state != null) {
                tooltip.add(Component.translatable("ttmisc.focusDislocation.tooltip").append(": ").append(state.getBlock().getName())
                        .withStyle(ChatFormatting.GOLD));
                if (stack.getTag().contains("Tile") && !stack.getTag().getCompound("Tile").isEmpty()) {
                    tooltip.add(Component.translatable("ttmisc.focusDislocation.tooltipExtra").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }
}
