package com.florosoft.florobeat.data.messenger

import kotlinx.serialization.Serializable

/**
 * Delivery and read states for chat messages.
 */
enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED;

    companion object {
        fun fromString(value: String?): MessageDeliveryStatus = when (value?.uppercase()) {
            "SENDING" -> SENDING
            "SENT" -> SENT
            "DELIVERED" -> DELIVERED
            "READ" -> READ
            "FAILED" -> FAILED
            else -> SENT
        }
    }
}

/**
 * Supported message content types.
 */
enum class MessageType {
    TEXT,
    IMAGE,
    SONG_SHARE;

    companion object {
        fun fromString(value: String?): MessageType = when (value?.uppercase()) {
            "IMAGE" -> IMAGE
            "SONG_SHARE" -> SONG_SHARE
            else -> TEXT
        }
    }
}

/**
 * Public FloroBeat Messenger user profile.
 */
@Serializable
data class MessengerUser(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: Long = 0L,
    val isBlocked: Boolean = false,
) {
    /** Safe formatted user handle (e.g. "@mutasim") */
    val handle: String
        get() = if (username.startsWith("@")) username else "@$username"
}

/**
 * Individual chat message with delivery status, timestamps, and optional reply reference.
 */
@Serializable
data class MessengerMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val createdAt: Long = System.currentTimeMillis(),
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val isOutgoing: Boolean = false,
)

/**
 * 1-on-1 conversation item summary for the conversation list.
 */
@Serializable
data class MessengerConversation(
    val id: String,
    val otherUser: MessengerUser,
    val lastMessage: MessengerMessage? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * WebSocket frame types for real-time messaging protocol.
 */
sealed class MessengerEvent {
    data class InboundMessage(val message: MessengerMessage) : MessengerEvent()
    data class MessageSentAck(val messageId: String, val status: MessageDeliveryStatus, val createdAt: Long) : MessengerEvent()
    data class DeliveryReceipt(val messageId: String, val status: MessageDeliveryStatus) : MessengerEvent()
    data class ReadReceipt(val conversationId: String, val messageIds: List<String>) : MessengerEvent()
    data class Typing(val conversationId: String, val userId: String, val isTyping: Boolean) : MessengerEvent()
    data class Presence(val userId: String, val isOnline: Boolean, val lastSeenAt: Long) : MessengerEvent()
    data class MessageDeleted(val messageId: String, val conversationId: String, val forEveryone: Boolean) : MessengerEvent()
}
