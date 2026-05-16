package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.annotation.FontRes
import org.thewealthgapresolutionalgorithm.pdfseal.R
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject.SignatureStyle

/** Single source of truth mapping a signature style to its bundled OFL font. */
object SignatureFonts {
    @FontRes
    fun fontRes(style: SignatureStyle): Int = when (style) {
        SignatureStyle.ELEGANT_CURSIVE -> R.font.great_vibes
        SignatureStyle.BOLD_HANDWRITTEN -> R.font.pacifico
        SignatureStyle.CLEAN_FORMAL_SCRIPT -> R.font.pinyon_script
    }

    fun label(style: SignatureStyle): String = when (style) {
        SignatureStyle.ELEGANT_CURSIVE -> "Elegant cursive"
        SignatureStyle.BOLD_HANDWRITTEN -> "Bold handwritten"
        SignatureStyle.CLEAN_FORMAL_SCRIPT -> "Clean formal script"
    }
}
