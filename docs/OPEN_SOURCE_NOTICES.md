# FloroBeat — Open Source Notices

> **Product:** FloroBeat
> **Developed & maintained by:** FloroSoft
> **Document Date:** 2026-09-21

---

## Origin & Licensing

FloroBeat is a modified version of [BitChord](https://github.com/kushagrasinghx/BitChord), an open-source Android music application originally created by Kushagra Singh and contributors.

FloroBeat is licensed under the **GNU General Public License v3.0 (GPLv3)**, the same license as the original BitChord project. The complete license text is available in the [LICENSE](../LICENSE) file.

In compliance with GPLv3 §5:
- This software is a **modified version** of BitChord.
- It is marked as changed and maintained under the FloroBeat product identity by FloroSoft.
- The entire work is licensed under GPLv3.
- The corresponding source code is available at the project repository.

FloroSoft does not claim original authorship of code inherited from the BitChord project or its upstream contributors.

---

## GPLv3 Notice

```
FloroBeat — A modern music experience by FloroSoft.
Based on BitChord, Copyright (C) Kushagra Singh and contributors.
Modifications Copyright (C) 2026 FloroSoft.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
```

---

## Third-Party Libraries & Licenses

### Android & Google Libraries (Apache License 2.0)

| Library | Version | License |
|---|---|---|
| AndroidX Core KTX | 1.15.0 | Apache 2.0 |
| AndroidX AppCompat | 1.7.0 | Apache 2.0 |
| AndroidX Activity Compose | 1.9.3 | Apache 2.0 |
| AndroidX Navigation Compose | 2.8.5 | Apache 2.0 |
| AndroidX Lifecycle Runtime KTX | 2.8.7 | Apache 2.0 |
| AndroidX Lifecycle ViewModel Compose | 2.8.7 | Apache 2.0 |
| AndroidX Compose BOM | 2024.12.01 | Apache 2.0 |
| AndroidX Compose Foundation | 1.10.0 | Apache 2.0 |
| AndroidX Compose UI | (BOM) | Apache 2.0 |
| AndroidX Compose Material 3 | (BOM) | Apache 2.0 |
| AndroidX Compose Material Icons Extended | (BOM) | Apache 2.0 |
| AndroidX Media3 ExoPlayer | 1.11.0 | Apache 2.0 |
| AndroidX Media3 Session | 1.11.0 | Apache 2.0 |
| AndroidX Media3 Common | 1.11.0 | Apache 2.0 |
| AndroidX Media3 DataSource OkHttp | 1.11.0 | Apache 2.0 |
| AndroidX Media3 ExoPlayer HLS | 1.11.0 | Apache 2.0 |
| AndroidX Media3 ExoPlayer DASH | 1.11.0 | Apache 2.0 |
| AndroidX Palette KTX | 1.0.0 | Apache 2.0 |
| AndroidX Security Crypto | 1.1.0-alpha06 | Apache 2.0 |
| Google Protobuf JavaLite | 4.35.0 | BSD 3-Clause |

### Kotlin & JetBrains (Apache License 2.0)

| Library | Version | License |
|---|---|---|
| Kotlin | 2.3.20 | Apache 2.0 |
| kotlinx.serialization JSON | 1.7.3 | Apache 2.0 |
| kotlinx.coroutines Guava | 1.9.0 | Apache 2.0 |

### Networking (Apache License 2.0)

| Library | Version | License |
|---|---|---|
| Ktor Client Core | 3.0.3 | Apache 2.0 |
| Ktor Client OkHttp | 3.0.3 | Apache 2.0 |
| Ktor Client Content Negotiation | 3.0.3 | Apache 2.0 |
| Ktor Client WebSockets | 3.0.3 | Apache 2.0 |
| Ktor Serialization kotlinx JSON | 3.0.3 | Apache 2.0 |

### Image Loading (Apache License 2.0)

| Library | Version | License |
|---|---|---|
| Coil Compose | 3.0.4 | Apache 2.0 |
| Coil Network OkHttp | 3.0.4 | Apache 2.0 |

### UI Effects (Apache License 2.0)

| Library | Version | License |
|---|---|---|
| Haze | 1.3.1 | Apache 2.0 |
| Haze Materials | 1.3.1 | Apache 2.0 |

### Markdown Rendering

| Library | Version | License |
|---|---|---|
| compose-richtext UI Material3 | 0.20.0 | MIT |
| compose-richtext CommonMark | 0.20.0 | MIT |

### Stream Resolution

| Library | Version | License |
|---|---|---|
| NewPipeExtractor | v0.26.3 | GPLv3 |
| nanojson | (bundled) | MIT |
| jsoup | 1.22.2 | MIT |
| FindBugs JSR305 | 3.0.2 | BSD |
| Mozilla Rhino | 1.8.1 | MPL 2.0 |
| Mozilla Rhino Engine | 1.8.1 | MPL 2.0 |

### JavaScript Engine

| Library | Version | License |
|---|---|---|
| QuickJS-KT Android | 1.0.5 | MIT |

### Machine Learning

| Library | Version | License |
|---|---|---|
| ONNX Runtime Android | 1.28.0 | MIT |
| Beat This! model (beat_this_int8.onnx) | — | MIT |
| Open-Unmix model (vocals_umxhq_int8.onnx) | — | MIT |

### Vendored Code

| Component | Origin | License |
|---|---|---|
| `org.schabi.newpipe.extractor.utils.Utils` | NewPipeExtractor (patched) | GPLv3 |
| `com.my.kizzy.*` | Kizzy Discord RPC | — |

> **Note:** The Kizzy Discord RPC code vendored under `com.my.kizzy` should be verified for its specific license terms.

### Testing

| Library | Version | License |
|---|---|---|
| JUnit | 4.13.2 | EPL 1.0 |
| OkHttp MockWebServer | 4.12.0 | Apache 2.0 |
| kotlinx.coroutines Test | 1.10.2 | Apache 2.0 |
| AndroidX Test JUnit | 1.3.0 | Apache 2.0 |
| AndroidX Espresso Core | 3.7.0 | Apache 2.0 |

### Backend (Python)

| Library | Version | License |
|---|---|---|
| FastAPI | (requirements.txt) | MIT |
| uvicorn | (requirements.txt) | BSD |

---

## Acknowledgements

FloroBeat is built on the work of many open-source projects and contributors:

- **BitChord** by Kushagra Singh and contributors — the technical foundation of this project
- **NewPipeExtractor** by the TeamNewPipe contributors — stream resolution
- **Kizzy** — Discord Rich Presence implementation
- **Beat This!** — beat/downbeat detection model (MIT license)
- **Open-Unmix** — vocal separation model
- **Haze** by Chris Banes — frosted glass UI effects
- **Coil** — image loading
- **am-lyrics** by binimum — Apple-like lyrics animation inspiration

---

## How to Access Licenses

The full text of each license can be found:

1. **GPLv3** — `LICENSE` file in the project root
2. **Apache 2.0** — https://www.apache.org/licenses/LICENSE-2.0
3. **MIT** — https://opensource.org/licenses/MIT
4. **MPL 2.0** — https://www.mozilla.org/en-US/MPL/2.0/
5. **BSD** — https://opensource.org/licenses/BSD-3-Clause
6. **EPL 1.0** — https://www.eclipse.org/legal/epl-v10.html

---

## Contact

For questions about FloroBeat's open-source licensing or compliance:
- **FloroSoft Engineering**
- Repository: [FloroBeat on GitHub]
