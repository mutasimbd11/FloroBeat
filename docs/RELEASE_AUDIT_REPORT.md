# FloroBeat — Production Release & Android Security Audit Report

**Date:** September 25, 2026  
**Application Name:** FloroBeat  
**Developer Organization:** FloroSoft  
**Application ID:** `com.florosoft.florobeat`  
**Current Version:** 1.6 (`versionCode: 17`)  
**Target SDK:** 36 (Android 16)  
**Min SDK:** 26 (Android 8.0 Oreo)  

---

## 1. Executive Summary

This audit and configuration pass elevates FloroBeat from development-grade builds to a securely signed, production-ready Android application. It directly addresses the Google Play Protect warning shown when sideloading development builds (*"Play Protect hasn't seen an app from this developer before. It may be unsafe."*).

Users should **never** be instructed to disable Play Protect. Instead, the application has been engineered to eliminate suspicious heuristics, employ a stable 4096-bit RSA cryptographic signing identity, enforce non-debuggable release configuration, support modern Android 16 (API 36) standards, and provide both direct-distribution signed APKs and Google Play App Bundle (`.aab`) distribution.

---

## 2. Root Cause Analysis: Play Protect Warning

The block captured in the installation screenshot was investigated across three distinct vectors:

| Finding | Risk Factor | Remediation |
|---|---|---|
| **Debug Build Sideloaded** | The previous build was a `devDebug` variant (`com.dev.florobeat`, `debuggable="true"`), signed with Android SDK's public `debug.keystore`. Debug APKs trigger immediate warnings when sideloaded because the public debug key is well-known and insecure. | Switched strictly to `prodRelease` (`com.florosoft.florobeat`), built with `android:debuggable="false"` and signed with the private FloroSoft production keystore. |
| **`REQUEST_INSTALL_PACKAGES` Declared** | The manifest declared `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`. Google Play Protect's heuristic engine heavily penalizes non-app-store applications requesting this permission, classifying them as potential "droppers" or unvetted package installers. | **Removed entirely** from `AndroidManifest.xml`. If direct package installation is unavailable on device, the app cleanly hands off to the official browser download URL without needing elevated installer privileges. |
| **New Developer Sideloading Heuristic** | Play Protect explicitly states: *"Play Protect hasn't seen an app from this developer before. It may be unsafe."* This is an automated Google reputation system for newly encountered sideloaded signing certificates that have not yet established reputation across Google Play's ecosystem. | Proper 4096-bit RSA release signing ensures a **permanent, stable signing identity**. For zero-friction user installation, publishing via Google Play App Bundle (`.aab`) with Google Play App Signing is the standard path. For sideloading, an official Google Play Protect developer appeal can be submitted if false positives occur. |

---

## 3. Production Signing Configuration

A dedicated, permanent release keystore has been provisioned and configured:

- **Keystore File:** `florobeat-release.jks` *(strictly gitignored)*
- **Key Alias:** `florobeat`
- **Key Algorithm:** RSA 4096-bit
- **Certificate Validity:** 10,000 days (valid through February 2054)
- **Certificate Subject:** `CN=FloroBeat Release, OU=Mobile Engineering, O=FloroSoft, L=Dhaka, ST=Dhaka, C=BD`
- **Certificate SHA-256 Digest:**  
  `A0:99:1D:D8:77:C0:9D:9C:51:09:9E:62:01:2E:59:A8:9C:9F:40:E3:20:57:FF:A7:A4:29:40:4E:FE:9F:14:F1`
- **Certificate SHA-1 Digest:**  
  `E5:9E:2A:4C:AA:8B:D7:4A:6E:B0:E9:80:D8:D3:B2:6C:CB:24:61:A1`

### Signature Schemes Verified
The release build was verified using Android SDK build-tools `apksigner`:
- **v2 Scheme (APK Signature Scheme v2):** `true`
- **v3 Scheme (APK Signature Scheme v3):** `true`
- **v4 Scheme (APK Signature Scheme v4):** `true` (generated `.idsig` for Android 11+ fast streaming installation)
- **v1 Scheme (JAR signing):** Omitted by AGP because `minSdk >= 24` (v2/v3 whole-file digest is cryptographically superior).

### Secret Management
- `florobeat-release.jks` and `keystore.properties` are strictly excluded in `.gitignore`.
- Gradle reads credentials via `keystore.properties` locally or via CI environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

---

## 4. Manifest & Permissions Audit

Every permission declared in `AndroidManifest.xml` has been reviewed and justified. Suspicious and unnecessary permissions have been eliminated:

| Permission | Status | Justification |
|---|---|---|
| `INTERNET` | Retained | Required for streaming audio, metadata fetching, and lyrics. |
| `ACCESS_NETWORK_STATE` | Retained | Used to apply user's Wi-Fi vs mobile cellular quality caps. |
| `WAKE_LOCK` | Retained | Prevents CPU sleep during active audio decoding and playback. |
| `FOREGROUND_SERVICE` | Retained | Required for background media playback service. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Retained | Required by Android 14+ for foreground audio playback. |
| `FOREGROUND_SERVICE_DATA_SYNC` | Retained | Required for background audio downloads to Music storage. |
| `POST_NOTIFICATIONS` | Retained | Required for media player notifications on Android 13+. |
| `VIBRATE` | Retained | Contextual haptic feedback (respects system haptics toggle). |
| `BLUETOOTH_CONNECT` | Retained | Required on API 31+ to resolve paired headset device names. |
| `READ_MEDIA_AUDIO` | Retained | Scoped audio-only permission for scanning local music files. |
| `READ_EXTERNAL_STORAGE` | Scoped (`maxSdkVersion="32"`) | Legacy fallback for Android 12 and below. |
| `WRITE_EXTERNAL_STORAGE` | Scoped (`maxSdkVersion="28"`) | Legacy fallback for Android 9 and below. |
| `REQUEST_INSTALL_PACKAGES` | **REMOVED** | High-risk permission removed to eliminate dropper detection. |
| `SYSTEM_ALERT_WINDOW` | **NOT PRESENT** | No overlay permission requested. |
| `QUERY_ALL_PACKAGES` | **NOT PRESENT** | No package enumeration permission requested. |

### Component Security
- **`MainActivity`:** `exported="true"`, protected with explicit `LAUNCHER`, `APP_MUSIC`, and `VIEW` intent filters.
- **`PlaybackService`:** `exported="true"`, bounded to `mediaPlayback` foreground type and Android MediaSession / Android Auto / Bluetooth intents.
- **`DownloadService`:** `exported="false"`, internal background download management only.
- **Widgets (`MediaWidgetSquare`, `MediaWidgetWide`, `MediaWidgetActions`):** `exported="false"`.
- **`FileProvider`:** `exported="false"`, `grantUriPermissions="true"`.

---

## 5. Network Security & Data Privacy

- **Cleartext Traffic:** Disabled globally (`android:usesCleartextTraffic="false"`). All network communication enforces TLS/HTTPS.
- **Endpoints:** Production endpoints only (YouTube Music, LRCLIB, Genius, Last.fm, Listen Together production server).
- **Authentication:** OAuth authentication is handled cleanly without hardcoded tokens, fake credentials, or sensitive leaks.
- **Privacy:** No unauthorized user telemetry, third-party analytics SDKs, or background trackers are included.

---

## 6. Target SDK & Platform Compatibility

- **`compileSdk: 36`** (Android 16)
- **`targetSdk: 36`** (Android 16)  
  *Satisfies Google Play's upcoming requirement mandating target SDK 36.*
- **`minSdk: 26`** (Android 8.0 Oreo)  
  *Covers ~96% of active Android devices globally.*
- **Screen & Density Support:** `small`, `normal`, `large`, `xlarge`; responsive layouts and foldable/tablet configurations supported.
- **Native ABI Matrix:**
  - `arm64-v8a` (Primary 64-bit ARM architecture for modern phones)
  - `armeabi-v7a` (32-bit ARM for legacy devices)
  - `x86_64` (64-bit x86 for Chromebooks and emulators)
  - Universal APK (includes all native binaries for one-click sideloading)

---

## 7. Build Artifacts & Checksums

### Production Build Outputs

| Artifact Name | Size | SHA-256 Checksum | Target Use Case |
|---|---|---|---|
| **`app-prod-release.aab`** | 101.8 MB | `B7D7C8D6B1DC6CBF11468403302A3642DC32297D13315B2C4634DA0CA5EEF6C2` | **Google Play Console** (Recommended for zero-friction distribution) |
| **`app-prod-universal-release.apk`** | 151.8 MB | `362366BBF397899F59E2BAC9FFF7C099B7ADAE54F18A565E82E671767F6488ED` | **Universal Sideload APK** (Compatible with all supported Android phones) |
| **`app-prod-arm64-v8a-release.apk`** | 63.3 MB | `F316481405531F2356A0ABE85C0C9F80BF7B15AFF31C1177FBBF680BD9602FCB` | Sideload APK optimized for modern 64-bit ARM devices |
| **`app-prod-armeabi-v7a-release.apk`** | 55.0 MB | `6B0138A2003CA8E1FCDC695CBF5960E78BEED296FF8410B3F51F64C1DF7EE63D` | Sideload APK optimized for 32-bit ARM legacy devices |
| **`app-prod-x86_64-release.apk`** | 69.0 MB | `8C40F22989655BBF6BD8B4C359D1D1AAC70B66B290C0C8B91495BB7A2B99B0E2` | Sideload APK optimized for x86_64 tablets / Chromebooks |

---

## 8. Play Protect & Verification Guidance

### Understanding Play Protect Sideload Warnings
Google Play Protect operates a cloud-based reputation service. When an APK is downloaded directly from a web browser (outside Google Play), Play Protect inspects the signing key against known applications. If the signing key has not yet accumulated sufficient install volume on Google Play:
1. Play Protect may display: *"Play Protect hasn't seen an app from this developer before. It may be unsafe."*
2. Under "More details", users can tap *"Install anyway"*.
3. **Never instruct users to turn off Play Protect entirely.**

### Recommended Paths to Eliminate Sideload Warnings
1. **Google Play Store Publishing (Primary & Recommended):**  
   Upload `app-prod-release.aab` to Google Play Console with Google Play App Signing. Apps distributed via Google Play undergo automated review and are recognized immediately by Play Protect across all Android devices.
2. **Google Play Protect Developer Appeal:**  
   If directly distributing via GitHub Releases and Play Protect displays false positive warnings, Google provides an official dispute form:  
   👉 [Google Play Protect Developer Appeal Form](https://support.google.com/googleplay/android-developer/contact/protectappeals)  
   Submit the APK URL, SHA-256 fingerprint, and developer contact (`FloroSoft`). Google's security team reviews the binary and whitelists the signing certificate.
