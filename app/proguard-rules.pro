# Keep Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
