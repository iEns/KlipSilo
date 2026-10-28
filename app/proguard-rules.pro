# KlipSilo R8 keep rules.
#
# Release and beta are minified (R8 full mode). Most dependencies ship their
# own consumer rules; the entries below cover the reflection/JNI entry points
# that R8 cannot see on its own. Keep new rules narrow and tied to a real
# dependency - do not add speculative keeps for libraries the app does not use.

# Keep line numbers and source file names so Play crash reports line up with
# the mapping.txt uploaded for the release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room -------------------------------------------------------------------
# Room generates <Database>_Impl and resolves it by name. Entities and
# projections are referenced from the generated code; keeping the app's db
# package also keeps migration/entity constructors stable across R8 versions.
-keep class com.phaprodevelop.klipsilo.data.db.** { *; }

# --- PDFBox-Android (com.tom-roush:pdfbox-android) --------------------------
# PDFBox instantiates security handlers reflectively from the PDF's /Filter
# entries and document-interchange (markup/annotation) handlers from COS
# dictionaries. These rules mirror the AAR consumer rules explicitly so a
# future AAR update cannot silently drop them.
-keep,allowobfuscation class * extends com.tom_roush.pdfbox.pdmodel.encryption.SecurityHandler {
    public <init>(...);
}
-keep,allowobfuscation class com.tom_roush.pdfbox.pdmodel.documentinterchange.** { *; }

# PDFBox loads CMaps, standard-14 font metrics and ICC resources from assets
# through PDFBoxResourceLoader at runtime.
-keep class com.tom_roush.pdfbox.android.PDFBoxResourceLoader { *; }

# JPEG 2000 decoding is optional in PDFBox-Android: JPXFilter probes for
# com.gemalto.jp2.JP2Decoder at runtime and reports a missing-reader error when
# the JP2Android library is absent. KlipSilo does not bundle it, so suppress
# the missing-class warning; do not add the library just for R8.
-dontwarn com.gemalto.jp2.**

# --- ML Kit (text-recognition + barcode-scanning) ---------------------------
# ML Kit resolves recognizer/detector implementations and model descriptors
# reflectively and crosses into JNI. With AGP 9 / R8 full mode the AAR
# consumer rules are not enough: R8 can remove internal constructors that are
# only reached through reflection (googlesamples/mlkit#1018). keep +
# allowoptimization preserves the entry points while still letting R8 rewrite
# their bodies.
-keep,allowoptimization class com.google.mlkit.common.** { *; }
-keep,allowoptimization class com.google.mlkit.vision.common.** { *; }
-keep,allowoptimization class com.google.mlkit.vision.text.** { *; }
-keep,allowoptimization class com.google.mlkit.vision.barcode.** { *; }
-keep,allowoptimization class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep,allowoptimization class com.google.android.gms.internal.mlkit_vision_text.** { *; }
-keep,allowoptimization class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }
-keep,allowoptimization class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep,allowoptimization class com.google.android.gms.internal.mlkit_vision_barcode_bundled.** { *; }

# JNI method names are part of the native ABI.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# --- CameraX ----------------------------------------------------------------
# CameraX ships consumer rules (Quirk subclasses, Camera2Config provider).
# CameraX also loads camera extensions/quirks by class name on some devices.
-keep,allowoptimization class androidx.camera.core.** extends androidx.camera.core.impl.Quirk { *; }
-keep,allowoptimization class androidx.camera.camera2.** extends androidx.camera.core.impl.Quirk { *; }

# CameraX keeps its provider in the manifest and instantiates it by name.
-keep class androidx.camera.camera2.Camera2Config$DefaultProvider { *; }
