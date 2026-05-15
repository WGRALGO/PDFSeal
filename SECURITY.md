# Security Policy

## Reporting a vulnerability

PDFSeal is local/offline software with no server component, no account system,
and no network calls for its core function. The most relevant security concerns
are therefore:

- Malformed PDF / OCR input causing crashes or memory corruption in the native
  engine layer (MuPDF / Tesseract / Leptonica).
- Incorrect handling of Storage Access Framework URIs leading to unintended file
  reads or writes.
- The **Cover & Replace** feature being mistaken for secure redaction (see below).

If you discover a vulnerability, please report it privately:

1. **Preferred:** open a private advisory via GitHub Security Advisories on
   <https://github.com/WGRALGO/PDFSeal/security/advisories/new>.
2. Alternatively, open a minimal issue at
   <https://github.com/WGRALGO/PDFSeal/issues> that states a security problem
   exists **without** disclosing exploit details, and request a private channel.

Please do **not** open a public issue containing a working exploit before a fix
is available.

### What to include

- PDFSeal version (`versionName` / `versionCode` from the About screen).
- Device model and Android version.
- A minimal sample file or steps to reproduce (only if safe to share).
- Expected vs. actual behaviour.

### Response expectations

This is a small open-source project maintained on a best-effort basis. There is
no paid bug bounty. Triage and fixes are prioritised by severity and exploitability.

## Important: Cover & Replace is NOT secure redaction

The **Cover & Replace** feature draws an opaque box over content and lets the
user place new text on top. Underlying content may persist in the exported file
unless the page is rasterised. **Do not use Cover & Replace to remove confidential
or regulated data.** True secure redaction (actual content removal) is tracked as
a future item in [docs/ROADMAP.md](docs/ROADMAP.md) and is intentionally not
presented as available until it genuinely is.

## Supply chain / build integrity

- The private release signing key is **never** committed to this repository.
- Release APKs published on GitHub Releases include a SHA-256 checksum and the
  signing certificate SHA-256 fingerprint so users can verify authenticity.
- The project is buildable entirely from source; no closed binary blobs carry
  app logic. Native libraries (MuPDF, Tesseract) come from their published,
  documented upstream artifacts (see [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)).
