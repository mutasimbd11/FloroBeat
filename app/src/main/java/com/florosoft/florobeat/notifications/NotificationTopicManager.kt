package com.florosoft.florobeat.notifications

import com.google.firebase.messaging.FirebaseMessaging

/**
 * Centralized manager for Firebase Cloud Messaging topic subscriptions.
 *
 * Prepares the application for broadcast topics without registering users for unnecessary feeds.
 */
object NotificationTopicManager {

    const val TOPIC_ALL = "florobeat_all"
    const val TOPIC_RELEASES = "florobeat_releases"
    const val TOPIC_ANNOUNCEMENTS = "florobeat_announcements"

    /**
     * Subscribes the current device to an FCM topic safely.
     */
    fun subscribe(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        runCatching {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful)
                }
        }.onFailure {
            onComplete?.invoke(false)
        }
    }

    /**
     * Unsubscribes the current device from an FCM topic safely.
     */
    fun unsubscribe(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        runCatching {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful)
                }
        }.onFailure {
            onComplete?.invoke(false)
        }
    }

    /**
     * Synchronizes topic subscriptions based on user preferences.
     * When push notifications are enabled, registers for critical releases and general updates.
     */
    fun syncSubscriptions(enabled: Boolean) {
        if (enabled) {
            subscribe(TOPIC_ALL)
            subscribe(TOPIC_RELEASES)
            subscribe(TOPIC_ANNOUNCEMENTS)
        } else {
            unsubscribe(TOPIC_ALL)
            unsubscribe(TOPIC_RELEASES)
            unsubscribe(TOPIC_ANNOUNCEMENTS)
        }
    }
}
