package com.florosoft.florobeat.notifications

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Relays push notification tap events from [com.florosoft.florobeat.MainActivity] to Compose.
 *
 * Adheres strictly to FloroBeat's existing deep linking design (mirrors [com.florosoft.florobeat.data.listentogether.JamInviteLink]).
 */
object NotificationDeepLink {

    private const val EXTRA_NOTIFICATION_CONSUMED = "florobeat.notificationConsumed"

    private val _pending = MutableStateFlow<NotificationPayload?>(null)
    val pending: StateFlow<NotificationPayload?> = _pending.asStateFlow()

    /**
     * Reads notification payload data from a cold launch or a new intent on the singleTask activity.
     */
    fun consume(intent: Intent?): Boolean {
        if (
            intent == null ||
            intent.getBooleanExtra(EXTRA_NOTIFICATION_CONSUMED, false)
        ) return false

        val payload = NotificationPayload.fromIntentExtras(intent.extras) ?: return false
        intent.putExtra(EXTRA_NOTIFICATION_CONSUMED, true)
        _pending.value = payload
        return true
    }

    /**
     * Clears the pending notification after it has been routed by the UI.
     */
    fun handled() {
        _pending.value = null
    }
}
