package thaumcraft.common.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import thaumcraft.Thaumcraft;

public final class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Thaumcraft.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncItemAspectsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncItemAspectsPacket::encode)
                .decoder(SyncItemAspectsPacket::decode)
                .consumerMainThread(SyncItemAspectsPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncKnowledgePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncKnowledgePacket::encode)
                .decoder(SyncKnowledgePacket::decode)
                .consumerMainThread(SyncKnowledgePacket::handle)
                .add();
        CHANNEL.messageBuilder(NotificationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NotificationPacket::encode)
                .decoder(NotificationPacket::decode)
                .consumerMainThread(NotificationPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncResearchPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncResearchPacket::encode)
                .decoder(SyncResearchPacket::decode)
                .consumerMainThread(SyncResearchPacket::handle)
                .add();
        CHANNEL.messageBuilder(PurchaseResearchPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PurchaseResearchPacket::encode)
                .decoder(PurchaseResearchPacket::decode)
                .consumerMainThread(PurchaseResearchPacket::handle)
                .add();
        CHANNEL.messageBuilder(ResearchTablePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResearchTablePacket::encode)
                .decoder(ResearchTablePacket::decode)
                .consumerMainThread(ResearchTablePacket::handle)
                .add();
        CHANNEL.messageBuilder(ScanSlotPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScanSlotPacket::encode)
                .decoder(ScanSlotPacket::decode)
                .consumerMainThread(ScanSlotPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScanTargetPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScanTargetPacket::encode)
                .decoder(ScanTargetPacket::decode)
                .consumerMainThread(ScanTargetPacket::handle)
                .add();
        CHANNEL.messageBuilder(FxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FxPacket::encode)
                .decoder(FxPacket::decode)
                .consumerMainThread(FxPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChangeFocusPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChangeFocusPacket::encode)
                .decoder(ChangeFocusPacket::decode)
                .consumerMainThread(ChangeFocusPacket::handle)
                .add();
        CHANNEL.messageBuilder(HoverPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HoverPacket::encode)
                .decoder(HoverPacket::decode)
                .consumerMainThread(HoverPacket::handle)
                .add();
        CHANNEL.messageBuilder(ToggleMiscPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleMiscPacket::encode)
                .decoder(ToggleMiscPacket::decode)
                .consumerMainThread(ToggleMiscPacket::handle)
                .add();
    }

    private ModNetwork() {
    }
}
