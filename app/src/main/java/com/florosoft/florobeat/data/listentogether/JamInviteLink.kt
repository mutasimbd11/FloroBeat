package com.florosoft.florobeat.data.listentogether

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

/** Relays a FloroBeat web invite from [com.florosoft.florobeat.MainActivity] to Compose. */
object JamInviteLink {

    const val ORIGIN = "https://florobeat.florosoft.com"

    private const val EXTRA_CONSUMED = "florobeat.jamInviteConsumed"
    private const val HOST = "florobeat.florosoft.com"

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Reads a web invite from a cold launch or a new intent on the existing task. */
    fun consume(intent: Intent?): Boolean {
        if (
            intent == null ||
            intent.action != Intent.ACTION_VIEW ||
            intent.getBooleanExtra(EXTRA_CONSUMED, false)
        ) return false

        val code = parse(intent.dataString) ?: return false
        intent.putExtra(EXTRA_CONSUMED, true)
        _pending.value = code
        return true
    }

    fun handled() {
        _pending.value = null
    }

    /** Returns the normalized party code only for the public invite URL shape. */
    fun parse(value: String?): String? {
        val uri = runCatching { URI(value ?: return null) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (!uri.host.equals(HOST, ignoreCase = true)) return null

        val match = INVITE_PATH.matchEntire(uri.path.orEmpty()) ?: return null
        return match.groupValues[1].uppercase()
    }

    fun url(code: String): String = "$ORIGIN/invite/${code.uppercase()}"

    private val INVITE_PATH = Regex("""/invite/([A-Za-z0-9]{${ListenTogether.CODE_LENGTH}})""")
}
