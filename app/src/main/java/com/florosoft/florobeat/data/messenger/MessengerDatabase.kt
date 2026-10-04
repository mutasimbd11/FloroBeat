package com.florosoft.florobeat.data.messenger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local persistent database for FloroBeat Messenger using native Android SQLite.
 *
 * Provides offline caching, instantaneous message lookups, and durable conversation history
 * with zero third-party dependencies or compile-time annotation processing overhead.
 */
class MessengerDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "florobeat_messenger.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CONVERSATIONS = "conversations"
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_BLOCKED = "blocked_users"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CONVERSATIONS (
                id TEXT PRIMARY KEY,
                other_user_id TEXT NOT NULL,
                other_user_handle TEXT NOT NULL,
                other_user_name TEXT NOT NULL,
                other_user_avatar TEXT,
                other_user_bio TEXT,
                other_user_online INTEGER NOT NULL DEFAULT 0,
                other_user_last_seen INTEGER NOT NULL DEFAULT 0,
                last_message_id TEXT,
                last_message_content TEXT,
                last_message_sender TEXT,
                last_message_time INTEGER,
                last_message_status TEXT,
                unread_count INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                receiver_id TEXT NOT NULL,
                content TEXT NOT NULL,
                type TEXT NOT NULL DEFAULT 'TEXT',
                status TEXT NOT NULL DEFAULT 'SENT',
                created_at INTEGER NOT NULL,
                reply_to_id TEXT,
                reply_to_text TEXT,
                reply_to_sender TEXT,
                is_outgoing INTEGER NOT NULL DEFAULT 0
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_BLOCKED (
                user_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                blocked_at INTEGER NOT NULL
            );
            """
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conv ON $TABLE_MESSAGES(conversation_id, created_at DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_conversations_updated ON $TABLE_CONVERSATIONS(updated_at DESC);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema migrations
    }

    fun saveMessage(msg: MessengerMessage) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", msg.id)
            put("conversation_id", msg.conversationId)
            put("sender_id", msg.senderId)
            put("receiver_id", msg.receiverId)
            put("content", msg.content)
            put("type", msg.type.name)
            put("status", msg.status.name)
            put("created_at", msg.createdAt)
            put("reply_to_id", msg.replyToId)
            put("reply_to_text", msg.replyToText)
            put("reply_to_sender", msg.replyToSenderName)
            put("is_outgoing", if (msg.isOutgoing) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateMessageStatus(messageId: String, status: MessageDeliveryStatus) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", status.name)
        }
        db.update(TABLE_MESSAGES, values, "id = ?", arrayOf(messageId))
    }

    fun markMessagesRead(conversationId: String, upToTime: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", MessageDeliveryStatus.READ.name)
        }
        db.update(
            TABLE_MESSAGES,
            values,
            "conversation_id = ? AND is_outgoing = 0 AND created_at <= ?",
            arrayOf(conversationId, upToTime.toString()),
        )

        // Clear unread count on conversation
        val convValues = ContentValues().apply {
            put("unread_count", 0)
        }
        db.update(TABLE_CONVERSATIONS, convValues, "id = ?", arrayOf(conversationId))
    }

    fun getMessages(conversationId: String, limit: Int = 100): List<MessengerMessage> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "conversation_id = ?",
            arrayOf(conversationId),
            null,
            null,
            "created_at ASC",
            limit.toString(),
        )

        val messages = mutableListOf<MessengerMessage>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val convIdIdx = c.getColumnIndexOrThrow("conversation_id")
            val senderIdIdx = c.getColumnIndexOrThrow("sender_id")
            val receiverIdIdx = c.getColumnIndexOrThrow("receiver_id")
            val contentIdx = c.getColumnIndexOrThrow("content")
            val typeIdx = c.getColumnIndexOrThrow("type")
            val statusIdx = c.getColumnIndexOrThrow("status")
            val createdAtIdx = c.getColumnIndexOrThrow("created_at")
            val replyToIdIdx = c.getColumnIndexOrThrow("reply_to_id")
            val replyToTextIdx = c.getColumnIndexOrThrow("reply_to_text")
            val replyToSenderIdx = c.getColumnIndexOrThrow("reply_to_sender")
            val isOutgoingIdx = c.getColumnIndexOrThrow("is_outgoing")

            while (c.moveToNext()) {
                messages.add(
                    MessengerMessage(
                        id = c.getString(idIdx),
                        conversationId = c.getString(convIdIdx),
                        senderId = c.getString(senderIdIdx),
                        receiverId = c.getString(receiverIdIdx),
                        content = c.getString(contentIdx),
                        type = MessageType.fromString(c.getString(typeIdx)),
                        status = MessageDeliveryStatus.fromString(c.getString(statusIdx)),
                        createdAt = c.getLong(createdAtIdx),
                        replyToId = c.getString(replyToIdIdx),
                        replyToText = c.getString(replyToTextIdx),
                        replyToSenderName = c.getString(replyToSenderIdx),
                        isOutgoing = c.getInt(isOutgoingIdx) == 1,
                    )
                )
            }
        }
        return messages
    }

    fun deleteMessage(messageId: String) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, "id = ?", arrayOf(messageId))
    }

    fun clearConversation(conversationId: String) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, "conversation_id = ?", arrayOf(conversationId))
        db.delete(TABLE_CONVERSATIONS, "id = ?", arrayOf(conversationId))
    }

    fun upsertConversation(conv: MessengerConversation) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", conv.id)
            put("other_user_id", conv.otherUser.id)
            put("other_user_handle", conv.otherUser.username)
            put("other_user_name", conv.otherUser.displayName)
            put("other_user_avatar", conv.otherUser.avatarUrl)
            put("other_user_bio", conv.otherUser.bio)
            put("other_user_online", if (conv.otherUser.isOnline) 1 else 0)
            put("other_user_last_seen", conv.otherUser.lastSeenAt)
            put("last_message_id", conv.lastMessage?.id)
            put("last_message_content", conv.lastMessage?.content)
            put("last_message_sender", conv.lastMessage?.senderId)
            put("last_message_time", conv.lastMessage?.createdAt)
            put("last_message_status", conv.lastMessage?.status?.name)
            put("unread_count", conv.unreadCount)
            put("updated_at", conv.updatedAt)
        }
        db.insertWithOnConflict(TABLE_CONVERSATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getConversations(): List<MessengerConversation> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONVERSATIONS,
            null,
            null,
            null,
            null,
            null,
            "updated_at DESC",
        )

        val list = mutableListOf<MessengerConversation>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val otherIdIdx = c.getColumnIndexOrThrow("other_user_id")
            val handleIdx = c.getColumnIndexOrThrow("other_user_handle")
            val nameIdx = c.getColumnIndexOrThrow("other_user_name")
            val avatarIdx = c.getColumnIndexOrThrow("other_user_avatar")
            val bioIdx = c.getColumnIndexOrThrow("other_user_bio")
            val onlineIdx = c.getColumnIndexOrThrow("other_user_online")
            val lastSeenIdx = c.getColumnIndexOrThrow("other_user_last_seen")
            val lastMsgIdIdx = c.getColumnIndexOrThrow("last_message_id")
            val lastMsgContentIdx = c.getColumnIndexOrThrow("last_message_content")
            val lastMsgSenderIdx = c.getColumnIndexOrThrow("last_message_sender")
            val lastMsgTimeIdx = c.getColumnIndexOrThrow("last_message_time")
            val lastMsgStatusIdx = c.getColumnIndexOrThrow("last_message_status")
            val unreadIdx = c.getColumnIndexOrThrow("unread_count")
            val updatedIdx = c.getColumnIndexOrThrow("updated_at")

            while (c.moveToNext()) {
                val otherUser = MessengerUser(
                    id = c.getString(otherIdIdx),
                    username = c.getString(handleIdx),
                    displayName = c.getString(nameIdx),
                    avatarUrl = c.getString(avatarIdx),
                    bio = c.getString(bioIdx),
                    isOnline = c.getInt(onlineIdx) == 1,
                    lastSeenAt = c.getLong(lastSeenIdx),
                )

                val lastMsgId = c.getString(lastMsgIdIdx)
                val lastMsg = if (lastMsgId != null) {
                    MessengerMessage(
                        id = lastMsgId,
                        conversationId = c.getString(idIdx),
                        senderId = c.getString(lastMsgSenderIdx) ?: "",
                        receiverId = otherUser.id,
                        content = c.getString(lastMsgContentIdx) ?: "",
                        status = MessageDeliveryStatus.fromString(c.getString(lastMsgStatusIdx)),
                        createdAt = c.getLong(lastMsgTimeIdx),
                    )
                } else null

                list.add(
                    MessengerConversation(
                        id = c.getString(idIdx),
                        otherUser = otherUser,
                        lastMessage = lastMsg,
                        unreadCount = c.getInt(unreadIdx),
                        updatedAt = c.getLong(updatedIdx),
                    )
                )
            }
        }
        return list
    }

    fun blockUser(userId: String, name: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("name", name)
            put("blocked_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_BLOCKED, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun unblockUser(userId: String) {
        val db = writableDatabase
        db.delete(TABLE_BLOCKED, "user_id = ?", arrayOf(userId))
    }

    fun isUserBlocked(userId: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(TABLE_BLOCKED, null, "user_id = ?", arrayOf(userId), null, null, null)
        val blocked = cursor.count > 0
        cursor.close()
        return blocked
    }
}
