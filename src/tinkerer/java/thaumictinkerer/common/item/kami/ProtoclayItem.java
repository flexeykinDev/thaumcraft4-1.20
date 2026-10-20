package thaumictinkerer.common.item.kami;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Протоглина (ItemProtoclay): пока она в инвентаре, пробуждённый инструмент в руке сам меняется на подходящий к блоку —
 * кирку, лопату или топор, — если такой тоже лежит в инвентаре.
 */
public class ProtoclayItem extends Item {
    public ProtoclayItem() {
        super(new Item.Properties().stacksTo(1).rarity(Kami.RARITY));
    }

    private static String type(Item item) {
        return item instanceof IchorTools.AdvPick ? "pick" : item instanceof IchorTools.AdvShovel ? "shovel"
                : item instanceof IchorTools.AdvAxe ? "axe" : item instanceof IchorTools.AdvSword ? "sword" : "";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || !player.swinging) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack held = player.getMainHandItem();
        String heldType = type(held.getItem());
        if (heldType.isEmpty() || heldType.equals("sword")) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(4.5)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = level.getBlockState(hit.getBlockPos());
        String want = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "pick" : state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "shovel"
                : state.is(BlockTags.MINEABLE_WITH_AXE) ? "axe" : "";
        if (want.isEmpty() || want.equals(heldType)) {
            return;
        }
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack other = inventory.items.get(i);
            if (other != held && type(other.getItem()).equals(want)) {
                inventory.items.set(inventory.selected, other);
                inventory.items.set(i, held);
                return;
            }
        }
    }
}
