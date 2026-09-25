package com.florosoft.florobeat.data.update

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Metadata for an update discovered from GitHub Releases.
 */
data class UpdateInfo(
    val version: String,
    val releaseName: String?,
    val releaseUrl: String,
    val apkUrl: String?,
    val sha256Url: String?,
    val expectedSha256: String?,
    /** Markdown body containing release notes ("What's New"). */
    val notes: String?,
    val isPrerelease: Boolean = false,
    val publishedAt: String? = null,
)

/**
 * State machine for in-app APK download and verification.
 */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val fraction: Float, val bytesRead: Long = 0L, val totalBytes: Long = 0L) : DownloadState
    data class Verifying(val fraction: Float = 0f) : DownloadState
    data class Ready(val file: File, val expectedSha256: String? = null) : DownloadState
    data class Failed(val message: String, val canRetry: Boolean = true) : DownloadState
    data class Incompatible(val file: File, val reason: IncompatibilityReason, val manualUrl: String) : DownloadState
}

/**
 * Reasons why a downloaded APK might be incompatible with the current installation.
 */
enum class IncompatibilityReason {
    /** APK archive is corrupted, incomplete, or cannot be parsed by Android PackageManager. */
    CORRUPTED_ARCHIVE,

    /** Package name does not match FloroBeat's applicationId. */
    PACKAGE_NAME_MISMATCH,

    /** Downloaded versionCode is older than currently installed versionCode. */
    OLDER_VERSION_CODE,

    /** APK does not support the device's CPU architecture (ABI). */
    UNSUPPORTED_ABI,

    /** APK signing certificate does not match the running application's signing identity. */
    SIGNATURE_MISMATCH,
}

/**
 * Result of pre-installation APK compatibility checks.
 */
sealed interface CompatibilityResult {
    data object Compatible : CompatibilityResult
    data class Incompatible(val reason: IncompatibilityReason, val message: String) : CompatibilityResult
}

/**
 * Result of checking GitHub Releases for updates.
 */
sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult
    data class Dismissed(val info: UpdateInfo) : UpdateCheckResult
    data object Offline : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}
