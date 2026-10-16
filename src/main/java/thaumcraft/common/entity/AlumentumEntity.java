package thaumcraft.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;

/** Брошенный алюментум (EntityAlumentum оригинала): взрывается при ударе. */
public class AlumentumEntity extends ThrowableItemProjectile {
    public AlumentumEntity(EntityType<? extends AlumentumEntity> type, Level level) {
        super(type, level);
    }

    public AlumentumEntity(Level level, LivingEntity thrower) {
        super(ModEntities.ALUMENTUM.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ALUMENTUM.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // как в оригинале: дымные тёмные огоньки на месте и по следу, плюс белые искры
            for (int i = 0; i < 3; i++) {
                thaumcraft.client.fx.Fx.wisp(getX() + (random.nextFloat() - random.nextFloat()) * 0.3f,
                        getY() + (random.nextFloat() - random.nextFloat()) * 0.3f,
                        getZ() + (random.nextFloat() - random.nextFloat()) * 0.3f, 0.3f, 5, true, true, 0.02f);
                thaumcraft.client.fx.Fx.wisp((getX() + xo) / 2 + (random.nextFloat() - random.nextFloat()) * 0.3f,
                        (getY() + yo) / 2 + (random.nextFloat() - random.nextFloat()) * 0.3f,
                        (getZ() + zo) / 2 + (random.nextFloat() - random.nextFloat()) * 0.3f, 0.3f, 5, true, true, 0.02f);
                thaumcraft.client.fx.Fx.sparkle(getX() + (random.nextFloat() - random.nextFloat()) * 0.1f,
                        getY() + (random.nextFloat() - random.nextFloat()) * 0.1f,
                        getZ() + (random.nextFloat() - random.nextFloat()) * 0.1f, 1f, 6, 0f);
            }
        }
    }

    /** Сила взрыва 1.66, как в оригинале; блоки ломаются, только если разрешено правилом mobGriefing. */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(null, getX(), getY(), getZ(), 1.66f, Level.ExplosionInteraction.MOB);
            discard();
        }
    }
}
