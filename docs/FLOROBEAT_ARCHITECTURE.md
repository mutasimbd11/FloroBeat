# FloroBeat — Architecture Documentation

> **Based on:** BitChord v1.6 (versionCode 17)
> **Document Date:** 2026-09-21
> **Maintained by:** FloroSoft Engineering

---

## 1. Module Structure

FloroBeat is a **single-module Android application** with an external Python backend and shared native C++ sources.

```
FloroBeat/
├── :app                    # Android application (single Gradle module)
├── backend/                # Listen Together party server (not a Gradle module)
└── native/                 # Shared C++ analysis code (built via CMake from :app)
```

There are no multi-module Gradle splits. All Kotlin source lives under `app/src/main/java/com/music/bitchord/`.

---

## 2. Major Packages

```mermaid
graph TD
    A[com.music.bitchord] --> B[auth]
    A --> C[data]
    A --> D[download]
    A --> E[playback]
    A --> F[ui]
    A --> G[widget]
    
    C --> C1[canvas]
    C --> C2[discord]
    C --> C3[innertube]
    C --> C4[jiosaavn]
    C --> C5[listentogether]
    C --> C6[lyrics]
    C --> C7[model]
    C --> C8[scrobbling]
    C --> C9[settings]
    C --> C10[sources]
    C --> C11[stats]
    
    C10 --> C10a[addon]
    C10 --> C10b[module]
    
    E --> E1[smart]
    
    F --> F1[components]
    F --> F2[haptics]
    F --> F3[icons]
    F --> F4[performance]
    F --> F5[player]
    F --> F6[replay]
    F --> F7[screens]
    F --> F8[theme]
    F --> F9[utils]
    
    F1 --> F1a[backdrop]
    F1 --> F1b[floatingtabbar]
```

### Package Responsibilities

| Package | Purpose |
|---|---|
| **`auth`** | Google account authentication (WebView-based OAuth), Discord login, encrypted session storage |
| **`data.canvas`** | Spotify Canvas animated artwork fetching and caching |
| **`data.discord`** | Discord Rich Presence gateway connection |
| **`data.innertube`** | YouTube Music (Innertube) API client — search, browse, playback, auth |
| **`data.jiosaavn`** | JioSaavn music service integration |
| **`data.listentogether`** | Multi-device synchronised listening (WebSocket client) |
| **`data.lyrics`** | Multi-source lyrics fetching (synced, plain, translations) |
| **`data.model`** | Core data models: Song, Album, Artist, Playlist, SearchResult, etc. |
| **`data.scrobbling`** | Last.fm and ListenBrainz scrobbling |
| **`data.settings`** | AppSettings (Jetpack DataStore), audio quality, theme, preferences |
| **`data.sources`** | Pluggable music source system (registry, health, config) |
| **`data.sources.addon`** | HTTP-based external add-on source protocol |
| **`data.sources.module`** | QuickJS JavaScript module source plugins |
| **`data.stats`** | On-device listening statistics, artist facts, Replay/Wrapped feature |
| **`download`** | Track download service, queue, metadata embedding, progress tracking |
| **`playback`** | Media3 player, PlaybackService, queue, caching, audio focus, media session |
| **`playback.smart`** | Automix: beat analysis via JNI + ONNX, tempo stretching, DJ transitions |
| **`ui.components`** | Shared composables: MiniPlayer, SearchField, SongActionsSheet, TopBar, etc. |
| **`ui.components.backdrop`** | Custom glass/blur system with runtime shaders |
| **`ui.components.floatingtabbar`** | Animated bottom navigation bar |
| **`ui.haptics`** | Haptic feedback utilities |
| **`ui.icons`** | Custom icon set (BitChordIcons → FloroBeatIcons) |
| **`ui.performance`** | Display refresh rate management |
| **`ui.player`** | Now Playing screen, mesh gradient, lyrics display, audio output picker |
| **`ui.replay`** | Year-in-review feature (Spotify Wrapped-style) |
| **`ui.screens`** | All main screens: Home, Explore, Library, Search, Settings, Detail, etc. |
| **`ui.theme`** | Material 3 theme, typography, artwork-driven dynamic colors |
| **`ui.utils`** | iOS-style overscroll effect |
| **`widget`** | Android home screen widgets (square + wide, with transport controls) |

---

## 3. Data Flow

```mermaid
flowchart LR
    subgraph UI["UI Layer (Compose)"]
        MA[MainActivity / BitChordApp]
        VM[MainViewModel]
        Screens[Screen Composables]
    end
    
    subgraph Data["Data Layer"]
        IT[Innertube Client]
        SR[SourceRegistry]
        LM[LocalMediaRepository]
        DL[Downloads]
        LS[ListeningStats]
        LY[Lyrics]
        AS[AppSettings]
    end
    
    subgraph Playback["Playback Layer"]
        PS[PlaybackService]
        EP[ExoPlayer]
        MS[MediaSession]
        AC[AudioCache]
    end
    
    subgraph External["External"]
        YTM[YouTube Music API]
        SP[Source Plugins]
        LFM[Last.fm / ListenBrainz]
        DC[Discord Gateway]
        LT[Listen Together Server]
    end
    
    MA --> VM
    VM --> IT
    VM --> SR
    VM --> LM
    VM --> LY
    
    Screens --> VM
    Screens --> PS
    
    IT --> YTM
    SR --> SP
    
    PS --> EP
    PS --> MS
    PS --> AC
    EP --> IT
    EP --> SR
    
    PS --> LFM
    PS --> DC
    PS --> LT
    PS --> LS
    PS --> DL
```

---

## 4. UI Flow

```mermaid
flowchart TD
    App[BitChordApp] --> Tabs{Bottom Tab Bar}
    
    Tabs --> Home[Home Screen]
    Tabs --> Explore[Explore Screen]
    Tabs --> Library[Library Screen]
    Tabs --> Search[Search Screen]
    
    Home --> Detail[Detail Screen<br>Album / Artist / Playlist]
    Explore --> Detail
    Library --> Detail
    Search --> Detail
    
    App --> Settings[Settings Sheet]
    Settings --> AccountScrobbling[Account & Scrobbling]
    Settings --> Sources[Sources Screen]
    Settings --> ListenTogether[Listen Together]
    Settings --> Equalizer[Equalizer]
    Settings --> Discord[Discord Screen]
    Settings --> SpotifyCanvas[Spotify Canvas Auth]
    
    App --> NowPlaying[Now Playing Screen]
    NowPlaying --> Lyrics[Lyrics Panel]
    NowPlaying --> Queue[Queue Panel]
    NowPlaying --> AudioOutput[Audio Output Sheet]
    
    App --> MiniPlayer[Mini Player]
    MiniPlayer --> NowPlaying
    
    App --> Replay[Replay Screen]
    Replay --> ReplayStories[Replay Stories]
    Replay --> ReplayShare[Replay Share Sheet]
    
    App --> Login{Login Flows}
    Login --> YTLogin[YouTube Music Login<br>WebView]
    Login --> DiscordLogin[Discord Login<br>WebView]
    
    App --> History[History Screen]
    App --> LocalMusic[Local Music Screen]
```

---

## 5. Playback Flow

```mermaid
sequenceDiagram
    participant U as User / UI
    participant PS as PlaybackService
    participant EP as ExoPlayer
    participant SR as SourceRegistry
    participant IT as Innertube
    participant AC as AudioCache
    participant MS as MediaSession
    participant N as Notification
    
    U->>PS: Play song (via MediaController)
    PS->>SR: Resolve stream URL
    SR->>IT: Get stream (fallback)
    SR-->>PS: Stream URL + metadata
    PS->>AC: Check cache
    AC-->>PS: Cache hit/miss
    PS->>EP: Prepare + play MediaItem
    EP->>EP: Audio focus request
    EP->>MS: Update media session
    MS->>N: Update notification
    EP-->>U: Playback state updates
    
    Note over PS,EP: Background playback via<br>foreground service
```

### Key Playback Components

- **PlaybackService** extends `MediaLibraryService` — runs as foreground service with `mediaPlayback` type
- **ExoPlayer** with OkHttp data source for network streaming
- **Media3 Session** provides system-wide media controls, Android Auto, lock screen
- **AudioCache** — disk LRU cache for streamed audio
- **QueueBuilder** — constructs and manages the playback queue
- **QueueShuffle** — maintains shuffle state independently from queue order
- **AutoPlay** — loads continuation tracks from YouTube Music radio
- **Crossfade** — gapless crossfade (0–12s configurable)
- **Smart/Automix** — DJ-style transitions using beat detection (JNI + ONNX Runtime)

---

## 6. Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant WV as WebView (YT Music)
    participant AS as AuthStore
    participant IT as Innertube
    
    U->>WV: Sign in to YouTube Music
    WV-->>AS: Extract cookies
    AS->>AS: Encrypt & store session
    AS->>IT: Set cookie
    IT->>IT: Ensure session scope
    IT-->>U: Authenticated content
    
    Note over AS: EncryptedSharedPreferences<br>Multi-account support
```

- **AuthStore** — manages multiple Google accounts with encrypted storage
- **WebView-based** — no OAuth client ID needed; captures cookies from YouTube Music sign-in
- **Multi-profile** — supports YouTube Music channel/brand account switching
- **Session restoration** — restores on app launch from `BitChordApplication`

---

## 7. Download Flow

```mermaid
sequenceDiagram
    participant U as User
    participant DS as DownloadService
    participant SR as SourceRegistry
    participant MS as MediaStore
    participant DL as Downloads
    
    U->>DS: Download request (song)
    DS->>SR: Resolve stream URL
    SR-->>DS: Stream URL
    DS->>DS: Download to temp file
    DS->>DS: Embed metadata + artwork
    DS->>MS: Write to Music/ folder
    DS->>DL: Record download
    DL-->>U: Update UI (downloaded badge)
```

- **DownloadService** — foreground service (`dataSync` type)
- Downloads to `Music/BitChord` folder (→ `Music/FloroBeat`)
- Embeds metadata tags and artwork into downloaded files
- Tracks what's downloaded via `Downloads` singleton

---

## 8. Lyrics Flow

- Multi-source lyrics with configurable priority
- Sources include: YouTube Music (Innertube), PaxSenix, and others
- Word-synced highlighting (syllable-level)
- Translation support
- Lyrics offset adjustment
- Falls back through sources until one provides lyrics

---

## 9. Source System

```mermaid
flowchart TD
    SR[SourceRegistry] --> YTM[YouTube Music<br>Innertube - Built-in]
    SR --> JS[JioSaavn<br>Built-in]
    SR --> AD[Add-on Sources<br>HTTP Protocol]
    SR --> MOD[Module Sources<br>QuickJS Plugins]
    
    AD --> AD1[External Server]
    MOD --> MOD1[JavaScript Module]
    
    SR --> HC[Health Check]
    SR --> TM[Track Matching]
```

- **SourceRegistry** manages all available sources
- **YouTube Music (Innertube)** is the primary and fallback source
- **Add-on sources** communicate via HTTP protocol (tested via MockWebServer in unit tests)
- **Module sources** run JavaScript in QuickJS VM for custom resolution logic
- Sources provide: stream URLs, quality tiers, metadata
- Health checks validate source availability
- Track matching resolves content across sources

---

## 10. Backend Architecture

### Listen Together Server (`backend/`)

```mermaid
flowchart LR
    D1[Device 1] <-->|WebSocket| S[FastAPI Server]
    D2[Device 2] <-->|WebSocket| S
    D3[Device 3] <-->|WebSocket| S
    
    S --> P[Party Store<br>In-Memory]
    S --> C[Clock Sync]
    S --> H[Health Check<br>/healthz]
```

- **FastAPI** + WebSockets (uvicorn)
- **No database** — all party state in-memory
- **Clock synchronisation** via ping/pong with offset calculation
- **Party features:** 6-character codes, up to 5 devices, any member controls
- **Deployment:** Render free tier, single instance (parties are per-instance)
- **Key config (env vars):** `JAM_MAX_MEMBERS`, `JAM_STATE_HEARTBEAT_MS`, `JAM_PLAY_LEAD_MS`, `JAM_DISCONNECT_GRACE_MS`

---

## 11. Native Components

### Audio Analyzer Pipeline

```mermaid
flowchart LR
    A[Raw Audio PCM] --> R[Resampler]
    R --> AA[Audio Analysis<br>Tempo/Key/Energy/Structure]
    R --> MS[Mel Spectrogram]
    R --> VS[Vocal Spectrogram]
    
    MS --> BT[Beat This! ONNX<br>Beat/Downbeat Detection]
    VS --> UM[Open-Unmix ONNX<br>Vocal Separation]
    
    subgraph JNI["JNI Bridge"]
        AA
        MS
        VS
    end
    
    subgraph Kotlin["Kotlin / ONNX Runtime"]
        BT
        UM
    end
```

- **C++17** compiled via CMake 3.22.1
- **ABIs:** armeabi-v7a, arm64-v8a, x86_64
- **Library name:** `bitchord_analysis`
- **JNI classes:** `TrackFeatures`, `MelSpectrogram`, `VocalSpectrogram` (in `playback.smart` package)
- **ONNX models:** `beat_this_int8.onnx` (4.5MB), `vocals_umxhq_int8.onnx` (9MB)

---

## 12. Database & Caching

| Store | Technology | Purpose |
|---|---|---|
| **AppSettings** | Jetpack DataStore | All user preferences, theme, quality, etc. |
| **AuthStore** | EncryptedSharedPreferences | Encrypted auth sessions, cookies, profiles |
| **Widget state** | SharedPreferences (`bitchord_widget`) | Widget playback snapshot |
| **AudioCache** | DiskLruCache (custom) | Cached audio streams |
| **CanvasCache** | DiskLruCache (custom) | Cached canvas video clips |
| **Image cache** | Coil DiskCache (100MB) | Album artwork |
| **ListeningStats** | File-based | Listening history for Replay |
| **Downloads record** | File-based | What's been downloaded |
| **SearchHistory** | DataStore/Preferences | Recent searches |
| **LastPlayed** | DataStore/Preferences | Playback resume state |
| **SourceRegistry** | DataStore/Preferences | Source configurations |

> **Note:** There is NO Room database. All persistence uses DataStore, SharedPreferences, or file-based storage.

---

## 13. Background Services

| Service | Type | Purpose |
|---|---|---|
| **PlaybackService** | Foreground (mediaPlayback) | Music playback, media session, notification |
| **DownloadService** | Foreground (dataSync) | Track downloading |
| **AppLocalesMetadataHolderService** | Disabled by default | AndroidX per-app locale storage |

---

## 14. Localization

The app supports **15 locales** plus the default (English):
- German (de), Spanish (es), French (fr), Hebrew (he), Hindi (hi)
- Indonesian (id), Italian (it), Japanese (ja), Polish (pl)
- Portuguese (pt), Russian (ru), Thai (th), Turkish (tr)
- Vietnamese (vi), Chinese (zh)

Plus resource qualifiers:
- `values-night` — dark theme colors
- `values-v31` — API 31+ specific values
- `values-sw600dp` — tablet layout values
