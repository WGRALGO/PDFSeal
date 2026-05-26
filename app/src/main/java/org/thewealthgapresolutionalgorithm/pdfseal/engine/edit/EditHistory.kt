package org.thewealthgapresolutionalgorithm.pdfseal.engine.edit

/**
 * Bounded undo/redo history for the session's per-page edit objects.
 *
 * Snapshot model: each entry captures the full `pageIndex -> edits` map as a
 * deep-cloned `Map<Int, List<PdfEditObject>>`. That keeps the implementation
 * trivial — every reversible operation just calls [snapshot] BEFORE mutating
 * and the undo button restores the previous snapshot. Cost is fine because
 * edit lists are tiny (handful of objects per page) and [maxDepth] is 20.
 *
 * Gestures (move/resize/scale) snapshot at gesture START only, so a single
 * drag is one undo step rather than thousands of intermediate frames.
 */
class EditHistory(private val maxDepth: Int = 20) {

    /** Deep clones an entire edits map. Edit subclasses are data classes whose
     * fields are primitives or other data classes, so a shallow `copy()` is
     * effectively deep — except [CoverReplaceObject], which holds a
     * MutableList<TextEditObject> we must clone explicitly. */
    private fun cloneEdits(
        src: Map<Int, List<PdfEditObject>>,
    ): MutableMap<Int, MutableList<PdfEditObject>> {
        val out = HashMap<Int, MutableList<PdfEditObject>>(src.size)
        for ((page, list) in src) {
            out[page] = list.mapTo(mutableListOf()) { obj -> cloneObj(obj) }
        }
        return out
    }

    private fun cloneObj(o: PdfEditObject): PdfEditObject = when (o) {
        is TextEditObject -> o.copy()
        is SignatureEditObject -> o.copy()
        is HighlightObject -> o.copy()
        is StrikethroughObject -> o.copy()
        is CoverReplaceObject -> o.copy(
            overlayText = o.overlayText.mapTo(mutableListOf()) { it.copy() },
        )
    }

    private val undoStack: ArrayDeque<Map<Int, List<PdfEditObject>>> =
        ArrayDeque()
    private val redoStack: ArrayDeque<Map<Int, List<PdfEditObject>>> =
        ArrayDeque()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Capture the current edits BEFORE a mutation. Drops redo history (we're
     *  branching off the timeline) and trims the oldest entry past [maxDepth]. */
    fun snapshot(current: Map<Int, List<PdfEditObject>>) {
        undoStack.addLast(cloneEdits(current))
        if (undoStack.size > maxDepth) undoStack.removeFirst()
        redoStack.clear()
    }

    /** Pop the latest undo and return the prior state; also pushes the supplied
     *  [current] onto the redo stack so the next redo restores it. */
    fun undo(
        current: Map<Int, List<PdfEditObject>>,
    ): MutableMap<Int, MutableList<PdfEditObject>>? {
        val prev = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(cloneEdits(current))
        if (redoStack.size > maxDepth) redoStack.removeFirst()
        return cloneEdits(prev)
    }

    fun redo(
        current: Map<Int, List<PdfEditObject>>,
    ): MutableMap<Int, MutableList<PdfEditObject>>? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(cloneEdits(current))
        if (undoStack.size > maxDepth) undoStack.removeFirst()
        return cloneEdits(next)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
