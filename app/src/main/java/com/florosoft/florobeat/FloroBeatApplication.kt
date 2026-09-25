package com.florosoft.florobeat

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.florosoft.florobeat.auth.AuthStore
import com.florosoft.florobeat.data.canvas.CanvasCache
import com.florosoft.florobeat.data.canvas.SpotifyToken
import com.florosoft.florobeat.playback.AudioCache
import com.florosoft.florobeat.playback.LastPlayed
import com.florosoft.florobeat.playback.OriginalVersion
import com.florosoft.florobeat.data.innertube.Innertube
import com.florosoft.florobeat.data.listentogether.ListenTogether
import com.florosoft.florobeat.data.scrobbling.LastFM
import com.florosoft.florobeat.data.settings.AppSettings
import com.florosoft.florobeat.data.settings.SearchHistory
import com.florosoft.florobeat.data.sources.SourceRegistry
import com.florosoft.florobeat.data.stats.ArtistFacts
import com.florosoft.florobeat.data.stats.ListeningStats
import com.florosoft.florobeat.download.Downloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloroBeatApplication : Application(), SingletonImageLoader.Factory {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // PlaybackService shares this process, so seeding the cookie here means
        // stream resolution is authenticated from the first play onwards.
        authStore = AuthStore(this)
        // Migration-safe: an old single cookie becomes the first encrypted
        // session, while newer installs restore the profile the listener chose.
        val restoredSession = authStore.activeSession
        if (restoredSession != null && authStore.activeAccountId == null) {
            authStore.select(restoredSession.accountId, restoredSession.activeProfileId)
        }
        authStore.cookie = restoredSession?.cookie
        Innertube.cookie = restoredSession?.cookie
        // Which account that cookie actually acts as. Read here rather than on
        // demand so the answer is usually in hand before the first request needs
        // it: a play registered under the wrong account is indistinguishable, to
        // the listener, from one that was never registered at all. Fire and
        // forget — every caller works without it, just less precisely.
        if (restoredSession != null) {
            // After the cookie, never before: setting the cookie clears any
            // channel the last session was acting as, so restoring the choice
            // first would restore it into the value about to be wiped.
            restoredSession.profiles.firstOrNull { it.profileId == restoredSession.activeProfileId }
                ?.let { Innertube.selectChannel(it.pageId, it.dataSyncId, it.authUser) }
            CoroutineScope(Dispatchers.IO).launch { Innertube.ensureSessionScope() }
        }
        AppSettings.init(this)
        com.florosoft.florobeat.data.HomeFeedCache.init(this)
        ListenTogether.init(this)
        SourceRegistry.init(this)
        SearchHistory.init(this)
        LastPlayed.init(this)
        OriginalVersion.init(this)
        Downloads.init(this)
        AudioCache.init(this)

        // Non-critical background initializations off the main UI thread
        CoroutineScope(Dispatchers.IO).launch {
            ListeningStats.init(this@FloroBeatApplication)
            ArtistFacts.init(this@FloroBeatApplication)
            CanvasCache.init(this@FloroBeatApplication)
            SpotifyToken.init(this@FloroBeatApplication)
            initLastfm()
        }

        if (AppSettings.consumeVersionUpdate(BuildConfig.VERSION_CODE)) {
            AudioCache.clear()
            SingletonImageLoader.get(this).let { loader ->
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
            }
        }
    }

    /**
     * Artwork loading, which was previously left entirely on Coil's defaults.
     *
     * The defaults aren't unreasonable, but the disk cache is sized at 2% of
     * free space — which on a full phone is the 10MB floor, a few screens of
     * covers, and covers are exactly the thing worth still having tomorrow.
     * Naming a directory alongside it keeps that cache somewhere identifiable
     * rather than in the process's temp dir.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            // Covers arriving with a hard cut read as the list flickering as
            // it scrolls; a short fade reads as them developing.
            .crossfade(200)
            .build()

    private fun initLastfm() {
        val sessionKey = AppSettings.lastfmSessionKey.value
        if (sessionKey.isBlank()) return
        val endpoint = AppSettings.lastfmEndpoint.value.ifBlank { LastFM.DEFAULT_API_ENDPOINT }
        val apiKey = AppSettings.lastfmApiKey.value.trim()
        val secret = AppSettings.lastfmSecret.value.trim()
        if (apiKey.isBlank() || secret.isBlank()) return
        LastFM.configure(
            endpoint = endpoint,
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
        )
    }

    companion object {
        lateinit var authStore: AuthStore
            private set
    }
}
