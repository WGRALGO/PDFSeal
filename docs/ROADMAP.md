# PDFSeal Roadmap

Semantic versions. Features are listed as done only when they work end-to-end:
open → edit → export → reopen in another viewer, with the original unchanged.

## v0.1.0 — Foundation + first vertical slice
- [x] Repo foundation: README, LICENSE, NOTICE, THIRD_PARTY_LICENSES, SECURITY
- [x] docs/: BUILDING, RELEASING, ARCHITECTURE, ROADMAP
- [x] `.gitignore` hardened; `key.properties.example`
- [x] Android Gradle + Kotlin + Compose project skeleton
- [x] Release signing config (reads gitignored `key.properties`)
- [x] App launcher icon from the PDFSeal logo
- [x] PDF engine skeleton (PdfEngine, Session, Renderer, CoordinateMapper,
      edit models, Exporter skeleton, OCR/IO interfaces)
- [ ] Open PDF (SAF) → render → zoom/pan → Add Text → export edited copy
- [ ] Reopen exported PDF elsewhere to confirm the edit

## v0.2.0 — Signature, Cover & Replace, navigation
- [x] Typed-name signature (3 OFL font styles), place/move, flatten on export
- [x] Cover & Replace (visual cover + replacement text) — **not** redaction
- [x] Page thumbnails (delivered in the v0.4.0 line)
- [x] Recent files (persisted SAF permissions)

## v0.3.0 — OCR
- [x] Bundle `eng.traineddata`; offline Tesseract integration
- [x] OCR current page; show recognised text; store per-page result
- [x] Map OCR boxes to PDF coordinates (EditableCopyBuilder)
- [x] Basic Make Editable Copy

## v0.4.0 — Better editable copy
- [ ] Improved OCR editable overlays
- [ ] OCR selected pages
- [ ] Improved export fidelity

## v1.0.0 — Stable release ✅ DONE (2026-05-16)

Tagged `v1.0.0` (commit `b952aa0`), **GitHub Release published**:
<https://github.com/WGRALGO/PDFSeal/releases/tag/v1.0.0> — dual signed APKs
(arm64-v8a + universal) + SHA-256, provenance-verified. See
[RELEASING.md](RELEASING.md) and [TESTING.md](TESTING.md).

- [x] Stable open / view / edit / export
- [x] Typed signatures
- [x] OCR editable copy
- [x] Page tools: rotate, delete, reorder, split (cross-file merge deferred)
- [x] About screen with signing-cert SHA-256
- [x] Signed APKs on GitHub Releases + checksums + release docs (v1.0.0:
      arm64-v8a + universal, `-PabiSplit`, embedded-commit == tag verified)
- [x] Page thumbnails (tap-to-jump navigator)
- [x] Honest UI/About/README + first-launch limits + export/tool warnings
      (single source `HonestCopy`)
- [x] Static release test matrix PASS ([TESTING.md](TESTING.md))
- [ ] On-device verification pass — **maintainer follow-up** on physical
      hardware (build env has no device/emulator); DEVICE rows in
      [TESTING.md](TESTING.md). Any finding → 1.0.x patch.

## Future (not promised, not faked)
- Stronger layout reconstruction; better font matching
- Undo / redo
- Reusable signature presets
- PDF forms (if feasible)
- **True secure redaction** (actual content removal — distinct from Cover &
  Replace, which is visual only)
- F-Droid: metadata in place (see [FDROID.md](FDROID.md)); official-repo
  inclusion blocked only by needing source-built MuPDF/Tesseract (documented,
  not a licensing issue)

## Known constraints / open risks
- Workspace is on an exFAT external disk — Gradle cache kept on internal disk
  (see [BUILDING.md](BUILDING.md)).
- NDK not installed on the maintainer machine — MuPDF/Tesseract are used as
  prebuilt artifacts. If a needed capability is only available via source build,
  it will be documented here, not silently dropped.
- Cover & Replace is visual only; secure redaction is explicitly future work.
