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
- [ ] Typed-name signature (3 OFL font styles), place/move/resize/rotate, flatten
- [ ] Cover & Replace (visual cover + replacement text) — **not** redaction
- [ ] Page thumbnails
- [ ] Recent files (persisted SAF permissions)

## v0.3.0 — OCR
- [ ] Bundle `eng.traineddata`; offline Tesseract integration
- [ ] OCR current page; show recognised text; store per-page result
- [ ] Map OCR boxes to PDF coordinates
- [ ] Basic Make Editable Copy

## v0.4.0 — Better editable copy
- [ ] Improved OCR editable overlays
- [ ] OCR selected pages
- [ ] Improved export fidelity

## v1.0.0 — Stable release
- [ ] Stable open / view / edit / export
- [ ] Typed signatures
- [ ] OCR editable copy
- [ ] Page tools: rotate, delete, reorder, merge, split (where feasible)
- [ ] About screen with signing-cert SHA-256
- [ ] Signed APK on GitHub Releases + checksum + release docs

## Future (not promised, not faked)
- Stronger layout reconstruction; better font matching
- Undo / redo
- Reusable signature presets
- PDF forms (if feasible)
- **True secure redaction** (actual content removal — distinct from Cover &
  Replace, which is visual only)
- F-Droid readiness

## Known constraints / open risks
- Workspace is on an exFAT external disk — Gradle cache kept on internal disk
  (see [BUILDING.md](BUILDING.md)).
- NDK not installed on the maintainer machine — MuPDF/Tesseract are used as
  prebuilt artifacts. If a needed capability is only available via source build,
  it will be documented here, not silently dropped.
- Cover & Replace is visual only; secure redaction is explicitly future work.
