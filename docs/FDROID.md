# F-Droid

PDFSeal ships F-Droid-compatible metadata so it is ready for inclusion. This
document explains what is in place and the one honest blocker for the **official**
F-Droid repository.

## What is in place

Fastlane-structured store metadata (read by F-Droid):

```
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt        (<= 80 chars)
├── full_description.txt
├── images/icon.png              (512 px, from the PDFSeal logo)
└── changelogs/
    ├── 6.txt                    (versionCode 6 — v0.4.0)
    └── 7.txt                    (versionCode 7)
```

Privacy / anti-features: **none**. PDFSeal has no ads, no analytics, no
trackers, no account, no network use, and no Google Play Services / GMS. License
is AGPL-3.0-or-later. It builds from source with the Gradle wrapper (see
[BUILDING.md](BUILDING.md)).

## fdroiddata build recipe (template)

For the official repo, add a metadata file
`metadata/org.thewealthgapresolutionalgorithm.pdfseal.yml` to the `fdroiddata`
repository:

```yaml
Categories:
  - Writing
  - Office
License: AGPL-3.0-or-later
SourceCode: https://github.com/WGRALGO/WGRALGO-PDFSeal
IssueTracker: https://github.com/WGRALGO/WGRALGO-PDFSeal/issues
RepoType: git
Repo: https://github.com/WGRALGO/WGRALGO-PDFSeal.git

Builds:
  - versionName: "0.4.0"
    versionCode: 6
    commit: v0.4.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[0-9.]+$
CurrentVersion: "0.4.0"
CurrentVersionCode: 6
```

## Honest blocker for the *official* F-Droid repo

F-Droid's main repository requires that all native code be built from source by
their buildserver. PDFSeal currently consumes **prebuilt** native artifacts:

- MuPDF (`com.artifex.mupdf:fitz`) from `maven.ghostscript.com`
- Tesseract4Android from JitPack (prebuilt `.so` for tesseract/leptonica)

These are free software (AGPL-3.0 / Apache-2.0), so there is **no licensing
problem** — but F-Droid would flag them as not built from source. Options,
documented rather than hidden:

1. **F-Droid main repo:** add NDK build steps so the buildserver compiles MuPDF
   and Tesseract/Leptonica from source (significant work; tracked as future).
2. **A self-hosted / third-party F-Droid repo** that permits prebuilt free
   dependencies — works today with the metadata above.
3. Ship via GitHub Releases (already done: signed APK + SHA-256 + signing-cert
   fingerprint), which needs none of the above.

This is a real constraint, not a defect to paper over. Until option 1 is done,
treat PDFSeal as F-Droid-*ready metadata-wise* but not yet accepted into the
official binary repo.
