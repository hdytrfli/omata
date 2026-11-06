# ML Kit + R8 full mode (the AGP 9 default)
#
# ML Kit discovers its components by class name: MlKitComponentDiscoveryService
# in the merged manifest lists registrars as strings, and ComponentRegistrar
# implementations register the internal services (including the text recognizer)
# at startup. R8 full mode cannot see those reflective references and strips the
# registrars, so TextRecognition.getClient() blows up with:
#
#   NullPointerException: Attempt to read from field '...' on a null object reference
#     at com.google.mlkit.vision.text.TextRecognition.getClient
#
# The ML Kit AARs do not ship the consumer rules that guard against this
# (firebase-components 16.1.0 predates them), so keep the reflective surface
# explicitly. See googlesamples/mlkit#1018.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep,allowshrinking interface com.google.firebase.components.ComponentRegistrar

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }

# Keep line numbers so crash reports stay useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
