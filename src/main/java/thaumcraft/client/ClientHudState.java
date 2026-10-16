package thaumcraft.client;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.network.ScanTargetPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Данные для экранных элементов мода: цель таумометра и всплывающие уведомления. */
public final class ClientHudState {
    public static final long NOTIFICATION_MILLIS = 6000;
    private static final int MAX_NOTIFICATIONS = 8;

    public record Notification(@Nullable Aspect aspect, Component text, long created) {
    }

    @Nullable
    private static ScanTargetPacket scanTarget;
    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();

    public static void setScanTarget(@Nullable ScanTargetPacket target) {
        scanTarget = target;
    }

    @Nullable
    public static ScanTargetPacket getScanTarget() {
        return scanTarget;
    }

    public static void addNotification(@Nullable Aspect aspect, Component text) {
        NOTIFICATIONS.add(new Notification(aspect, text, System.currentTimeMillis()));
        while (NOTIFICATIONS.size() > MAX_NOTIFICATIONS) {
            NOTIFICATIONS.remove(0);
        }
    }

    /** Актуальные уведомления, от старых к новым; просроченные удаляются. */
    public static List<Notification> notifications() {
        long now = System.currentTimeMillis();
        for (Iterator<Notification> it = NOTIFICATIONS.iterator(); it.hasNext(); ) {
            if (now - it.next().created() > NOTIFICATION_MILLIS) {
                it.remove();
            }
        }
        return NOTIFICATIONS;
    }

    private ClientHudState() {
    }
}
