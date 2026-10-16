package thaumcraft.api.nodes;

import net.minecraft.network.chat.Component;

/** Яркость узла: от неё зависит скорость подзарядки. У обычного узла модификатора нет. */
public enum NodeModifier {
    BRIGHT, PALE, FADING;

    public Component getName() {
        return Component.translatable("nodemod." + name() + ".name");
    }
}
