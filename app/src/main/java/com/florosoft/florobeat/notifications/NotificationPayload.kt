package com.florosoft.florobeat.notifications

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage

/**
 * Categorization of push notifications received from Firebase Cloud Messaging.
 */
enum class NotificationType {
    RELEASE,
    ANNOUNCEMENT,
    MAINTENANCE,
    FEATURE,
    IMPORTANT,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): NotificationType = when (value?.trim()?.uppercase()) {
            "RELEASE" -> RELEASE
            "ANNOUNCEMENT" -> ANNOUNCEMENT
            "MAINTENANCE" -> MAINTENANCE
            "FEATURE" -> FEATURE
            "IMPORTANT" -> IMPORTANT
            else -> UNKNOWN
        }
    }
}

/**
 * Structured FCM notification payload model.
 *
 * Supports notifications with title, body, custom data routes, and external URLs.
 * Handles missing fields and malformed payloads safely without throwing exceptions.
 */
data class NotificationPayload(
    val type: NotificationType = NotificationType.UNKNOWN,
    val title: String = "",
    val body: String = "",
    val route: String? = null,
    val url: String? = null,
    val notificationId: String? = null,
    val rawData: Map<String, String> = emptyMap(),
) {
    companion object {
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_ROUTE = "route"
        const val KEY_URL = "url"
        const val KEY_NOTIFICATION_ID = "notification_id"

        /**
         * Safely extracts a [NotificationPayload] from an incoming FCM [RemoteMessage].
         * Evaluates both data payload and notification payload fields, prioritizing explicit data values.
         */
        fun fromRemoteMessage(remoteMessage: RemoteMessage): NotificationPayload {
            val data = remoteMessage.data ?: emptyMap()
            val notif = remoteMessage.notification

            val title = data[KEY_TITLE]?.takeIf { it.isNotBlank() }
                ?: notif?.title.orEmpty()
            val body = data[KEY_BODY]?.takeIf { it.isNotBlank() }
                ?: notif?.body.orEmpty()
            val type = NotificationType.fromString(data[KEY_TYPE])
            val route = data[KEY_ROUTE]?.trim()?.takeIf { it.isNotEmpty() }
            val url = data[KEY_URL]?.trim()?.takeIf { it.isNotEmpty() }
            val notificationId = data[KEY_NOTIFICATION_ID]?.takeIf { it.isNotBlank() }
                ?: remoteMessage.messageId

            return NotificationPayload(
                type = type,
                title = title,
                body = body,
                route = route,
                url = url,
                notificationId = notificationId,
                rawData = data,
            )
        }

        /**
         * Safely extracts a [NotificationPayload] from intent extras when a user taps a notification.
         */
        fun fromIntentExtras(bundle: Bundle?): NotificationPayload? {
            if (bundle == null) return null

            val route = bundle.getString(KEY_ROUTE)?.trim()
            val typeStr = bundle.getString(KEY_TYPE)?.trim()
            val title = bundle.getString(KEY_TITLE)
                ?: bundle.getString("gcm.notification.title")
                ?: ""
            val body = bundle.getString(KEY_BODY)
                ?: bundle.getString("gcm.notification.body")
                ?: ""
            val url = bundle.getString(KEY_URL)?.trim()
            val notifId = bundle.getString(KEY_NOTIFICATION_ID)
                ?: bundle.getString("google.message_id")

            // If none of the known notification fields are present, this intent is not an FCM notification tap
            if (route.isNullOrBlank() && typeStr.isNullOrBlank() && title.isBlank() && body.isBlank()) {
                return null
            }

            val dataMap = mutableMapOf<String, String>()
            for (key in bundle.keySet()) {
                bundle.getString(key)?.let { dataMap[key] = it }
            }

            return NotificationPayload(
                type = NotificationType.fromString(typeStr),
                title = title,
                body = body,
                route = route?.takeIf { it.isNotEmpty() },
                url = url?.takeIf { it.isNotEmpty() },
                notificationId = notifId,
                rawData = dataMap,
            )
        }
    }
}
