package org.thewealthgapresolutionalgorithm.pdfseal.engine

import java.util.UUID

/**
 * One entry in the document outline (bookmark). Read from the PDF's outline and
 * shown in the Bookmarks dialog; tapping one jumps the viewer to [pageIndex].
 *
 * [depth] is the nesting level (0 = top level) so the dialog can indent nested
 * bookmarks. [pageIndex] is 0-based, or -1 when the outline entry's target
 * could not be resolved to a page (e.g. an external/web link).
 *
 * [id] is a stable per-session key for Compose lists and for delete; it has no
 * meaning in the PDF itself.
 */
data class Bookmark(
    val title: String,
    val pageIndex: Int,
    val depth: Int = 0,
    val id: String = UUID.randomUUID().toString(),
)
