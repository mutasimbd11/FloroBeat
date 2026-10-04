package com.florosoft.florobeat.notifications

import android.app.ActivityManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service handling Firebase Cloud Messaging events, tokens, and inbound messages.
 *
 * Implements non-blocking, crash-safe processing for notification and data payloads.
 */
class FloroBeatFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        runCatching {
            val payload = NotificationPayload.fromRemoteMessage(remoteMessage)

            // Do not display notifications if title and body are completely empty (silent sync payloads)
            if (payload.title.isBlank() && payload.body.isBlank()) {
                return
            }

            val hasSystemNotification = remoteMessage.notification != null
            val inForeground = isAppInForeground()

            /*
             * Duplicate notification prevention:
             * - When the app is in the background and the FCM payload contains a `notification` object,
             *   the Android system tray displays it automatically.
             * - When the app is in the foreground, or when the message is a data-only payload,
             *   the system tray will NOT display it automatically, so FloroBeat creates it manually.
             */
            if (!hasSystemNotification || inForeground) {
                FloroBeatNotificationManager.showNotification(applicationContext, payload)
            }
        }
    }

    /**
     * Determines whether the app is currently visible in the foreground.
     */
    private fun isAppInForeground(): Boolean {
        return runCatching {
            val appProcessInfo = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(appProcessInfo)
            appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }.getOrDefault(false)
    }
}
