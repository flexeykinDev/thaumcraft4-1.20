package thaumcraft.client;

import thaumcraft.common.research.PlayerKnowledge;

/** Копия знаний игрока на клиенте, присланная сервером. */
public final class ClientKnowledge {
    private static final PlayerKnowledge KNOWLEDGE = new PlayerKnowledge();

    public static PlayerKnowledge get() {
        return KNOWLEDGE;
    }

    private ClientKnowledge() {
    }
}
