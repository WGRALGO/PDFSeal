# PDFSeal — Project Status

Snapshot of where PDFSeal stands. For the plan see [ROADMAP.md](ROADMAP.md);
for design see [ARCHITECTURE.md](ARCHITECTURE.md); for the release process see
[RELEASING.md](RELEASING.md) (dual-APK arm64-v8a + universal flow with build
provenance).

_Last updated: 2026-05-16 — current `versionName` 1.0.1, `versionCode` 11
(local build only; not publicly released)._

## Ownership

Owned and maintained by **WGRALGO / The Wealth Gap Resolution Algorithm Inc.**
Copyright © 2026 WGRALGO / The Wealth Gap Resolution Algorithm Inc.;
AGPL-3.0-or-later. Credits and ownership terms are in
[CONTRIBUTORS.md](../CONTRIBUTORS.md): WGRALGO is owner/creator/maintainer;
AI tools (Claude) are credited for development assistance only and hold no
ownership. `NOTICE` and the README License section carry the same copyright
line. There is intentionally **no in-app Credits screen**.

## Released

- **v1.0.0 was WITHDRAWN** — see [RELEASE_WITHDRAWAL.md](../RELEASE_WITHDRAWAL.md).
  Real-device testing showed the markup editor was unusable. GitHub Release
  v1.0.0 was deleted; tag replaced with `v1.0.0-withdrawn`. Repo stays public.
- Remaining published releases: `v0.5.1`, `v0.5.0`, `v0.4.0` (untouched).
- Tags: `v0.1.0`, `v0.4.0`, `v0.5.1`, `v1.0.0-withdrawn`.

## v1.0.1 — viewer/editor UX overhaul (local build, NOT released)

Fixes the defects that caused the v1.0.0 withdrawal:

- **WYSIWYG overlay**: on-screen text/signature now drawn through the same
  `EditObjectPainter` the exporter uses — preview matches export exactly.
  Fixes "signature doesn't work" and "font size doesn't work".
- **Unified gesture handler**: tap-to-select, drag-to-move, corner-resize,
  pinch-zoom/pan with off-screen clamp — no more gesture conflict. Fixes
  "can't move text once placed".
- **Toolbar redesign**: stable, horizontally scrollable grouped action bar;
  selection actions (Edit/Delete/Done) on a separate contextual row so the
  main bar never reflows. Fixes "buttons flex/move like HTML".
- **Go-to-page** dialog (tap the page indicator) + thumbnails navigator.
- **Brand theme** (`ui/theme`): explicit gold-seal light/dark schemes,
  typography, shapes — replaces bare default Material.
- Adaptive `renderScale` (crisp when zoomed); `PanClamp` unit-tested.

No tag, no GitHub release — `RELEASE_WITHDRAWAL.md` gate still applies until
the DEVICE matrix passes on real hardware.

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
