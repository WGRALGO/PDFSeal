# PDFSeal — Project Status

Snapshot of where PDFSeal stands. For the plan see [ROADMAP.md](ROADMAP.md);
for design see [ARCHITECTURE.md](ARCHITECTURE.md).

_Last updated: 2026-05-16 — current `versionName` 1.0.0, `versionCode` 10._

## Released

- **GitHub Release v1.0.0** — first stable GitHub sideload release
  (arm64-v8a + universal signed APKs + SHA-256 + signing-cert fingerprint).
  See [TESTING.md](TESTING.md) for the release test matrix.
- Tags: `v0.1.0`, `v0.4.0`, `v0.5.1`, `v1.0.0`.

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

## v1.0.0 — done at tag time

- Honest README, app UI, and About / Privacy / Licenses copy (single source:
  `HonestCopy`).
- First-launch limits screen (ack stored locally); same text in About.
- Export confirmation before every export; Cover & Replace notice when used.
- Cover / Signature / OCR tool warnings; post-OCR review warning.
- No `INTERNET` (or any) permission; `allowBackup=false`; SAF-only file I/O;
  original PDF never in a write path.
- arm64-v8a + universal signed APKs, SHA-256, embedded build commit ==
  `v1.0.0` tag.
- Static test matrix PASS — see [TESTING.md](TESTING.md).

## Maintainer follow-up (post-tag, on real hardware)

- [ ] Run the **DEVICE** rows in [TESTING.md](TESTING.md) on a real Android
  tablet/phone (build env has no emulator/device): install both APKs,
  v0.5.1 → v1.0.0 in-place update, open/edit/export round trips, reopen
  elsewhere, confirm original unchanged.
- [ ] Fix anything on-device testing surfaces in a 1.0.x patch.
