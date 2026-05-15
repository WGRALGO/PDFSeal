# Third-Party Licenses

PDFSeal itself is licensed under **AGPL-3.0-or-later** (see [LICENSE](LICENSE)).
It uses the third-party components listed below. Each component remains under its
own license. Where a component is AGPL (MuPDF), it is one of the reasons the whole
PDFSeal project is AGPL-compatible and distributed as AGPL-3.0-or-later.

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
  is distributed under AGPL-3.0-or-later. The full AGPL text is in [LICENSE](LICENSE).

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
- **Trained data:** `eng.traineddata` (and any future language packs) are
  distributed by the tessdata project under the Apache License, Version 2.0.
  https://github.com/tesseract-ocr/tessdata

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

| Font | Style role | Copyright |
|------|-----------|-----------|
| **Great Vibes** | Elegant cursive | Copyright © 2012 TipoType. |
| **Caveat** | Bold handwritten | Copyright © 2015 The Caveat Project Authors (Impallari Type). |
| **Pinyon Script** | Clean formal script | Copyright © 2011 Nicole Fally. |

Font files and the OFL text will be placed under `app/src/main/res/font/` with the
OFL license copied alongside them when the signature feature is implemented
(Phase 5). No commercial or restrictively-licensed fonts are used.

> **Note:** Final embedded font choices are confirmed at implementation time.
> If a chosen font's license is not OFL/embeddable, it will be replaced with an
> OFL alternative and this table updated — not silently substituted.

## 8. PDFSeal logo

The `PDFSeal` logo/icon artwork is part of this project and is distributed under
the same **AGPL-3.0-or-later** license as the rest of the source.

---

## License texts

- GNU AGPL v3: [LICENSE](LICENSE)
- Apache-2.0, BSD-2-Clause, and SIL OFL 1.1 full texts are reproduced in this
  file's appendix as components are integrated. Until a component is actually
  added to the build, its full license text will be added here in the same
  commit that introduces the dependency.
