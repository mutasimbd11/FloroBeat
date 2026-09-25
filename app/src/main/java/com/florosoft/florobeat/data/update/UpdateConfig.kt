package com.florosoft.florobeat.data.update

/**
 * Centralized configuration for FloroBeat's GitHub Release based in-app update system.
 *
 * FloroBeat is distributed directly via GitHub Releases as an open-source APK without Google Play.
 * All repository identifiers, channels, and update policies are maintained here in one place
 * so future rebranding, forks, or sister FloroSoft projects can reconfigure the update system cleanly.
 */
object UpdateConfig {

    /**
     * GitHub repository owner/organization.
     * Official FloroBeat repository: https://github.com/mutasimbd11/FloroBeat
     */
    const val GITHUB_OWNER = "mutasimbd11"

    /**
     * GitHub repository name.
     */
    const val GITHUB_REPO = "FloroBeat"

    /**
     * Release distribution channels.
     */
    enum class ReleaseChannel {
        /** Official stable production builds. Pre-releases and drafts are ignored. */
        STABLE,

        /** Preview/beta releases. Includes stable and pre-releases marked as beta. */
        BETA,

        /** Bleeding-edge nightly/continuous builds. */
        NIGHTLY,
    }

    /**
     * The active release channel. Default is [ReleaseChannel.STABLE].
     * Can optionally be toggled in future developer/experimental settings.
     */
    var currentChannel: ReleaseChannel = ReleaseChannel.STABLE

    /**
     * Base directory within the app's cache directory where downloaded update APKs are stored.
     * Mapped to the <cache-path name="updates" path="updates/" /> in file_paths.xml.
     */
    const val CACHE_SUBDIR = "updates"

    /**
     * Minimum interval between automatic background update queries on application startup (6 hours).
     * Prevents excessive GitHub API queries while the app is started and restarted frequently.
     */
    const val BACKGROUND_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L

    /**
     * Cooldown period after the user clicks "Later" on an update dialog (24 hours).
     * During this period, automatic startup popups for this version are suppressed,
     * while the quiet top-bar badge and manual "Check for Updates" still remain available.
     */
    const val DISMISSED_REMIND_COOLDOWN_MS = 24 * 60 * 60 * 1000L

    /**
     * GitHub API URL for the latest stable release:
     * https://api.github.com/repos/mutasimbd11/FloroBeat/releases/latest
     */
    val latestReleaseApiUrl: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * GitHub API URL to list releases:
     * https://api.github.com/repos/mutasimbd11/FloroBeat/releases
     */
    val releasesListApiUrl: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"

    /**
     * Web URL to the GitHub Releases page:
     * https://github.com/mutasimbd11/FloroBeat/releases
     */
    val releasesWebUrl: String
        get() = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
}
