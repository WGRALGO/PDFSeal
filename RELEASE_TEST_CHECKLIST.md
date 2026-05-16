# PDFSeal — Release / Stress Test Checklist

Manual checklist to run before publishing any PDFSeal release APK. Use a real
Android tablet for the primary pass. Mark each item PASS / FAIL / N/A and note
the device + Android version.

Device: ______________________  Android: ______  APK: PDFSeal-________.apk

## Open / load — input variety

- [ ] Small text PDF — opens, pages render, navigation works
- [ ] Large scanned PDF (100+ MB / many pages) — opens without OOM crash; scrolling stays responsive (validates streamed temp-file open, not full RAM load)
- [ ] Image-heavy PDF — renders, zoom/pan smooth
- [ ] Password-protected PDF — shows the "password-protected" message, does NOT crash
- [ ] Corrupt / truncated PDF — shows the "corrupt or unsupported" message, does NOT crash
- [ ] Unsupported file renamed to .pdf — graceful error message
- [ ] Revoke URI permission then reopen from Recent — shows "access lost" message, no crash
- [ ] PDF with AcroForm fields — opens (note: forms are NOT preserved on export)
- [ ] PDF with hyperlinks — opens (links NOT preserved on export)
- [ ] PDF with bookmarks / outline — opens (bookmarks NOT preserved on export)
- [ ] PDF with existing annotations — opens (annotations NOT preserved on export)
- [ ] PDF with rotated pages (90/180/270) — renders upright correctly
- [ ] PDF with an existing digital signature — opens; user understands export will not preserve it

## Editing features

- [ ] OCR current page — recognises text, no network access
- [ ] Add Text — place, move, resize, edit, delete
- [ ] Cover / Replace area — visual cover + replacement text
- [ ] Visual Signature — typed name, all 3 styles, place/move/rotate
- [ ] Confirm "Visual Signature" wording everywhere (no "certified/digital/secure signing" claims)
- [ ] Rotate page (export plan)
- [ ] Reorder / delete pages (export plan)
- [ ] Make Editable Copy — OCR reconstruction overlays appear

## Export

- [ ] Export button shows the flattened-visual-copy warning dialog FIRST
- [ ] Cancel on warning aborts export
- [ ] Export anyway → writes a new PDF; original file unchanged
- [ ] Reopen exported PDF in PDFSeal — renders, edits are baked in
- [ ] Reopen exported PDF in a different viewer (e.g. system viewer) — opens fine
- [ ] Confirm exported text is raster (expected: not selectable — this is the documented flatten behaviour)

## Privacy / packaging

- [ ] `aapt dump permissions` (or App Info) shows NO `android.permission.INTERNET`
- [ ] Airplane mode: full open → edit → OCR → export cycle works offline
- [ ] About / Privacy / Licenses screen opens from Home
- [ ] About screen shows app version + build number
- [ ] About screen shows the source-code URL line
- [ ] About screen displays the bundled THIRD_PARTY_LICENSES.md notices (not blank / not "could not be read")
- [ ] `unzip -l app.apk | grep THIRD_PARTY_LICENSES` shows it under `assets/`
- [ ] Android backup disabled: manifest `allowBackup=false`; `adb shell bmgr` shows nothing useful to back up
- [ ] No temp files left in app cache after closing a document (cache/pdfseal_open empties)

## Install targets

- [ ] Install + smoke test on a real Android tablet (primary)
- [ ] Install + smoke test on an x86_64 emulator (if x86 builds are kept / universal APK)
- [ ] If `-PabiSplit` used: arm64-v8a APK installs on real ARM tablet; universal APK still produced

## Sign-off

Tested by: ______________________  Date: ____________  Result: PASS / FAIL
