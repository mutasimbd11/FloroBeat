package com.florosoft.florobeat.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Official Android package installation helper using FileProvider and ACTION_VIEW.
 *
 * Adheres strictly to Android security guidelines:
 * - Uses official Android package installation APIs
 * - Handles the Android 8.0+ (API 26+) per-app "Install unknown apps" permission
 * - Does not attempt silent background installs, root, or accessibility workarounds
 * - Stores a pending install file reference to resume installation smoothly when returning from Settings
 */
object PackageInstaller {

    /**
     * Set when the user is guided to Settings to enable "Install unknown apps",
     * so MainActivity can immediately resume installation on resume.
     */
    @Volatile
    var pendingInstallFile: File? = null

    /**
     * Checks whether the application has permission to request package installs.
     * Returns true on Android 7.1 and below (where unknown sources was a global switch),
     * and on Android 8.0+ if the per-app permission is granted.
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    /**
     * Opens Android's official "Install unknown apps" settings screen for FloroBeat.
     */
    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Hands the verified APK file to Android's system package installer.
     */
    fun startInstallation(context: Context, file: File): Boolean {
        return runCatching {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            pendingInstallFile = null
            true
        }.getOrElse {
            false
        }
    }
}
