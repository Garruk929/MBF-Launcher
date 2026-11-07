package com.mbf.launcher;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** User-enabled system notification listener. No network access or data collection. */
public final class MBFNotificationService extends NotificationListenerService {
    private static volatile MBFNotificationService current;

    static StatusBarNotification[] snapshot() {
        MBFNotificationService instance = current;
        if (instance == null) return new StatusBarNotification[0];
        try {
            StatusBarNotification[] entries = instance.getActiveNotifications();
            return entries == null ? new StatusBarNotification[0] : entries;
        } catch (SecurityException | IllegalStateException ignored) {
            return new StatusBarNotification[0];
        }
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        current = this;
        MainActivity.notifyCardsChanged();
    }

    @Override public void onListenerDisconnected() {
        current = null;
        MainActivity.notifyCardsChanged();
        super.onListenerDisconnected();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        MainActivity.notifyCardsChanged();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        MainActivity.notifyCardsChanged();
    }

    @Override public void onDestroy() {
        current = null;
        MainActivity.notifyCardsChanged();
        super.onDestroy();
    }
}
