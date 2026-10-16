package thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Брызги слизи порчи (FXBreaking оригинала с текстурой слизи): осколок слизистого шарика, перекрашенный в лиловый.
 * Живёт долго — от трёх до тридцати секунд, — так что брызги остаются лежать на земле.
 */
public class SplooshParticle extends BreakingItemParticle {
    SplooshParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, float alpha) {
        super(level, x, y, z, new ItemStack(Items.SLIME_BALL));
        setColor(r, g, b);
        this.alpha = alpha;
        this.lifetime = (int) (66f / (random.nextFloat() * 0.9f + 0.1f));
    }

    SplooshParticle motion(double dx, double dy, double dz) {
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        return this;
    }
}
