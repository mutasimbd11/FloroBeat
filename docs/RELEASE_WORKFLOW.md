# FloroBeat — Developer Release Workflow & Update Distribution Guide

> **Official Repository:** `https://github.com/mutasimbd11/FloroBeat`  
> **Releases Page:** `https://github.com/mutasimbd11/FloroBeat/releases`  
> **Maintainer:** FloroSoft Engineering / Mutasim Billah  

This guide provides the complete developer instructions for building, signing, verifying, and publishing FloroBeat production releases through GitHub Releases without Google Play Store.

---

## 1. Distribution Architecture

FloroBeat is distributed as an open-source, sideloaded Android application:
- Releases are published as **GitHub Releases** on `mutasimbd11/FloroBeat`.
- The Android `applicationId` remains constant across all releases (`com.florosoft.florobeat`).
- Existing installs automatically discover new releases via the GitHub Releases API (`/releases/latest`), download the APK in the background with SHA-256 integrity verification, and guide the user through the official Android package installer.

---

## 2. Versioning Specification

Android requires two version identifiers configured in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 18       // Monotonically increasing positive integer
        versionName = "1.7.0"   // Human-readable Semantic Version (X.Y.Z)
    }
}
```

### Versioning Rules
1. **`versionCode`**: Must be incremented with every single release (e.g., `17 -> 18 -> 19`). **Never decrease or reuse a `versionCode`**; Android's package manager rejects downgrades (`INSTALL_FAILED_VERSION_DOWNGRADE`).
2. **`versionName`**: Follows Semantic Versioning:
   - `v1.0.0` — Major release
   - `v1.1.0` — Feature release
   - `v1.1.1` — Patch/bugfix release
3. **Git Tags**: Prepend `v` to the `versionName` (e.g. `v1.7.0`).

---

## 3. Production Signing Key Safety

Android's package installer strictly requires that an update be signed with the **exact same signing identity** as the currently installed application.

> [!CAUTION]
> **CRITICAL: NEVER COMMIT YOUR KEYSTORE OR PASSWORDS TO GIT.**
> If the signing key is lost, future updates cannot be installed over existing installations — users would be forced to uninstall FloroBeat and lose all local playlists, cache, and app settings.

### Generating the Production Keystore (One-Time Setup)

To create the official FloroBeat production signing keystore:

```bash
keytool -genkey -v -keystore florobeat-release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias florobeat
```

Store `florobeat-release.jks` in a secure, backed-up vault (e.g. password manager or offline encrypted storage).

### Local Signing Setup

Copy `keystore.properties.example` to `keystore.properties` (this file is `.gitignore`d):

```properties
storeFile=florobeat-release.jks
storePassword=YourKeystorePassword
keyAlias=florobeat
keyPassword=YourKeyPassword
```

---

## 4. Local Build & Release Process

### Step 1: Increment Version Numbers
In `app/build.gradle.kts`:
- Increment `versionCode`
- Update `versionName`

### Step 2: Build Signed Release APK
Run the Gradle release task:

```bash
# On Linux/macOS:
./gradlew assembleProdRelease

# On Windows (PowerShell):
.\gradlew.bat assembleProdRelease
```

The output APK will be placed at:
`app/build/outputs/apk/prod/release/app-prod-release.apk`

### Step 3: Rename APK & Generate SHA-256 Checksum

Consistent naming standard:
- APK: `FloroBeat-<versionName>.apk` (e.g. `FloroBeat-1.7.0.apk`)
- Checksum: `FloroBeat-<versionName>.apk.sha256` (e.g. `FloroBeat-1.7.0.apk.sha256`)

```bash
# Linux / macOS:
cp app/build/outputs/apk/prod/release/app-prod-release.apk FloroBeat-1.7.0.apk
sha256sum FloroBeat-1.7.0.apk > FloroBeat-1.7.0.apk.sha256

# Windows (PowerShell):
Copy-Item app/build/outputs/apk/prod/release/app-prod-release.apk FloroBeat-1.7.0.apk
$hash = (Get-FileHash FloroBeat-1.7.0.apk -Algorithm SHA256).Hash.ToLower()
"$hash  FloroBeat-1.7.0.apk" | Out-File -Encoding ascii FloroBeat-1.7.0.apk.sha256
```

### Step 4: Publish to GitHub Releases

1. Navigate to: `https://github.com/mutasimbd11/FloroBeat/releases/new`
2. **Tag:** `v1.7.0`
3. **Release Title:** `FloroBeat v1.7.0`
4. **Release Notes Template:**

```markdown
## What's New
- Add high-resolution audio streaming improvements
- Add automated GitHub release updates
- Performance enhancements and UI fluid transitions

## Technical
- Version: 1.7.0
- Version Code: 18
- Minimum Android: Android 8.0 (API 26)
- SHA-256: <hash>
```

5. **Attach Assets:**
   - Attach `FloroBeat-1.7.0.apk`
   - Attach `FloroBeat-1.7.0.apk.sha256`
6. Click **Publish release**.

---

## 5. Automated GitHub Actions Release Workflow

An automated CI/CD workflow is provided at `.github/workflows/release.yml`.

### Setting Up GitHub Repository Secrets

Configure the following secrets under **Settings → Secrets and variables → Actions**:

1. `KEYSTORE_BASE64`: The entire `.jks` file encoded as a base64 string.
   - On Windows PowerShell:
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("florobeat-release.jks")) | Set-Clipboard
     ```
   - On Linux/macOS:
     ```bash
     base64 -w 0 florobeat-release.jks
     ```
2. `KEYSTORE_PASSWORD`: Keystore password.
3. `KEY_ALIAS`: Key alias (`florobeat`).
4. `KEY_PASSWORD`: Key alias password.
5. `LASTFM_API_KEY`: (Optional) Last.fm credentials.
6. `LASTFM_SECRET`: (Optional) Last.fm secret.

### Triggering an Automated Release

Push a git tag matching `v*`:

```bash
git tag v1.7.0
git push origin v1.7.0
```

The GitHub Actions workflow will:
1. Check out repository
2. Set up JDK 17
3. Restore keystore securely from GitHub secrets
4. Build signed production release APK
5. Calculate SHA-256 digest
6. Create and publish GitHub Release
7. Attach both `.apk` and `.apk.sha256` assets
8. Generate release changelog

---

## 6. How Users Experience Updates

### Automatic Detection
1. FloroBeat checks GitHub Releases on launch (with a 6-hour rate-limiting cooldown).
2. If an update is detected, an alert appears:
   - **Current Version:** `1.6.0`
   - **New Version:** `1.7.0`
   - **What's New:** Formatted Markdown changelog
   - **Buttons:** `Update Now` | `Later`
3. If the user taps **Later**, startup dialogs are silenced for 24 hours, while the quiet top-bar badge remains accessible.

### Manual Check
Users can check at any time via:
**Settings → About FloroBeat → Check for Updates**
- Displays `"Checking for updates…"`
- If up to date: `"You're using the latest version of FloroBeat."`
- If newer: `"FloroBeat 1.7.0 is available."` with an `Update Now` button.

### Download & Verification
1. Tapping **Update Now** streams the APK over HTTPS into `cache/updates/`.
2. Live progress bar shows percentage.
3. Once downloaded:
   - **SHA-256 Check:** Computes hash and compares against the published `.sha256` asset.
   - **Compatibility Check:** Verifies APK integrity, `packageName == com.florosoft.florobeat`, `versionCode >= current`, and signing certificate match.

### Official Installation
1. If "Install unknown apps" permission is needed (Android 8.0+), a dialog guides the user to Android Settings.
2. Upon returning to FloroBeat, installation automatically resumes.
3. Handed to system installer via `FileProvider` (`content://com.florosoft.florobeat.fileprovider/updates/...`).
4. System package installer asks the user to confirm update.

---

| Android Version | Behavior |
|---|---|
| **Android 8.0 - 16 (API 26 - 36)** | Fully supported. Uses scoped internal storage and `FileProvider`. Does not require or declare `REQUEST_INSTALL_PACKAGES`, adhering to Google Play Protect anti-dropper guidelines. |
| **Storage Scoping (API 29+)** | Uses app-internal cache directory (`context.cacheDir/updates/`) and `FileProvider`, requiring no broad storage permissions. |
| **Signature Enforcement** | Android OS strictly enforces identical signing certificates on APK overwrites. FloroBeat pre-verifies signatures with `ApkVerifier` to provide helpful error guidance instead of silent system failure. |
