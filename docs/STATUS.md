# PDFSeal — Project Status

Snapshot of where PDFSeal stands. For the plan see [ROADMAP.md](ROADMAP.md);
for design see [ARCHITECTURE.md](ARCHITECTURE.md).

_Last updated: 2026-05-16 — current `versionName` 0.4.0, `versionCode` 7._

## Released

- **GitHub Release v0.4.0** —
  <https://github.com/WGRALGO/PDFSeal/releases/tag/v0.4.0>
  (signed APK + SHA-256 + signing-cert fingerprint).
- Tags: `v0.1.0`, `v0.4.0`.

## Feature status

| Area | Status | Notes |
|------|--------|-------|
| Open / view / zoom / pan / page nav | Done | SAF; recent files |
| Page thumbnails | Done | Tap-to-jump navigator |
| Add Text | Done | Place / move / edit / delete |
| Typed signature | Done | 3 SIL OFL styles; **not** a certified signature |
| Cover & Replace | Done | **Visual only — not secure redaction** |
| OCR (current page) | Done | Offline Tesseract `eng`; review output |
| Make Editable Copy | Done | OCR reconstruction, not native text editing |
| Page tools | Done | Rotate / delete / reorder / split (export plan) |
| Export | Done | Flattened **copy**; original never overwritten |
| Engine layer isolation | Enforced | No MuPDF imports in `ui/` |
| Coordinate mapper | Tested | 6 JVM unit tests green |

## Known limitations (by design, documented — not defects to hide)

- Export is **rasterised**: exported page text is not selectable. True
  annotation-level flattening is future work.
- **Cover & Replace is visual only.** Covered content may persist in the
  exported file. Do not use it to remove sensitive data. True secure redaction
  is future work.
- OCR can be wrong — always review recognised text before relying on it.
- **Cross-file Merge** is not implemented yet (planned; "if feasible").
- F-Droid: metadata is ready ([FDROID.md](FDROID.md)); official-repo inclusion
  is blocked only by needing source-built MuPDF/Tesseract (free-licensed, not a
  license problem).

## Outstanding before a v1.0.0 tag

- [ ] **On-device verification pass** on a real Android tablet, following the
  test matrix in the main [README](../README.md#testing). The build
  environment has no emulator/device, so the maintainer must run it:
  open → edit (text/signature/cover/OCR/editable-copy/pages) → export →
  reopen elsewhere → confirm the original is unchanged → confirm an
  update APK installs over the previous one with the same signing key.
- [ ] Fix anything that on-device testing surfaces.

Everything else for v1.0.0 is code-complete.
