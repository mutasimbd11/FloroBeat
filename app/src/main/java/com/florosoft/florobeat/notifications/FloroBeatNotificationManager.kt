package com.florosoft.florobeat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.florosoft.florobeat.MainActivity
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.settings.AppSettings

/**
 * Manages push notification display and dedicated notification channels for FloroBeat.
 *
 * Distinct and isolated from Media3's foreground playback notification channel.
 */
object FloroBeatNotificationManager {

    const val CHANNEL_UPDATES_ID = "florobeat_updates"
    private const val CHANNEL_UPDATES_NAME = "FloroBeat Updates"
    private const val CHANNEL_UPDATES_DESC = "New releases, important announcements, and major feature updates."

    /** FloroMint accent brand color for notification badges and accents. */
    private const val NOTIFICATION_COLOR = 0xFF2CEEB3.toInt()

    /**
     * Creates the dedicated notification channel idempotently.
     * Safe to call repeatedly on app launch or service initialization.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                CHANNEL_UPDATES_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = CHANNEL_UPDATES_DESC
                enableLights(true)
                lightColor = NOTIFICATION_COLOR
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks whether runtime notification permission is required by the Android OS.
     * Required on Android 13 (API 33) and above.
     */
    fun isRuntimePermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Checks whether system-level notifications are currently enabled for FloroBeat.
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Automatically initializes and activates the FloroBeat FCM notification system.
     *
     * 1. Creates the dedicated notification channel idempotently.
     * 2. Sets user notification preference to enabled in [AppSettings].
     * 3. Fetches and registers the FCM token.
     * 4. Centralizes topic subscriptions (e.g. florobeat_all, florobeat_releases).
     *
     * Idempotent and safe to call multiple times.
     */
    fun activateNotificationSystem(context: Context) {
        createNotificationChannels(context)
        AppSettings.setPushNotificationsEnabled(true)
        FcmTokenManager.fetchCurrentToken()
        NotificationTopicManager.syncSubscriptions(true)
    }

    /**
     * Handles notification permission granted during onboarding or settings.
     */
    fun handlePermissionGranted(context: Context) {
        AppSettings.setNotificationPermissionRequested(true)
        activateNotificationSystem(context)
    }

    /**
     * Handles notification permission denied during onboarding.
     * Keeps notification system safely inactive without blocking app launch.
     */
    fun handlePermissionDenied() {
        AppSettings.setNotificationPermissionRequested(true)
        AppSettings.setPushNotificationsEnabled(false)
    }

    /**
     * Displays a notification for the given [NotificationPayload].
     *
     * Respects user settings ([AppSettings.pushNotificationsEnabled]), system permissions,
     * and handles security constraints safely.
     */
    fun showNotification(context: Context, payload: NotificationPayload) {
        // Respect user preference in AppSettings
        if (!AppSettings.pushNotificationsEnabled.value) {
            return
        }

        // Check if system notifications are enabled
        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) {
            return
        }

        // Ensure channel exists
        createNotificationChannels(context)

        val intId = payload.notificationId?.hashCode() ?: System.currentTimeMillis().toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationPayload.KEY_TYPE, payload.type.name)
            putExtra(NotificationPayload.KEY_TITLE, payload.title)
            putExtra(NotificationPayload.KEY_BODY, payload.body)
            payload.route?.let { putExtra(NotificationPayload.KEY_ROUTE, it) }
            payload.url?.let { putExtra(NotificationPayload.KEY_URL, it) }
            payload.notificationId?.let { putExtra(NotificationPayload.KEY_NOTIFICATION_ID, it) }
            for ((key, value) in payload.rawData) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            intId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(NOTIFICATION_COLOR)
            .setContentTitle(payload.title.ifBlank { "FloroBeat" })
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            managerCompat.notify(intId, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission revoked in the background
        } catch (_: Exception) {
            // Catch any transient system service errors
        }
    }
}
