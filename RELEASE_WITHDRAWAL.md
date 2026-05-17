# PDFSeal 1.0.0 Withdrawal — RESOLVED in v1.0.3

**Status: resolved.** The v1.0.0 withdrawal is lifted as of **v1.0.3**
(`versionCode` 15). This file is kept for the record.

Root cause of the v1.0.0 defect: a Compose effect-keying bug — `MainActivity`
cleared the pending view URI *before* awaiting the open, which changed the
`LaunchedEffect` key and cancelled the open mid-flight, leaving the viewer on a
permanent "Loading…". (Earlier I/O-timeout work was a misdiagnosis and was
reverted.) Fixed in v1.0.3 by collecting the URI via `snapshotFlow` inside a
`LaunchedEffect(Unit)` so clearing the flag cannot cancel the open, plus a real
error + Back state instead of an endless "Loading…".

v1.0.3 is device-verified on an Amazon Fire HD 10 for the open → edit → export
→ reopen round trip, with the original file confirmed byte-for-byte unchanged
(SHA-256) on every export. The "Before Any Future Release" checklist below is
mostly satisfied; items **not** exhaustively device-tested for v1.0.3 are
called out honestly in [README.md](README.md) and
[docs/STATUS.md](docs/STATUS.md) (scanned/corrupt/password PDFs,
install/upgrade matrix, very large PDFs, and the OS file-picker list-tap which
the test automation could not drive on this hardware).

The original v1.0.0 withdrawal notice follows unchanged.

---

# PDFSeal 1.0.0 Withdrawal Notice

PDFSeal 1.0.0 was withdrawn after real-device testing showed the APK was not acceptable for public release.

The source code remains available because PDFSeal is an open-source project under active review and repair.

## Project Purpose

PDFSeal exists because basic PDF editing should not be locked behind expensive subscriptions, cloud accounts, ads, trackers, or bloated software.

The goal of this project is to build a free, open-source, offline PDF markup tool for serious people with limited resources, and for serious people who do not support corporate greed disguised as productivity software.

PDFSeal is for students, workers, small nonprofits, independent creators, job seekers, organizers, small business owners, and anyone else who needs practical PDF tools without being pushed into another monthly payment.

This project is not trying to clone Adobe Acrobat or pretend to do everything commercial PDF software does. The goal is simpler and more honest: create a useful, privacy-respecting, on-device PDF editor that real people can use, inspect, improve, and share.

PDFSeal will remain open source so the code can be reviewed, repaired, improved, and trusted by the community.

## Before Any Future Release

Before any future release, PDFSeal must pass real-device testing for:

- Opening PDFs from Android file picker
- Opening PDFs through Android “Open with”
- Rendering pages correctly
- Adding text overlays
- Adding typed visual signatures
- Cover & Replace behavior
- OCR-assisted editable text overlay behavior
- Exporting flattened edited copies
- Confirming the original PDF is not modified
- Handling scanned PDFs
- Handling large PDFs
- Handling corrupt PDFs
- Handling password-protected PDFs
- Confirming no Internet permission
- Confirming no ads, analytics, trackers, cloud upload, account system, billing, or Google Play Services dependency
- Verifying APK signature with apksigner
- Verifying checksums
- Testing install/uninstall/reinstall behavior

Status: **RESOLVED in v1.0.3** — withdrawal lifted; verified-core release
(see README.md / docs/STATUS.md for exact verified scope and residual
untested areas).
