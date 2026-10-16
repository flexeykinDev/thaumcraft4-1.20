package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

import java.util.List;
import java.util.function.Consumer;

/** ItemJarNode оригинала: банка с узлом в инвентаре — в подсказке вид узла, его свойство и аспекты. */
public class NodeJarItem extends BlockItem {
    public NodeJarItem(Block block) {
        super(block, new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.hasTag()) {
            return;
        }
        var tag = stack.getTag();
        NodeType type = NodeType.values()[Math.max(0, Math.min(NodeType.values().length - 1, tag.getByte("type")))];
        Component line = type.getName().copy();
        int mod = tag.getByte("modifier");
        if (mod >= 0 && mod < NodeModifier.values().length) {
            line = line.copy().append(", ").append(NodeModifier.values()[mod].getName());
        }
        tooltip.add(line.copy().withStyle(ChatFormatting.BLUE));
        AspectList aspects = AspectList.load(tag.getCompound("Aspects"));
        boolean client = level != null && level.isClientSide;
        for (Aspect aspect : aspects.getAspectsSortedByName()) {
            boolean known = !client || thaumcraft.client.ClientKnowledge.get().hasDiscovered(aspect);
            tooltip.add(known ? aspect.getName().copy().append(" x " + aspects.getAmount(aspect)) : Component.translatable("tc.aspect.unknown"));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return thaumcraft.client.render.NodeJarRenderer.ITEM;
            }
        });
    }
}
