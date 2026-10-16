package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ManaPodBlock;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Зерно маны (ItemManaBean оригинала): плод стручка маны, несёт единицу одного аспекта. Его можно посадить под бревном
 * в волшебном биоме, растворить в тигле или съесть — тогда оно даст случайный эффект зелья, а в четверти случаев
 * ещё и единицу своего аспекта в запас исследователя.
 */
public class ManaBeanItem extends Item {
    private static final String TAG_ASPECT = "Aspect";

    public ManaBeanItem() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(0.5f).meat().alwaysEat().fast().build()));
    }

    public static ItemStack of(Item bean, Aspect aspect) {
        ItemStack stack = new ItemStack(bean);
        stack.getOrCreateTag().putString(TAG_ASPECT, aspect.getTag());
        return stack;
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        return stack.getItem() instanceof ManaBeanItem && stack.hasTag() ? Aspect.byTag(stack.getTag().getString(TAG_ASPECT)) : null;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 10;
    }

    /** Зерно без аспекта (выданное командой или из творческого режима) получает случайный, едва попав к кому-нибудь. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && getAspect(stack) == null) {
            List<Aspect> all = new ArrayList<>(Aspect.all());
            stack.getOrCreateTag().putString(TAG_ASPECT, all.get(level.random.nextInt(all.size())).getTag());
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            List<MobEffect> effects = new ArrayList<>(ForgeRegistries.MOB_EFFECTS.getValues());
            MobEffect effect = effects.get(level.random.nextInt(effects.size()));
            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(entity, entity, entity, 2, 3.0);
            } else {
                entity.addEffect(new MobEffectInstance(effect, 160 + level.random.nextInt(80), 0));
            }
            Aspect aspect = getAspect(stack);
            if (aspect != null && entity instanceof ServerPlayer player && level.random.nextFloat() < 0.25f) {
                PlayerKnowledge.get(player).addPool(aspect, 1);
                thaumcraft.common.network.SyncKnowledgePacket.send(player);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** Посадка: щелчок по нижней грани бревна в волшебном биоме. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos log = context.getClickedPos();
        BlockPos below = log.below();
        if (context.getClickedFace() != Direction.DOWN || !level.getBlockState(log).is(BlockTags.LOGS)
                || !level.getBiome(below).is(Tags.Biomes.IS_MAGICAL)) {
            return InteractionResult.PASS;
        }
        if (level.isEmptyBlock(below)) {
            if (!level.isClientSide) {
                level.setBlock(below, ModBlocks.MANA_POD.get().defaultBlockState(), 3);
                if (level.getBlockEntity(below) instanceof ManaPodBlock.Pod pod) {
                    pod.aspect = getAspect(context.getItemInHand());
                    pod.setChanged();
                }
                Player player = context.getPlayer();
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = getAspect(stack);
        if (aspect != null && level != null && level.isClientSide) {
            boolean known = thaumcraft.client.ClientKnowledge.get().hasDiscovered(aspect);
            tooltip.add((known ? aspect.getName().copy().append(" x1") : Component.translatable("tc.aspect.unknown"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
