# PDFSeal v1.0.0 — Release Test Matrix

This is the release test checklist for PDFSeal v1.0.0. It records what was
verified statically in the build/source environment and what must be run by
the maintainer on a real Android device.

Status legend:

- **PASS (static)** — verified by source/manifest/APK inspection in the build
  environment (no device required; result is deterministic).
- **DEVICE** — must be run by the maintainer on a physical Android
  phone/tablet. The build environment has no emulator or device (see
  [STATUS.md](STATUS.md)); these are confirmed on-device before publishing.

Build under test:

- versionName `1.0.0`, versionCode `10`
- APKs: `PDFSeal-1.0.0-arm64-v8a.apk`, `PDFSeal-1.0.0-universal.apk`
- Embedded source commit = the `v1.0.0` git tag commit (see
  `META-INF/version-control-info.textproto`).

## Install tests

| # | Test | Status |
|---|------|--------|
| 1 | Install arm64 APK on a modern Android phone/tablet | DEVICE |
| 2 | Install universal APK on a compatible device/emulator | DEVICE |
| 3 | Update v0.5.1 → v1.0.0 with the same signing key (no uninstall) | DEVICE |
| 4 | App data (recent files, limits ack) preserved on update | DEVICE |

versionCode increased 9 → 10 and the release signing config is unchanged
(same keystore), so an in-place update is accepted by Android — confirm
on-device.

## Privacy tests

| # | Test | Status | Evidence |
|---|------|--------|----------|
| 5 | Manifest has no `INTERNET` permission | PASS (static) | `AndroidManifest.xml` declares **zero** `<uses-permission>` entries |
| 6 | No `ACCESS_NETWORK_STATE`/`CAMERA`/`RECORD_AUDIO`/location/contacts/`BILLING`/`MANAGE_EXTERNAL_STORAGE` | PASS (static) | same — no permissions declared at all |
| 7 | `android:allowBackup="false"` | PASS (static) | set in `<application>` |
| 8 | No analytics/tracking SDKs | PASS (static) | deps: AndroidX/Compose, coroutines, datastore, documentfile, MuPDF `fitz`, Tesseract4Android — no analytics/crash/ads SDK |
| 9 | No Google Play Services dependency | PASS (static) | no `com.google.android.gms` / `play-services` / `firebase` in `app/build.gradle.kts` |
| 10 | Works offline / airplane mode | PASS (static) | no `INTERNET` permission ⇒ the OS makes network access impossible; also DEVICE smoke-check in airplane mode |

## Open / view tests (DEVICE)

| # | Test | Status |
|---|------|--------|
| 11 | Normal text PDF | DEVICE |
| 12 | Scanned image-only PDF | DEVICE |
| 13 | Image-heavy PDF | DEVICE |
| 14 | 100+ page PDF (memory pressure) | DEVICE |
| 15 | Rotated-page PDF | DEVICE |
| 16 | Password-protected PDF → clear error, no crash | DEVICE (msg verified static: "This PDF is password-protected or encrypted. PDFSeal cannot open encrypted PDFs yet.") |
| 17 | Corrupted/invalid PDF → clear error, no crash | DEVICE (msg verified static: "PDFSeal could not open this PDF. The file may be damaged, encrypted, or unsupported.") |
| 18 | PDF opened from Downloads | DEVICE |
| 19 | PDF opened from Android SAF picker | DEVICE |

## Editing tests (DEVICE — export round trips)

| # | Test | Status |
|---|------|--------|
| 20 | Add Text → export → reopen elsewhere | DEVICE |
| 21 | Typed Visual Signature → export → reopen elsewhere | DEVICE |
| 22 | Cover & Replace → export → reopen elsewhere | DEVICE |
| 23 | OCR page → review text → export | DEVICE |
| 24 | Make Editable Copy → edit OCR box → export | DEVICE |
| 25 | Rotate page → export | DEVICE |
| 26 | Delete page → export | DEVICE |
| 27 | Reorder page → export | DEVICE |
| 28 | Combined text + signature + cover + OCR → export | DEVICE |

Page tools operate on an export plan applied only at export time; the source
session is never mutated (`PdfDocumentSession` / `PdfExporter`). Confirmed by
code review; the round trips above confirm on-device.

## Safety tests

| # | Test | Status | Evidence |
|---|------|--------|----------|
| 29 | Original PDF checksum unchanged before/after export | PASS (static) + DEVICE | export only ever writes the SAF `CreateDocument` target URI via `FileAccessManager.openOutput`; the source URI is opened read-only and copied to a private cache temp — no write path to the source exists in code |
| 30 | Original file not silently overwritten | PASS (static) | export destination is always user-chosen via SAF `CreateDocument`; default name `original-name-PDFSeal-copy.pdf`, never the source |
| 31 | Export warning appears before every export | PASS (static) | the Export button only sets `showExportWarning`; the SAF launcher fires solely from the dialog's "Export Flattened Copy" button |
| 32 | Cover & Replace warning appears | PASS (static) | enabling cover mode routes through the `COVER_WARNING` dialog |
| 33 | Visual Signature warning appears | PASS (static) | `SignatureDialog` shows `SIGNATURE_WARNING` |
| 34 | OCR warning appears + post-OCR review warning | PASS (static) | `OcrPanel` shows `OCR_WARNING`; result view shows `OCR_REVIEW_WARNING` in error color |
| 35 | Password-protected PDF → clear error, no crash | DEVICE |
| 36 | Export failure → clear error, original undamaged | PASS (static) + DEVICE | failures caught in `PdfViewerState.export`; message "Export failed. Your original PDF was not changed."; original never in the write path |

## Reopen tests (DEVICE)

| # | Test | Status |
|---|------|--------|
| 37 | Reopen exported PDF in PDFSeal | DEVICE |
| 38 | Reopen exported PDF in Android system viewer | DEVICE |
| 39 | Reopen exported PDF in Google Drive viewer (if installed) | DEVICE |
| 40 | Reopen exported PDF on desktop | DEVICE |

## First-launch limits screen

| # | Test | Status | Evidence |
|---|------|--------|----------|
| 41 | Limits screen shows once on first launch | PASS (static) | `MainActivity` gates the app on `AppPrefs.limitsAcknowledged` |
| 42 | "I understand" stores ack locally; not shown again | PASS (static) | `AppPrefs.setLimitsAcknowledged()` (DataStore, on-device only) |
| 43 | Same info available from About / Privacy / Licenses | PASS (static) | `AboutScreen` renders the same `HonestCopy.FIRST_RUN_LIMITS` text |

## Provenance / source compliance

| # | Test | Status |
|---|------|--------|
| 44 | `v1.0.0` git tag exists and is pushed | see release step |
| 45 | APK `META-INF/version-control-info.textproto` revision == `v1.0.0` tag commit | verified at build (recorded in the GitHub release notes) |
| 46 | THIRD_PARTY_LICENSES.md in repo + bundled in APK assets | PASS (static) |
| 47 | Full AGPL-3.0 / Apache-2.0 / BSD-2-Clause / OFL texts bundled | PASS (static) |
| 48 | No private signing key / password committed | PASS (static) — `.gitignore` excludes `*.jks`, `key.properties`, `/release/` |

## Summary

All statically verifiable items: **PASS**. Items marked **DEVICE** are the
on-device round-trip and install checks; the maintainer runs them on a real
Android tablet/phone before the GitHub release is marked final. None of the
DEVICE items can be exercised in the build environment (no emulator/device).
