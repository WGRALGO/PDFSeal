package org.thewealthgapresolutionalgorithm.pdfseal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Distinct, readable type scale: strong serif-ish weight for headers via
// bumped weights, comfortable body. Uses platform default family (no extra
// font assets shipped) but with a deliberate, non-default rhythm.
private val base = Typography()

val PdfSealTypography = Typography(
    titleLarge = base.titleLarge.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
    ),
    bodyLarge = base.bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
