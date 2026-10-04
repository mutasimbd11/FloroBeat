package com.florosoft.florobeat.data.messenger

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit

/**
 * Real-time WebSocket connection client for FloroBeat Messenger.
 *
 * Implements non-blocking frame dispatch, heartbeat keep-alives, and an exponential
 * backoff reconnect loop that survives screen-off, radio sleep, and network handovers.
 */
class MessengerClient(
    private val serverUrlProvider: () -> String,
) {
    companion object {
        private const val TAG = "FloroBeatMessenger"
    }

    enum class ConnectionStatus { OFFLINE, CONNECTING, CONNECTED }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http = HttpClient(OkHttp) {
        engine {
            config {
                readTimeout(0, TimeUnit.MILLISECONDS)
                connectTimeout(15, TimeUnit.SECONDS)
                pingInterval(20, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout)
        install(WebSockets)
        expectSuccess = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socketJob: Job? = null
    @Volatile
    private var session: DefaultClientWebSocketSession? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _events = MutableSharedFlow<MessengerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MessengerEvent> = _events.asSharedFlow()

    private var activeUserId: String? = null

    val isConnected: Boolean
        get() = _connectionStatus.value == ConnectionStatus.CONNECTED && session != null

    fun connect(userId: String) {
        if (activeUserId == userId && socketJob?.isActive == true) return
        activeUserId = userId
        socketJob?.cancel()
        socketJob = scope.launch {
            runSocketLoop(userId)
        }
    }

    fun disconnect() {
        activeUserId = null
        socketJob?.cancel()
        socketJob = null
        session = null
        _connectionStatus.value = ConnectionStatus.OFFLINE
    }

    private suspend fun runSocketLoop(userId: String) {
        var backoffMs = 1_000L
        while (currentCoroutineContext().isActive && activeUserId == userId) {
            val base = serverUrlProvider().trim().trimEnd('/')
            if (base.isBlank()) {
                _connectionStatus.value = ConnectionStatus.OFFLINE
                delay(3_000L)
                continue
            }

            val wsUrl = "${wsBase(base)}/api/messaging/ws/$userId"

            try {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                http.webSocket(wsUrl) {
                    session = this
                    backoffMs = 1_000L
                    _connectionStatus.value = ConnectionStatus.CONNECTED

                    val pingJob = launch { pingLoop() }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleIncomingFrame(frame.readText())
                            }
                        }
                    } finally {
                        pingJob.cancel()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Messenger socket dropped: ${failure.message}")
            } finally {
                session = null
            }

            if (!currentCoroutineContext().isActive || activeUserId != userId) return
            _connectionStatus.value = ConnectionStatus.CONNECTING
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(20_000L)
        }
    }

    private suspend fun pingLoop() {
        while (currentCoroutineContext().isActive) {
            delay(25_000L)
            val pingFrame = buildJsonObject { put("type", "ping") }
            sendFrame(pingFrame)
        }
    }

    private suspend fun handleIncomingFrame(text: String) {
        runCatching {
            val obj = json.parseToJsonElement(text).jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return

            when (type) {
                "message" -> {
                    val msgObj = obj["message"]?.jsonObject ?: return
                    val msg = parseMessage(msgObj)
                    _events.emit(MessengerEvent.InboundMessage(msg))
                }
                "message_sent_ack" -> {
                    val msgId = obj["messageId"]?.jsonPrimitive?.contentOrNull ?: return
                    val status = MessageDeliveryStatus.fromString(obj["status"]?.jsonPrimitive?.contentOrNull)
                    val createdAt = obj["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                    _events.emit(MessengerEvent.MessageSentAck(msgId, status, createdAt))
                }
                "delivery_receipt" -> {
                    val msgId = obj["messageId"]?.jsonPrimitive?.contentOrNull ?: return
                    val status = MessageDeliveryStatus.fromString(obj["status"]?.jsonPrimitive?.contentOrNull)
                    _events.emit(MessengerEvent.DeliveryReceipt(msgId, status))
                }
                "read_receipt" -> {
                    val convId = obj["conversationId"]?.jsonPrimitive?.contentOrNull ?: return
                    val ids = obj["messageIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    _events.emit(MessengerEvent.ReadReceipt(convId, ids))
                }
                "typing" -> {
                    val convId = obj["conversationId"]?.jsonPrimitive?.contentOrNull ?: return
                    val userId = obj["userId"]?.jsonPrimitive?.contentOrNull ?: return
                    val isTyping = obj["isTyping"]?.jsonPrimitive?.booleanOrNull ?: false
                    _events.emit(MessengerEvent.Typing(convId, userId, isTyping))
                }
                "presence" -> {
                    val userId = obj["userId"]?.jsonPrimitive?.contentOrNull ?: return
                    val isOnline = obj["isOnline"]?.jsonPrimitive?.booleanOrNull ?: false
                    val lastSeen = obj["lastSeenAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                    _events.emit(MessengerEvent.Presence(userId, isOnline, lastSeen))
                }
                "message_deleted" -> {
                    val msgId = obj["messageId"]?.jsonPrimitive?.contentOrNull ?: return
                    val convId = obj["conversationId"]?.jsonPrimitive?.contentOrNull ?: return
                    val forEveryone = obj["forEveryone"]?.jsonPrimitive?.booleanOrNull ?: false
                    _events.emit(MessengerEvent.MessageDeleted(msgId, convId, forEveryone))
                }
            }
        }.onFailure {
            Log.w(TAG, "Failed to parse frame: ${it.message}")
        }
    }

    private fun parseMessage(obj: JsonObject): MessengerMessage {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val convId = obj["conversation_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val senderId = obj["sender_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val receiverId = obj["receiver_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val content = obj["content"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val type = MessageType.fromString(obj["type"]?.jsonPrimitive?.contentOrNull)
        val status = MessageDeliveryStatus.fromString(obj["status"]?.jsonPrimitive?.contentOrNull)
        val createdAt = obj["created_at"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        val replyToId = obj["reply_to_id"]?.jsonPrimitive?.contentOrNull

        return MessengerMessage(
            id = id,
            conversationId = convId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            type = type,
            status = status,
            createdAt = createdAt,
            replyToId = replyToId,
            isOutgoing = senderId == activeUserId,
        )
    }

    fun sendMessage(msg: MessengerMessage) {
        val frame = buildJsonObject {
            put("type", "send_message")
            put("id", msg.id)
            put("conversationId", msg.conversationId)
            put("receiverId", msg.receiverId)
            put("content", msg.content)
            put("msgType", msg.type.name)
            msg.replyToId?.let { put("replyToId", it) }
        }
        sendFrame(frame)
    }

    fun sendTyping(conversationId: String, targetUserId: String, isTyping: Boolean) {
        val frame = buildJsonObject {
            put("type", "typing")
            put("conversationId", conversationId)
            put("targetUserId", targetUserId)
            put("isTyping", isTyping)
        }
        sendFrame(frame)
    }

    fun sendReadReceipt(conversationId: String, senderId: String, upToTime: Long) {
        val frame = buildJsonObject {
            put("type", "read")
            put("conversationId", conversationId)
            put("senderId", senderId)
            put("upToTime", upToTime)
        }
        sendFrame(frame)
    }

    fun sendDeliveryReceipt(messageId: String, senderId: String) {
        val frame = buildJsonObject {
            put("type", "delivery_receipt")
            put("messageId", messageId)
            put("senderId", senderId)
        }
        sendFrame(frame)
    }

    private fun sendFrame(frame: JsonObject) {
        val live = session ?: return
        scope.launch {
            runCatching {
                live.send(Frame.Text(frame.toString()))
            }
        }
    }

    private fun wsBase(httpBase: String): String = when {
        httpBase.startsWith("https://") -> "wss://" + httpBase.removePrefix("https://")
        httpBase.startsWith("http://") -> "ws://" + httpBase.removePrefix("http://")
        else -> "wss://$httpBase"
    }
}
