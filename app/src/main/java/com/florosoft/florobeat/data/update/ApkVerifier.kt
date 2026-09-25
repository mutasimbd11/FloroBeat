package com.florosoft.florobeat.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Arrays

/**
 * Validates APK file integrity (SHA-256) and compatibility with the current installation
 * (archive validity, packageName, versionCode, and signature compatibility).
 */
object ApkVerifier {

    private val SHA256_HEX_REGEX = Regex("^[a-fA-F0-9]{64}")

    /**
     * Extracts a 64-character hex SHA-256 hash from either a standalone hash string
     * or a standard sha256sum file output ("<hash>  <filename>").
     */
    fun extractSha256(raw: String): String? {
        val trimmed = raw.trim()
        val firstToken = trimmed.split(Regex("\\s+")).firstOrNull() ?: return null
        return if (firstToken.length == 64 && SHA256_HEX_REGEX.matches(firstToken)) {
            firstToken.lowercase()
        } else {
            null
        }
    }

    /**
     * Computes the SHA-256 hex digest of [file] by streaming through [MessageDigest].
     */
    fun calculateSha256(file: File, onProgress: ((Float) -> Unit)? = null): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val totalBytes = file.length()
        var readBytes = 0L

        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytes = input.read(buffer)
            while (bytes != -1) {
                digest.update(buffer, 0, bytes)
                readBytes += bytes
                if (totalBytes > 0 && onProgress != null) {
                    onProgress((readBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                }
                bytes = input.read(buffer)
            }
        }

        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the SHA-256 digest of [file] matches [expectedSha256].
     * Returns true if verified, false if mismatched.
     */
    fun verifySha256(file: File, expectedSha256: String, onProgress: ((Float) -> Unit)? = null): Boolean {
        val cleanExpected = extractSha256(expectedSha256) ?: expectedSha256.trim().lowercase()
        if (cleanExpected.length != 64) return false
        val calculated = calculateSha256(file, onProgress).lowercase()
        return calculated == cleanExpected
    }

    /**
     * Checks if the downloaded APK can be cleanly installed over the current application.
     *
     * Validates:
     * 1. APK file exists and is readable
     * 2. Archive is valid and parseable by Android PackageManager
     * 3. Package name matches FloroBeat (`context.packageName`)
     * 4. Version code is greater than or equal to the running version
     * 5. Signing certificates are compatible with the installed application
     */
    fun verifyApkCompatibility(context: Context, apkFile: File): CompatibilityResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return CompatibilityResult.Incompatible(
                IncompatibilityReason.CORRUPTED_ARCHIVE,
                "Downloaded file does not exist or is empty.",
            )
        }

        val pm = context.packageManager

        // Parse package archive info
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
        }

        val archiveInfo = runCatching {
            pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
        }.getOrNull()

        if (archiveInfo == null) {
            return CompatibilityResult.Incompatible(
                IncompatibilityReason.CORRUPTED_ARCHIVE,
                "This update file is corrupted or not a valid Android application package.",
            )
        }

        // Package name check
        if (archiveInfo.packageName != context.packageName) {
            return CompatibilityResult.Incompatible(
                IncompatibilityReason.PACKAGE_NAME_MISMATCH,
                "Package name mismatch: Expected ${context.packageName}, found ${archiveInfo.packageName}.",
            )
        }

        // Version code check
        val currentPackageInfo = getCurrentPackageInfo(context, flags)
        val currentVersionCode = getVersionCode(currentPackageInfo)
        val archiveVersionCode = getVersionCode(archiveInfo)

        if (archiveVersionCode < currentVersionCode) {
            return CompatibilityResult.Incompatible(
                IncompatibilityReason.OLDER_VERSION_CODE,
                "The update versionCode ($archiveVersionCode) is older than currently installed ($currentVersionCode).",
            )
        }

        // Signature compatibility check
        val signaturesMatch = checkSignatureCompatibility(currentPackageInfo, archiveInfo)
        if (!signaturesMatch) {
            return CompatibilityResult.Incompatible(
                IncompatibilityReason.SIGNATURE_MISMATCH,
                "This update cannot be installed over your current version due to an incompatible signing certificate.",
            )
        }

        return CompatibilityResult.Compatible
    }

    private fun getCurrentPackageInfo(context: Context, flags: Int): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags)
        }
    }

    private fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    @Suppress("DEPRECATION")
    private fun checkSignatureCompatibility(installed: PackageInfo, archive: PackageInfo): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val installedSigning = installed.signingInfo ?: return@runCatching true
                val archiveSigning = archive.signingInfo ?: return@runCatching true

                val installedSigs = if (installedSigning.hasMultipleSigners()) {
                    installedSigning.apkContentsSigners
                } else {
                    installedSigning.signingCertificateHistory
                }

                val archiveSigs = if (archiveSigning.hasMultipleSigners()) {
                    archiveSigning.apkContentsSigners
                } else {
                    archiveSigning.signingCertificateHistory
                }

                if (installedSigs == null || archiveSigs == null || installedSigs.isEmpty() || archiveSigs.isEmpty()) {
                    return@runCatching true
                }

                // Check if any signing certificate matches
                archiveSigs.any { archiveSig ->
                    installedSigs.any { installedSig ->
                        Arrays.equals(archiveSig.toByteArray(), installedSig.toByteArray())
                    }
                }
            } else {
                val installedSigs: Array<Signature>? = installed.signatures
                val archiveSigs: Array<Signature>? = archive.signatures
                if (installedSigs == null || archiveSigs == null || installedSigs.isEmpty() || archiveSigs.isEmpty()) {
                    return@runCatching true
                }
                archiveSigs.any { archiveSig ->
                    installedSigs.any { installedSig ->
                        Arrays.equals(archiveSig.toByteArray(), installedSig.toByteArray())
                    }
                }
            }
        }.getOrDefault(true)
    }
}
