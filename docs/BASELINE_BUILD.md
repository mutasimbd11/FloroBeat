# FloroBeat — Baseline Build Report

> **Date:** 2026-09-21
> **Branch:** `florobeat-development`
> **Build Target:** `assembleDevDebug`

---

## Environment

| Component | Version |
|---|---|
| **OS** | Windows |
| **JDK** | 17 (required) |
| **Android SDK** | Located at `C:\Users\mutas\AppData\Local\Android\Sdk` |
| **Gradle** | 8.11.1 (wrapper) |
| **AGP** | 8.10.1 |
| **NDK** | Required for `florobeat_analysis` native library |
| **CMake** | 3.22.1 (required for native build) |

---

## Configuration Required

The following configuration was needed before the baseline could build:

### 1. `local.properties` (Required)
```properties
sdk.dir=C\:\\Users\\mutas\\AppData\\Local\\Android\\Sdk
```
This file is gitignored and must be created on each machine.

### 2. Optional (not required for build)
```properties
LASTFM_API_KEY=     # Last.fm scrobbling (empty = feature disabled)
LASTFM_SECRET=      # Last.fm scrobbling
LISTEN_TOGETHER_SERVER=  # Listen Together default server URL
```

### 3. Signing (optional for debug, required for release)
`keystore.properties` with:
```properties
storeFile=florobeat-release.jks
storePassword=<password>
keyAlias=florobeat
keyPassword=<password>
```

---

## Build Status

> ⏳ **Build in progress** — `assembleDevDebug` running with `--no-daemon`

### Build Result
_To be updated when the build completes._

### Known Requirements
- **NDK with CMake 3.22.1** must be installed via SDK Manager
- **Compile SDK 36** must be available
- **JDK 17** is required (source/target compatibility)
- The `suppressUnsupportedCompileSdk=36` flag is set in `gradle.properties`

### Build Variants
| Variant | App ID | Description |
|---|---|---|
| `devDebug` | `com.dev.bitchord` | Development debug build |
| `devRelease` | `com.dev.bitchord` | Development release build |
| `prodDebug` | `com.music.bitchord` | Production debug build |
| `prodRelease` | `com.music.bitchord` | Production release (requires signing) |

### ABI Splits
The build produces separate APKs for:
- `armeabi-v7a`
- `arm64-v8a`
- `x86_64`
- `universal` (all ABIs)

---

## Warnings & Notes

1. **R8/D8 Override:** The project overrides D8 from 8.10.9 → 8.13.23 to fix a register allocation bug in debug dexing that causes a `VerifyError` in `NowPlayingScreen` (see root `build.gradle.kts` comments).

2. **ProGuard/R8 Disabled:** `isMinifyEnabled = false` for release builds. The comments explain this is deliberate due to Rhino, NewPipe, Ktor, and kotlinx.serialization using reflection extensively.

3. **NewPipe Utils Patch:** A custom `Utils.java` is vendored at `org.schabi.newpipe.extractor.utils.Utils` to override the library's copy. The library's class is stripped from the jar before dexing.

4. **Kotlin 2.3.20:** Using the latest Kotlin with the compose compiler plugin. The R8 8.13.x override is needed to read Kotlin 2.3 `@Metadata` correctly.
