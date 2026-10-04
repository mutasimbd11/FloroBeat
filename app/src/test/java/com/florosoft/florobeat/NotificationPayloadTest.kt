package com.florosoft.florobeat

import com.florosoft.florobeat.notifications.NotificationDeepLink
import com.florosoft.florobeat.notifications.NotificationPayload
import com.florosoft.florobeat.notifications.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPayloadTest {

    @Test
    fun `parses known notification types correctly`() {
        assertEquals(NotificationType.RELEASE, NotificationType.fromString("RELEASE"))
        assertEquals(NotificationType.RELEASE, NotificationType.fromString("release"))
        assertEquals(NotificationType.RELEASE, NotificationType.fromString("  Release  "))

        assertEquals(NotificationType.ANNOUNCEMENT, NotificationType.fromString("ANNOUNCEMENT"))
        assertEquals(NotificationType.MAINTENANCE, NotificationType.fromString("MAINTENANCE"))
        assertEquals(NotificationType.FEATURE, NotificationType.fromString("FEATURE"))
        assertEquals(NotificationType.IMPORTANT, NotificationType.fromString("IMPORTANT"))
        assertEquals(NotificationType.CHAT, NotificationType.fromString("CHAT"))
        assertEquals(NotificationType.CHAT, NotificationType.fromString("MESSAGE"))
        assertEquals(NotificationType.CHAT, NotificationType.fromString("chat"))
    }

    @Test
    fun `safely handles null and unknown notification types`() {
        assertEquals(NotificationType.UNKNOWN, NotificationType.fromString(null))
        assertEquals(NotificationType.UNKNOWN, NotificationType.fromString(""))
        assertEquals(NotificationType.UNKNOWN, NotificationType.fromString("   "))
        assertEquals(NotificationType.UNKNOWN, NotificationType.fromString("RANDOM_CUSTOM_TYPE"))
        assertEquals(NotificationType.UNKNOWN, NotificationType.fromString("12345"))
    }

    @Test
    fun `payload model preserves structured fields`() {
        val payload = NotificationPayload(
            type = NotificationType.RELEASE,
            title = "New FloroBeat Release",
            body = "A new version of FloroBeat is available.",
            route = "/settings/updates",
            url = "https://github.com/mutasimbd11/FloroBeat/releases",
            notificationId = "release_001",
            rawData = mapOf("custom_key" to "custom_value"),
        )

        assertEquals(NotificationType.RELEASE, payload.type)
        assertEquals("New FloroBeat Release", payload.title)
        assertEquals("A new version of FloroBeat is available.", payload.body)
        assertEquals("/settings/updates", payload.route)
        assertEquals("https://github.com/mutasimbd11/FloroBeat/releases", payload.url)
        assertEquals("release_001", payload.notificationId)
        assertEquals("custom_value", payload.rawData["custom_key"])
    }

    @Test
    fun `NotificationDeepLink handles null and empty intents safely`() {
        assertFalse(NotificationDeepLink.consume(null))
        assertNull(NotificationDeepLink.pending.value)
    }

    @Test
    fun `NotificationDeepLink clears state on handled`() {
        NotificationDeepLink.handled()
        assertNull(NotificationDeepLink.pending.value)
    }

    @Test
    fun `channel constants match required specification`() {
        assertEquals("florobeat_updates", com.florosoft.florobeat.notifications.FloroBeatNotificationManager.CHANNEL_UPDATES_ID)
    }
}
