package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.Notification;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Toasts. The client accepts 5 every 10 seconds; more are rate-limited here first. */
@Stable
@NonExtendable
public interface NotificationService {

    /**
     * @param notification what to show
     * @return the outcome
     */
    SendResult send(Notification notification);

    /**
     * @param title at most 32 bytes
     * @param text at most 160 bytes
     * @return the outcome (info, 5 s)
     */
    default SendResult send(String title, String text) {
        return send(Notification.of(title, text));
    }
}
