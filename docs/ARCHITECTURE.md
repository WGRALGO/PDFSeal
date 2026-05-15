# PDFSeal Architecture

## Core principle

**The UI never manipulates PDF files directly.** All PDF work goes through a
dedicated internal engine layer. The UI calls `PdfEngine`; `PdfEngine` owns
MuPDF, OCR, export, and file access. No `com.artifex.mupdf` import is allowed
inside the `ui/` package. This keeps PDF logic in one place, testable and
replaceable, instead of scattered across screens.

```
┌───────────────────────────────────────────────┐
│  ui/   (Jetpack Compose screens + viewer)       │
│  - HomeScreen, ViewerScreen, AboutScreen, ...   │
│  - knows about edit-object MODELS, not MuPDF    │
└───────────────────────┬─────────────────────────┘
                         │ calls only
                         ▼
┌───────────────────────────────────────────────┐
│  engine/  PdfEngine  (single facade / API)      │
│                                                 │
│  PdfDocumentSession   PdfPageRenderer           │
│  PdfCoordinateMapper  EditableCopyBuilder       │
│  edit/  PdfEditObject TextEditObject            │
│         SignatureEditObject CoverReplaceObject  │
│  ocr/   OcrService    OcrPageResult             │
│  export/ PdfExporter                            │
│  io/    FileAccessManager RecentFilesManager    │
└───────────────────────┬─────────────────────────┘
                         │ wraps
                         ▼
        MuPDF (AGPL)        Tesseract4Android (Apache-2.0)
        Android SAF         DataStore
```

## Package layout

```
org.thewealthgapresolutionalgorithm.pdfseal
├── ui/
│   ├── MainActivity.kt
│   ├── screens/      HomeScreen, ViewerScreen, AboutScreen, ...
│   ├── viewer/       PdfViewerState, EditObjectsLayer, TextToolDialog
│   ├── signature/    SignatureDialog            (Phase 5)
│   ├── cover/        CoverTool                  (Phase 6)
│   ├── ocr/          OcrPanel                   (Phase 7)
│   └── pages/        PageThumbnailsScreen       (Phase 9)
└── engine/
    ├── PdfEngine.kt
    ├── PdfDocumentSession.kt
    ├── PdfPageRenderer.kt
    ├── PdfCoordinateMapper.kt
    ├── EditableCopyBuilder.kt
    ├── edit/    PdfEditObject, TextEditObject,
    │            SignatureEditObject, CoverReplaceObject
    ├── ocr/     OcrService, OcrPageResult
    ├── export/  PdfExporter
    ├── io/      FileAccessManager, RecentFilesManager
    └── pages/   PageOps, PdfMerger, PdfSplitter  (Phase 9)
```

## Component responsibilities

### `PdfEngine`
Central API used by the UI. Opens documents, manages sessions, routes commands
to renderer / exporter / OCR / edit system. Coroutine-friendly (`suspend`),
confines MuPDF calls to a single dispatcher (MuPDF objects are not thread-safe).

### `PdfDocumentSession`
The currently open PDF: source URI, page count, page sizes (PDF points), the
list of unsaved edit objects, and a per-page OCR result cache.

### `PdfPageRenderer`
Renders pages to bitmaps at three DPI tiers: viewer, thumbnail, and high-res for
OCR. Uses MuPDF's draw device.

### `PdfCoordinateMapper`
Pure functions converting between screen, viewport (zoom/pan), rendered-bitmap,
and PDF-point coordinate systems, honouring page rotation. This is the most
correctness-critical module — text, signatures, OCR boxes, and covers must land
exactly. It has no Android/MuPDF dependency and is fully unit-tested on the JVM.

### `edit/` models
- `PdfEditObject` — base: `pageIndex`, `pdfRect`, `rotationDeg`, `zOrder`,
  transient `selected`.
- `TextEditObject` — text, font family, size (pt), colour, alignment.
- `SignatureEditObject` — typed name + style id (cursive / bold / formal).
- `CoverReplaceObject` — fill rect (+ optional nested text overlays).
  **Visual cover only — never named or treated as redaction.**

### `ocr/OcrService` + `OcrPageResult`
Offline Tesseract. Lazy-copies `eng.traineddata` from assets to `filesDir`.
Returns blocks/lines/words + boxes + confidence + language, plus the rendered
bitmap size and PDF page size so boxes can be mapped back later.

### `EditableCopyBuilder`
Turns an `OcrPageResult` into `TextEditObject`s positioned in PDF coordinates,
producing the "Make Editable Copy" overlay set.

### `export/PdfExporter`
Writes an edited **copy**. Flattens text / signature / cover objects and OCR
edits. **Never overwrites the source URI** unless the user explicitly chooses
overwrite and the SAF permission allows it.

### `io/FileAccessManager` + `RecentFilesManager`
SAF wrappers (open/create document, persistable URI permissions) and a
DataStore-backed recent-files list that drops entries whose permission was revoked.

## Threading model

- MuPDF objects live on a single confined dispatcher inside `PdfEngine`.
- Rendering/OCR/export are `suspend` functions; the UI collects results via
  Compose state holders (`PdfViewerState`).

## Why MuPDF + Tesseract (and not alternatives)

- MuPDF is a real PDF engine (render + edit + page-tree ops), available as a
  prebuilt AGPL AAR — no NDK build needed, license aligns with this AGPL project.
- Tesseract is offline and open-source. No cloud OCR, no ML Kit, no network.

If MuPDF/Tesseract integration hits a blocker, the blocker is documented openly
(here and in ROADMAP) and the strongest working path is taken **without**
silently downgrading to a weaker PDF approach.
