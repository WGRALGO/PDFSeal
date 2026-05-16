package org.thewealthgapresolutionalgorithm.pdfseal.ui

/**
 * Single source of truth for the user-facing honesty/limits copy.
 *
 * The same text is shown on the first-launch limits screen, in the export
 * confirmation, in the tool warnings, and in About / Privacy / Licenses, so
 * the wording cannot drift between them and cannot overstate what PDFSeal
 * does. Do not soften or remove these statements.
 */
object HonestCopy {

    /** One-line honest description used in the UI and the README. */
    const val ONE_LINER =
        "PDFSeal is an offline PDF markup and flattened-export app."

    /** First-launch limits screen body (shown once, also kept in About). */
    const val FIRST_RUN_LIMITS =
        "PDFSeal works offline and does not upload your PDFs.\n\n" +
            "Important limits:\n" +
            "- PDFSeal exports flattened visual PDF copies.\n" +
            "- Cover & Replace is visual only. It is not secure redaction.\n" +
            "- Visual Signature is not a certified cryptographic digital " +
            "signature.\n" +
            "- OCR-assisted editable copies may contain recognition errors.\n" +
            "- Forms, links, bookmarks, layers, annotations, selectable text, " +
            "accessibility structure, metadata, and existing digital " +
            "signatures may not be preserved.\n" +
            "- Your original PDF is not modified."

    /** Export confirmation body (shown before every export). */
    const val EXPORT_CONFIRM =
        "PDFSeal will create a flattened visual PDF copy.\n\n" +
            "Some original PDF features may not be preserved, including " +
            "forms, links, bookmarks, layers, annotations, selectable text, " +
            "accessibility structure, metadata, and existing digital " +
            "signatures.\n\n" +
            "Your original file will not be modified."

    /** Appended to the export confirmation when Cover & Replace was used. */
    const val EXPORT_COVER_NOTICE =
        "This file uses Cover & Replace. Covered content may still exist in " +
            "the original PDF structure or source document. This is not " +
            "secure redaction."

    /** Cover & Replace tool warning. */
    const val COVER_WARNING =
        "Cover & Replace is visual only. It places a visible cover over " +
            "content but does not securely remove hidden PDF text, images, " +
            "or objects. Do not use it for legal, medical, financial, or " +
            "private redaction."

    /** Visual Signature tool warning. */
    const val SIGNATURE_WARNING =
        "Typed Visual Signature adds a typed signature appearance. It is " +
            "not a certificate-based digital signature, identity " +
            "verification, or tamper-proof signature."

    /** OCR / Make Editable Copy warning. */
    const val OCR_WARNING =
        "Make Editable Copy uses OCR to reconstruct text into editable " +
            "overlays. OCR can make mistakes. Review OCR text carefully " +
            "before exporting."

    /** Shown after OCR completes. */
    const val OCR_REVIEW_WARNING = "Review OCR text before relying on it."
}
