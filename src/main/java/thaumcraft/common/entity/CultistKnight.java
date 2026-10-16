package thaumcraft.common.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import thaumcraft.common.registry.ModItems;

/**
 * Рыцарь культа — перенос EntityCultistKnight оригинала: латы культа и железный меч (изредка тауминиевый или
 * пустотный — тогда вместо шлема капюшон клирика), иногда зачарованный.
 */
public class CultistKnight extends Cultist {
    public CultistKnight(EntityType<? extends CultistKnight> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createCultistAttributes().add(Attributes.MAX_HEALTH, 36.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void equip(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CULTIST_PLATE_HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CULTIST_PLATE_CHESTPLATE.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CULTIST_PLATE_LEGGINGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CULTIST_BOOTS.get()));
        if (random.nextFloat() < (level().getDifficulty() == Difficulty.HARD ? 0.05f : 0.01f)) {
            if (random.nextInt(5) == 0) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.VOID_SWORD.get()));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CULTIST_ROBE_HELMET.get()));
            } else {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.THAUMIUM_SWORD.get()));
                if (random.nextBoolean()) {
                    setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        // enchantEquipment оригинала: с вероятностью 25% от «напряжённости» места
        float tension = difficulty.getSpecialMultiplier();
        ItemStack held = getMainHandItem();
        if (!held.isEmpty() && random.nextFloat() < 0.25f * tension) {
            setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(random, held, (int) (5f + tension * random.nextInt(18)), false));
        }
    }
}
