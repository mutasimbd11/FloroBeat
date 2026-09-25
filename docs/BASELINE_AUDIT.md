# FloroBeat — Baseline Audit

> **Repository:** [BitChord](https://github.com/kushagrasinghx/BitChord)
> **Audit Date:** 2026-09-21
> **Branch:** `florobeat-development` (forked from `main` at `42712cb`)
> **Auditor:** FloroSoft Engineering

---

## 1. Repository Structure

```
BitChord/
├── app/                          # Android application module (single module)
│   ├── build.gradle.kts          # App-level Gradle config
│   ├── proguard-rules.pro        # ProGuard/R8 rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/           # ONNX models + SVG logos
│       │   ├── cpp/              # JNI bridge (CMake-based)
│       │   │   ├── CMakeLists.txt
│       │   │   └── jni/          # analysis_jni.cpp, mel_jni.cpp, vocal_jni.cpp
│       │   ├── java/com/music/bitchord/   # Kotlin source (242 .kt files)
│       │   │   ├── BitChordApplication.kt
│       │   │   ├── MainActivity.kt (~3900 lines)
│       │   │   ├── auth/
│       │   │   ├── data/
│       │   │   │   ├── canvas/    # Spotify canvas (animated artwork)
│       │   │   │   ├── discord/   # Discord Rich Presence
│       │   │   │   ├── innertube/ # YouTube Music API client
│       │   │   │   ├── jiosaavn/  # JioSaavn integration
│       │   │   │   ├── listentogether/ # Multi-device sync feature
│       │   │   │   ├── lyrics/    # Multi-source lyrics
│       │   │   │   ├── model/     # Data models
│       │   │   │   ├── scrobbling/ # Last.fm + ListenBrainz
│       │   │   │   ├── settings/  # AppSettings (DataStore)
│       │   │   │   ├── sources/   # Pluggable music sources
│       │   │   │   │   ├── addon/ # Add-on source protocol
│       │   │   │   │   └── module/ # QuickJS module system
│       │   │   │   └── stats/     # Listening statistics / Replay
│       │   │   ├── download/      # Download service + management
│       │   │   ├── playback/      # Media3 player, service, queue, caching
│       │   │   │   └── smart/     # Automix (beat analysis, ONNX inference)
│       │   │   ├── ui/
│       │   │   │   ├── components/ # Shared UI components
│       │   │   │   │   ├── backdrop/   # Custom glass/blur effects
│       │   │   │   │   └── floatingtabbar/
│       │   │   │   ├── haptics/
│       │   │   │   ├── icons/     # BitChordIcons custom icon set
│       │   │   │   ├── performance/ # Refresh rate management
│       │   │   │   ├── player/    # NowPlayingScreen, mesh gradients, lyrics
│       │   │   │   ├── replay/    # Year-in-review / Wrapped feature
│       │   │   │   ├── screens/   # All main screens
│       │   │   │   ├── theme/     # Material3 theme, artwork palette
│       │   │   │   └── utils/     # iOS-style overscroll
│       │   │   └── widget/        # Home screen widgets (square + wide)
│       │   └── res/               # Resources (29 directories)
│       │       ├── drawable/      # 19 vector drawables
│       │       ├── font/          # Custom fonts
│       │       ├── layout/        # Widget layouts
│       │       ├── mipmap-*/      # Launcher icons (6 density buckets + anydpi)
│       │       ├── values*/       # 15 locale directories + night/v31/sw600dp
│       │       └── xml/           # Configs (widget, backup, locales, file paths)
│       ├── dev/                   # Dev flavor manifest override
│       ├── debug/                 # Debug source set
│       └── test/                  # Unit tests
├── backend/                       # Listen Together party server (Python/FastAPI)
│   ├── app/                       # Server source
│   ├── tests/                     # Server tests
│   ├── render.yaml                # Render deployment blueprint
│   └── requirements.txt
├── native/                        # Shared native C++ source
│   └── analyzer/                  # Audio analysis, mel spectrogram, resampler, vocal separation
├── gradle/                        # Gradle wrapper
├── .github/workflows/android.yml  # CI/CD
├── docs/                          # Existing docs (lyrics translation only)
├── build.gradle.kts               # Root Gradle config
├── settings.gradle.kts            # Project settings
├── gradle.properties              # Build properties
├── keystore.properties.example    # Signing config template
├── LICENSE                        # GPLv3
└── README.md                      # Project README
```

---

## 2. Technology Stack

| Component | Version / Detail |
|---|---|
| **Language** | Kotlin 2.3.20 |
| **UI Framework** | Jetpack Compose (Material 3, BOM 2024.12.01) |
| **Android Gradle Plugin** | 8.10.1 |
| **Gradle** | 8.11.1 |
| **JDK** | 17 (source + target compatibility) |
| **Compile SDK** | 36 |
| **Target SDK** | 36 |
| **Min SDK** | 26 (Android 8.0) |
| **R8/D8** | 8.13.23 (overridden to fix dex register bug) |
| **NDK** | Required (CMake 3.22.1, C++17) |
| **ABI Splits** | armeabi-v7a, arm64-v8a, x86_64 + universal |
| **Media Playback** | Media3/ExoPlayer 1.11.0 (session, HLS, DASH, OkHttp datasource) |
| **Networking** | Ktor 3.0.3 (OkHttp engine, content negotiation, WebSockets) |
| **Serialization** | kotlinx.serialization 1.7.3 |
| **Image Loading** | Coil 3.0.4 (Compose + OkHttp network) |
| **Blur/Glass** | Haze 1.3.1 (dev.chrisbanes.haze) |
| **Markdown** | compose-richtext 0.20.0 |
| **JS Engine** | QuickJS-KT 1.0.5 (for source module plugins) |
| **Stream Resolution** | NewPipeExtractor v0.26.3 (stripped, with patched Utils) |
| **ML Inference** | ONNX Runtime Android 1.28.0 |
| **Auth Storage** | AndroidX Security Crypto 1.1.0-alpha06 |
| **Palette** | AndroidX Palette 1.0.0 |
| **Navigation** | AndroidX Navigation Compose 2.8.5 |
| **Testing** | JUnit 4.13.2, MockWebServer 4.12.0, Espresso 3.7.0 |
| **Backend** | Python 3.12.6, FastAPI, WebSockets, deployed on Render |

---

## 3. Android Configuration

| Property | Value |
|---|---|
| **applicationId** | `com.music.bitchord` |
| **namespace** | `com.music.bitchord` |
| **versionCode** | 17 |
| **versionName** | 1.6 |
| **Product Flavors** | `dev` (`com.dev.bitchord`, label "BitChord Dev"), `prod` (default) |
| **Build Types** | `debug`, `release` (minify OFF, signing optional) |
| **Compose** | Enabled via `kotlin.plugin.compose` |
| **BuildConfig** | Enabled (LASTFM_API_KEY, LASTFM_SECRET, LISTEN_TOGETHER_SERVER) |
| **ProGuard** | Rules exist but shrinking disabled |
| **Signing** | External `keystore.properties` + `.jks` (both gitignored) |

---

## 4. Application Architecture

### 4.1 Application Entry Point
- **`BitChordApplication`** — `Application` subclass + `SingletonImageLoader.Factory`
  - Initializes: AuthStore, Innertube, AppSettings, ListenTogether, SourceRegistry, SearchHistory, LastPlayed, OriginalVersion, Downloads, ListeningStats, ArtistFacts, AudioCache, CanvasCache, SpotifyToken, LastFM
  - Configures Coil with 100MB disk cache + 20% memory cache

### 4.2 Activity
- **`MainActivity`** — Single `AppCompatActivity`, `singleTask` launch mode
  - Handles: deep links, widget intents, music link sharing, YouTube Music links, Listen Together invites
  - Contains `BitChordApp` composable (~3100 lines) — the entire app UI in one massive Composable function
  - No fragment-based navigation — everything is Compose state-driven

### 4.3 Services
- **`PlaybackService`** — Media3 `MediaLibraryService` (foreground, media playback type)
- **`DownloadService`** — Foreground service for track downloads (dataSync type)

### 4.4 Receivers / Widgets
- **`MediaWidgetSquare`**, **`MediaWidgetWide`** — Home screen widget providers
- **`MediaWidgetActions`** — Widget button handler (play/pause, next, previous)

### 4.5 Providers
- **`FileProvider`** — Shares Replay posters and downloaded APKs (`${applicationId}.fileprovider`)

### 4.6 Navigation
- Tab-based: Home, Explore, Library, Search (floating tab bar)
- Stacked detail screens for Artist, Album, Playlist
- Settings as overlay sheet
- Full-screen: NowPlaying, Login, Discord, Sources, AccountScrobbling, History, ListenTogether, Equalizer, Replay, SpotifyCanvasAuth

### 4.7 State Management
- **`MainViewModel`** — Central ViewModel for all data loading
- **`AppSettings`** — DataStore-based reactive settings (collected as StateFlow)
- Compose state (`remember`, `rememberSaveable`) for UI state
- No DI framework (manual initialization in Application)

### 4.8 Data Layer
- **Innertube** — YouTube Music API client (Ktor)
- **SourceRegistry** — Pluggable music source management
- **LocalMediaRepository** — Device media store queries
- **Downloads** — Download tracking and management
- **ListeningStats** — On-device listening history
- **AuthStore** — Encrypted session/cookie storage

### 4.9 Database / Storage
- **No Room database** — all persistence via DataStore, SharedPreferences, and file-based caches
- `SharedPreferences`: widget state (`bitchord_widget`)
- `DataStore`: AppSettings
- `EncryptedSharedPreferences`: Auth sessions
- File caches: AudioCache, CanvasCache, Coil image cache

---

## 5. Playback Architecture

| Component | Purpose |
|---|---|
| **PlaybackService** | Media3 MediaLibraryService, foreground notification, media session |
| **ExoPlayer** | Core player engine (Media3 1.11.0) |
| **Media3 Session** | MediaSession for system integration, Android Auto, notifications |
| **OkHttp DataSource** | Network streaming via OkHttp |
| **HLS support** | For Apple canvas motion artwork |
| **DASH support** | For adaptive streaming formats from providers |
| **AudioCache** | DiskLruCache for audio stream caching |
| **QueueBuilder** | Queue construction and management |
| **QueueShuffle** | Shuffle state management |
| **AutoPlay** | Radio/autoplay track loading |
| **OriginalVersion** | Tracks reverted to YouTube's own upload |
| **LastPlayed** | Persists last playback state for resume |
| **Smart/Automix** | Beat analysis (TrackFeatures, MelSpectrogram, VocalSpectrogram via JNI + ONNX) |

---

## 6. Source / Provider Architecture

- **SourceRegistry** — Central registry for music sources
- **SourceConfig** — Per-source configuration
- **SourceKind** — Source type enumeration
- **SourceHealth** — Health check mechanism
- **AddonSource** — External add-on protocol (HTTP-based)
- **ModuleSource** — QuickJS-based module plugins
- Sources provide: track resolution, stream URLs, quality tiers
- YouTube Music (Innertube) is the primary/fallback source

---

## 7. Native Components

### Native Analyzer (`native/analyzer/`)
- **`audio_analysis.cpp`** — DSP analysis (tempo, key, energy, structure)
- **`tempo_analysis.cpp`** — Autocorrelation-based tempo detection
- **`mel_spectrogram.cpp`** — Log-mel spectrogram for Beat This! ONNX model
- **`vocal_spectrogram.cpp`** — Linear STFT for open-unmix vocal separation
- **`resampler.cpp`** — Audio resampling

### JNI Bridge (`app/src/main/cpp/jni/`)
- `analysis_jni.cpp` → `TrackFeatures` Kotlin class
- `mel_jni.cpp` → `MelSpectrogram` Kotlin class
- `vocal_jni.cpp` → `VocalSpectrogram` Kotlin class
- **All JNI functions use `com_music_bitchord_playback_smart_*` naming** ⚠️

### ONNX Models (`app/src/main/assets/`)
- `beat_this_int8.onnx` (~4.5MB) — Beat/downbeat detection
- `vocals_umxhq_int8.onnx` (~9MB) — Vocal separation

---

## 8. Backend (`backend/`)

- **Purpose:** Listen Together party server
- **Stack:** Python 3.12.6, FastAPI, WebSockets
- **Deployment:** Render (free tier, single instance)
- **Features:** Party creation, device sync, clock offset, playback state broadcasting
- **No database** — all state in-memory
- **Key files:** `main.py`, `party.py`, `protocol.py`, `hub.py`, `clock.py`, `config.py`
- **Service name in render.yaml:** `bitchord-listen-together`

---

## 9. Branding Locations

### 9.1 Product Branding (→ Replace with FloroBeat)

| Location | Reference | Type |
|---|---|---|
| `settings.gradle.kts:18` | `rootProject.name = "BitChord"` | Project name |
| `app/build.gradle.kts:56` | `namespace = "com.music.bitchord"` | Namespace |
| `app/build.gradle.kts:60` | `applicationId = "com.music.bitchord"` | App ID |
| `app/build.gradle.kts:103` | `applicationId = "com.dev.bitchord"` | Dev flavor ID |
| `app/build.gradle.kts:104` | `resValue("string", "app_name", "BitChord Dev")` | Dev app name |
| `app/src/main/AndroidManifest.xml:51` | `android:name=".BitChordApplication"` | Application class |
| `app/src/main/AndroidManifest.xml:62,116` | `android:theme="@style/Theme.BitChord"` | Theme name |
| `app/src/main/res/values/strings.xml:2` | `<string name="app_name">BitChord</string>` | App name |
| `app/src/main/res/values/themes.xml:4` | `style name="Theme.BitChord"` | Theme style |
| `app/src/dev/AndroidManifest.xml:6` | `android:label="BitChord Dev"` | Dev label |
| 242 `.kt` files | `package com.music.bitchord.*` | Package declarations |
| 242 `.kt` files | `import com.music.bitchord.*` | Import statements |
| `ui/theme/Theme.kt` | `BitChordTypography`, `BitChordTheme` | Theme composables |
| `ui/icons/BitChordIcons.kt` | `BitChordIcons` object | Icon set |
| `MainActivity.kt:292,327,377` | `BitChordTheme`, `BitChordApp` | App composables |
| `BitChordApplication.kt:32` | `class BitChordApplication` | Application class |
| `ui/replay/ReplayStories.kt:412` | `text = "BitChord"` | Replay branding |
| `ui/replay/ReplayPoster.kt:398-402` | `drawText("BitChord")` | Poster branding |
| `ui/replay/ReplayShareSheet.kt:291` | `Pictures/BitChord` | Download folder |
| `ui/screens/SettingsSheet.kt:508` | `Music/BitChord` | Download folder |
| `ui/screens/SettingsSheet.kt:1267` | GitHub URL | Repository link |
| `widget/MediaWidgetSnapshot.kt:84` | `"bitchord_widget"` | SharedPreferences key |
| `widget/MediaWidgetActions.kt:106-108` | `com.music.bitchord.widget.*` | Action intents |
| JNI files (3 files, 13 functions) | `Java_com_music_bitchord_*` | JNI function names |
| `CMakeLists.txt:14,16,23,34` | `bitchord_analysis` | Native library name |
| `README.md` | Multiple references | Documentation |
| `Banner.png`, `Logo.png` | BitChord visual assets | Images |
| `app/src/main/assets/Logo.svg` | SVG logo | In-app logo |
| `app/src/main/assets/LogoTransparent.svg` | SVG transparent logo | In-app logo |
| `app/src/main/ic_launcher-playstore.png` | Play Store icon | Store listing |
| `drawable/ic_launcher_*` | Launcher icon vectors | App icon |
| `drawable/ic_logo.xml` | Logo vector | In-app branding |
| `drawable/ic_notification_logo.xml` | Notification logo | Notifications |
| `.github/workflows/android.yml:35,73` | `bitchord-dev-debug`, `bitchord-prod-release-signed` | CI artifacts |
| `.github/workflows/android.yml:58-59` | `bitchord-release.jks` | CI signing |
| `keystore.properties.example` | `bitchord-release.jks`, alias `bitchord` | Signing template |
| `backend/render.yaml:13` | `bitchord-listen-together` | Backend service name |
| `backend/README.md:1` | `BitChord — Listen Together` | Backend docs |

### 9.2 Legal Attribution (→ Preserve)

| Location | Reference |
|---|---|
| `LICENSE` | GPLv3 full text |
| `README.md` | Disclaimer & Legal Notice section |
| `app/src/main/AndroidManifest.xml:125-127,159-163` | Comments mentioning BitChord as historical context |

### 9.3 Third-Party References (→ Preserve as-is)

| Location | Reference |
|---|---|
| `app/src/main/java/org/schabi/newpipe/extractor/utils/Utils.java` | Patched NewPipe class |
| `app/src/main/java/com/my/kizzy/` | Discord RPC library (vendored) |
| `app/build.gradle.kts` | All dependency declarations |

---

## 10. Licensing Requirements

- **License:** GNU General Public License v3.0 (GPLv3)
- **Requirements:**
  1. Must preserve the GPLv3 license file
  2. Modified versions must be marked as changed (GPLv3 §5a)
  3. Must carry notices stating it is released under GPLv3 (§5b)
  4. Must license the entire work under GPLv3 (§5c)
  5. Source code must be made available
  6. Third-party library licenses must be preserved
  7. Cannot misrepresent origin of material (§7c)
- **Key third-party licenses to preserve:**
  - NewPipeExtractor (GPLv3)
  - Kizzy/Discord RPC (check license)
  - ONNX models: Beat This! (MIT), open-unmix (check license)
  - All AndroidX / Google libraries (Apache 2.0)
  - Haze (Apache 2.0)
  - Coil (Apache 2.0)
  - Ktor (Apache 2.0)

---

## 11. Security Concerns

| Item | Status |
|---|---|
| Keystore excluded from git | ✅ `.gitignore` covers `*.jks`, `*.keystore`, `keystore.properties` |
| `local.properties` excluded | ✅ gitignored |
| API keys in BuildConfig | ✅ Sourced from environment/local.properties, not committed |
| Auth tokens | ✅ EncryptedSharedPreferences |
| No hardcoded secrets found | ✅ |
| Listen Together server URL | ⚠️ Configurable but `bitchord.kushagrasingh.in` in manifest deep link |

---

## 12. Migration Risks

### Critical
1. **JNI function names** — All 13 JNI functions encode `com_music_bitchord` in their C++ names. Changing the Kotlin package requires updating all JNI function signatures.
2. **SharedPreferences key** — `bitchord_widget` is used for widget state persistence. Renaming breaks existing widget installations.
3. **Widget action intents** — Hardcoded `com.music.bitchord.widget.*` action strings.
4. **FileProvider authority** — Uses `${applicationId}.fileprovider`, which auto-adapts.
5. **Deep link host** — `bitchord.kushagrasingh.in` in manifest for Listen Together invites.

### High
6. **242 Kotlin files** — Every file has `package com.music.bitchord.*` declarations and imports.
7. **CMake native library name** — `bitchord_analysis` throughout native build.
8. **Download/picture folder paths** — Hardcoded `Music/BitChord` and `Pictures/BitChord`.
9. **CI/CD artifact names** — Reference `bitchord-*` in workflow.

### Medium
10. **Theme/composable names** — `BitChordTheme`, `BitChordTypography`, `BitChordApp`, `BitChordIcons`.
11. **Backend service name** — `bitchord-listen-together` in `render.yaml`.
12. **Existing user data** — Users upgrading from BitChord would lose settings/cache.

### Low
13. **Comments** — Numerous code comments reference "BitChord" contextually.
14. **Logo SVGs in assets** — Need replacement with FloroBeat logo.
15. **README and documentation** — Full rewrite needed.

---

## 13. Recommended Next Steps

### Phase 1: Complete Build Verification
1. Verify baseline build succeeds (currently in progress)
2. Document any build errors or missing configuration
3. Record build warnings

### Phase 2: Architecture Documentation
1. Create `FLOROBEAT_ARCHITECTURE.md` with detailed data/UI/playback flow diagrams
2. Document the source/provider system

### Phase 3: Legal & Open Source
1. Create `OPEN_SOURCE_NOTICES.md`
2. Verify all third-party license compliance
3. Document GPLv3 obligations for the fork

### Phase 4: Identity Migration Plan
1. Create `PACKAGE_MIGRATION.md` with file-by-file change plan
2. Plan JNI function rename strategy
3. Plan SharedPreferences migration
4. Plan data migration path for existing users

### Phase 5: Begin Transformation (after approval)
1. Rename package `com.music.bitchord` → `com.florosoft.florobeat`
2. Update all branding
3. Create new visual assets
4. Update documentation
5. Configure new signing

---

## 14. Summary Statistics

| Metric | Count |
|---|---|
| Total Kotlin source files | 242 |
| Total lines in MainActivity.kt | ~3,900 |
| Resource locale directories | 15 |
| Vector drawable resources | 19 |
| ONNX model assets | 2 (~13.5 MB total) |
| SVG logo assets | 2 |
| JNI bridge functions | 13 |
| Native C++ source files | 9 |
| Backend Python source files | 8 |
| CI/CD workflows | 1 |
| Product flavors | 2 (dev, prod) |
| Total "BitChord" references in source | ~1700+ (package, import, branding) |
