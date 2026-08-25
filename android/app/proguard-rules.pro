# Add project specific ProGuard rules here.
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }

# Apache Commons Compress (used internally by youtubedl-android to extract the bundled native
# binaries from their .zip.so archives) instantiates its ZipExtraField implementations via
# reflection. Without an explicit keep, R8 breaks that reflection and YoutubeDL.init() fails
# with ExceptionInInitializerError: "class ... is not a concrete class" - every analyze/download
# call then fails with the misleading "instance not initialized".
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
