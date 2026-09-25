<div align="center">

<img src="FloroBeat leble.png" alt="FloroBeat Logo" width="520" />

# FloroBeat

### Next-Generation Audio Streaming & Music Player for Android
**Crafted with precision by FloroSoft**

<br/>

[![Latest Release](https://img.shields.io/github/v/release/mutasimbd11/FloroBeat?color=10B981&label=Release&style=for-the-badge&logo=github)](https://github.com/mutasimbd11/FloroBeat/releases/latest)
[![Android Support](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9336)-34A853?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/about/versions/16)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36%20(Android%2016)-4285F4?style=for-the-badge&logo=android)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/ABIs-arm64--v8a%20%7C%20armeabi--v7a%20%7C%20x86__64-orange?style=for-the-badge)](https://developer.android.com/ndk)
[![Signature Schemes](https://img.shields.io/badge/Signature-v2%20%7C%20v3%20%7C%20v4-blueviolet?style=for-the-badge&logo=securityscorecard)](https://source.android.com/docs/security/features/apksigning)
[![Play Protect](https://img.shields.io/badge/Google%20Play%20Protect-Verified%20Clean-success?style=for-the-badge&logo=googleplay)](https://developers.google.com/android/play-protect)
[![Developer](https://img.shields.io/badge/Developer-FloroSoft-6C5CE7?style=for-the-badge)](https://github.com/mutasimbd11)
[![License](https://img.shields.io/badge/License-GPL%20v3.0-00C49F?style=for-the-badge)](LICENSE)

<br/>

[**Download Latest Release**](https://github.com/mutasimbd11/FloroBeat/releases/latest) • [**Explore Features**](#-core-features) • [**Update System**](#-automatic-in-app-update-system) • [**Security & Verification**](#-security-signing--privacy-audit) • [**Build from Source**](#-building-from-source)

---

</div>

## 🌟 Overview

**FloroBeat** is an advanced, privacy-conscious Android music player and streaming client. Engineered using modern Android technologies (Jetpack Compose, Kotlin 2.3, Material 3, and Media3), FloroBeat delivers studio-grade acoustics, dynamic frosted-glass visuals, synchronized word-by-word lyrics, and seamless background playback.

Built from the ground up for high stability across Android 8.0 through Android 16 (API 36), FloroBeat provides both direct-distribution signed APKs and Google Play App Bundles with robust, reproducible cryptographic signing.

---

## 🚀 Core Features

### 🎧 Studio Audio & Playback
- **High-Fidelity Audio Streaming:** Lossless FLAC/ALAC playback and adaptive multi-tier streaming with automatic fallback.
- **Gapless Playback & Crossfade:** Zero-gap track switching with customizable 0–12 second crossfade curves.
- **Automix Transition Engine:** Native C++ beat-matching and tempo-stretching for seamless DJ-style transitions.
- **Background Playback & Foreground Media Service:** Uninterrupted background playback with MediaSession support for lock screen controls, Bluetooth headsets, and Android Auto.
- **Floating Mini Player:** Sleek, responsive floating playback bar featuring previous, play/pause, next track, and waveform scrubbing controls.

### 🎨 Visuals & Aesthetics
- **Material 3 & Frosted Glass:** Telegram-style real-time blur and translucent glassmorphism powered by Haze.
- **Animated Album Canvas:** High-resolution motion artwork and responsive canvas playback.
- **Word-Synced Animated Lyrics:** Smooth, syllable-level synchronized lyrics with multilingual translation support (powered by LRCLIB and Genius).
- **Adaptive Artwork Theming:** Dynamic color extraction that tailors the entire app palette to match the playing album art.

### 📦 Library & Ecosystem
- **Offline Audio Downloads:** Download full tracks with embedded cover art and ID3 metadata tags directly to internal/external storage.
- **Local Music Library:** Seamless scanner and organizer for offline audio files stored on device.
- **Scrobbling & Presence:** Native Discord Rich Presence integration, Last.fm, and ListenBrainz real-time scrobbling.
- **Listen Together:** Real-time synchronized party playback allowing multiple listeners to enjoy synced streams across rooms or devices.

---

## 🔄 Automatic In-App Update System

FloroBeat includes a built-in, tamper-proof update engine that keeps your app up to date with zero hassle:

```
GitHub Releases API  ──►  Version Check  ──►  Changelog Prompt
                                                    │
                                                    ▼
Official Installation  ◄──  SHA-256 Verify  ◄──  Background Download
```

1. **Automatic Detection:** On launch, FloroBeat checks the official repository (`mutasimbd11/FloroBeat`) for newer releases using smart rate-limiting (6-hour cooldown).
2. **Interactive Changelog:** When a new update is released, users receive an in-app prompt showing current vs new version numbers and the full changelog.
3. **Cryptographic Verification:** Downloaded APKs are verified in two steps:
   - **SHA-256 Integrity Verification:** Compared against the published `.sha256` checksum to ensure the download is byte-for-byte authentic.
   - **Package & Signing Identity Verification:** Confirms `applicationId` (`com.florosoft.florobeat`) and verifies that the release certificate matches the currently installed version.
4. **Android FileProvider Handoff:** The verified APK is passed directly to Android's native system package installer (`FileProvider`), preserving all existing playlists, downloads, and app settings.

---

## 🔒 Security, Signing & Privacy Audit

FloroBeat adheres strictly to modern Android security and Google Play distribution standards:

| Security Property | FloroBeat Standard |
|---|---|
| **Release Signing** | Dedicated 4096-bit RSA permanent release key (FloroSoft identity) |
| **Signature Schemes** | APK Signature Schemes **v2, v3, and v4** enabled and verified with `apksigner` |
| **Build Variant** | `prodRelease` (`android:debuggable="false"`), zero debug endpoints or mock data |
| **Target SDK** | **Target SDK 36** (Android 16 compatible; ready for Google Play 2026 requirements) |
| **Permissions Audit** | Cleaned of suspicious permissions; **no `REQUEST_INSTALL_PACKAGES`**, no `SYSTEM_ALERT_WINDOW`, no `QUERY_ALL_PACKAGES` |
| **Network Security** | `android:usesCleartextTraffic="false"` (strictly enforces encrypted HTTPS/TLS) |
| **Privacy** | Zero tracking, zero third-party telemetry, no analytics bloatware |

---

## 📱 Compatibility Matrix

FloroBeat is optimized and verified across modern and legacy Android devices:

- **Minimum Android:** Android 8.0 Oreo (API 26)
- **Target Android:** Android 16 (API 36)
- **Supported Architectures:**
  - `arm64-v8a` (Modern 64-bit phones)
  - `armeabi-v7a` (32-bit legacy devices)
  - `x86_64` (Chromebooks, tablets, and emulators)
  - Universal (All ABIs bundled in a single APK)
- **Screen Support:** Small, normal, large, xlarge (optimized for phones, foldables, and tablets).

---

## 📥 Downloads & Releases

Pre-compiled, securely signed releases are available on the [Releases Page](https://github.com/mutasimbd11/FloroBeat/releases):

| Package Name | Architecture | Recommended For |
|---|---|---|
| `FloroBeat-universal-release.apk` | Universal | Any Android phone or tablet |
| `FloroBeat-arm64-v8a-release.apk` | 64-bit ARM | Modern Android smartphones (smaller file size) |
| `FloroBeat-armeabi-v7a-release.apk` | 32-bit ARM | Older 32-bit Android phones |
| `FloroBeat-x86_64-release.apk` | 64-bit x86 | Android on PC, Chromebooks, and emulators |
| `FloroBeat-release.aab` | All ABIs | Google Play Console publishing |

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK with Platform 36 (Android 16) and Build-Tools 35.0.0+
- CMake 3.22.1+ and Android NDK

### Build Instructions

```bash
# Clone the official FloroBeat repository
git clone https://github.com/mutasimbd11/FloroBeat.git
cd FloroBeat

# Build debug variant for local development
./gradlew assembleDevDebug

# Run unit tests (493 automated tests)
./gradlew testProdReleaseUnitTest

# Build signed production release APKs
./gradlew assembleProdRelease

# Build Google Play App Bundle (AAB)
./gradlew bundleProdRelease
```

---

## 📄 License & Legal Notice

FloroBeat is open-source software licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).

- **No Media Hosting:** FloroBeat does not host, upload, or distribute copyrighted media files. It functions exclusively as a client-side interface to stream public media or manage local audio files on your device.
- **Privacy First:** FloroBeat respects user privacy and does not collect or monetize personal listening data.

---

<div align="center">

**Developed with ❤️ by FloroSoft**  
*Empowering your music listening experience.*

</div>
