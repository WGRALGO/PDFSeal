# PDFSeal ProGuard / R8 rules

# MuPDF JNI — keep native-bound classes and methods.
-keep class com.artifex.mupdf.fitz.** { *; }
-keepclasseswithmembernames class com.artifex.mupdf.fitz.** {
    native <methods>;
}

# Tesseract4Android JNI.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin / Compose defaults are handled by the AGP-provided rules.
