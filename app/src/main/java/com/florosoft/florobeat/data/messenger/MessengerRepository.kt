package com.florosoft.florobeat.data.messenger

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.florosoft.florobeat.BuildConfig
import com.florosoft.florobeat.FloroBeatApplication
import com.florosoft.florobeat.notifications.FcmTokenManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

/**
 * High-level repository coordinating FloroBeat's real-time messaging, local database persistence,
 * REST sync, and WebSocket event streams.
 */
object MessengerRepository {

    private const val TAG = "FloroBeatMessenger"
    private const val PREFS_NAME = "florobeat_messenger_prefs"
    private const val KEY_SERVER = "custom_messenger_server"
    private const val KEY_PROFILE_NAME = "messenger_profile_name"
    private const val KEY_PROFILE_HANDLE = "messenger_profile_handle"
    private const val KEY_PROFILE_BIO = "messenger_profile_bio"
    private const val KEY_LOCAL_UID = "messenger_local_uid"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout)
        expectSuccess = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: SharedPreferences
    private lateinit var database: MessengerDatabase
    private lateinit var client: MessengerClient

    private val _currentUser = MutableStateFlow<MessengerUser?>(null)
    val currentUser: StateFlow<MessengerUser?> = _currentUser.asStateFlow()

    private val _conversations = MutableStateFlow<List<MessengerConversation>>(emptyList())
    val conversations: StateFlow<List<MessengerConversation>> = _conversations.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeChatMessages = MutableStateFlow<List<MessengerMessage>>(emptyList())
    val activeChatMessages: StateFlow<List<MessengerMessage>> = _activeChatMessages.asStateFlow()

    private val _activeChatUser = MutableStateFlow<MessengerUser?>(null)
    val activeChatUser: StateFlow<MessengerUser?> = _activeChatUser.asStateFlow()

    /** Tracks typing status per conversation ID (true if other user is typing) */
    private val _typingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingState: StateFlow<Map<String, Boolean>> = _typingState.asStateFlow()

    private val _serverUrl = MutableStateFlow(BuildConfig.MESSAGING_SERVER)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private var eventCollectJob: Job? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        database = MessengerDatabase(context.applicationContext)

        val savedServer = prefs.getString(KEY_SERVER, null)?.trim().orEmpty()
        if (savedServer.isNotBlank()) {
            _serverUrl.value = savedServer
        }

        client = MessengerClient { _serverUrl.value }

        // Resolve user identity and connect
        resolveIdentity()

        // Load local conversations initially
        loadLocalConversations()

        // Listen for client events
        observeClientEvents()

        // Sync with backend if reachable
        syncWithBackend()
    }

    val connectionStatus: StateFlow<MessengerClient.ConnectionStatus>
        get() = client.connectionStatus

    fun setCustomServerUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        _serverUrl.value = if (cleaned.isBlank()) BuildConfig.MESSAGING_SERVER else cleaned
        prefs.edit().putString(KEY_SERVER, cleaned).apply()
        _currentUser.value?.id?.let { client.connect(it) }
    }

    fun updateProfile(displayName: String, handle: String, bio: String?) {
        prefs.edit()
            .putString(KEY_PROFILE_NAME, displayName.trim())
            .putString(KEY_PROFILE_HANDLE, handle.trim().lstrip("@"))
            .putString(KEY_PROFILE_BIO, bio?.trim())
            .apply()

        resolveIdentity()
        scope.launch {
            syncProfileToBackend()
        }
    }

    private fun resolveIdentity() {
        val authStore = FloroBeatApplication.authStore
        val account = authStore.activeSession
        val profile = account?.profiles?.firstOrNull { it.profileId == account.activeProfileId }
            ?: account?.profiles?.firstOrNull()

        val userId: String
        val displayName: String
        val handle: String
        val avatarUrl: String?
        val bio: String? = prefs.getString(KEY_PROFILE_BIO, null)

        if (account != null) {
            userId = sha256("${account.accountId}:${profile?.profileId.orEmpty()}").take(32)
            displayName = prefs.getString(KEY_PROFILE_NAME, null)
                ?: profile?.name?.takeIf { it.isNotBlank() }
                ?: account.name.takeIf { it.isNotBlank() }
                ?: account.email.substringBefore('@')

            handle = prefs.getString(KEY_PROFILE_HANDLE, null)
                ?: account.email.substringBefore('@').lowercase().replace(Regex("[^a-z0-9_]"), "")
            avatarUrl = profile?.avatar?.takeIf { it.startsWith("http") }
        } else {
            // Local offline/guest profile
            var localId = prefs.getString(KEY_LOCAL_UID, null)
            if (localId.isNullOrBlank()) {
                localId = UUID.randomUUID().toString().replace("-", "").take(32)
                prefs.edit().putString(KEY_LOCAL_UID, localId).apply()
            }
            userId = localId
            displayName = prefs.getString(KEY_PROFILE_NAME, null) ?: "FloroBeat Listener"
            handle = prefs.getString(KEY_PROFILE_HANDLE, null) ?: "listener_${userId.take(6)}"
            avatarUrl = null
        }

        val user = MessengerUser(
            id = userId,
            username = handle,
            displayName = displayName,
            avatarUrl = avatarUrl,
            bio = bio,
            isOnline = true,
            lastSeenAt = System.currentTimeMillis(),
        )

        _currentUser.value = user
        client.connect(userId)
    }

    private fun observeClientEvents() {
        eventCollectJob?.cancel()
        eventCollectJob = scope.launch {
            client.events.collect { event ->
                when (event) {
                    is MessengerEvent.InboundMessage -> handleInboundMessage(event.message)
                    is MessengerEvent.MessageSentAck -> handleMessageSentAck(event.messageId, event.status)
                    is MessengerEvent.DeliveryReceipt -> handleDeliveryReceipt(event.messageId, event.status)
                    is MessengerEvent.ReadReceipt -> handleReadReceipt(event.conversationId, event.messageIds)
                    is MessengerEvent.Typing -> handleTypingEvent(event.conversationId, event.isTyping)
                    is MessengerEvent.Presence -> handlePresenceEvent(event.userId, event.isOnline, event.lastSeenAt)
                    is MessengerEvent.MessageDeleted -> handleMessageDeleted(event.messageId, event.conversationId)
                }
            }
        }
    }

    private fun handleInboundMessage(msg: MessengerMessage) {
        if (database.isUserBlocked(msg.senderId)) return

        val currentActiveConv = _activeConversationId.value
        val isViewingThisChat = currentActiveConv == msg.conversationId

        val status = if (isViewingThisChat) MessageDeliveryStatus.READ else MessageDeliveryStatus.DELIVERED
        val updatedMsg = msg.copy(status = status, isOutgoing = false)

        database.saveMessage(updatedMsg)

        if (isViewingThisChat) {
            _activeChatMessages.update { current ->
                if (current.any { it.id == msg.id }) current else current + updatedMsg
            }
            client.sendReadReceipt(msg.conversationId, msg.senderId, msg.createdAt)
        } else {
            client.sendDeliveryReceipt(msg.id, msg.senderId)
        }

        loadLocalConversations()
    }

    private fun handleMessageSentAck(messageId: String, status: MessageDeliveryStatus) {
        database.updateMessageStatus(messageId, status)
        _activeChatMessages.update { list ->
            list.map { if (it.id == messageId) it.copy(status = status) else it }
        }
        loadLocalConversations()
    }

    private fun handleDeliveryReceipt(messageId: String, status: MessageDeliveryStatus) {
        database.updateMessageStatus(messageId, status)
        _activeChatMessages.update { list ->
            list.map { if (it.id == messageId) it.copy(status = status) else it }
        }
        loadLocalConversations()
    }

    private fun handleReadReceipt(conversationId: String, messageIds: List<String>) {
        val idSet = messageIds.toSet()
        _activeChatMessages.update { list ->
            list.map { if (it.conversationId == conversationId && (idSet.isEmpty() || it.id in idSet)) it.copy(status = MessageDeliveryStatus.READ) else it }
        }
        loadLocalConversations()
    }

    private fun handleTypingEvent(conversationId: String, isTyping: Boolean) {
        _typingState.update { current ->
            current + (conversationId to isTyping)
        }
    }

    private fun handlePresenceEvent(userId: String, isOnline: Boolean, lastSeenAt: Long) {
        _conversations.update { list ->
            list.map {
                if (it.otherUser.id == userId) {
                    it.copy(otherUser = it.otherUser.copy(isOnline = isOnline, lastSeenAt = lastSeenAt))
                } else it
            }
        }
        _activeChatUser.update { current ->
            if (current?.id == userId) current.copy(isOnline = isOnline, lastSeenAt = lastSeenAt) else current
        }
    }

    private fun handleMessageDeleted(messageId: String, conversationId: String) {
        database.deleteMessage(messageId)
        _activeChatMessages.update { list -> list.filterNot { it.id == messageId } }
        loadLocalConversations()
    }

    private fun loadLocalConversations() {
        scope.launch {
            val list = database.getConversations()
            _conversations.value = list
        }
    }

    fun openConversation(conversationId: String, otherUser: MessengerUser) {
        _activeConversationId.value = conversationId
        _activeChatUser.value = otherUser

        scope.launch {
            val cachedMessages = database.getMessages(conversationId)
            _activeChatMessages.value = cachedMessages

            // Mark unread incoming messages as read
            database.markMessagesRead(conversationId)
            _currentUser.value?.id?.let { myId ->
                client.sendReadReceipt(conversationId, otherUser.id, System.currentTimeMillis())
            }
            loadLocalConversations()

            // Fetch any newer messages from server
            fetchRemoteMessages(conversationId)
        }
    }

    fun closeConversation() {
        _activeConversationId.value = null
        _activeChatUser.value = null
        _activeChatMessages.value = emptyList()
    }

    fun sendMessage(
        conversationId: String,
        receiverId: String,
        content: String,
        replyTo: MessengerMessage? = null,
        type: MessageType = MessageType.TEXT,
    ) {
        val user = _currentUser.value ?: return
        val text = content.trim()
        if (text.isBlank()) return

        val msgId = "m_${System.currentTimeMillis()}_${user.id.take(6)}"
        val msg = MessengerMessage(
            id = msgId,
            conversationId = conversationId,
            senderId = user.id,
            receiverId = receiverId,
            content = text,
            type = type,
            status = if (client.isConnected) MessageDeliveryStatus.SENT else MessageDeliveryStatus.SENDING,
            createdAt = System.currentTimeMillis(),
            replyToId = replyTo?.id,
            replyToText = replyTo?.content?.take(80),
            replyToSenderName = replyTo?.senderId?.let { if (it == user.id) "You" else _activeChatUser.value?.displayName },
            isOutgoing = true,
        )

        // Optimistic local save
        database.saveMessage(msg)
        _activeChatMessages.update { it + msg }

        // Update conversation summary
        val other = _activeChatUser.value ?: MessengerUser(receiverId, "user", "User")
        database.upsertConversation(
            MessengerConversation(
                id = conversationId,
                otherUser = other,
                lastMessage = msg,
                unreadCount = 0,
                updatedAt = msg.createdAt,
            )
        )
        loadLocalConversations()

        // Send via WebSocket or fallback
        if (client.isConnected) {
            client.sendMessage(msg)
        } else {
            scope.launch {
                sendViaRest(msg)
            }
        }
    }

    fun retryMessage(msg: MessengerMessage) {
        val updated = msg.copy(status = MessageDeliveryStatus.SENDING)
        database.saveMessage(updated)
        _activeChatMessages.update { list -> list.map { if (it.id == msg.id) updated else it } }

        if (client.isConnected) {
            client.sendMessage(updated)
        } else {
            scope.launch { sendViaRest(updated) }
        }
    }

    private suspend fun sendViaRest(msg: MessengerMessage) {
        val base = _serverUrl.value.trim().trimEnd('/')
        val user = _currentUser.value ?: return
        runCatching {
            val response = http.post("$base/api/messaging/conversations/${msg.conversationId}/messages") {
                contentType(ContentType.Application.Json)
                header("X-User-Id", user.id)
                setBody(
                    mapOf(
                        "id" to msg.id,
                        "content" to msg.content,
                        "type" to msg.type.name,
                        "reply_to_id" to msg.replyToId,
                    )
                )
            }
            if (response.status.isSuccess()) {
                database.updateMessageStatus(msg.id, MessageDeliveryStatus.SENT)
                _activeChatMessages.update { list -> list.map { if (it.id == msg.id) it.copy(status = MessageDeliveryStatus.SENT) else it } }
            } else {
                database.updateMessageStatus(msg.id, MessageDeliveryStatus.FAILED)
                _activeChatMessages.update { list -> list.map { if (it.id == msg.id) it.copy(status = MessageDeliveryStatus.FAILED) else it } }
            }
        }.onFailure {
            database.updateMessageStatus(msg.id, MessageDeliveryStatus.FAILED)
            _activeChatMessages.update { list -> list.map { if (it.id == msg.id) it.copy(status = MessageDeliveryStatus.FAILED) else it } }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        val convId = _activeConversationId.value ?: return
        val targetUser = _activeChatUser.value ?: return
        client.sendTyping(convId, targetUser.id, isTyping)
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        val user = _currentUser.value ?: return
        val convId = _activeConversationId.value ?: return

        database.deleteMessage(messageId)
        _activeChatMessages.update { list -> list.filterNot { it.id == messageId } }
        loadLocalConversations()

        scope.launch {
            val base = _serverUrl.value.trim().trimEnd('/')
            runCatching {
                http.delete("$base/api/messaging/messages/$messageId") {
                    header("X-User-Id", user.id)
                    parameter("for_everyone", forEveryone)
                }
            }
        }
    }

    suspend fun searchUsers(query: String): List<MessengerUser> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext emptyList()
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val base = _serverUrl.value.trim().trimEnd('/')
        runCatching {
            val response = http.get("$base/api/messaging/users/search") {
                header("X-User-Id", user.id)
                parameter("q", q)
            }
            if (response.status.isSuccess()) {
                val data: UserSearchResponse = response.body()
                data.users.map { u ->
                    MessengerUser(
                        id = u.user_id,
                        username = u.username,
                        displayName = u.display_name,
                        avatarUrl = u.avatar_url,
                        bio = u.bio,
                        isOnline = u.isOnline,
                        lastSeenAt = u.last_seen_at,
                    )
                }
            } else emptyList()
        }.getOrElse {
            Log.w(TAG, "Search failed: ${it.message}")
            emptyList()
        }
    }

    fun blockUser(otherUser: MessengerUser) {
        database.blockUser(otherUser.id, otherUser.displayName)
        _conversations.update { list -> list.filterNot { it.otherUser.id == otherUser.id } }

        scope.launch {
            val base = _serverUrl.value.trim().trimEnd('/')
            val user = _currentUser.value ?: return@launch
            runCatching {
                http.post("$base/api/messaging/users/${otherUser.id}/block") {
                    header("X-User-Id", user.id)
                }
            }
        }
    }

    fun unblockUser(userId: String) {
        database.unblockUser(userId)
        scope.launch {
            val base = _serverUrl.value.trim().trimEnd('/')
            val user = _currentUser.value ?: return@launch
            runCatching {
                http.delete("$base/api/messaging/users/$userId/block") {
                    header("X-User-Id", user.id)
                }
            }
        }
    }

    fun isUserBlocked(userId: String): Boolean {
        return if (::database.isInitialized) database.isUserBlocked(userId) else false
    }

    fun clearConversation(conversationId: String) {
        if (::database.isInitialized) {
            database.clearConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeChatMessages.value = emptyList()
            }
            loadLocalConversations()
        }
    }

    fun reportUser(userId: String, reason: String, details: String?) {
        scope.launch {
            val base = _serverUrl.value.trim().trimEnd('/')
            val user = _currentUser.value ?: return@launch
            runCatching {
                http.post("$base/api/messaging/users/$userId/report") {
                    contentType(ContentType.Application.Json)
                    header("X-User-Id", user.id)
                    setBody(mapOf("target_user_id" to userId, "reason" to reason, "details" to (details ?: "")))
                }
            }
        }
    }

    private fun syncWithBackend() {
        scope.launch {
            syncProfileToBackend()
            fetchRemoteConversations()
        }
    }

    private suspend fun syncProfileToBackend() {
        val user = _currentUser.value ?: return
        val base = _serverUrl.value.trim().trimEnd('/')
        val fcmToken = FcmTokenManager.token.value

        runCatching {
            http.post("$base/api/messaging/profile") {
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "user_id" to user.id,
                        "display_name" to user.displayName,
                        "username" to user.username,
                        "avatar_url" to user.avatarUrl,
                        "bio" to user.bio,
                        "fcm_token" to fcmToken,
                    )
                )
            }
        }
    }

    private suspend fun fetchRemoteConversations() {
        val user = _currentUser.value ?: return
        val base = _serverUrl.value.trim().trimEnd('/')

        runCatching {
            val response = http.get("$base/api/messaging/conversations") {
                header("X-User-Id", user.id)
            }
            if (response.status.isSuccess()) {
                val data: ConversationsResponse = response.body()
                data.conversations.forEach { c ->
                    val other = MessengerUser(
                        id = c.otherUser.user_id,
                        username = c.otherUser.username,
                        displayName = c.otherUser.display_name,
                        avatarUrl = c.otherUser.avatar_url,
                        bio = c.otherUser.bio,
                        isOnline = c.otherUser.isOnline,
                        lastSeenAt = c.otherUser.last_seen_at,
                    )

                    val lastMsg = c.lastMessage?.let { m ->
                        MessengerMessage(
                            id = m.id,
                            conversationId = m.conversation_id,
                            senderId = m.sender_id,
                            receiverId = m.receiver_id,
                            content = m.content,
                            type = MessageType.fromString(m.type),
                            status = MessageDeliveryStatus.fromString(m.status),
                            createdAt = m.created_at,
                            isOutgoing = m.sender_id == user.id,
                        )
                    }

                    database.upsertConversation(
                        MessengerConversation(
                            id = c.id,
                            otherUser = other,
                            lastMessage = lastMsg,
                            unreadCount = c.unreadCount,
                            updatedAt = c.updatedAt,
                        )
                    )
                }
                loadLocalConversations()
            }
        }
    }

    private suspend fun fetchRemoteMessages(conversationId: String) {
        val user = _currentUser.value ?: return
        val base = _serverUrl.value.trim().trimEnd('/')

        runCatching {
            val response = http.get("$base/api/messaging/conversations/$conversationId/messages") {
                header("X-User-Id", user.id)
                parameter("limit", 50)
            }
            if (response.status.isSuccess()) {
                val data: MessagesResponse = response.body()
                data.messages.forEach { m ->
                    val msg = MessengerMessage(
                        id = m.id,
                        conversationId = m.conversation_id,
                        senderId = m.sender_id,
                        receiverId = m.receiver_id,
                        content = m.content,
                        type = MessageType.fromString(m.type),
                        status = MessageDeliveryStatus.fromString(m.status),
                        createdAt = m.created_at,
                        replyToId = m.reply_to_id,
                        isOutgoing = m.sender_id == user.id,
                    )
                    database.saveMessage(msg)
                }
                val fresh = database.getMessages(conversationId)
                _activeChatMessages.value = fresh
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun String.lstrip(prefix: String): String =
        if (startsWith(prefix)) substring(prefix.length) else this
}

@Serializable
private data class RawBackendUser(
    val user_id: String,
    val username: String,
    val display_name: String,
    val avatar_url: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false,
    val last_seen_at: Long = 0L,
)

@Serializable
private data class UserSearchResponse(
    val users: List<RawBackendUser> = emptyList(),
)

@Serializable
private data class RawBackendMessage(
    val id: String,
    val conversation_id: String,
    val sender_id: String,
    val receiver_id: String,
    val content: String,
    val type: String = "TEXT",
    val status: String = "SENT",
    val created_at: Long = 0L,
    val reply_to_id: String? = null,
)

@Serializable
private data class RawBackendConversation(
    val id: String,
    val otherUser: RawBackendUser,
    val lastMessage: RawBackendMessage? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = 0L,
)

@Serializable
private data class ConversationsResponse(
    val conversations: List<RawBackendConversation> = emptyList(),
)

@Serializable
private data class MessagesResponse(
    val messages: List<RawBackendMessage> = emptyList(),
)
