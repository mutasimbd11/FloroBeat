package com.florosoft.florobeat.notifications

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Firebase Cloud Messaging registration tokens.
 *
 * Keeps token state in-memory and safely persisted in local private storage.
 * Does not transmit tokens to third parties or log full tokens to system logs.
 */
object FcmTokenManager {

    private const val PREFS_NAME = "florobeat_fcm"
    private const val KEY_TOKEN = "fcm_token"
    private const val KEY_TOKEN_TIMESTAMP = "fcm_token_timestamp"

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private var prefs: SharedPreferences? = null

    /**
     * Initializes the token manager with application context, loading any cached token.
     */
    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _token.value = sp.getString(KEY_TOKEN, null)

        // Attempt initial token retrieval if Firebase is configured
        fetchCurrentToken()
    }

    /**
     * Called when FCM generates a new registration token.
     */
    fun onNewToken(newToken: String) {
        if (newToken.isBlank()) return
        _token.value = newToken
        prefs?.edit()
            ?.putString(KEY_TOKEN, newToken)
            ?.putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            ?.apply()

        // Architectural extension point: ready for future backend registration
        // e.g. BackendApi.registerDeviceToken(newToken)
    }

    /**
     * Retrieves the active token from FirebaseMessaging asynchronously and safely.
     */
    fun fetchCurrentToken(onComplete: ((String?) -> Unit)? = null) {
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && !task.result.isNullOrBlank()) {
                        val tokenValue = task.result
                        onNewToken(tokenValue)
                        onComplete?.invoke(tokenValue)
                    } else {
                        onComplete?.invoke(_token.value)
                    }
                }
        }.onFailure {
            // Firebase may not be initialized yet if google-services.json is not present
            onComplete?.invoke(_token.value)
        }
    }

    /**
     * Returns a safely masked token string for diagnostics (e.g. "fK8j...9x2A").
     * Never prints or exposes the full token.
     */
    fun getMaskedToken(): String {
        val current = _token.value
        if (current.isNullOrBlank()) return "[none]"
        return if (current.length > 12) {
            "${current.take(6)}...${current.takeLast(4)}"
        } else {
            "***"
        }
    }
}
