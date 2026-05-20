# PDFSeal Withdrawal Notice

**Status: withdrawn. Not ready for public use.**

PDFSeal has been withdrawn after real-device testing. No public APK is
distributed (the v1.0.3 and v1.0.4 GitHub releases exist as records but carry
no installable APK).

Real-device use showed the markup editor is not acceptable for serious work:

- **OCR does nothing useful** — the recognised text is not actually usable.
- **Placed text cannot be deleted.**
- ~~**The typed signature cannot be positioned or resized.**~~ **Fixed
  2026-05-20** — the typed signature now auto-fits to the chosen font,
  shows four corner handles, and moves/resizes correctly on a real tablet
  (debug build). The session model is updated alongside the on-screen
  overlay so the signature survives export. No new APK has been published;
  the fix lives in the source tree only.
- Overall, the editor still cannot do real work end-to-end.

A v1.0.3 fix did resolve a separate defect that had stranded the viewer on a
permanent "Loading…" (a Compose `LaunchedEffect` self-cancel), and an
automated open → edit → export → reopen check passed. **That automated check
was insufficient:** it only confirmed that an export file is produced and the
original file is left byte-for-byte unchanged — it did not, and could not,
prove the editing tools are usable. Hands-on use shows they are not. The
automated "verified" claim was an overreach and is retracted.

The source code remains available because PDFSeal is an open-source project
under active review and repair. Build from source only.

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

Status: **withdrawn. Not ready for public use.** The editor must do real
work (usable OCR result, delete placed text, and the rest of the list
above) before any future release. Signature place/resize is no longer a
blocker as of 2026-05-20 — see the "Fixed 2026-05-20" note above.
