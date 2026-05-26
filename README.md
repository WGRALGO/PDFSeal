# PDFSeal

🧪 Pre-release — source available, **pre-release APK published**.

PDFSeal is being repaired and tested feature by feature on real hardware. The
**source code is public** so the work can be reviewed, built, and improved. A
**pre-release APK (v1.1.1)** is now available on the
[Releases page](https://github.com/WGRALGO/WGRALGO-PDFSeal/releases) — or build
from source.

This is a pre-release: core features (bookmarks, Pages menu, add-text, signature,
cover & replace, memory-safe flatten export) are working and verified on-device,
but **OCR and OCR-based Edit (tap-to-edit) are not fully functional yet.** Still
being validated end-to-end (open → edit → export → reopen elsewhere) before a
stable release.

### What's new in v1.1.1

Fixes from on-device testing on a Samsung Galaxy Z Flip 6:

- **Exported text/signatures land where you placed them.** The on-screen
  Compose preview included font padding the exporter did not, so flattened
  text drifted upward (signatures slightly, text considerably). Editor and
  exporter now share the same glyph-top baseline.
- **Resize handles moved fully outside the selection box.** The blue corner
  handles no longer cover the content underneath — handy for placing an X in
  a small checkbox or sizing a tight box. Hit area expanded to match.
- **Undo / Redo (20-step)** for move, resize, add, delete, and text edits.
  A drag counts as one undo step, not one per pixel.
- **2-column scrollable bottom menu.** Two rows (four buttons) visible at a
  time, vertical scroll for the rest. Undo / Redo pinned to the top row.

## Project Purpose

PDFSeal exists because basic PDF editing should not be locked behind expensive subscriptions, cloud accounts, ads, trackers, or bloated software.

The goal of this project is to build a free, open-source, offline PDF markup tool for serious people with limited resources, and for serious people who do not support corporate greed disguised as productivity software.

PDFSeal is for students, workers, small nonprofits, independent creators, job seekers, organizers, small business owners, and anyone else who needs practical PDF tools without being pushed into another monthly payment.

This project is not trying to clone Adobe Acrobat or pretend to do everything commercial PDF software does. The goal is simpler and more honest: create a useful, privacy-respecting, on-device PDF editor that real people can use, inspect, improve, and share.

PDFSeal will remain open source so the code can be reviewed, repaired, improved, and trusted by the community.

---

[Source code](https://github.com/WGRALGO/WGRALGO-PDFSeal) · License: **AGPL-3.0-or-later**

## What PDFSeal is

PDFSeal is an open-source, offline Android PDF markup app. It can open PDFs, let
users add new text, typed visual signatures, visual cover/replace blocks,
OCR-assisted editable text overlays, and export a flattened edited copy.

PDFSeal is not Acrobat. It does not claim native word-processor-style editing of
the original PDF text stream. It does not provide certified cryptographic digital
signatures. Its Cover & Replace tool is visual only and is not secure redaction.

Everything runs **on-device**. No cloud upload, no server processing, no account,
no analytics, no ads, no trackers, no Google Play Services.

## Important limits

PDFSeal exports flattened visual copies. It does not preserve all original PDF
structures.

- Cover & Replace is visual only. It is not secure redaction.
- Visual Signature is a typed visual mark only. It is not a certified
  cryptographic digital signature.
- Make Editable Copy uses OCR and editable overlays. It does not edit the
  original PDF text stream like a word processor.
- Export may not preserve forms, links, bookmarks, layers, annotations,
  selectable text, accessibility structure, metadata, or existing digital
  signatures.

## What PDFSeal currently does

Working and verified on a real tablet (debug build):

- Open local PDFs via the Android Storage Access Framework (SAF)
- View, zoom, pan, page navigation
- **Bookmarks** — read the PDF outline and tap an entry to jump to its page; add a bookmark for the current page; delete bookmarks; "Save PDF with bookmarks" writes the updated outline to a real PDF
- **Pages menu** — page thumbnails plus:
  - **Rotate** the current page left/right 90° (live in the viewer)
  - **Crop** one page or all pages with an adjustable handle box (composes with rotation)
  - **Delete** pages by range (e.g. `3-7`, `2,5,9-11`)
  - **Add PDF** — merge another PDF in at the start, after the current page, or at the end
  - The viewer navigates the edited page plan, so rotate/crop/delete/add are WYSIWYG
- **Add Text** — place new text; the box auto-fits and you resize the text by dragging a corner
- **Typed Visual Signature** — type a name, pick a style, place/move/resize/rotate. A *visual* mark only — **not** a certified cryptographic digital signature.
- **Cover & Replace** — visually cover a rectangular area and put replacement text on top (visual only — **not** secure redaction)
- **Export Flattened PDF** — burns all edits (text, signature, cover/replace, highlight, strikethrough) into a new flattened PDF copy. Verified to produce a valid, openable `%PDF` file; copies to the chosen location via SAF; the original is never overwritten. Handles large documents without running out of memory.

### Not fully functional yet

These run but are **not** reliable end-to-end — treat them as experimental:

- **OCR** — offline Tesseract OCR executes, but recognition/placement is not dependable yet. Always review results.
- **Edit / tap-to-edit (Make Editable Copy)** — depends on OCR, so it is also not fully functional. Tapping a recognised line to edit it may misread or misplace text.

## Planned / future features

These are **not** finished yet:

- Reliable **OCR** and OCR-based **tap-to-edit / Make Editable Copy**
- Page **split/extract**
- Additional OCR languages beyond English/Latin

## Current status

> **Under testing — source available, not yet released.** PDFSeal is in
> pre-release development. No prebuilt APK is published; build from source to
> try it. As of the 2026-05-21 device pass, bookmarks, the Pages menu
> (rotate / crop / delete-by-range / add-PDF merge), Add Text (auto-fit +
> drag-to-resize), typed signature, cover & replace, and **Export Flattened
> PDF** (now produces a verified valid PDF and handles large documents) all
> work on a real tablet (debug build). **OCR and the OCR-based Edit /
> tap-to-edit feature are not fully functional yet** and are still being
> worked on.
> See [RELEASE_WITHDRAWAL.md](RELEASE_WITHDRAWAL.md) for history,
> [docs/TESTING.md](docs/TESTING.md) for the test matrix,
> [docs/ROADMAP.md](docs/ROADMAP.md) for the phased plan
> and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the engine design.

Feature status is tracked honestly. Features are not shown as done until they
actually work end-to-end (open → edit → export → reopen elsewhere).

## Honest feature definitions — please read

PDFSeal does **not** claim Acrobat-level native PDF text editing.

- **Add Text** — adds *new* text on top of the page. It does not reflow or edit the
  document's original text stream.
- **Cover & Replace** — *visual* editing only. It draws an opaque box over an area
  and lets you put new text on top. **This is NOT secure redaction.** The covered
  content may still exist underneath in the exported file's structure unless the
  page is rasterised. Do not rely on Cover & Replace to remove sensitive data.
- **Make Editable Copy** — OCR-based reconstruction. The app renders the page,
  runs offline OCR, and creates editable text boxes from recognised text. This is
  **not** true word-processor-style editing of the original PDF text.
- **Export = flattened visual copy.** Export draws each page onto a new PDF and
  paints edits on top. The output is a flattened/rasterised visual copy, **not**
  a native-object PDF edit. Original PDF objects such as forms, links, bookmarks,
  layers, annotations, selectable text, accessibility structure, metadata, or
  existing digital signatures may not be preserved. The app shows this warning
  before every export.
- **Typed Visual Signature** — a visual typed-name mark only. It is **not** a
  certified cryptographic digital signature and carries no legal signing
  guarantee.

### OCR caveat

OCR can and does make mistakes. Always review OCR output before relying on it.
PDFSeal uses **offline Tesseract OCR** (English / Latin first). No text ever
leaves the device.

## Privacy statement

PDFSeal is local and offline by design:

- No cloud upload
- No server-side processing
- No account or login
- No analytics
- No ads
- No trackers
- No Google Play Services / Google Mobile Services

## Building from source

PDFSeal is fully buildable from source. See **[docs/BUILDING.md](docs/BUILDING.md)**.

Quick version:

```bash
export JAVA_HOME=/path/to/jdk-17

# Debug APK — universal (all ABIs, installs on emulators + real tablets)
./gradlew :app:assembleDebug

# Release APK — universal
./gradlew :app:assembleRelease

# Smaller ARM-only release APKs for sideloading to physical tablets.
# Produces per-ABI arm64-v8a / armeabi-v7a APKs AND a universal APK.
./gradlew :app:assembleRelease -PabiSplit
```

ABI splitting is **opt-in** (`-PabiSplit`). Without it every build is a single
universal APK so x86/x86_64 emulators keep working. Outputs land in
`app/build/outputs/apk/`.

Third-party license notices live in **[THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)**
(repo root). That exact file is copied into the APK at `assets/THIRD_PARTY_LICENSES.md`
at build time and shown in-app on the **About / Privacy / Licenses** screen, so
the bundled notices can never drift from source.

## Sideloading

> No stable public APK release is currently available. PDFSeal is in
> pre-release development. The withdrawn v1.0.0 APK must **not** be downloaded,
> installed, shared, or mirrored. The steps below apply only to an APK you
> **build yourself from source** for development or testing.

1. Build a debug or release APK from source (see *Building from source* above).
2. Verify the SHA-256 checksum of the APK you built.
3. On the tablet, enable "Install unknown apps" for your file manager.
4. Open the APK and install.

**Which APK do I use?**

| Build | File | Use it for |
|-------|------|------------|
| Universal (default) | `PDFSeal-<ver>-universal.apk` | Anything — real ARM tablets/phones **and** x86/x86_64 emulators. Largest file (all ABIs). |
| ARM 64-bit | `PDFSeal-<ver>-arm64-v8a.apk` | **Recommended for sideloading onto a real modern Android tablet/phone** (almost all devices since ~2017). Smallest. |
| ARM 32-bit | `PDFSeal-<ver>-armeabi-v7a.apk` | Older 32-bit-only ARM devices. |

The per-ABI ARM APKs are only produced when you build with `-PabiSplit`
(see below); the default build is always the single universal APK so emulators
keep working.

**Build provenance.** No public release APK is currently distributed. Any APK
you build from source embeds the exact source commit at
`META-INF/version-control-info.textproto`, so the APK build revision and the
source you built from match. Verify before trusting any APK:

```bash
unzip -p PDFSeal-<ver>-<abi>.apk META-INF/version-control-info.textproto
```

Signing keys are **never** committed to this repository.
See [docs/RELEASING.md](docs/RELEASING.md).

## Third-party software

PDFSeal builds on open-source components. Full attribution and license texts are
in **[THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)** and **[NOTICE](NOTICE)**.

Key dependencies:

| Component | Purpose | License |
|-----------|---------|---------|
| [MuPDF](https://mupdf.com/) | PDF render/edit engine | AGPL-3.0 |
| [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) | Offline OCR bindings | Apache-2.0 |
| [Tesseract OCR](https://github.com/tesseract-ocr/tesseract) | OCR engine | Apache-2.0 |
| AndroidX / Jetpack Compose | UI toolkit | Apache-2.0 |
| Kotlin stdlib | Language runtime | Apache-2.0 |
| Great Vibes / Pacifico / Pinyon Script | Signature fonts | SIL OFL 1.1 |
| [Tesseract `eng` traineddata](https://github.com/tesseract-ocr/tessdata_fast) | OCR language data | Apache-2.0 |

Because PDFSeal links MuPDF, the entire project is distributed under the
**GNU Affero General Public License v3.0 or later**. When PDFSeal is
distributed publicly, the complete corresponding source code for that build
must remain publicly available at
[https://github.com/WGRALGO/WGRALGO-PDFSeal](https://github.com/WGRALGO/WGRALGO-PDFSeal)
(or another clearly stated public location) — this is a hard AGPL-3.0
obligation.

**Full license texts are packaged inside every APK** (no source tree needed
to read them):

| License | Path inside the APK |
|---------|---------------------|
| GNU AGPL-3.0 | `assets/licenses/AGPL-3.0.txt` |
| Apache-2.0 | `assets/licenses/Apache-2.0.txt` |
| Leptonica BSD-2-Clause | `assets/licenses/Leptonica-BSD-2-Clause.txt` |
| SIL OFL 1.1 (fonts) | `assets/fonts_licenses/{GreatVibes,Pacifico,PinyonScript}-OFL.txt` |
| Notice summary | `assets/THIRD_PARTY_LICENSES.md` |

The same notices and source URL are shown in-app on the
**About / Privacy / Licenses** screen.

## Testing

PDFSeal has JVM unit tests for the coordinate mapper:

```bash
./gradlew :app:testDebugUnitTest
```

The rest must be verified **on a real Android tablet** (sideload a debug or
signed release APK). MuPDF/Tesseract are native — an emulator without the right
ABI will not exercise them. Use two sample PDFs: one normal text PDF and one
scanned (image-only) PDF.

Before each test, note the original file's checksum so you can prove it is
untouched:

```bash
sha256sum original.pdf
```

| # | Test | Expected |
|---|------|----------|
| 1 | Open a normal text PDF (Open PDF → SAF picker) | Renders; zoom/pan, Prev/Next, Thumbs work |
| 2 | Open a scanned (image-only) PDF | Renders as images |
| 3 | Add Text, type, drag to position, Export | SAF "create document" prompt; success snackbar |
| 4 | Reopen the exported PDF in another viewer (Drive/Adobe) | Added text present at the right place |
| 5 | Add Signature → type name → pick each of the 3 styles → place → Export | Signature flattened in each style |
| 6 | Cover → drag a rectangle over text → Add Text on top → Export | Area visually covered; replacement text on top |
| 7 | OCR a scanned page (OCR → Run OCR) | Recognised text + confidence shown; caveat visible |
| 8 | Make Editable Copy on a scanned page | Editable text boxes created over recognised lines |
| 9 | Edit an OCR text box (select → Edit), then Export | Corrected text appears in the exported copy |
| 10 | Pages → rotate a page ⟳90 → Export | That page is rotated in the export |
| 11 | Pages → delete a page → Export | Page absent; remaining pages in order |
| 12 | Pages → reorder (↑/↓) → Export | Pages exported in the new order |
| 13 | After every export: re-check `sha256sum original.pdf` | **Unchanged** — original never overwritten |
| 14 | Install a self-built APK by sideloading | Installs on Android 7.0+ |
| 15 | Install a higher-`versionCode` APK over it (same key) | Updates **without uninstall**, data kept |
| 16 | Confirm no secrets in the repo | `git log -p \| grep -iE 'PRIVATE KEY\|storePassword\|keyPassword'` → none; `*.jks`/`key.properties` not tracked |

> Honesty checks: Cover & Replace is visual only — do **not** rely on it to
> remove sensitive data. OCR can be wrong — review before trusting it. The
> exported copy is rasterised, so its text is not selectable.

## License

Copyright (C) 2026 WGRALGO / The Wealth Gap Resolution Algorithm Inc.

Owned and maintained by WGRALGO. See [CONTRIBUTORS.md](CONTRIBUTORS.md). AI
tools (Claude) assisted with development only and hold no ownership.

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed WITHOUT ANY WARRANTY; without even the implied warranty
of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
[GNU AGPL v3](LICENSE) for more details.

## Security

To report a vulnerability, see [SECURITY.md](SECURITY.md).
