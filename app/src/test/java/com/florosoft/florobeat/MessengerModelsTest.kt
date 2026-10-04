package com.florosoft.florobeat

import com.florosoft.florobeat.data.messenger.MessageDeliveryStatus
import com.florosoft.florobeat.data.messenger.MessageType
import com.florosoft.florobeat.data.messenger.MessengerConversation
import com.florosoft.florobeat.data.messenger.MessengerEvent
import com.florosoft.florobeat.data.messenger.MessengerMessage
import com.florosoft.florobeat.data.messenger.MessengerUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessengerModelsTest {

    @Test
    fun `MessageDeliveryStatus parses string values correctly`() {
        assertEquals(MessageDeliveryStatus.SENDING, MessageDeliveryStatus.fromString("SENDING"))
        assertEquals(MessageDeliveryStatus.SENDING, MessageDeliveryStatus.fromString("sending"))
        assertEquals(MessageDeliveryStatus.SENT, MessageDeliveryStatus.fromString("SENT"))
        assertEquals(MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.fromString("DELIVERED"))
        assertEquals(MessageDeliveryStatus.READ, MessageDeliveryStatus.fromString("READ"))
        assertEquals(MessageDeliveryStatus.FAILED, MessageDeliveryStatus.fromString("FAILED"))
        assertEquals(MessageDeliveryStatus.SENT, MessageDeliveryStatus.fromString("UNKNOWN_VALUE"))
        assertEquals(MessageDeliveryStatus.SENT, MessageDeliveryStatus.fromString(null))
    }

    @Test
    fun `MessageType parses string values correctly`() {
        assertEquals(MessageType.TEXT, MessageType.fromString("TEXT"))
        assertEquals(MessageType.IMAGE, MessageType.fromString("IMAGE"))
        assertEquals(MessageType.SONG_SHARE, MessageType.fromString("SONG_SHARE"))
        assertEquals(MessageType.TEXT, MessageType.fromString("unknown"))
        assertEquals(MessageType.TEXT, MessageType.fromString(null))
    }

    @Test
    fun `MessengerUser formats handle and properties accurately`() {
        val userWithAt = MessengerUser(
            id = "u123",
            username = "@mutasim",
            displayName = "Mutasim",
            isOnline = true,
            lastSeenAt = 1000L,
        )
        assertEquals("@mutasim", userWithAt.handle)
        assertTrue(userWithAt.isOnline)

        val userWithoutAt = MessengerUser(
            id = "u456",
            username = "listener_01",
            displayName = "Listener 01",
            isOnline = false,
            lastSeenAt = 2000L,
        )
        assertEquals("@listener_01", userWithoutAt.handle)
        assertFalse(userWithoutAt.isOnline)
        assertNull(userWithoutAt.avatarUrl)
    }

    @Test
    fun `MessengerMessage holds reply metadata and outgoing flags`() {
        val original = MessengerMessage(
            id = "m1",
            conversationId = "c1",
            senderId = "u1",
            receiverId = "u2",
            content = "Check out this song!",
            status = MessageDeliveryStatus.SENT,
            isOutgoing = false,
        )

        val reply = MessengerMessage(
            id = "m2",
            conversationId = "c1",
            senderId = "u2",
            receiverId = "u1",
            content = "Loving it!",
            status = MessageDeliveryStatus.READ,
            replyToId = original.id,
            replyToText = original.content,
            replyToSenderName = "Mutasim",
            isOutgoing = true,
        )

        assertEquals("m1", reply.replyToId)
        assertEquals("Check out this song!", reply.replyToText)
        assertEquals("Mutasim", reply.replyToSenderName)
        assertTrue(reply.isOutgoing)
        assertEquals(MessageDeliveryStatus.READ, reply.status)
    }

    @Test
    fun `MessengerConversation tracks unread count and other user correctly`() {
        val user = MessengerUser(id = "u2", username = "bob", displayName = "Bob")
        val conversation = MessengerConversation(
            id = "c_u1_u2",
            otherUser = user,
            lastMessage = null,
            unreadCount = 3,
            updatedAt = 5000L,
        )

        assertEquals("c_u1_u2", conversation.id)
        assertEquals("Bob", conversation.otherUser.displayName)
        assertEquals(3, conversation.unreadCount)
        assertNull(conversation.lastMessage)
    }

    @Test
    fun `MessengerEvent types preserve event data`() {
        val msg = MessengerMessage(
            id = "m10",
            conversationId = "c10",
            senderId = "u1",
            receiverId = "u2",
            content = "Hello",
        )
        val inbound = MessengerEvent.InboundMessage(msg)
        assertEquals("m10", inbound.message.id)

        val typing = MessengerEvent.Typing(conversationId = "c10", userId = "u1", isTyping = true)
        assertTrue(typing.isTyping)
        assertEquals("c10", typing.conversationId)

        val presence = MessengerEvent.Presence(userId = "u1", isOnline = true, lastSeenAt = 12345L)
        assertTrue(presence.isOnline)
        assertEquals(12345L, presence.lastSeenAt)

        val read = MessengerEvent.ReadReceipt(conversationId = "c10", messageIds = listOf("m10"))
        assertEquals(1, read.messageIds.size)
    }
}
