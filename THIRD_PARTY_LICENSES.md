# Third-Party Licenses

PDFSeal itself is licensed under **AGPL-3.0-or-later**. The full GNU AGPL-3.0
text ships with the app at `assets/licenses/AGPL-3.0.txt` (copied from the
repo-root `LICENSE` at build time) and in the repo root as `LICENSE`.
It uses the third-party components listed below. Each component remains under its
own license. Where a component is AGPL (MuPDF), it is one of the reasons the whole
PDFSeal project is AGPL-compatible and distributed as AGPL-3.0-or-later.

**Full license texts are bundled inside the APK** so they are available offline
without the source tree:

| License | Bundled path in the APK |
|---------|-------------------------|
| GNU AGPL-3.0 (PDFSeal, MuPDF) | `assets/licenses/AGPL-3.0.txt` |
| Apache-2.0 (Tesseract4Android, Tesseract, AndroidX/Compose, Kotlin) | `assets/licenses/Apache-2.0.txt` |
| BSD-2-Clause-style (Leptonica) | `assets/licenses/Leptonica-BSD-2-Clause.txt` |
| SIL Open Font License 1.1 (signature fonts) | `assets/fonts_licenses/{GreatVibes,Pacifico,PinyonScript}-OFL.txt` |

If any dependency listed here is found to have a licensing conflict with
AGPL distribution, that issue must be documented openly in this file and in
[docs/ROADMAP.md](docs/ROADMAP.md) rather than hidden. No closed-source,
binary-only app logic is permitted in this project.

---

## 1. MuPDF

- **Purpose:** Primary PDF rendering and editing engine.
- **Copyright:** © Artifex Software, Inc.
- **License:** GNU Affero General Public License, version 3 (AGPL-3.0).
- **Project:** https://mupdf.com/ — https://github.com/ArtifexSoftware/mupdf
- **Notes:** MuPDF is dual-licensed (AGPL or a commercial license from Artifex).
  PDFSeal uses it under the **AGPL-3.0** option. This is why PDFSeal as a whole
  is distributed under AGPL-3.0-or-later. The full AGPL text is bundled in the
  APK at `assets/licenses/AGPL-3.0.txt` (and in the repo root as `LICENSE`).

## 2. Tesseract4Android

- **Purpose:** Android JNI bindings + prebuilt native libraries for Tesseract.
- **Copyright:** © Adaptech s.r.o. and contributors.
- **License:** Apache License, Version 2.0.
- **Project:** https://github.com/adaptech-cz/Tesseract4Android

## 3. Tesseract OCR engine

- **Purpose:** Offline optical character recognition.
- **Copyright:** © Google Inc. and the Tesseract contributors.
- **License:** Apache License, Version 2.0.
- **Project:** https://github.com/tesseract-ocr/tesseract
- **Trained data:** `eng.traineddata` is bundled at
  `app/src/main/assets/tessdata/eng.traineddata` (~4 MB), taken from the
  **tessdata_fast** project, Apache License 2.0.
  https://github.com/tesseract-ocr/tessdata_fast — copied to app filesDir on
  first OCR run and memory-mapped (kept uncompressed in the APK via
  `androidResources.noCompress`).

## 4. Leptonica

- **Purpose:** Image processing library used by Tesseract.
- **Copyright:** © Leptonica.
- **License:** BSD 2-Clause-style license.
- **Project:** http://www.leptonica.org/

## 5. AndroidX / Jetpack Compose / Android Open Source Project

- **Purpose:** UI toolkit, lifecycle, navigation, DocumentFile, DataStore, etc.
- **Copyright:** © The Android Open Source Project.
- **License:** Apache License, Version 2.0.

## 6. Kotlin Standard Library & Coroutines

- **Purpose:** Language runtime and coroutine support.
- **Copyright:** © JetBrains s.r.o. and Kotlin contributors.
- **License:** Apache License, Version 2.0.
- **Project:** https://github.com/JetBrains/kotlin

## 7. Signature fonts (SIL Open Font License 1.1)

PDFSeal bundles the following open fonts for the typed-signature feature. Each is
licensed under the **SIL Open Font License, Version 1.1**, which permits embedding
and redistribution within an open-source application.

| Font (file) | Signature style | Copyright |
|-------------|-----------------|-----------|
| **Great Vibes** (`res/font/great_vibes.ttf`) | Elegant cursive (`ELEGANT_CURSIVE`) | Copyright © 2015 The Great Vibes Pro Project Authors. |
| **Pacifico** (`res/font/pacifico.ttf`) | Bold handwritten (`BOLD_HANDWRITTEN`) | Copyright © 2018 The Pacifico Project Authors. |
| **Pinyon Script** (`res/font/pinyon_script.ttf`) | Clean formal script (`CLEAN_FORMAL_SCRIPT`) | Copyright © 2024 The Pinyon Project Authors. |

All three are under the **SIL Open Font License v1.1**. The full per-font OFL
text is bundled in the APK at `assets/fonts_licenses/`:

- `GreatVibes-OFL.txt`
- `Pacifico-OFL.txt`
- `PinyonScript-OFL.txt`

No commercial or restrictively-licensed fonts are used. (Permanent Marker was
considered for the bold style but is Apache-2.0, not OFL; Pacifico — OFL — was
chosen instead to keep the font licensing uniform.)

## 8. PDFSeal logo

The `PDFSeal` logo/icon artwork is part of this project and is distributed under
the same **AGPL-3.0-or-later** license as the rest of the source.

---

## Source code availability (AGPL-3.0 obligation)

PDFSeal links **MuPDF**, which is used here under the **GNU AGPL-3.0** option.
Because of this, the *entire* PDFSeal application is distributed under
AGPL-3.0-or-later and the complete corresponding source code MUST be made
available to anyone who receives the app.

PDFSeal is **not** a closed-source app and must never be described as one.

Source code: https://github.com/WGRALGO/WGRALGO-PDFSeal

When PDFSeal is distributed publicly (any APK handed to anyone), the complete
corresponding source code for that exact build MUST remain publicly available
at the URL above, or at another clearly stated public source location announced
together with the binary. This is a hard AGPL-3.0 obligation, not optional.
The same URL is compiled into the app (`BuildConfig.SOURCE_URL`) and shown on
the in-app About / Privacy / Licenses screen — it must stay consistent across
the README, the About screen, and this file.

---

## License texts

All full license texts ship inside the APK so a recipient never needs the
source tree to read them:

- **GNU AGPL v3** — `assets/licenses/AGPL-3.0.txt` (build-copied from repo-root
  `LICENSE`; governs PDFSeal itself and MuPDF).
- **Apache-2.0** — `assets/licenses/Apache-2.0.txt` (Tesseract4Android,
  Tesseract OCR engine + `eng.traineddata`, AndroidX / Jetpack Compose,
  Kotlin stdlib & coroutines).
- **Leptonica BSD-2-Clause-style** — `assets/licenses/Leptonica-BSD-2-Clause.txt`.
- **SIL Open Font License 1.1** — `assets/fonts_licenses/GreatVibes-OFL.txt`,
  `assets/fonts_licenses/Pacifico-OFL.txt`,
  `assets/fonts_licenses/PinyonScript-OFL.txt`.

No referenced license file is missing from the package, and no closed-source
language is used anywhere.
