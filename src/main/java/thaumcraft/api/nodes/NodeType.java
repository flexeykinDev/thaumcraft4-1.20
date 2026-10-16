package thaumcraft.api.nodes;

import net.minecraft.network.chat.Component;

/** Тип узла ауры. Порядок как в оригинале: по нему тип записывается в сохранение. */
public enum NodeType {
    NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY, PURE;

    public Component getName() {
        return Component.translatable("nodetype." + name() + ".name");
    }
}
