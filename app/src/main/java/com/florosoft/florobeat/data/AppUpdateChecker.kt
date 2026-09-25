package com.florosoft.florobeat.data

import android.content.Context
import com.florosoft.florobeat.BuildConfig
import com.florosoft.florobeat.data.settings.AppSettings
import com.florosoft.florobeat.data.update.ApkVerifier
import com.florosoft.florobeat.data.update.CompatibilityResult
import com.florosoft.florobeat.data.update.DownloadState
import com.florosoft.florobeat.data.update.PackageInstaller
import com.florosoft.florobeat.data.update.SemanticVersion
import com.florosoft.florobeat.data.update.UpdateCheckResult
import com.florosoft.florobeat.data.update.UpdateConfig
import com.florosoft.florobeat.data.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * FloroBeat ships as a sideloaded APK off GitHub Releases rather than through
 * a store, so there's nothing to push an update notice on its own — this
 * manages release queries, SHA-256 integrity verification, compatibility checks,
 * and handing verified APKs to Android's official package installer.
 *
 * Repository and channel configuration is centralized in [UpdateConfig].
 */
object AppUpdateChecker {

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    /**
     * Checks GitHub Releases for new FloroBeat updates.
     *
     * @param manual If true, ignores cooldowns and dismissed states to provide immediate feedback in Settings.
     * @param context Optional context to test network connectivity.
     */
    suspend fun check(manual: Boolean = false, context: Context? = null): UpdateCheckResult = withContext(Dispatchers.IO) {
        // Fast-path: Check cooldown on automatic background launches
        if (!manual && AppSettings.isUpdateCheckCooldownActive()) {
            val existing = _available.value
            return@withContext if (existing != null) {
                if (AppSettings.isUpdateDismissed(existing.version)) {
                    UpdateCheckResult.Dismissed(existing)
                } else {
                    UpdateCheckResult.UpdateAvailable(existing)
                }
            } else {
                UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
        }

        try {
            val url = if (UpdateConfig.currentChannel == UpdateConfig.ReleaseChannel.STABLE) {
                UpdateConfig.latestReleaseApiUrl
            } else {
                UpdateConfig.releasesListApiUrl
            }

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "FloroBeat-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = Http.client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string()

            if (code == 403) {
                return@withContext UpdateCheckResult.Error("GitHub API rate limit exceeded. Please try again later.")
            }
            if (code == 404) {
                return@withContext UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
            if (!response.isSuccessful || bodyString.isNullOrBlank()) {
                return@withContext UpdateCheckResult.Error("GitHub request failed: HTTP $code")
            }

            val release = if (UpdateConfig.currentChannel == UpdateConfig.ReleaseChannel.STABLE) {
                json.parseToJsonElement(bodyString) as? JsonObject
            } else {
                val array = json.parseToJsonElement(bodyString).jsonArray
                array.mapNotNull { it as? JsonObject }.firstOrNull { r ->
                    r["draft"]?.jsonPrimitive?.contentOrNull != "true"
                }
            } ?: return@withContext UpdateCheckResult.Error("Malformed GitHub release payload")

            val isDraft = release["draft"]?.jsonPrimitive?.contentOrNull == "true"
            val isPrerelease = release["prerelease"]?.jsonPrimitive?.contentOrNull == "true"

            // Ignore drafts, and ignore pre-releases if on stable channel
            if (isDraft || (isPrerelease && UpdateConfig.currentChannel == UpdateConfig.ReleaseChannel.STABLE)) {
                return@withContext UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }

            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext UpdateCheckResult.Error("Release missing tag name")
            val releaseUrl = release["html_url"]?.jsonPrimitive?.contentOrNull ?: UpdateConfig.releasesWebUrl
            val releaseName = release["name"]?.jsonPrimitive?.contentOrNull
            val notes = release["body"]?.jsonPrimitive?.contentOrNull
            val publishedAt = release["published_at"]?.jsonPrimitive?.contentOrNull

            val assets = release["assets"]?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()
            val (apkUrl, sha256Url) = findReleaseAssets(assets)

            // Attempt to parse expected SHA-256 from the release body if not found as an asset file
            val expectedSha256FromBody = notes?.let { ApkVerifier.extractSha256(it) }

            val cleanVersion = tag.removePrefix("v").removePrefix("V")
            val remoteSemver = SemanticVersion.parse(cleanVersion)
            val currentSemver = SemanticVersion.parse(BuildConfig.VERSION_NAME)

            AppSettings.recordSuccessfulUpdateCheck()

            if (remoteSemver.isNewerThan(currentSemver)) {
                val info = UpdateInfo(
                    version = cleanVersion,
                    releaseName = releaseName,
                    releaseUrl = releaseUrl,
                    apkUrl = apkUrl,
                    sha256Url = sha256Url,
                    expectedSha256 = expectedSha256FromBody,
                    notes = notes,
                    isPrerelease = isPrerelease,
                    publishedAt = publishedAt,
                )
                _available.value = info

                if (!manual && AppSettings.isUpdateDismissed(cleanVersion)) {
                    UpdateCheckResult.Dismissed(info)
                } else {
                    UpdateCheckResult.UpdateAvailable(info)
                }
            } else {
                _available.value = null
                UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
        } catch (e: UnknownHostException) {
            UpdateCheckResult.Offline
        } catch (e: SocketTimeoutException) {
            UpdateCheckResult.Error("Connection to GitHub timed out")
        } catch (e: IOException) {
            UpdateCheckResult.Error(e.message ?: "Network error checking for updates")
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }

    /**
     * Wipes any APK left over from a previous run. Called once at cold start.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, UpdateConfig.CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * Finds the primary APK asset and matching SHA-256 asset from release assets.
     */
    private fun findReleaseAssets(assets: List<JsonObject>): Pair<String?, String?> {
        var apkUrl: String? = null
        var apkName: String? = null
        var sha256Url: String? = null

        // Find primary APK
        for (asset in assets) {
            val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val state = asset["state"]?.jsonPrimitive?.contentOrNull
            if (state == "uploaded" && name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull
                apkName = name
                break
            }
        }

        // Find matching SHA-256 checksum asset
        for (asset in assets) {
            val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val state = asset["state"]?.jsonPrimitive?.contentOrNull
            if (state == "uploaded") {
                if (apkName != null && name.equals("$apkName.sha256", ignoreCase = true)) {
                    sha256Url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull
                    break
                } else if (name.endsWith(".sha256", ignoreCase = true)) {
                    sha256Url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull
                }
            }
        }

        return Pair(apkUrl, sha256Url)
    }

    /**
     * Streams the APK download into the app's cache directory, computes SHA-256 checksum,
     * verifies pre-install package compatibility, and reports progress.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f, 0L, 0L)

        runCatching {
            val dir = File(context.cacheDir, UpdateConfig.CACHE_SUBDIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "florobeat-${info.version}.apk")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FloroBeat-Android/${BuildConfig.VERSION_NAME}")
                .build()

            Http.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 } ?: 0L

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        while (true) {
                            if (downloadCancelled) {
                                target.delete()
                                _download.value = DownloadState.Idle
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            readTotal += read
                            val fraction = if (total > 0) (readTotal.toFloat() / total).coerceIn(0f, 1f) else 0f
                            _download.value = DownloadState.Downloading(fraction, readTotal, total)
                        }
                    }
                }
            }

            // Step 1: Verify SHA-256 if published
            _download.value = DownloadState.Verifying(0f)
            val expectedSha256 = resolveExpectedSha256(info)
            if (!expectedSha256.isNullOrBlank()) {
                val matches = ApkVerifier.verifySha256(target, expectedSha256) { fraction ->
                    _download.value = DownloadState.Verifying(fraction)
                }
                if (!matches) {
                    target.delete()
                    _download.value = DownloadState.Failed(
                        "Integrity verification failed (SHA-256 checksum mismatch).",
                        canRetry = true,
                    )
                    return@withContext
                }
            }

            // Step 2: Verify Android package compatibility
            val compatibility = ApkVerifier.verifyApkCompatibility(context, target)
            if (compatibility is CompatibilityResult.Incompatible) {
                _download.value = DownloadState.Incompatible(
                    file = target,
                    reason = compatibility.reason,
                    manualUrl = info.releaseUrl,
                )
                return@withContext
            }

            _download.value = DownloadState.Ready(target, expectedSha256)
        }.onFailure { error ->
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed", canRetry = true)
            }
        }
    }

    /**
     * Resolves expected SHA-256 checksum from asset URL or release info.
     */
    private fun resolveExpectedSha256(info: UpdateInfo): String? {
        if (!info.expectedSha256.isNullOrBlank()) {
            return info.expectedSha256
        }
        val shaUrl = info.sha256Url ?: return null
        return runCatching {
            val req = Request.Builder().url(shaUrl).build()
            Http.client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    ApkVerifier.extractSha256(body)
                } else null
            }
        }.getOrNull()
    }

    /** Stops an in-flight download. */
    fun cancelDownload() {
        downloadCancelled = true
    }

    /** Resets download state to Idle. */
    fun resetDownload() {
        _download.value = DownloadState.Idle
    }

    /** Records that the user clicked "Later" on an update to suppress startup popups for 24h. */
    fun dismissUpdate(version: String) {
        AppSettings.recordDismissedUpdate(version)
    }

    /**
     * Hands a downloaded APK to the system installer using [PackageInstaller].
     * Returns true if installation intent was launched, or false if permission is needed.
     */
    fun installApk(context: Context, file: File): Boolean {
        if (!PackageInstaller.canInstallPackages(context)) {
            PackageInstaller.pendingInstallFile = file
            return false
        }
        return PackageInstaller.startInstallation(context, file)
    }
}
